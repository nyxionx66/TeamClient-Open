package dev.ash.team.event;

/**
 * Defines the priority levels for event handlers.
 * Higher priority handlers are called first.
 */
public enum EventPriority {
    LOWEST(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    HIGHEST(4);
    
    private final int value;
    
    EventPriority(int value) {
        this.value = value;
    }
    
    /**
     * Gets the numeric value of this priority.
     * 
     * @return The priority value
     */
    public int getValue() {
        return value;
    }
}