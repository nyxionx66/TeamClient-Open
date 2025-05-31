package dev.ash.team.mixin;

import dev.ash.team.ui.screen.CustomTitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        // Replace TitleScreen with CustomTitleScreen
        if (this.client != null && !(this.client.currentScreen instanceof CustomTitleScreen)) {
            // Pass the current screen as parent to maintain navigation flow
            Screen parent = this.client.currentScreen instanceof TitleScreen ? null : this.client.currentScreen;
            this.client.setScreen(new CustomTitleScreen(parent));
            ci.cancel(); // Cancel the original TitleScreen's init method
        }
    }
}