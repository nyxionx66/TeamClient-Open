package dev.ash.team.module.setting;

/**
 * A setting that holds a boolean value.
 */
public class BooleanSetting extends Setting<Boolean> {
    /**
     * Creates a new boolean setting.
     * 
     * @param name The name of the setting
     * @param description The description of the setting
     * @param defaultValue The default value of the setting
     */
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }
    
    /**
     * Toggles the value of the setting.
     * 
     * @return The new value
     */
    public boolean toggle() {
        setValue(!value);
        return value;
    }
}