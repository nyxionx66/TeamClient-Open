package dev.ash.team.event;

/**
 * Base class for all events in the event system.
 * Events are used to notify listeners when something happens.
 */
public class Event {
    private boolean cancelled = false;
    
    /**
     * Checks if the event is cancelled.
     * 
     * @return true if the event is cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Sets whether the event is cancelled.
     * 
     * @param cancelled true to cancel the event, false otherwise
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Gets the name of the event.
     * 
     * @return The name of the event
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }
}