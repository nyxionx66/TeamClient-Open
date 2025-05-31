package dev.ash.team.module.modules.render;

import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import dev.ash.team.module.setting.NumberSetting;
import dev.ash.team.ui.clickgui.ClickGUIScreen;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends Module {
    private final NumberSetting<Double> animationSpeed = addSetting(
            new NumberSetting<>("Animation Speed", "Speed of GUI animations", 1.0, 0.1, 2.0, 0.1));

    public ClickGUI() {
        super("ClickGUI", "Visual interface for managing modules", ModuleCategory.RENDER);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mc.player != null) {
            mc.setScreen(new ClickGUIScreen());
            setEnabled(false); // Disable after opening
        }
    }

    public double getAnimationSpeed() {
        return animationSpeed.getValue();
    }
}