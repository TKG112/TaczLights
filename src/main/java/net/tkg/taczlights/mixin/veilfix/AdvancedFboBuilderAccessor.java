package net.tkg.taczlights.mixin.veilfix;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AdvancedFbo.Builder.class, remap = false)
public interface AdvancedFboBuilderAccessor {
    @Accessor("name")
    @Nullable
    String taczlights$getName();
}
