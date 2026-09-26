package net.tkg.taczlights.network;

import com.tacz.guns.api.item.attachment.AttachmentType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

final class AttachmentTypeCodec {
    /** Same encoding TaCZ uses for attachment types in its own packets. */
    static final StreamCodec<ByteBuf, AttachmentType> STREAM_CODEC = ByteBufCodecs.idMapper(AttachmentType::fromId, AttachmentType::ordinal);

    private AttachmentTypeCodec() {
    }
}
