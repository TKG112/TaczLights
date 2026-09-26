package net.tkg.taczlights.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLightsComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Hides the laser beam built into a gun's own model when its laser is switched off.
 */
@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class BedrockGunModelMixin {
    @WrapWithCondition(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/functional/BeamRenderer;renderLaserBeam(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Ljava/util/List;)V"))
    private boolean taczlights$shouldRenderLaser(ItemStack gunItem, PoseStack poseStack, ItemDisplayContext transformType, List<BedrockPart> path) {
        return gunItem == null || TaczLightsComponents.isLaserOn(gunItem);
    }
}
