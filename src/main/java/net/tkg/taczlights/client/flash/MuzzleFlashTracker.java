package net.tkg.taczlights.client.flash;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import foundry.veil.api.client.render.light.data.PointLightData;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.LogicalSide;
import net.tkg.taczlights.TaczLightsConfig;
import net.tkg.taczlights.client.display.DisplayLights;
import net.tkg.taczlights.client.light.LightPool;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Flashes a short-lived point light in front of every shooter the client sees fire.
 * <p>
 * Each shooter owns at most one flash, restarted by every shot, so fast firing guns never pile up lights.
 */
public final class MuzzleFlashTracker {
    private static final Map<Integer, Flash> FLASHES = new HashMap<>();
    private static final LightPool<PointLightData> LIGHTS = new LightPool<>(PointLightData::new);

    private MuzzleFlashTracker() {
    }

    /**
     * Called for each shot. TaCZ posts this on the client for the local player's own shots and for shots of other
     * entities the server tells it about.
     */
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide() != LogicalSide.CLIENT || !TaczLightsConfig.MUZZLE_FLASHES.get()) {
            return;
        }
        ItemStack gun = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return;
        }
        MuzzleFlashLight light = DisplayLights.getGunLights(gun).muzzleFlash();
        LivingEntity shooter = event.getShooter();
        if (!light.enabled() || isSilenced(shooter, iGun, gun)) {
            return;
        }

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 position = shooter.getEyePosition(partialTick).add(shooter.getViewVector(partialTick).scale(light.offset()));
        FLASHES.put(shooter.getId(), new Flash(light, new Vector3d(position.x, position.y, position.z), System.nanoTime()));
    }

    /**
     * Fades the active flashes and applies them to Veil's light renderer.
     */
    public static void flush() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !TaczLightsConfig.MUZZLE_FLASHES.get()) {
            clear();
            return;
        }

        long now = System.nanoTime();
        List<Flash> active = new ArrayList<>(FLASHES.size());
        for (Iterator<Flash> iterator = FLASHES.values().iterator(); iterator.hasNext(); ) {
            Flash flash = iterator.next();
            if (flash.progress(now) >= 1.0F) {
                iterator.remove();
            } else {
                active.add(flash);
            }
        }

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vector3d camera = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);
        active.sort(Comparator.comparingDouble(flash -> flash.position.distanceSquared(camera)));

        int count = Math.min(active.size(), TaczLightsConfig.MAX_MUZZLE_FLASHES.get());
        for (int i = 0; i < count; i++) {
            Flash flash = active.get(i);
            MuzzleFlashLight light = flash.light;
            // Ease out so the flash drops off quickly after the initial burst
            float fade = 1.0F - flash.progress(now);
            LIGHTS.get(i)
                    .setPosition(flash.position)
                    .setColor(light.color())
                    .setBrightness(light.brightness() * fade * fade)
                    .setRadius(light.radius())
                    .setOcclusionEnabled(light.occlusion());
        }
        LIGHTS.trim(count);
    }

    /**
     * Removes every muzzle flash light.
     */
    public static void clear() {
        FLASHES.clear();
        LIGHTS.trim(0);
    }

    /**
     * Same check TaCZ uses to hide its muzzle flash sprite for suppressed guns.
     */
    private static boolean isSilenced(LivingEntity shooter, IGun iGun, ItemStack gun) {
        ItemStack muzzle = iGun.getAttachment(shooter.registryAccess(), gun, AttachmentType.MUZZLE);
        if (muzzle.isEmpty()) {
            muzzle = iGun.getBuiltinAttachment(gun, AttachmentType.MUZZLE);
        }
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(muzzle);
        if (iAttachment == null) {
            return false;
        }
        return TimelessAPI.getCommonAttachmentIndex(iAttachment.getAttachmentId(muzzle))
                .map(index -> {
                    JsonProperty<?> silence = index.getData().getModifier().get(SilenceModifier.ID);
                    return silence != null && silence.getValue() instanceof Pair<?, ?> pair && Boolean.TRUE.equals(pair.right());
                })
                .orElse(false);
    }

    private record Flash(MuzzleFlashLight light, Vector3d position, long startNanos) {
        /**
         * @return How far through its fade the flash is, from 0 to 1
         */
        float progress(long now) {
            return (now - this.startNanos) / (this.light.duration() * 1_000_000.0F);
        }
    }
}
