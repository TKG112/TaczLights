package net.tkg.taczlights.mixin.veilfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import foundry.veil.impl.client.render.framebuffer.DSAAdvancedFboImpl;
import net.tkg.taczlights.client.veilfix.DepthStencilClear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;

@Mixin(value = DSAAdvancedFboImpl.class, remap = false)
public abstract class DSAAdvancedFboImplMixin {
    @WrapOperation(method = "clear(FFFFFI[I)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBClearTexture;glClearTexImage(IIIILjava/nio/ByteBuffer;)V"))
    private void taczlights$clearDepthStencil(int texture, int level, int format, int type, ByteBuffer data, Operation<Void> original,
                                              @Local(argsOnly = true, ordinal = 4) float depth) {
        DepthStencilClear.clearTexImage(texture, level, format, type, data, depth, original);
    }
}
