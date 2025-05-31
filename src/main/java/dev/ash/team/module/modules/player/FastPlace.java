package dev.ash.team.module.modules.player;

import dev.ash.team.event.EventTarget;
import dev.ash.team.event.events.client.ClientTickEvent;
import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import dev.ash.team.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;

public class FastPlace extends Module {
    private final NumberSetting<Integer> delay = addSetting(
            new NumberSetting<>("Delay", "Delay between using items (in ticks)", 0, 0, 4, 1));

    private Field itemUseCooldownField;

    public FastPlace() {
        super("FastPlace", "Reduces the delay between using items", ModuleCategory.PLAYER);

        // Get the itemUseCooldown field using reflection
        try {
            itemUseCooldownField = MinecraftClient.class.getDeclaredField("itemUseCooldown");
            itemUseCooldownField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @EventTarget
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || itemUseCooldownField == null) return;

        try {
            // Get the current cooldown
            int currentCooldown = itemUseCooldownField.getInt(mc);

            // Set the cooldown to our delay if it's higher
            if (currentCooldown > delay.getValue()) {
                itemUseCooldownField.setInt(mc, delay.getValue());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}