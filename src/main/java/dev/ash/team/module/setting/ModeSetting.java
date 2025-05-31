package dev.ash.team.module.setting;

import java.util.Arrays;
import java.util.List;

/**
 * A setting that holds a string value from a list of allowed modes.
 */
public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    
    /**
     * Creates a new mode setting.
     * 
     * @param name The name of the setting
     * @param description The description of the setting
     * @param defaultValue The default value of the setting
     * @param modes The allowed modes
     */
    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = Arrays.asList(modes);
        
        // Ensure the default value is in the allowed modes
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value must be in the allowed modes");
        }
    }
    
    /**
     * Gets the allowed modes.
     * 
     * @return The allowed modes
     */
    public List<String> getModes() {
        return modes;
    }
    
    /**
     * Gets the index of the current mode.
     * 
     * @return The index
     */
    public int getIndex() {
        return modes.indexOf(value);
    }
    
    /**
     * Sets the current mode by index.
     * 
     * @param index The index
     * @return true if the value was changed, false otherwise
     */
    public boolean setIndex(int index) {
        if (index < 0 || index >= modes.size()) {
            return false;
        }
        
        return setValue(modes.get(index));
    }
    
    /**
     * Cycles to the next mode.
     * 
     * @return The new mode
     */
    public String cycle() {
        int index = getIndex();
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
        return value;
    }
    
    @Override
    public boolean setValue(String value) {
        if (!modes.contains(value)) {
            return false;
        }
        
        return super.setValue(value);
    }
}