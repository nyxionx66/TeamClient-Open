package dev.ash.team.event.events.world;

import dev.ash.team.event.events.CancellableEvent;
import net.minecraft.util.math.BlockPos;

public class BlockBreakEvent extends CancellableEvent {
    private final BlockPos pos;

    public BlockBreakEvent(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }
}