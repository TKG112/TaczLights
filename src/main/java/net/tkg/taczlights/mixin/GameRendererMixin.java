package net.tkg.taczlights.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.tkg.taczlights.api.LaserGlow;
import net.tkg.taczlights.client.beam.LaserBloomTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * Applies the glow of first person laser beams once the hand is drawn, so it lands on top of the gun where the beam
     * is in front of it. Runs after Veil has finished its own first person pass. Skipped when another mod adds the glow
     * through {@link LaserGlow}.
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void taczlights$compositeFirstPersonLaserBloom(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (!LaserGlow.isCompositedExternally()) {
            LaserBloomTarget.FIRST_PERSON.composite();
        }
    }
}
