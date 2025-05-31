package dev.ash.team.event.events.input;

import dev.ash.team.event.Event;

public class MouseEvent extends Event {
    private final int button;
    private final int action;
    private final int modifiers;

    public MouseEvent(int button, int action, int modifiers) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getButton() {
        return button;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }
}