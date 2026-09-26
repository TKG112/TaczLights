package net.tkg.taczlights.mixin;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BedrockGunModel.class, remap = false)
public interface BedrockGunModelAccessor {
    @Accessor("laserBeamPaths")
    @Nullable
    List<BedrockPart> taczlights$getLaserBeamPaths();
}
