package net.tkg.taczlights.client.veilfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11C.GL_STENCIL_CLEAR_VALUE;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL;

/**
 * Veil clears depth-stencil textures with {@code glClearTexImage} and no data, which clears depth to 0 instead of the
 * requested value. Passes the actual clear value instead.
 */
public final class DepthStencilClear {
    private DepthStencilClear() {
    }

    public static void clearTexImage(int texture, int level, int format, int type, ByteBuffer data, float depth, Operation<Void> original) {
        if (data != null || format != GL_DEPTH_STENCIL) {
            original.call(texture, level, format, type, data);
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // FLOAT_32_UNSIGNED_INT_24_8_REV is a 32-bit float depth followed by 32 bits with stencil in the low 8
            ByteBuffer clearValue = stack.malloc(8);
            clearValue.putFloat(0, depth).putInt(4, glGetInteger(GL_STENCIL_CLEAR_VALUE) & 0xFF);
            original.call(texture, level, format, type, clearValue);
        }
    }
}
