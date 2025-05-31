package dev.ash.team.module.setting;

/**
 * A setting that holds a numeric value with a minimum and maximum.
 * 
 * @param <T> The type of the number (Integer, Double, etc.)
 */
public class NumberSetting<T extends Number> extends Setting<T> {
    private final T min;
    private final T max;
    private final T step;
    
    /**
     * Creates a new number setting.
     * 
     * @param name The name of the setting
     * @param description The description of the setting
     * @param defaultValue The default value of the setting
     * @param min The minimum value
     * @param max The maximum value
     * @param step The step value for incrementing/decrementing
     */
    public NumberSetting(String name, String description, T defaultValue, T min, T max, T step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }
    
    /**
     * Gets the minimum value.
     * 
     * @return The minimum value
     */
    public T getMin() {
        return min;
    }
    
    /**
     * Gets the maximum value.
     * 
     * @return The maximum value
     */
    public T getMax() {
        return max;
    }
    
    /**
     * Gets the step value.
     * 
     * @return The step value
     */
    public T getStep() {
        return step;
    }
    
    @Override
    public boolean setValue(T value) {
        // Ensure the value is within the range
        if (value.doubleValue() < min.doubleValue()) {
            value = min;
        } else if (value.doubleValue() > max.doubleValue()) {
            value = max;
        }
        
        return super.setValue(value);
    }
}