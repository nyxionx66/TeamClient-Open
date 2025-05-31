package dev.ash.team.event.events.render;

import dev.ash.team.event.Event;
import net.minecraft.client.util.math.MatrixStack;

public class RenderEvent3D extends Event {
    private final MatrixStack matrixStack;
    private final float partialTicks;

    public RenderEvent3D(MatrixStack matrixStack, float partialTicks) {
        this.matrixStack = matrixStack;
        this.partialTicks = partialTicks;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}