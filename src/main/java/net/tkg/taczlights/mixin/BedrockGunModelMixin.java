package net.tkg.taczlights.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.util.LaserColorUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLightsComponents;
import net.tkg.taczlights.client.beam.LaserBloom;
import net.tkg.taczlights.client.light.AttachmentLightTracker;
import net.tkg.taczlights.client.light.LaserLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

/**
 * Hides the laser beam built into a gun's own model when its laser is switched off. When on, makes it glow and cast
 * its dot of light.
 */
@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class BedrockGunModelMixin {
    @WrapWithCondition(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/functional/BeamRenderer;renderLaserBeam(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Ljava/util/List;)V"))
    private boolean taczlights$shouldRenderLaser(ItemStack gunItem, PoseStack poseStack, ItemDisplayContext transformType, List<BedrockPart> path) {
        if (gunItem == null) {
            return true;
        }
        if (!TaczLightsComponents.isLaserOn(gunItem)) {
            return false;
        }
        LaserLight laserLight = LaserLight.forLaser(Optional.empty());
        if (laserLight != null) {
            AttachmentLightTracker.captureLaser(laserLight, transformType, poseStack, path,
                    LaserColorUtil.getLaserColor(gunItem), LaserBloom.getLaserConfig(gunItem).getLength(), false);
        }
        LaserBloom.render(gunItem, poseStack, transformType, path);
        return true;
    }
}
