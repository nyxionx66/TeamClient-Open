package dev.ash.team.config;

public interface Config<T> {
    String getName();

    T getValue();

    void setValue(T value);

    Class<T> getType();

    T getDefaultValue();
}