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

import java.util.HashSet;
import java.util.Set;

/**
 * Sent by the client to set the lasers on the gun in its main hand. The client decides the states: which lasers can
 * go infrared is in its display files, which the server doesn't have.
 *
 * @param attachments The attachment slots whose laser to set
 * @param on          Of those, the slots whose laser is on; the rest are switched off
 * @param ir          Of those, the slots whose laser is infrared; the rest are visible
 * @param gunLaser    Whether to also set the laser built into the gun's own model
 * @param gunOn       Whether that laser is on
 */
public record SetLaserPayload(Set<AttachmentType> attachments, Set<AttachmentType> on, Set<AttachmentType> ir,
                              boolean gunLaser, boolean gunOn) implements CustomPacketPayload {
    public static final Type<SetLaserPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "set_laser"));
    private static final StreamCodec<ByteBuf, Set<AttachmentType>> SLOTS = ByteBufCodecs.collection(HashSet::new, AttachmentTypeCodec.STREAM_CODEC);
    public static final StreamCodec<ByteBuf, SetLaserPayload> STREAM_CODEC = StreamCodec.composite(
            SLOTS, payload -> new HashSet<>(payload.attachments),
            SLOTS, payload -> new HashSet<>(payload.on),
            SLOTS, payload -> new HashSet<>(payload.ir),
            ByteBufCodecs.BOOL, SetLaserPayload::gunLaser,
            ByteBufCodecs.BOOL, SetLaserPayload::gunOn,
            SetLaserPayload::new);

    @Override
    public Type<SetLaserPayload> type() {
        return TYPE;
    }

    public static void handle(SetLaserPayload payload, IPayloadContext context) {
        Player player = context.player();
        ItemStack gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        apply(player, gun, payload);
    }

    /**
     * Sets the laser components on the gun and its attachments. Used by the server, and by the client to show the
     * change right away.
     */
    public static void apply(Player player, ItemStack gun, SetLaserPayload payload) {
        for (AttachmentType type : payload.attachments) {
            TaczLightsComponents.setOnAttachment(player.registryAccess(), gun, type, TaczLightsComponents.LASER_ON, payload.on.contains(type));
            TaczLightsComponents.setOnAttachment(player.registryAccess(), gun, type, TaczLightsComponents.LASER_IR, payload.ir.contains(type));
        }
        if (payload.gunLaser) {
            gun.set(TaczLightsComponents.LASER_ON, payload.gunOn);
        }
    }
}
