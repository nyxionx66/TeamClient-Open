package dev.ash.team.event.events.player;

import dev.ash.team.event.events.CancellableEvent;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

/**
 * Event fired when the player moves.
 * This can be used to modify player movement or cancel it entirely.
 */
public class PlayerMoveEvent extends CancellableEvent {
    private final MovementType type;
    private Vec3d movement;
    
    public PlayerMoveEvent(MovementType type, Vec3d movement) {
        this.type = type;
        this.movement = movement;
    }
    
    /**
     * Gets the movement type.
     * 
     * @return The movement type
     */
    public MovementType getType() {
        return type;
    }
    
    /**
     * Gets the movement vector.
     * 
     * @return The movement vector
     */
    public Vec3d getMovement() {
        return movement;
    }
    
    /**
     * Sets the movement vector.
     * 
     * @param movement The new movement vector
     */
    public void setMovement(Vec3d movement) {
        this.movement = movement;
    }
}