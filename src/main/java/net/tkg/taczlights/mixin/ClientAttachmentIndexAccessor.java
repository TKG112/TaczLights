package net.tkg.taczlights.mixin;

import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientAttachmentIndex.class, remap = false)
public interface ClientAttachmentIndexAccessor {
    @Accessor("display")
    AttachmentDisplay taczlights$getDisplay();
}
