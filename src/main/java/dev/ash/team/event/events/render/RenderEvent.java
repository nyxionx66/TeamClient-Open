package dev.ash.team.event.events.render;

import dev.ash.team.event.Event;
import net.minecraft.client.gui.DrawContext;

/**
 * Event fired during rendering.
 * This can be used to add custom rendering to the game.
 */
public class RenderEvent extends Event {
    private final DrawContext context;
    private final float tickDelta;

    public RenderEvent(DrawContext context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    /**
     * Gets the draw context for rendering.
     *
     * @return The DrawContext
     */
    public DrawContext getContext() {
        return context;
    }

    /**
     * Gets the tick delta for smooth rendering.
     *
     * @return The tick delta
     */
    public float getTickDelta() {
        return tickDelta;
    }
}
