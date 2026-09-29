package net.tkg.taczlights.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.util.LaserColorUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.TaczLightsComponents;
import net.tkg.taczlights.client.beam.LaserBloom;
import net.tkg.taczlights.client.display.DisplayLights;
import net.tkg.taczlights.client.light.AttachmentLight;
import net.tkg.taczlights.client.light.AttachmentLightModes;
import net.tkg.taczlights.client.light.AttachmentLightTracker;
import net.tkg.taczlights.client.light.IRLightBridge;
import net.tkg.taczlights.client.light.LaserLight;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Captures light positions whenever TaCZ renders an attachment mounted on a gun, and applies its laser switch and
 * laser light.
 */
@Mixin(value = BedrockAttachmentModel.class, remap = false)
public abstract class BedrockAttachmentModelMixin {
    /** Bone name to the bones from the model root down to it, empty if the model has no such bone. */
    @Unique
    private final Map<String, Optional<List<BedrockPart>>> taczlights$bonePaths = new HashMap<>();

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V", at = @At("HEAD"))
    private void taczlights$captureLights(@Nullable ItemStack attachmentItem, ItemStack gunItem, PoseStack poseStack, ItemDisplayContext transformType,
                                          RenderType renderType, int light, int overlay, CallbackInfo ci) {
        // TaCZ passes a null stack when rendering the attachment item on its own rather than on a gun
        if (attachmentItem == null || !(attachmentItem.getItem() instanceof IAttachment attachment)) {
            return;
        }
        AttachmentLightModes modes = DisplayLights.getAttachmentModes(attachment.getAttachmentId(attachmentItem));
        if (modes == null) {
            return;
        }
        for (AttachmentLight attachmentLight : modes.getLights(modes.getMode(attachmentItem))) {
            this.taczlights$getBonePath(attachmentLight.bone())
                    .ifPresent(path -> AttachmentLightTracker.capture(attachmentLight, transformType, poseStack, path));
        }
    }

    /**
     * Hides the attachment's laser beams when its laser is switched off, and draws them as infrared (through
     * ModernMayhem) when the laser key put them in IR -- as long as the display still allows it.
     */
    @WrapOperation(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/functional/BeamRenderer;renderLaserBeam(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Ljava/util/List;)V"))
    private void taczlights$renderLaser(@Nullable ItemStack attachmentItem, PoseStack poseStack, ItemDisplayContext transformType, List<BedrockPart> path,
                                        Operation<Void> original) {
        if (attachmentItem == null) {
            original.call(attachmentItem, poseStack, transformType, path);
            return;
        }
        if (!TaczLightsComponents.isLaserOn(attachmentItem)) {
            return;
        }
        AttachmentLightModes modes = attachmentItem.getItem() instanceof IAttachment attachment
                ? DisplayLights.getAttachmentModes(attachment.getAttachmentId(attachmentItem))
                : null;
        boolean ir = modes != null && modes.irLaser() && TaczLightsComponents.isLaserIR(attachmentItem);
        LaserLight laserLight = LaserLight.forLaser(modes != null ? modes.laserLight() : Optional.empty());
        if (laserLight != null) {
            AttachmentLightTracker.captureLaser(laserLight, transformType, poseStack, path,
                    LaserColorUtil.getLaserColor(attachmentItem), LaserBloom.getLaserConfig(attachmentItem).getLength(), ir);
        }
        if (ir) {
            IRLightBridge.renderIRLaser(() -> original.call(attachmentItem, poseStack, transformType, path));
            return;
        }
        original.call(attachmentItem, poseStack, transformType, path);
        LaserBloom.render(attachmentItem, poseStack, transformType, path);
    }

    @Unique
    private Optional<List<BedrockPart>> taczlights$getBonePath(String bone) {
        return this.taczlights$bonePaths.computeIfAbsent(bone, name -> {
            BedrockModelAccessor accessor = (BedrockModelAccessor) this;
            Optional<List<BedrockPart>> path = Optional.ofNullable(accessor.taczlights$getPath(accessor.taczlights$getModelMap().get(name)));
            if (path.isEmpty()) {
                TaczLights.LOGGER.warn("Attachment model has no bone named '{}' for its light", name);
            }
            return path;
        });
    }
}
