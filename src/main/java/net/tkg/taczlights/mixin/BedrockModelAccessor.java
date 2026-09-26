package net.tkg.taczlights.mixin;

import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.HashMap;
import java.util.List;

@Mixin(value = BedrockModel.class, remap = false)
public interface BedrockModelAccessor {
    @Accessor("modelMap")
    HashMap<String, ModelRendererWrapper> taczlights$getModelMap();

    @Invoker("getPath")
    List<BedrockPart> taczlights$getPath(ModelRendererWrapper rendererWrapper);
}
