package dev.ash.team.mixin;

import dev.ash.team.TeamClient;
import dev.ash.team.event.events.client.ClientTickEvent; // Assuming this is your event class
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for the Minecraft client.
 * Used to inject code into Minecraft's tick method to post ClientTickEvent.
 */
@Mixin(MinecraftClient.class)
public class MixinMinecraft { // Changed to abstract as it's a mixin not meant to be instantiated directly

    /**
     * Injects code at the beginning of the tick method.
     * Posts a ClientTickEvent.Start to the event bus.
     *
     * @param ci Callback info
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientTickStart(CallbackInfo ci) {
        // It's good practice to check if TeamClient instance or event bus is null,
        // though for tick events, it's usually safe.
        if (TeamClient.getInstance() != null && TeamClient.getInstance().getEventBus() != null) {
            TeamClient.getInstance().getEventBus().post(new ClientTickEvent.Start());
        }
    }

    /**
     * Injects code at the end of the tick method.
     * Posts a ClientTickEvent.End to the event bus.
     *
     * @param ci Callback info
     */
    @Inject(method = "tick", at = @At(value = "RETURN")) // Using RETURN is generally safer/more common than TAIL for end-of-method
    private void onClientTickEnd(CallbackInfo ci) {
        if (TeamClient.getInstance() != null && TeamClient.getInstance().getEventBus() != null) {
            TeamClient.getInstance().getEventBus().post(new ClientTickEvent.End());
        }
    }

    // REMOVED the redundant third injection:
    // @Inject(method = "tick", at = @At("RETURN"))
    // private void onTick(CallbackInfo ci) {
    //     TeamClient.getInstance().getEventBus().post(new ClientTickEvent());
    // }
}