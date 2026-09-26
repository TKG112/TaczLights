package net.tkg.taczlights.network;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.TaczLightsComponents;

import java.util.HashMap;
import java.util.Map;

/**
 * Sent by the client to set the light mode of attachments on the gun in its main hand.
 * The client picks the modes, since only the client knows which attachments have lights and how many modes they have.
 *
 * @param modes The new mode of each attachment slot to change
 */
public record SetLightModePayload(Map<AttachmentType, Integer> modes) implements CustomPacketPayload {
    /** Upper bound so a client can't store arbitrary numbers on the item. */
    private static final int MAX_MODE = 64;

    public static final Type<SetLightModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "set_light_mode"));
    public static final StreamCodec<ByteBuf, SetLightModePayload> STREAM_CODEC = ByteBufCodecs.map(HashMap::new, AttachmentTypeCodec.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(SetLightModePayload::new, payload -> new HashMap<>(payload.modes));

    @Override
    public Type<SetLightModePayload> type() {
        return TYPE;
    }

    public static void handle(SetLightModePayload payload, IPayloadContext context) {
        Player player = context.player();
        ItemStack gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        payload.modes.forEach((type, mode) -> {
            if (mode >= 0 && mode <= MAX_MODE) {
                TaczLightsComponents.setOnAttachment(player.registryAccess(), gun, type, TaczLightsComponents.LIGHT_MODE, mode);
            }
        });
    }
}
