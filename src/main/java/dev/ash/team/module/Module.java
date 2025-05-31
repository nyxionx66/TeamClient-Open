
package dev.ash.team.module;

import dev.ash.team.TeamClient;
import dev.ash.team.event.EventTarget; // Make sure this is correctly imported
import dev.ash.team.event.events.client.ClientTickEvent; // Your specific ClientTickEvent
import dev.ash.team.module.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList; // Standard Java ArrayList
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final ModuleCategory category;
    private boolean enabled;
    private final boolean enabledInitially; // New flag to track constructor's enabled state
    private int keyBind;

    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, ModuleCategory category) {
        this(name, description, category, false, -1); // Default: disabled, no keybind
    }

    public Module(String name, String description, ModuleCategory category, boolean defaultEnabled) {
        this(name, description, category, defaultEnabled, -1); // Default: no keybind
    }

    public Module(String name, String description, ModuleCategory category, int defaultKeyBind) {
        this(name, description, category, false, defaultKeyBind); // Default: disabled
    }

    public Module(String name, String description, ModuleCategory category, boolean defaultEnabled, int defaultKeyBind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false; // Will be set correctly by ModuleManager if defaultEnabled is true
        this.enabledInitially = defaultEnabled;
        this.keyBind = defaultKeyBind;
    }


    public void onEnable() {
        // TeamClient.LOGGER.info("Module {} enabled", name); // Optional: logging
        TeamClient.getInstance().getEventBus().register(this);
    }

    public void onDisable() {
        // TeamClient.LOGGER.info("Module {} disabled", name); // Optional: logging
        TeamClient.getInstance().getEventBus().unregister(this);
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (this.enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    /**
     * Called by ModuleManager during initial registration if enabledInitially is true.
     * Avoids double-registration/logging if setEnabled(true) was called in constructor.
     */
    public void setEnabledFromConstructor(boolean enabled) {
        if (this.enabled == enabled) return; // Should only be called if it's different
        this.enabled = enabled;
        if (this.enabled) {
            onEnable(); // Register to event bus, etc.
        }
        // No onDisable call needed here as it starts disabled then gets enabled
    }

    public boolean isEnabledInitially() {
        return enabledInitially;
    }


    protected <T, S extends Setting<T>> S addSetting(S setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public List<Setting<?>> getSettings() {
        return new ArrayList<>(settings); // Return a copy to prevent external modification
    }

    public Setting<?> getSetting(String name) {
        if (name == null) return null;
        for (Setting<?> setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    // Modules should override this if they need to perform actions on tick.
    // If ModuleManager is globally calling this, individual modules don't need @EventTarget here
    // unless they want to subscribe to ClientTickEvent independently (which can lead to double calls).
    // For simplicity with ModuleManager's current onTick, remove @EventTarget here.
    // @EventTarget
    public void onTick(ClientTickEvent event) {
        // Override in subclasses
    }
}