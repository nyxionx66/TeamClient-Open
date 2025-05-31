// In dev.ash.team.event.events.client.ClientTickEvent.java

package dev.ash.team.event.events.client;

import dev.ash.team.event.Event;

public class ClientTickEvent extends Event {
    private final Phase phase;

    public ClientTickEvent(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return phase;
    }

    public enum Phase {
        START, END
    }

    public static class Start extends ClientTickEvent {
        public Start() {
            super(Phase.START);
        }
    }

    public static class End extends ClientTickEvent {
        public End() {
            super(Phase.END);
        }
    }
}