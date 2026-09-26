package net.tkg.taczlights;

import com.mojang.serialization.Codec;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class TaczLightsComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TaczLights.MODID);

    /**
     * The light mode of an attachment, 0 being off. When absent, the attachment uses its {@code default_mode}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LIGHT_MODE = REGISTER.registerComponentType("light_mode",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * Whether TaCZ draws the laser beams of an attachment, or of the laser built into a gun's own model.
     * When absent, the lasers are on.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LASER_ON = REGISTER.registerComponentType("laser_on",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * Whether an attachment's laser is in its infrared setting (only for attachments whose display allows it with
     * {@code "ir_laser"}). When absent, the laser is visible.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LASER_IR = REGISTER.registerComponentType("laser_ir",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private TaczLightsComponents() {
    }

    /**
     * @param stack An attachment, or a gun for the laser built into its model
     */
    public static boolean isLaserOn(ItemStack stack) {
        return stack.getOrDefault(LASER_ON, true);
    }

    /**
     * @param attachment An attachment
     */
    public static boolean isLaserIR(ItemStack attachment) {
        return attachment.getOrDefault(LASER_IR, false);
    }

    /**
     * Sets a component on the attachment installed in a gun's slot and saves the attachment back into the gun.
     * TaCZ stores installed attachments as full item stacks inside the gun, so the value moves with the attachment.
     *
     * @return Whether an attachment is installed in that slot. Built-in attachments aren't stored on the gun and can't be changed
     */
    public static <T> boolean setOnAttachment(HolderLookup.Provider registries, ItemStack gun, AttachmentType type,
                                              Supplier<DataComponentType<T>> component, T value) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return false;
        }
        ItemStack attachment = iGun.getAttachment(registries, gun, type);
        if (attachment.isEmpty()) {
            return false;
        }
        attachment.set(component.get(), value);
        iGun.installAttachment(registries, gun, attachment);
        return true;
    }
}
