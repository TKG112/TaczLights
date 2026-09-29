package net.tkg.taczlights.mixin.veilfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.impl.client.render.wrapper.VanillaAdvancedFboWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL11C.glGetInteger;

/**
 * Checking whether the main target has stencil binds its depth texture and leaves it bound, which can break whatever
 * texture was in use. Puts the previous binding back.
 */
@Mixin(value = VanillaAdvancedFboWrapper.class, remap = false)
public abstract class VanillaAdvancedFboWrapperMixin {
    @WrapOperation(method = "hasStencilAttachment", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_bindTexture(I)V"))
    private void taczlights$rememberTexture(int texture, Operation<Void> original, @Share("previousTexture") LocalIntRef previousTexture) {
        previousTexture.set(glGetInteger(GL_TEXTURE_BINDING_2D));
        original.call(texture);
    }

    @WrapOperation(method = "hasStencilAttachment", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGetTexLevelParameteri(III)I"))
    private int taczlights$restoreTexture(int target, int level, int parameter, Operation<Integer> original, @Share("previousTexture") LocalIntRef previousTexture) {
        int value = original.call(target, level, parameter);
        GlStateManager._bindTexture(previousTexture.get());
        return value;
    }
}
