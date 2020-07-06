package arekkuusu.enderskills.common.skill.ability.offence.blood;

import arekkuusu.enderskills.api.capability.AdvancementCapability;
import arekkuusu.enderskills.api.capability.Capabilities;
import arekkuusu.enderskills.api.capability.data.IInfoCooldown;
import arekkuusu.enderskills.api.capability.data.IInfoUpgradeable;
import arekkuusu.enderskills.api.capability.data.SkillData;
import arekkuusu.enderskills.api.capability.data.SkillInfo;
import arekkuusu.enderskills.api.capability.data.nbt.UUIDWatcher;
import arekkuusu.enderskills.api.event.SkillDamageSource;
import arekkuusu.enderskills.api.helper.ExpressionHelper;
import arekkuusu.enderskills.api.helper.NBTHelper;
import arekkuusu.enderskills.api.registry.Skill;
import arekkuusu.enderskills.client.gui.data.ISkillAdvancement;
import arekkuusu.enderskills.client.util.ResourceLibrary;
import arekkuusu.enderskills.client.util.helper.TextHelper;
import arekkuusu.enderskills.common.CommonConfig;
import arekkuusu.enderskills.common.ES;
import arekkuusu.enderskills.common.lib.LibMod;
import arekkuusu.enderskills.common.lib.LibNames;
import arekkuusu.enderskills.common.skill.ModAbilities;
import arekkuusu.enderskills.common.skill.ModAttributes;
import arekkuusu.enderskills.common.skill.SkillHelper;
import arekkuusu.enderskills.common.skill.ability.AbilityInfo;
import arekkuusu.enderskills.common.skill.ability.BaseAbility;
import arekkuusu.enderskills.common.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LifeSteal extends BaseAbility implements ISkillAdvancement {

    public LifeSteal() {
        super(LibNames.LIFE_STEAL);
        setTexture(ResourceLibrary.LIFE_STEAL);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void use(EntityLivingBase user, SkillInfo skillInfo) {
        if (isClientWorld(user) || !shouldUse(user)) return;
        AbilityInfo abilityInfo = (AbilityInfo) skillInfo;

        Capabilities.get(user).ifPresent(capability -> {
            if (!capability.isActive(this)) {
                if (!((IInfoCooldown) skillInfo).hasCooldown() && canUse(user)) {
                    if (!(user instanceof EntityPlayer) || !((EntityPlayer) user).capabilities.isCreativeMode) {
                        abilityInfo.setCooldown(getCooldown(abilityInfo));
                    }
                    float heal = getHeal(abilityInfo);
                    NBTTagCompound compound = new NBTTagCompound();
                    NBTHelper.setEntity(compound, user, "user");
                    NBTHelper.setFloat(compound, "heal", heal);
                    SkillData data = SkillData.of(this)
                            .with(INDEFINITE)
                            .put(compound, UUIDWatcher.INSTANCE)
                            .create();
                    apply(user, data);
                    sync(user, data);
                    sync(user);
                }
            } else {
                unapply(user);
                async(user);
            }

            if (user.world instanceof WorldServer) {
                ((WorldServer) user.world).playSound(null, user.posX, user.posY, user.posZ, ModSounds.LIFE_STEAL, SoundCategory.PLAYERS, 1.0F, (1.0F + (user.world.rand.nextFloat() - user.world.rand.nextFloat()) * 0.2F) * 0.7F);
            }
        });
    }

    @Override
    public void update(EntityLivingBase user, SkillData data, int tick) {
        if (isClientWorld(user)) return;
        if (tick % 20 == 0 && (!(user instanceof EntityPlayer) || !((EntityPlayer) user).capabilities.isCreativeMode)) {
            Capabilities.endurance(user).ifPresent(capability -> {
                int drain = ModAttributes.ENDURANCE.getEnduranceDrain(this);
                if (capability.getEndurance() - drain >= 0) {
                    capability.setEndurance(capability.getEndurance() - drain);
                    capability.setEnduranceDelay(30);
                } else {
                    unapply(user);
                    async(user);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityDamage(LivingHurtEvent event) {
        if (isClientWorld(event.getEntityLiving())) return;
        DamageSource source = event.getSource();
        if (source.getTrueSource() == null || source instanceof SkillDamageSource || event.getAmount() <= 0) return;
        EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
        EntityLivingBase attacked = event.getEntityLiving();
        SkillHelper.getActiveOwner(attacker, this, holder -> {
            float heal = NBTHelper.getFloat(holder.data.nbt, "heal");
            attacker.heal(attacker.getMaxHealth() * heal);
            for (int i = 0; i < 6; i++) {
                Vec3d vec = attacked.getPositionVector();
                double posX = vec.x + (attacked.width / 2) * (attacked.world.rand.nextDouble() - 0.5);
                double posY = vec.y + attacked.height * attacked.world.rand.nextDouble();
                double posZ = vec.z + (attacked.width / 2) * (attacked.world.rand.nextDouble() - 0.5);
                ES.getProxy().spawnParticle(attacked.world, new Vec3d(posX, posY, posZ), new Vec3d(0, 0, 0), 3, 50, 0x8A0303, ResourceLibrary.MINUS);
            }
            for (int i = 0; i < 6; i++) {
                Vec3d vec = attacker.getPositionVector();
                double posX = vec.x + (attacker.width / 2) * (attacker.world.rand.nextDouble() - 0.5);
                double posY = vec.y + attacker.height * attacker.world.rand.nextDouble();
                double posZ = vec.z + (attacker.width / 2) * (attacker.world.rand.nextDouble() - 0.5);
                ES.getProxy().spawnParticle(attacker.world, new Vec3d(posX, posY, posZ), new Vec3d(0, 0, 0), 3, 50, 0x8A0303, ResourceLibrary.PLUS);
            }

            if (attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).playSound(null, attacker.posX, attacker.posY, attacker.posZ, ModSounds.LIFE_STEAL_ATTACK, SoundCategory.PLAYERS, 1.0F, (1.0F + (attacker.world.rand.nextFloat() - attacker.world.rand.nextFloat()) * 0.2F) * 0.7F);
            }
        });
    }

    public int getLevel(IInfoUpgradeable info) {
        return info.getLevel();
    }

    @Override
    public int getMaxLevel() {
        return Configuration.getSyncValues().maxLevel;
    }

    public float getHeal(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().extra.heal, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.extra.globalEffectEffectiveness);
        return (float) (result * getEffectiveness());
    }

    public int getCooldown(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().cooldown, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.globalCooldown);
        return (int) (result * getEffectiveness());
    }

    public double getEffectiveness() {
        return Configuration.getSyncValues().effectiveness * CommonConfig.getSyncValues().skill.globalEffectiveness;
    }

    /*Advancement Section*/
    @Override
    @SideOnly(Side.CLIENT)
    public void addDescription(List<String> description) {
        Capabilities.get(Minecraft.getMinecraft().player).ifPresent(c -> {
            if (c.owns(this)) {
                if (!GuiScreen.isShiftKeyDown()) {
                    description.add("");
                    description.add("Hold SHIFT for stats.");
                } else {
                    c.get(this).ifPresent(skillInfo -> {
                        AbilityInfo abilityInfo = (AbilityInfo) skillInfo;
                        description.clear();
                        description.add("Endurance Drain: " + ModAttributes.ENDURANCE.getEnduranceDrain(this));
                        description.add("");
                        if (abilityInfo.getLevel() >= getMaxLevel()) {
                            description.add("Max Level:");
                        } else {
                            description.add("Current Level:");
                        }
                        description.add("Cooldown: " + TextHelper.format2FloatPoint(getCooldown(abilityInfo) / 20D) + "s");
                        description.add("Heal: " + TextHelper.format2FloatPoint(getHeal(abilityInfo) * 100) + "%");
                        if (abilityInfo.getLevel() < getMaxLevel()) { //Copy info and set a higher level...
                            AbilityInfo infoNew = new AbilityInfo(abilityInfo.serializeNBT());
                            infoNew.setLevel(infoNew.getLevel() + 1);
                            description.add("");
                            description.add("Next Level:");
                            description.add("Cooldown: " + TextHelper.format2FloatPoint(getCooldown(infoNew) / 20D) + "s");
                            description.add("Heal: " + TextHelper.format2FloatPoint(getHeal(infoNew) * 100) + "%");
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean canUpgrade(EntityLivingBase entity) {
        return Capabilities.advancement(entity).map(c -> {
            Requirement requirement = getRequirement(entity);
            int tokens = requirement.getLevels();
            int xp = requirement.getXp();
            return c.level >= tokens && c.getExperienceTotal(entity) >= xp;
        }).orElse(false);
    }

    @Override
    public void onUpgrade(EntityLivingBase entity) {
        Capabilities.advancement(entity).ifPresent(c -> {
            Requirement requirement = getRequirement(entity);
            int tokens = requirement.getLevels();
            int xp = requirement.getXp();
            if (c.level >= tokens && c.getExperienceTotal(entity) >= xp) {
                //c.tokensLevel -= tokens;
                c.consumeExperienceFromTotal(entity, xp);
            }
        });
    }

    @Override
    public Requirement getRequirement(EntityLivingBase entity) {
        AbilityInfo info = (AbilityInfo) Capabilities.get(entity).flatMap(a -> a.get(this)).orElse(null);
        int tokensNeeded = 0;
        int xpNeeded;
        if (info == null) {
            int abilities = Capabilities.get(entity).map(c -> (int) c.getAll().keySet().stream().filter(s -> s instanceof BaseAbility).count()).orElse(0);
            if (abilities > 0) {
                tokensNeeded = abilities + 1;
            } else {
                tokensNeeded = 1;
            }
        }
        xpNeeded = getUpgradeCost(info);
        return new DefaultRequirement(tokensNeeded, getCostIncrement(entity, xpNeeded));
    }

    public int getCostIncrement(EntityLivingBase entity, int total) {
        Optional<AdvancementCapability> optional = Capabilities.advancement(entity);
        if (optional.isPresent()) {
            AdvancementCapability advancement = optional.get();
            List<Skill> skillUnlockOrder = Arrays.asList(advancement.skillUnlockOrder);
            int index = skillUnlockOrder.indexOf(ModAbilities.BLEED);
            if (index == -1) {
                index = advancement.skillUnlockOrder.length;
            }
            return (int) (total * (1D + index * 0.5D));
        }
        return total;
    }

    public int getUpgradeCost(@Nullable AbilityInfo info) {
        int level = info != null ? getLevel(info) + 1 : 0;
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().advancement.upgrade, level, levelMax);
        return (int) (func * CommonConfig.getSyncValues().advancement.xp.globalCostMultiplier);
    }
    /*Advancement Section*/

    @Override
    public void initSyncConfig() {
        Configuration.getSyncValues().maxLevel = Configuration.getValues().maxLevel;
        Configuration.getSyncValues().cooldown = Configuration.getValues().cooldown;
        Configuration.getSyncValues().effectiveness = Configuration.getValues().effectiveness;
        Configuration.getSyncValues().extra.heal = Configuration.getValues().extra.heal;
        Configuration.getSyncValues().advancement.upgrade = Configuration.getValues().advancement.upgrade;
    }

    @Override
    public void writeSyncConfig(NBTTagCompound compound) {
        compound.setInteger("maxLevel", Configuration.getValues().maxLevel);
        NBTHelper.setArray(compound, "cooldown", Configuration.getValues().cooldown);
        compound.setDouble("effectiveness", Configuration.getValues().effectiveness);
        NBTHelper.setArray(compound, "extra.heal", Configuration.getValues().extra.heal);
        NBTHelper.setArray(compound, "advancement.upgrade", Configuration.getValues().advancement.upgrade);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readSyncConfig(NBTTagCompound compound) {
        Configuration.getSyncValues().maxLevel = compound.getInteger("maxLevel");
        Configuration.getSyncValues().cooldown = NBTHelper.getArray(compound, "cooldown");
        Configuration.getSyncValues().effectiveness = compound.getDouble("effectiveness");
        Configuration.getSyncValues().extra.heal = NBTHelper.getArray(compound,"extra.heal");
        Configuration.getSyncValues().advancement.upgrade = NBTHelper.getArray(compound, "advancement.upgrade");
    }

    @Config(modid = LibMod.MOD_ID, name = LibMod.MOD_ID + "/Ability/" + LibNames.LIFE_STEAL)
    public static class Configuration {

        @Config.Comment("Ability Values")
        @Config.LangKey(LibMod.MOD_ID + ".config." + LibNames.LIFE_STEAL)
        public static Values CONFIG = new Values();

        public static Values getValues() {
            return CONFIG;
        }

        @Config.Ignore
        protected static Values CONFIG_SYNC = new Values();

        public static Values getSyncValues() {
            return CONFIG_SYNC;
        }

        public static class Values {
            @Config.Comment("Skill specific extra Configuration")
            public final Extra extra = new Extra();
            @Config.Comment("Skill specific Advancement Configuration")
            public final Advancement advancement = new Advancement();

            @Config.Comment("Max level obtainable")
            @Config.RangeInt(min = 0)
            public int maxLevel = 4;

            @Config.Comment("Cooldown Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
            public String[] cooldown = {"(0+){(5 * 20) + (5 * 20) * (1 - ((e^(-0.1 * (x / y)) - 1)/((e^-0.1) - 1)))}"};

            @Config.Comment("Effectiveness Modifier")
            @Config.RangeDouble
            public double effectiveness = 1D;

            public static class Extra {
                @Config.Comment("Heal Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
                public String[] heal = {"(0+){0.01 + ((x / y) * (0.05 - 0.01))}"};
            }

            public static class Advancement {
                @Config.Comment("Function f(x)=? where 'x' is [Next Level] and 'y' is [Max Level], XP Cost is in units [NOT LEVELS]")
                public String[] upgrade = {"(0+){(5730 * (1 - (0 ^ (0 ^ x)))) + ((22070 / y) * (0 ^ (0 ^ x)))}"};
            }
        }
    }
}
