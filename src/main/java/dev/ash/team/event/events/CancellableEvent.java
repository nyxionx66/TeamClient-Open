package dev.ash.team.event.events;

import dev.ash.team.event.Event;

/**
 * Base class for events that can be cancelled.
 * When an event is cancelled, subsequent handlers will not be called.
 */
public class CancellableEvent extends Event {
    private boolean cancelled = false;
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}