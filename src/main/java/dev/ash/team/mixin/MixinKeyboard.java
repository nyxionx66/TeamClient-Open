package dev.ash.team.mixin;

import dev.ash.team.TeamClient;
import dev.ash.team.event.events.input.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Screen currentScreen = mc.currentScreen;

        // Only handle key events when no screen is open
        if (currentScreen == null) {
            TeamClient.getInstance().getEventBus().post(new KeyEvent(key, scanCode, action, modifiers));
        }
    }
}