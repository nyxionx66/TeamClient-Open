package dev.ash.team.mixin;

import dev.ash.team.ui.clickgui.ClickGUIScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(CallbackInfo ci) {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof ClickGUIScreen) {
            ci.cancel();
        }
    }
}