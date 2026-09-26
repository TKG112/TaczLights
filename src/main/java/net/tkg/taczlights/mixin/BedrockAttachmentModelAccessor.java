package net.tkg.taczlights.mixin;

import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BedrockAttachmentModel.class, remap = false)
public interface BedrockAttachmentModelAccessor {
    @Accessor("laserBeamPaths")
    @Nullable
    List<List<BedrockPart>> taczlights$getLaserBeamPaths();
}
