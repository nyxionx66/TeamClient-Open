package dev.ash.team.event.events.player;

import dev.ash.team.event.Event;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Event fired every player tick.
 * This is useful for updating modules that need to modify player behavior.
 */
public class PlayerTickEvent extends Event {
    private final ClientPlayerEntity player;
    
    public PlayerTickEvent(ClientPlayerEntity player) {
        this.player = player;
    }
    
    /**
     * Gets the player associated with this event.
     * 
     * @return The player
     */
    public ClientPlayerEntity getPlayer() {
        return player;
    }
    
    /**
     * Creates a new PlayerTickEvent for the current player.
     * 
     * @return A new PlayerTickEvent, or null if the player is not available
     */
    public static PlayerTickEvent create() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            return new PlayerTickEvent(client.player);
        }
        return null;
    }
}