package dev.ash.team.mixin;

import dev.ash.team.TeamClient;
import dev.ash.team.event.events.world.BlockBreakEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakEvent event = new BlockBreakEvent(pos);
        TeamClient.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}