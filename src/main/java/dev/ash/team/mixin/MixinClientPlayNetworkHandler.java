package dev.ash.team.mixin;

import dev.ash.team.TeamClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.startsWith(TeamClient.getInstance().getCommandManager().getPrefix())) {
            if (TeamClient.getInstance().getCommandManager().executeCommand(message)) {
                ci.cancel(); // Cancel sending the message to the server if it was a command
            }
        }
    }
}