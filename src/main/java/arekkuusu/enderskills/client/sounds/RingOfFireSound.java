package arekkuusu.enderskills.client.sounds;

import arekkuusu.enderskills.common.entity.placeable.EntityPlaceableData;
import arekkuusu.enderskills.common.sound.ModSounds;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.ref.WeakReference;

@SideOnly(Side.CLIENT)
public class RingOfFireSound extends MovingSound {

    public final WeakReference<EntityPlaceableData> reference;

    public RingOfFireSound(EntityPlaceableData entity) {
        super(ModSounds.RING_OF_FIRE, SoundCategory.PLAYERS);
        this.reference = new WeakReference<>(entity);
        this.xPosF = (float) entity.posX;
        this.yPosF = (float) entity.posY + entity.getRadius() / 2;
        this.zPosF = (float) entity.posZ;
        this.repeat = true;
    }

    @Override
    public void update() {
        EntityPlaceableData entity = reference.get();
        donePlaying = entity == null || entity.isDead;
    }

    @Override
    public float getVolume() {
        EntityPlaceableData entity = reference.get();
        return super.getVolume() * (entity == null ? 0F : 5F * (1F - ((float) entity.tick / (float) entity.getLifeTime())));
    }
}
