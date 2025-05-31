package dev.ash.team.module.modules.render;

import dev.ash.team.TeamClient;
import dev.ash.team.event.EventTarget;
import dev.ash.team.event.events.render.RenderEvent;
import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import dev.ash.team.module.setting.ModeSetting;
import dev.ash.team.module.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayList extends Module {
    // --- Core Settings ---
    // REMOVE 'static' from these lines
    private final ModeSetting position = addSetting(new ModeSetting("Position", "Position on screen", "TopRight", "TopRight", "TopLeft", "BottomRight", "BottomLeft"));
    private final ModeSetting stylePreset = addSetting(new ModeSetting("StylePreset", "Visual style of the ArrayList", "ModernWave", "ModernWave", "SimpleDark", "SimpleLight", "ClassicRainbow"));
    private final NumberSetting<Double> scale = addSetting(new NumberSetting<>("Scale", "UI Scale", 1.0, 0.5, 2.0, 0.1));

    // --- Watermark Specific Setting ---
    // REMOVE 'static' if you had it here too (it wasn't in your last snippet for this one)
    private final ModeSetting watermarkStyle = addSetting(new ModeSetting("WatermarkStyle", "Style of the watermark", "Subtle", "Subtle", "BoldGradient", "Minimal"));


    // --- Animation & Internal State ---
    private final List<AnimatedModuleEntry> animatedModules = new java.util.ArrayList<>();
    private float lastScreenWidth = 0;
    private float lastScreenHeight = 0;
    private static final float BASE_ANIMATION_DURATION_MS = 250f; // Base duration

    // --- Style Properties (Derived from StylePreset) ---
    private int marginValue;
    private boolean sortByNameWidth;
    private float animationDurationMs;
    private float rainbowCycleSpeed;
    private float rainbowSat;
    private float rainbowBri;
    private int verticalGap;


    public ArrayList() {
        super("ArrayList", "Shows enabled modules on screen", ModuleCategory.RENDER);
        setEnabled(true);
        // Initialize style properties based on the default preset
        updateStyleProperties(stylePreset.getValue()); // Now 'stylePreset' is an instance field
        stylePreset.addChangeListener(this::updateStyleProperties); // Update when preset changes
    }

    // ... rest of the ArrayList class remains the same ...
    private void updateStyleProperties(String presetName) {
        float animationSpeedMultiplier;
        switch (presetName) {
            case "SimpleDark":
                marginValue = 3;
                sortByNameWidth = true;
                animationSpeedMultiplier = 1.0f;
                verticalGap = 1;
                break;
            case "SimpleLight":
                marginValue = 3;
                sortByNameWidth = true;
                animationSpeedMultiplier = 1.0f;
                verticalGap = 1;
                break;
            case "ClassicRainbow":
                marginValue = 2;
                sortByNameWidth = true;
                animationSpeedMultiplier = 1.0f;
                rainbowCycleSpeed = 4.0f;
                rainbowSat = 0.7f;
                rainbowBri = 1.0f;
                verticalGap = 0;
                break;
            case "ModernWave":
            default:
                marginValue = 5;
                sortByNameWidth = true;
                animationSpeedMultiplier = 0.9f;
                rainbowCycleSpeed = 5.0f;
                rainbowSat = 0.6f;
                rainbowBri = 0.95f;
                verticalGap = 2;
                break;
        }
        this.animationDurationMs = BASE_ANIMATION_DURATION_MS / animationSpeedMultiplier;
    }

    private boolean shouldShowBackground() {
        String preset = stylePreset.getValue();
        return preset.equals("ModernWave") || preset.equals("SimpleDark");
    }
    private int getBackgroundColor(float alpha) {
        String preset = stylePreset.getValue();
        if (preset.equals("ModernWave")) return new Color(15, 15, 20, (int) (160 * alpha)).getRGB();
        if (preset.equals("SimpleDark")) return new Color(20, 20, 20, (int) (180 * alpha)).getRGB();
        return 0;
    }
    private int getArrayListTextColor(float alpha, long rainbowIndex) {
        String preset = stylePreset.getValue();
        if (preset.equals("ClassicRainbow")) {
            return applyAlpha(getRainbow(rainbowCycleSpeed, rainbowSat, rainbowBri, rainbowIndex * 100), alpha);
        }
        if (preset.equals("ModernWave")) return applyAlpha(new Color(220, 220, 225).getRGB(), alpha);
        if (preset.equals("SimpleDark")) return applyAlpha(new Color(200,200,200).getRGB(), alpha);
        if (preset.equals("SimpleLight")) return applyAlpha(new Color(50,50,50).getRGB(), alpha);
        return applyAlpha(Color.WHITE.getRGB(), alpha);
    }
    private boolean shouldShowSideAccent() {
        String preset = stylePreset.getValue();
        return preset.equals("ModernWave") || preset.equals("ClassicRainbow");
    }
    private int getSideAccentWidth() {
        String preset = stylePreset.getValue();
        if (preset.equals("ModernWave")) return 2;
        if (preset.equals("ClassicRainbow")) return 1;
        return 0;
    }
    private int getSideAccentColor(float alpha, long rainbowIndex) {
        String preset = stylePreset.getValue();
        if (preset.equals("ModernWave") || preset.equals("ClassicRainbow")) {
            return applyAlpha(getRainbow(rainbowCycleSpeed, rainbowSat, rainbowBri, rainbowIndex * 100), alpha);
        }
        return 0;
    }

    private void renderWatermark(DrawContext context, float scaledScreenWidth, float scaledScreenHeight) {
        String mainText = getWatermarkText();
        String subText = getWatermarkSubText();
        int wmMargin = 5;

        boolean arrayListIsLeft = position.getValue().contains("Left");
        boolean arrayListIsTop = position.getValue().contains("Top");

        int wmX, wmY;

        if (arrayListIsLeft && arrayListIsTop) {
            wmX = (int) (scaledScreenWidth - mc.textRenderer.getWidth(mainText) - wmMargin);
            if (!subText.isEmpty() && (watermarkStyle.getValue().equals("Subtle") || watermarkStyle.getValue().equals("BoldGradient"))) {
                wmX = (int) (scaledScreenWidth - Math.max(mc.textRenderer.getWidth(mainText), mc.textRenderer.getWidth(subText)) - wmMargin);
            }
            wmY = wmMargin;
        } else {
            wmX = wmMargin;
            wmY = wmMargin;
        }


        float textAlpha = 1.0f;
        int mainTextColor, subTextColor;
        int V_PAD_WM = 1;

        switch (watermarkStyle.getValue()) {
            case "BoldGradient":
                mainTextColor = getRainbow(6.0f, 0.8f, 1.0f, 0);
                subTextColor = applyAlpha(Color.LIGHT_GRAY.getRGB(), textAlpha * 0.8f);
                int bgHeight = mc.textRenderer.fontHeight + (!subText.isEmpty() ? mc.textRenderer.fontHeight + V_PAD_WM : 0) + V_PAD_WM * 2;
                int bgWidth = Math.max(mc.textRenderer.getWidth(mainText), mc.textRenderer.getWidth(subText)) + H_PADDING_WM * 2;
                int gradCol1 = applyAlpha(getRainbow(6.0f,0.7f,0.5f,0), 0.6f);
                int gradCol2 = applyAlpha(getRainbow(6.0f,0.7f,0.5f,500), 0.3f);
                context.fillGradient(wmX - H_PADDING_WM, wmY - V_PAD_WM,
                        wmX + bgWidth - H_PADDING_WM, wmY + bgHeight - V_PAD_WM, // Corrected: use bgWidth directly
                        gradCol1, gradCol2, 0); // zLevel is 0 for UI typically
                context.drawTextWithShadow(mc.textRenderer, mainText, wmX, wmY, mainTextColor);
                if (!subText.isEmpty()) {
                    context.drawTextWithShadow(mc.textRenderer, subText, wmX, wmY + mc.textRenderer.fontHeight + V_PAD_WM, subTextColor);
                }
                break;

            case "Minimal":
                mainTextColor = applyAlpha(new Color(200, 200, 200).getRGB(), textAlpha);
                context.drawTextWithShadow(mc.textRenderer, mainText, wmX, wmY, mainTextColor);
                break;

            case "Subtle":
            default:
                mainTextColor = applyAlpha(new Color(220, 220, 225).getRGB(), textAlpha);
                subTextColor = applyAlpha(new Color(180, 180, 185).getRGB(), textAlpha * 0.9f);
                context.drawTextWithShadow(mc.textRenderer, mainText, wmX, wmY, mainTextColor);
                if (!subText.isEmpty()) {
                    context.drawTextWithShadow(mc.textRenderer, subText, wmX, wmY + mc.textRenderer.fontHeight + V_PAD_WM, subTextColor);
                }
                break;
        }
    }
    private static final int H_PADDING_WM = 3;


    private String getWatermarkText() {
        return TeamClient.CLIENT_NAME;
    }
    private String getWatermarkSubText() {
        return "v" + TeamClient.CLIENT_VERSION;
    }

    private int applyAlpha(int color, float alpha) {
        alpha = MathHelper.clamp(alpha, 0f, 1f);
        return (color & 0x00FFFFFF) | ((int) (255 * alpha) << 24);
    }
    public static int getRainbow(float seconds, float saturation, float brightness, long indexOffset) {
        float hue = ((System.currentTimeMillis() + indexOffset) % (int) (seconds * 1000)) / (seconds * 1000f);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }


    @EventTarget
    public void onRender(RenderEvent event) {
        if (mc.player == null || mc.world == null || mc.textRenderer == null) {
            animatedModules.clear();
            return;
        }

        DrawContext context = event.getContext();
        MatrixStack matrices = context.getMatrices();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float scaleFactor = this.scale.getValue().floatValue();
        matrices.push();
        matrices.scale(scaleFactor, scaleFactor, 1.0f);

        final float scaledScreenWidth = screenWidth / scaleFactor;
        final float scaledScreenHeight = screenHeight / scaleFactor;

        renderWatermark(context, scaledScreenWidth, scaledScreenHeight);

        List<Module> currentEnabledModules = TeamClient.getInstance().getModuleManager().getModules().stream()
                .filter(Module::isEnabled)
                .filter(m -> m != this)
                .collect(Collectors.toList());

        final int H_PADDING_AL = 3;
        final boolean isBottom = position.getValue().contains("Bottom");
        final int textHeight = mc.textRenderer.fontHeight;
        final int V_PADDING_AL = 2;
        final int entryHeight = textHeight + V_PADDING_AL * 2;

        updateAnimatedModules(currentEnabledModules, scaledScreenWidth, H_PADDING_AL, isBottom, scaledScreenHeight, entryHeight);

        animatedModules.sort((am1, am2) -> {
            boolean am1Enabled = currentEnabledModules.contains(am1.module);
            boolean am2Enabled = currentEnabledModules.contains(am2.module);
            if (am1Enabled && !am2Enabled) return -1;
            if (!am1Enabled && am2Enabled) return 1;
            return getModuleComparator().compare(am1.module, am2.module);
        });

        boolean isRight = position.getValue().contains("Right");
        int sideAccentW = getSideAccentWidth();

        float calculatedY = isBottom ? scaledScreenHeight - this.marginValue : this.marginValue;
        int visibleCount = 0;
        for (AnimatedModuleEntry am : animatedModules) {
            if (am.shouldBeVisible()) {
                visibleCount++;
            }
        }
        if (isBottom) {
            calculatedY -= (visibleCount * entryHeight) + Math.max(0, visibleCount - 1) * verticalGap;
        }

        for (AnimatedModuleEntry am : animatedModules) {
            if (am.shouldBeVisible()) {
                am.targetY = calculatedY;
                calculatedY += entryHeight + verticalGap;
            } else if (am.isFadingOut()) {
                if (am.targetY == AnimatedModuleEntry.INVALID_Y_POS) am.targetY = am.currentY;
            }
        }

        long rainbowIndex = 0;
        for (AnimatedModuleEntry am : animatedModules) {
            am.update(this.animationDurationMs);

            if (currentEnabledModules.contains(am.module)) {
                am.targetX = isRight
                        ? scaledScreenWidth - this.marginValue - am.nameWidth - H_PADDING_AL * 2 - sideAccentW
                        : this.marginValue;
                am.animateToAlpha(1f);
            } else {
                am.targetX = isRight
                        ? scaledScreenWidth
                        : -am.nameWidth - H_PADDING_AL * 2 - sideAccentW - this.marginValue - 10;
                am.animateToAlpha(0f);
            }

            if (am.currentAlpha > 0.001f) {
                float x = am.currentX;
                float y = am.currentY;

                if (shouldShowBackground()) {
                    float bgX1 = x;
                    float bgX2 = isRight ? scaledScreenWidth - this.marginValue : x + am.nameWidth + H_PADDING_AL * 2 + sideAccentW;
                    context.fill((int) bgX1, (int) y, (int) bgX2, (int) (y + entryHeight), getBackgroundColor(am.currentAlpha));
                }

                if (shouldShowSideAccent()) {
                    float accentX;
                    String currentPreset = stylePreset.getValue();
                    if (isRight) {
                        accentX = x - sideAccentW;
                        if(currentPreset.equals("ClassicRainbow")){
                            accentX = x + am.nameWidth + H_PADDING_AL * 2;
                        }
                    } else {
                        accentX = x;
                        if(currentPreset.equals("ClassicRainbow")){
                            accentX = x + am.nameWidth + H_PADDING_AL * 2;
                        } else if (currentPreset.equals("ModernWave")){ // Modern wave has accent at the start of the colored block
                            accentX = x;
                        }
                    }
                    context.fill((int) accentX, (int) y, (int) (accentX + sideAccentW), (int) (y + entryHeight), getSideAccentColor(am.currentAlpha, rainbowIndex));
                }

                float textX;
                String currentPreset = stylePreset.getValue();
                if (isRight) {
                    textX = x + H_PADDING_AL;
                } else {
                    if ( (currentPreset.equals("ModernWave") || currentPreset.equals("ClassicRainbow")) && shouldShowSideAccent()) {
                        textX = x + sideAccentW + H_PADDING_AL;
                    } else {
                        textX = x + H_PADDING_AL;
                    }
                }

                context.drawTextWithShadow(mc.textRenderer, am.name, (int) textX, (int) (y + V_PADDING_AL), getArrayListTextColor(am.currentAlpha, rainbowIndex));
                rainbowIndex++;
            }
        }

        animatedModules.removeIf(AnimatedModuleEntry::isFullyFadedOutAndOffScreen);
        matrices.pop();

        if (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight) {
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
            for (AnimatedModuleEntry am : animatedModules) {
                boolean isEntryEnabled = currentEnabledModules.contains(am.module);
                if (isEntryEnabled) {
                    am.targetX = isRight
                            ? scaledScreenWidth - this.marginValue - am.nameWidth - H_PADDING_AL * 2 - getSideAccentWidth()
                            : this.marginValue;
                } else {
                    am.targetX = isRight
                            ? scaledScreenWidth
                            : -am.nameWidth - H_PADDING_AL * 2 - getSideAccentWidth() - this.marginValue - 10;
                }
                if (!am.isAnimatingX()) am.currentX = am.targetX;
            }
        }
    }

    private void updateAnimatedModules(List<Module> currentEnabledModules, float scaledScreenWidth, final int H_PADDING_AL_CONST, boolean isBottomList, float scaledScreenHeight, final int entryHeightConst) {
        for (AnimatedModuleEntry am : animatedModules) {
            if (!currentEnabledModules.contains(am.module)) {
                am.animateToAlpha(0f);
            }
        }

        for (Module m : currentEnabledModules) {
            if (m == this) continue;
            AnimatedModuleEntry entry = animatedModules.stream().filter(am -> am.module == m).findFirst().orElse(null);
            if (entry == null) {
                entry = new AnimatedModuleEntry(m);
                boolean isRight = position.getValue().contains("Right");
                entry.currentX = isRight
                        ? scaledScreenWidth + entry.nameWidth
                        : -entry.nameWidth - getSideAccentWidth() - H_PADDING_AL_CONST * 2 - this.marginValue;

                float initialY;
                if (isBottomList) {
                    float lowestY = scaledScreenHeight - this.marginValue - entryHeightConst;
                    for(AnimatedModuleEntry existing : animatedModules) {
                        if(existing.shouldBeVisible() && existing.targetY != AnimatedModuleEntry.INVALID_Y_POS) { // Use targetY if valid
                            lowestY = Math.min(lowestY, existing.targetY - entryHeightConst - verticalGap);
                        } else if (existing.shouldBeVisible()){ // Fallback to currentY if targetY not yet set for some reason
                            lowestY = Math.min(lowestY, existing.currentY - entryHeightConst - verticalGap);
                        }
                    }
                    initialY = lowestY;
                } else {
                    float highestY = this.marginValue;
                    for(AnimatedModuleEntry existing : animatedModules) {
                        if(existing.shouldBeVisible() && existing.targetY != AnimatedModuleEntry.INVALID_Y_POS) {
                            highestY = Math.max(highestY, existing.targetY + entryHeightConst + verticalGap);
                        } else if (existing.shouldBeVisible()) {
                            highestY = Math.max(highestY, existing.currentY + entryHeightConst + verticalGap);
                        }
                    }
                    initialY = highestY;
                }
                entry.currentY = initialY;
                entry.targetY = initialY;

                animatedModules.add(entry);
            }
            entry.animateToAlpha(1f);
        }
    }

    private Comparator<Module> getModuleComparator() {
        if (this.sortByNameWidth) {
            return Comparator.comparing((Module mod) -> mc.textRenderer.getWidth(mod.getName())).reversed()
                    .thenComparing(Module::getName, String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private static class AnimatedModuleEntry {
        Module module;
        String name;
        int nameWidth;

        float currentX, currentY, currentAlpha;
        float targetX, targetY, targetAlpha;

        boolean isAnimatingAlpha;
        long animationStartTimeAlpha;

        static final float INVALID_Y_POS = -2000f; // Further off-screen as a distinct "unset" value


        AnimatedModuleEntry(Module module) {
            this.module = module;
            this.name = module.getName();
            if (mc != null && mc.textRenderer != null) {
                this.nameWidth = mc.textRenderer.getWidth(this.name);
            } else {
                this.nameWidth = 50;
            }
            this.currentAlpha = 0f;
            this.targetAlpha = 0f;
            this.targetY = INVALID_Y_POS;
            this.currentY = INVALID_Y_POS; // Initialize to an clearly off-screen value
        }

        void animateToAlpha(float newTargetAlpha) {
            if (this.targetAlpha != newTargetAlpha) {
                this.targetAlpha = newTargetAlpha;
                this.isAnimatingAlpha = true;
                this.animationStartTimeAlpha = System.currentTimeMillis();
            } else if (!isAnimatingAlpha && currentAlpha != targetAlpha) {
                this.isAnimatingAlpha = true;
                this.animationStartTimeAlpha = System.currentTimeMillis();
            }
        }

        boolean isAnimatingX() {
            return Math.abs(currentX - targetX) > 0.05f;
        }
        boolean isAnimatingY() { // Y is considered animating if its target is valid and it's not close
            return targetY != INVALID_Y_POS && Math.abs(currentY - targetY) > 0.05f;
        }


        void update(float animDuration) {
            if (mc == null || mc.textRenderer == null) return;
            if (mc.textRenderer.getWidth(name) != nameWidth) nameWidth = mc.textRenderer.getWidth(name);

            long currentTime = System.currentTimeMillis();

            if (isAnimatingAlpha) {
                long elapsedTime = currentTime - animationStartTimeAlpha;
                float progress = Math.min(1.0f, elapsedTime / animDuration);
                currentAlpha = MathHelper.lerp(easeOutCubic(progress), currentAlpha, targetAlpha);
                if (progress >= 1.0f || Math.abs(currentAlpha - targetAlpha) < 0.01f) {
                    currentAlpha = targetAlpha;
                    isAnimatingAlpha = false;
                }
            } else {
                currentAlpha = targetAlpha;
            }

            if (isAnimatingX()) { // Check using the method
                currentX = MathHelper.lerp(0.2f, currentX, targetX);
                if (Math.abs(currentX - targetX) <= 0.05f) currentX = targetX; // Snap if close
            } else {
                currentX = targetX; // Ensure it's snapped if not "animating"
            }

            if (isAnimatingY()) { // Check using the method
                currentY = MathHelper.lerp(0.18f, currentY, targetY);
                if (Math.abs(currentY - targetY) <= 0.05f) currentY = targetY; // Snap if close
            } else if (targetY != INVALID_Y_POS){ // If not animating Y but targetY is valid, snap it.
                currentY = targetY;
            }
        }

        boolean shouldBeVisible() {
            return targetAlpha > 0.5f || (isAnimatingAlpha && targetAlpha > currentAlpha) || currentAlpha > 0.01f;
        }

        boolean isFadingOut(){
            return isAnimatingAlpha && targetAlpha < currentAlpha && targetAlpha < 0.5f;
        }

        boolean isFullyFadedOutAndOffScreen() {
            boolean alphaFaded = !isAnimatingAlpha && currentAlpha < 0.001f && targetAlpha == 0f;
            return alphaFaded;
        }

        private static float easeOutCubic(float x) {
            return (float) (1 - Math.pow(1 - x, 3));
        }
    }
}