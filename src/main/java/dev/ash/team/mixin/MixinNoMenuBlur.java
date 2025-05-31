package dev.ash.team.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinNoMenuBlur {
    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void onApplyBlur(CallbackInfo ci) {
        // Cancel the blur pass entirely
        ci.cancel();
    }
}
