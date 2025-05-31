package dev.ash.team.event.events.input;

import dev.ash.team.event.Event;

public class KeyEvent extends Event {
    private final int key;
    private final int scanCode;
    private final int action;
    private final int modifiers;

    public KeyEvent(int key, int scanCode, int action, int modifiers) {
        this.key = key;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() {
        return key;
    }

    public int getScanCode() {
        return scanCode;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }
}