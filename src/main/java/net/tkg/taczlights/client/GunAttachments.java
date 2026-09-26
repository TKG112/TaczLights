package net.tkg.taczlights.client;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

final class GunAttachments {
    private GunAttachments() {
    }

    /**
     * @return The attachments installed on the gun by slot. Built-in attachments are left out, since they can't store any state
     */
    static Map<AttachmentType, ItemStack> installed(HolderLookup.Provider registries, IGun iGun, ItemStack gun) {
        Map<AttachmentType, ItemStack> attachments = new EnumMap<>(AttachmentType.class);
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            ItemStack attachment = iGun.getAttachment(registries, gun, type);
            if (!attachment.isEmpty()) {
                attachments.put(type, attachment);
            }
        }
        return attachments;
    }
}
