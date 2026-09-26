package net.tkg.taczlights.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
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
import net.tkg.taczlights.network.SetLightModePayload;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class LightToggleKey {
    public static final KeyMapping TOGGLE_LIGHTS = new KeyMapping("key.taczlights.toggle_lights",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.taczlights");

    private LightToggleKey() {
    }

    static void tick() {
        while (TOGGLE_LIGHTS.consumeClick()) {
            cycle();
        }
    }

    /**
     * Moves the lit attachments on the held gun to their next light mode: off, mode 1, mode 2, ... then back to off.
     * With several lit attachments they cycle together through as many modes as the attachment with the most,
     * each staying on its own last mode once it runs out.
     */
    private static void cycle() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        ItemStack gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof IGun iGun)) {
            return;
        }
        Map<AttachmentType, ItemStack> installed = GunAttachments.installed(player.registryAccess(), iGun, gun);
        Map<AttachmentType, AttachmentLightModes> lit = new EnumMap<>(AttachmentType.class);
        installed.forEach((type, attachment) -> {
            if (attachment.getItem() instanceof IAttachment iAttachment) {
                AttachmentLightModes modes = DisplayLights.getAttachmentModes(iAttachment.getAttachmentId(attachment));
                // A block with no lights (just "ir_laser") has nothing for the light key to switch
                if (modes != null && modes.hasLights()) {
                    lit.put(type, modes);
                }
            }
        });
        if (lit.isEmpty()) {
            return;
        }

        int modeCount = lit.values().stream().mapToInt(modes -> modes.modes().size()).max().orElse(0);
        int current = lit.entrySet().stream().mapToInt(entry -> entry.getValue().getMode(installed.get(entry.getKey()))).max().orElse(0);
        int next = current >= modeCount ? 0 : current + 1;

        Map<AttachmentType, Integer> newModes = new EnumMap<>(AttachmentType.class);
        lit.forEach((type, modes) -> newModes.put(type, Math.min(next, modes.modes().size())));
        // Apply locally right away; the server's copy replaces it once synced
        newModes.forEach((type, mode) -> TaczLightsComponents.setOnAttachment(player.registryAccess(), gun, type, TaczLightsComponents.LIGHT_MODE, mode));
        PacketDistributor.sendToServer(new SetLightModePayload(newModes));
        player.displayClientMessage(Component.translatable("message.taczlights.light_mode", getModeName(lit.values(), next)), true);
    }

    private static Component getModeName(Collection<AttachmentLightModes> attachments, int mode) {
        if (mode == 0) {
            return Component.translatable("message.taczlights.light_mode.off");
        }
        for (AttachmentLightModes modes : attachments) {
            if (mode <= modes.modes().size()) {
                Optional<String> name = modes.modes().get(mode - 1).name();
                if (name.isPresent()) {
                    return Component.translatable(name.get());
                }
            }
        }
        return Component.translatable("message.taczlights.light_mode.numbered", mode);
    }
}
