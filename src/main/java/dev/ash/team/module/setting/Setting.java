package dev.ash.team.module.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for all settings.
 * Settings allow modules to be configured by the user.
 *
 * @param <T> The type of the setting value
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    protected final T defaultValue;
    private final List<Consumer<T>> changeListeners = new ArrayList<>();

    /**
     * Creates a new setting.
     *
     * @param name The name of the setting
     * @param description The description of the setting
     * @param defaultValue The default value of the setting
     */
    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /**
     * Gets the name of the setting.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of the setting.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the value of the setting.
     *
     * @return The value
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets the default value of the setting.
     *
     * @return The default value
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the value of the setting.
     *
     * @param value The new value
     * @return true if the value was changed, false otherwise
     */
    public boolean setValue(T value) {
        if (this.value.equals(value)) {
            return false;
        }

        this.value = value;

        // Notify listeners
        for (Consumer<T> listener : changeListeners) {
            listener.accept(value);
        }

        return true;
    }

    /**
     * Adds a listener that is called when the value changes.
     *
     * @param listener The listener
     */
    public void addChangeListener(Consumer<T> listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes a change listener.
     *
     * @param listener The listener to remove
     */
    public void removeChangeListener(Consumer<T> listener) {
        changeListeners.remove(listener);
    }
}