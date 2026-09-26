package net.tkg.taczlights.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tkg.taczlights.TaczLightsComponents;
import net.tkg.taczlights.client.display.DisplayLights;
import net.tkg.taczlights.client.light.AttachmentLightModes;
import net.tkg.taczlights.mixin.BedrockAttachmentModelAccessor;
import net.tkg.taczlights.mixin.BedrockGunModelAccessor;
import net.tkg.taczlights.network.SetLaserPayload;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LaserToggleKey {
    public static final KeyMapping TOGGLE_LASER = new KeyMapping("key.taczlights.toggle_laser",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.taczlights");

    private LaserToggleKey() {
    }

    static void tick() {
        while (TOGGLE_LASER.consumeClick()) {
            toggle();
        }
    }

    /** The laser key's steps. IR only exists when an installed laser can go infrared ({@code "ir_laser"}). */
    private enum State { OFF, ON, IR }

    /**
     * Steps every laser on the held gun through off, on, IR (if any laser can), off. In the IR step the lasers that
     * can't go infrared, and the gun's own laser, are off.
     */
    private static void toggle() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        ItemStack gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof IGun iGun)) {
            return;
        }
        Map<AttachmentType, ItemStack> installed = GunAttachments.installed(player.registryAccess(), iGun, gun);
        Set<AttachmentType> laserAttachments = EnumSet.noneOf(AttachmentType.class);
        installed.forEach((type, attachment) -> {
            if (hasLaser(attachment)) {
                laserAttachments.add(type);
            }
        });
        boolean gunLaser = hasGunLaser(gun);
        if (laserAttachments.isEmpty() && !gunLaser) {
            return;
        }

        Set<AttachmentType> irCapable = EnumSet.noneOf(AttachmentType.class);
        for (AttachmentType type : laserAttachments) {
            if (canGoIR(installed.get(type))) {
                irCapable.add(type);
            }
        }

        State current;
        if (irCapable.stream().anyMatch(type -> TaczLightsComponents.isLaserOn(installed.get(type))
                && TaczLightsComponents.isLaserIR(installed.get(type)))) {
            current = State.IR;
        } else if ((gunLaser && TaczLightsComponents.isLaserOn(gun))
                || laserAttachments.stream().anyMatch(type -> TaczLightsComponents.isLaserOn(installed.get(type)))) {
            current = State.ON;
        } else {
            current = State.OFF;
        }
        State next = switch (current) {
            case OFF -> State.ON;
            case ON -> irCapable.isEmpty() ? State.OFF : State.IR;
            case IR -> State.OFF;
        };

        Set<AttachmentType> on = EnumSet.noneOf(AttachmentType.class);
        if (next == State.ON) {
            on.addAll(laserAttachments);
        } else if (next == State.IR) {
            on.addAll(irCapable);
        }
        Set<AttachmentType> ir = EnumSet.noneOf(AttachmentType.class);
        if (next == State.IR) {
            ir.addAll(irCapable);
        }
        SetLaserPayload payload = new SetLaserPayload(laserAttachments, on, ir, gunLaser, next == State.ON);
        // Apply locally right away; the server's copy replaces it once synced
        SetLaserPayload.apply(player, gun, payload);
        PacketDistributor.sendToServer(payload);
        player.displayClientMessage(Component.translatable("message.taczlights.laser",
                Component.translatable("message.taczlights.laser." + next.name().toLowerCase(Locale.ROOT))), true);
    }

    /**
     * @return Whether the attachment's display allows its laser to be switched to infrared
     */
    private static boolean canGoIR(ItemStack attachment) {
        if (!(attachment.getItem() instanceof IAttachment iAttachment)) {
            return false;
        }
        AttachmentLightModes modes = DisplayLights.getAttachmentModes(iAttachment.getAttachmentId(attachment));
        return modes != null && modes.irLaser();
    }

    /**
     * @return Whether the gun's own model has a laser beam bone
     */
    private static boolean hasGunLaser(ItemStack gun) {
        return TimelessAPI.getGunDisplay(gun)
                .map(GunDisplayInstance::getGunModel)
                .map(model -> ((BedrockGunModelAccessor) model).taczlights$getLaserBeamPaths() != null)
                .orElse(false);
    }

    /**
     * @return Whether the attachment's model has a laser beam bone
     */
    private static boolean hasLaser(ItemStack attachment) {
        if (!(attachment.getItem() instanceof IAttachment iAttachment)) {
            return false;
        }
        return TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(attachment))
                .map(ClientAttachmentIndex::getAttachmentModel)
                .map(model -> {
                    List<?> paths = ((BedrockAttachmentModelAccessor) model).taczlights$getLaserBeamPaths();
                    return paths != null && !paths.isEmpty();
                })
                .orElse(false);
    }
}
