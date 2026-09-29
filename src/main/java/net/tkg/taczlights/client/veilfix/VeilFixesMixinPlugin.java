package net.tkg.taczlights.client.veilfix;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * Applies the Veil depth and stencil fixes only to Veil versions that don't have them yet.
 * <p>
 * Veil versions with the fix have {@code FramebufferAttachmentDefinition.Format#getMainDepthFormat}. The class is
 * read without loading it, so Veil isn't touched before it's ready.
 */
public class VeilFixesMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("TaczLights");
    private static final String FIXED_CLASS = "foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition$Format";
    private static final String FIXED_METHOD = "getMainDepthFormat";

    private boolean apply;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            ClassNode format = MixinService.getService().getBytecodeProvider().getClassNode(FIXED_CLASS);
            this.apply = format.methods.stream().noneMatch(method -> method.name.equals(FIXED_METHOD));
            LOGGER.info(this.apply
                    ? "Applying Veil depth and stencil fixes"
                    : "Veil already fixes depth and stencil formats, skipping TaczLights' own fixes");
        } catch (Exception e) {
            // Veil isn't there (e.g. on a server), so there's nothing to fix
            this.apply = false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.apply;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
