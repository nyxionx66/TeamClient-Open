package dev.ash.team.ui.clickgui;

import dev.ash.team.TeamClient;
import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import dev.ash.team.module.setting.BooleanSetting;
import dev.ash.team.module.setting.ModeSetting;
import dev.ash.team.module.setting.NumberSetting;
import dev.ash.team.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color; // Keep if used, otherwise can be removed if not directly used here
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUIScreen extends Screen {
    // ... (Theme, constants, fields - all remain the same as your last version) ...
    // Theme Constants
    private static final class Theme {
        static final int CATEGORY_WIDTH = 120;
        static final int CATEGORY_HEIGHT = 16;
        static final int MODULE_HEIGHT = 15;
        static final int SETTING_HEIGHT = 15;
        static final int PADDING = 3;

        static final int BACKGROUND = 0xFF000000;
        static final int CATEGORY_BG = 0xCC2C2C2C;
        static final int BORDER = 0xFF3C3C3C;
        static final int MODULE_ENABLED = 0xCC3030AA;
        static final int MODULE_HOVERED = 0xCC383838;
        static final int MODULE_DEFAULT = 0xCC202020;
        static final int SETTING_BG = 0xCC282828;
        static final int SEPARATOR = 0x40777777;
        static final int GRID = 0x22FFFFFF;
        static final int KEYBIND_OVERLAY = 0xE0000000;
        static final int TEXT_PRIMARY = 0xFFFFFFFF;
        static final int TEXT_SECONDARY = 0xFFAAAAAA;
        static final int SEARCH_GLOW = 0x803030FF;
        static final int SEARCH_FOCUSED_BORDER = 0xFF4040FF;
        static final int SLIDER_BG = 0xFF505050;
        static final int SLIDER_FILL = 0xFF4040FF;
        static final int SLIDER_KNOB = 0xFFFFFFFF;
    }

    private static final int GRID_SIZE = 10;
    private static final int SCROLL_SPEED = 15;
    private static final int SEARCH_WIDTH = 200;
    private static final int SEARCH_HEIGHT = 20;

    private final List<CategoryPanel> categoryPanels;
    private CategoryPanel draggingPanel = null;
    private double dragOffsetX, dragOffsetY;
    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private Module bindingModule = null;
    private String searchQuery = "";
    private boolean showSearch = false;

    private int searchX, searchY;
    private boolean draggingSearchBar = false;
    private double searchDragOffsetX, searchDragOffsetY;
    private boolean searchBarFocused = false;
    private float glowIntensity = 0.0f;
    private float targetGlowIntensity = 0.0f;

    private Setting<?> draggingSliderSetting = null;
    private CategoryPanel activeSliderPanel = null;
    private int sliderOriginX;
    private int sliderTotalWidth;

    private final Map<Object, MarqueeState> marqueeStates = new HashMap<>();
    private long lastFrameTimeMs = 0;

    // --- Tips ---
    private static final List<String> TIPS = List.of(
            "Right-click category headers to expand/collapse them.",
            "Middle-click modules to set keybinds.",
            "Press 'F' (or Ctrl/Cmd+F) to toggle the search bar.",
            "Scroll your mouse wheel to scroll the ClickGUI.",
            "Drag category panels by their headers to move them.",
            "Sliders can be dragged precisely to set values.",
            "Settings are usually saved automatically when changed.",
            "Explore module settings for fine-tuned customization!",
            "The search bar filters both categories and modules by name.",
            "You can also drag the search bar to reposition it."
    );
    private String currentTip = "";
    private long lastTipChangeTime = 0;
    private static final long TIP_DISPLAY_DURATION_MS = 10000;
    private float tipAlpha = 0.0f;
    private float targetTipAlpha = 0.0f;
    private boolean tipFadingIn = false;
    private static final float TIP_FADE_DURATION_S = 0.75f;


    private static class MarqueeState {
        float scrollOffset = 0;
        boolean scrollingForward = true;
        float currentPauseTime = 0;
        boolean isPaused = true;
        boolean needsMarquee = false;
        boolean initialPausePending = true;

        static final float SCROLL_SPEED_PPS = 20.0f;
        static final float PAUSE_DURATION_S = 1.5f;
        static final float INITIAL_PAUSE_DURATION_S = 1.0f;

        public void update(float frameDelta, int textWidth, int maxWidth) {
            if (maxWidth <= 0) {
                needsMarquee = false;
                scrollOffset = 0;
                return;
            }
            if (textWidth <= maxWidth) {
                if (needsMarquee) {
                    scrollOffset = 0; isPaused = true; scrollingForward = true;
                    initialPausePending = true; currentPauseTime = 0;
                }
                needsMarquee = false; return;
            }
            needsMarquee = true;
            if (isPaused) {
                currentPauseTime += frameDelta;
                float currentPauseTarget = initialPausePending ? INITIAL_PAUSE_DURATION_S : PAUSE_DURATION_S;
                if (currentPauseTime >= currentPauseTarget) {
                    isPaused = false; currentPauseTime = 0;
                    if (initialPausePending) initialPausePending = false;
                    else scrollingForward = (scrollOffset <= 0);
                }
            } else {
                float scrollAmount = SCROLL_SPEED_PPS * frameDelta;
                if (scrollingForward) {
                    scrollOffset += scrollAmount;
                    if (scrollOffset >= textWidth - maxWidth) {
                        scrollOffset = textWidth - maxWidth; isPaused = true;
                    }
                } else {
                    scrollOffset -= scrollAmount;
                    if (scrollOffset <= 0) {
                        scrollOffset = 0; isPaused = true;
                    }
                }
            }
        }
    }

    public ClickGUIScreen() {
        super(Text.literal("Click GUI"));
        this.categoryPanels = new ArrayList<>();
        initializePanels();
        initializeSearchBar();
        updateAndSelectTip(0);
        tipAlpha = 0f;
        tipFadingIn = true;
        targetTipAlpha = 1.0f;
    }

    private void initializePanels() {
        int startX = 10;
        for (ModuleCategory category : ModuleCategory.values()) {
            List<Module> modules = TeamClient.getInstance().getModuleManager().getModulesByCategory(category);
            if (!modules.isEmpty()) {
                categoryPanels.add(new CategoryPanel(category, modules, startX, 10));
                startX += Theme.CATEGORY_WIDTH + 10;
            }
        }
    }

    private void initializeSearchBar() { this.searchX = 50; this.searchY = 30; }
    private MarqueeState getMarqueeState(Object key) { return marqueeStates.computeIfAbsent(key, k -> new MarqueeState()); }

    public void drawMarqueeText(DrawContext context, TextRenderer textRenderer, String text,
                                int textDrawStartX, int textDrawBaselineY,
                                int scissorX, int scissorY, int scissorWidth, int scissorHeight,
                                int textColor, MarqueeState state, boolean shadow) {
        if (scissorWidth <=0 || scissorHeight <= 0) {
            context.drawText(textRenderer, text, textDrawStartX, textDrawBaselineY, textColor, shadow);
            return;
        }
        if (!state.needsMarquee) {
            context.drawText(textRenderer, text, textDrawStartX, textDrawBaselineY, textColor, shadow);
            return;
        }
        context.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
        context.drawText(textRenderer, text, textDrawStartX - (int) state.scrollOffset, textDrawBaselineY, textColor, shadow);
        context.disableScissor();
    }

    private void updateAndSelectTip(float frameDelta) {
        long currentTime = System.currentTimeMillis();
        float tipFadeSpeedPerFrame = (1.0f / TIP_FADE_DURATION_S) * frameDelta;
        if (currentTip.isEmpty() || (!tipFadingIn && tipAlpha < 0.01f)) {
            int randomIndex; String nextTip;
            if (TIPS.size() > 1) {
                do { randomIndex = (int) (Math.random() * TIPS.size()); nextTip = "Tip: " + TIPS.get(randomIndex); }
                while (nextTip.equals(currentTip));
                currentTip = nextTip;
            } else if (!TIPS.isEmpty()){ currentTip = "Tip: " + TIPS.get(0); }
            else { currentTip = ""; }
            lastTipChangeTime = currentTime; tipFadingIn = true; targetTipAlpha = 1.0f;
        } else if (tipFadingIn && tipAlpha >= 0.99f && currentTime - lastTipChangeTime > TIP_DISPLAY_DURATION_MS) {
            tipFadingIn = false; targetTipAlpha = 0.0f;
        }
        if (Math.abs(tipAlpha - targetTipAlpha) > 0.005f) {
            if (tipFadingIn) tipAlpha = Math.min(targetTipAlpha, tipAlpha + tipFadeSpeedPerFrame);
            else tipAlpha = Math.max(targetTipAlpha, tipAlpha - tipFadeSpeedPerFrame);
        } else {
            tipAlpha = targetTipAlpha;
            if (tipAlpha == 0.0f && !tipFadingIn) lastTipChangeTime = 0;
        }
        tipAlpha = MathHelper.clamp(tipAlpha, 0.0f, 1.0f);
    }

    private void drawTips(DrawContext context, TextRenderer textRenderer, int screenHeight, int screenWidth) {
        if (currentTip.isEmpty() || tipAlpha <= 0.001f) return;
        int tipColor = applyAlpha(Theme.TEXT_SECONDARY, tipAlpha);
        int tipWidth = textRenderer.getWidth(currentTip);
        int tipX = (screenWidth - tipWidth) / 2;
        int tipY = screenHeight - textRenderer.fontHeight - 5;
        context.drawTextWithShadow(textRenderer, currentTip, tipX, tipY, tipColor);
    }

    private int applyAlpha(int color, float alphaFactor) {
        alphaFactor = MathHelper.clamp(alphaFactor, 0f, 1f);
        int originalAlpha = (color >> 24) & 0xFF;
        if (originalAlpha == 0 && (color & 0x00FFFFFF) != 0) originalAlpha = 255;
        else if (originalAlpha == 0 && (color & 0x00FFFFFF) == 0) originalAlpha = 255;
        return (color & 0x00FFFFFF) | ((int) (originalAlpha * alphaFactor) << 24);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTimeMs = System.currentTimeMillis();
        float frameDelta = 0.0f;
        if (lastFrameTimeMs > 0) frameDelta = (currentTimeMs - lastFrameTimeMs) / 1000.0f;
        lastFrameTimeMs = currentTimeMs; frameDelta = Math.min(frameDelta, 0.1f);

        updateAnimations(frameDelta); updateAndSelectTip(frameDelta);
        context.fill(0, 0, this.width, this.height, Theme.BACKGROUND);
        if (draggingPanel != null || draggingSearchBar || draggingSliderSetting != null) drawGrid(context);
        if (showSearch) drawSearchBar(context, mouseX, mouseY, frameDelta);
        for (CategoryPanel panel : categoryPanels) {
            if (searchQuery.isEmpty() || panel.matchesSearch(searchQuery)) {
                panel.render(context, mouseX, mouseY, scrollOffset, this, frameDelta);
            }
        }
        drawTips(context, textRenderer, this.height, this.width);
        if (bindingModule != null) drawKeybindOverlay(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void updateAnimations(float frameDelta) {
        float lerpFactor = 0.2f * (frameDelta * 60f); lerpFactor = MathHelper.clamp(lerpFactor, 0.01f, 0.8f);
        if (Math.abs(targetScrollOffset - scrollOffset) > 0.01) scrollOffset = MathHelper.lerp(lerpFactor, scrollOffset, targetScrollOffset);
        else scrollOffset = targetScrollOffset;
        if (Math.abs(targetGlowIntensity - glowIntensity) > 0.01f) glowIntensity = MathHelper.lerp(lerpFactor * 1.25f, glowIntensity, targetGlowIntensity);
        else glowIntensity = targetGlowIntensity;
    }

    private void drawGrid(DrawContext context) {
        for (int x = 0; x < width; x += GRID_SIZE) context.fill(x, 0, x + 1, height, Theme.GRID);
        for (int y = 0; y < height; y += GRID_SIZE) context.fill(0, y, width, y + 1, Theme.GRID);
    }

    private void drawSearchBar(DrawContext context, int mouseX, int mouseY, float frameDelta) {
        boolean isHovered = isSearchBarHovered(mouseX, mouseY);
        if (searchBarFocused && glowIntensity > 0.01f) {
            int glowSize = (int) (4 * glowIntensity);
            int baseGlowAlpha = (Theme.SEARCH_GLOW >> 24) & 0xFF; int dynamicGlowAlpha = (int) (baseGlowAlpha * glowIntensity);
            int glowColor = (Theme.SEARCH_GLOW & 0x00FFFFFF) | (dynamicGlowAlpha << 24);
            context.fill(searchX - glowSize, searchY - glowSize, searchX + SEARCH_WIDTH + glowSize, searchY + SEARCH_HEIGHT + glowSize, glowColor);
        }
        int bgColor = searchBarFocused ? Theme.MODULE_HOVERED : (isHovered ? Theme.MODULE_HOVERED : Theme.MODULE_DEFAULT);
        context.fill(searchX, searchY, searchX + SEARCH_WIDTH, searchY + SEARCH_HEIGHT, bgColor);
        int borderColor = searchBarFocused ? Theme.SEARCH_FOCUSED_BORDER : Theme.BORDER;
        context.fill(searchX, searchY, searchX + SEARCH_WIDTH, searchY + 1, borderColor);
        context.fill(searchX, searchY + SEARCH_HEIGHT - 1, searchX + SEARCH_WIDTH, searchY + SEARCH_HEIGHT, borderColor);
        context.fill(searchX, searchY, searchX + 1, searchY + SEARCH_HEIGHT, borderColor);
        context.fill(searchX + SEARCH_WIDTH - 1, searchY, searchX + SEARCH_WIDTH, searchY + SEARCH_HEIGHT, borderColor);
        String queryForDisplay = searchQuery;
        if (searchQuery.isEmpty() && !searchBarFocused) queryForDisplay = "Search (F / Ctrl+F)...";
        else if (searchQuery.isEmpty() && searchBarFocused) queryForDisplay = "Search modules...";
        int textX = searchX + 5; int textY = searchY + (SEARCH_HEIGHT - textRenderer.fontHeight) / 2 + 1;
        int availableTextWidth = SEARCH_WIDTH - 10; if (draggingSearchBar) availableTextWidth -= textRenderer.getWidth("⋮⋮") + 5;
        availableTextWidth = Math.max(1, availableTextWidth);
        MarqueeState marqueeState = getMarqueeState("search_bar_text");
        marqueeState.update(frameDelta, textRenderer.getWidth(queryForDisplay), availableTextWidth);
        int textColor = (searchQuery.isEmpty() && queryForDisplay.startsWith("Search")) ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY;
        drawMarqueeText(context, textRenderer, queryForDisplay, textX, textY, textX, searchY, availableTextWidth, SEARCH_HEIGHT, textColor, marqueeState, false);
        if (draggingSearchBar) {
            String dragText = "⋮⋮"; int dragTextWidth = textRenderer.getWidth(dragText);
            context.drawText(textRenderer, dragText, searchX + SEARCH_WIDTH - dragTextWidth - 5, textY, Theme.TEXT_SECONDARY, false);
        }
    }

    private boolean isSearchBarHovered(int mouseX, int mouseY) {
        return mouseX >= searchX && mouseX <= searchX + SEARCH_WIDTH &&
                mouseY >= searchY && mouseY <= searchY + SEARCH_HEIGHT;
    }
    private void snapSearchBarToGrid() {
        searchX = Math.round(searchX / (float) GRID_SIZE) * GRID_SIZE;
        searchY = Math.round(searchY / (float) GRID_SIZE) * GRID_SIZE;
        searchX = MathHelper.clamp(searchX, 0, this.width - SEARCH_WIDTH);
        searchY = MathHelper.clamp(searchY, 0, this.height - SEARCH_HEIGHT);
    }
    private void drawKeybindOverlay(DrawContext context) {
        String text = "Press a key for " + bindingModule.getName() + "... (ESC to clear, ENTER to accept)";
        int textWidth = textRenderer.getWidth(text); int boxWidth = textWidth + 20; int boxHeight = textRenderer.fontHeight + 20;
        int x = (width - boxWidth) / 2; int y = (height - boxHeight) / 2;
        context.fill(x, y, x + boxWidth, y + boxHeight, Theme.KEYBIND_OVERLAY);
        context.fill(x, y, x + boxWidth, y + 1, Theme.BORDER); context.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, Theme.BORDER);
        context.fill(x, y, x + 1, y + boxHeight, Theme.BORDER); context.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, Theme.BORDER);
        context.drawText(textRenderer, text, x + 10, y + (boxHeight - textRenderer.fontHeight) / 2, Theme.TEXT_PRIMARY, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindingModule != null) return true;
        if (showSearch && isSearchBarHovered((int) mouseX, (int) mouseY)) {
            if (button == 0) {
                searchBarFocused = true; targetGlowIntensity = 1.0f;
                if (!draggingSearchBar) {
                    draggingSearchBar = true; searchDragOffsetX = searchX - mouseX; searchDragOffsetY = searchY - mouseY;
                } return true;
            }
        } else {
            if (searchBarFocused && button == 0 && !draggingSearchBar) searchBarFocused = false; targetGlowIntensity = 0.0f;
        }
        for (CategoryPanel panel : categoryPanels) {
            if (searchQuery.isEmpty() || panel.matchesSearch(searchQuery)) {
                if (panel.handleMouseClick(mouseX, mouseY, button, this, scrollOffset)) return true;
                double panelHeaderScreenY = panel.y + scrollOffset;
                if (mouseX >= panel.x && mouseX <= panel.x + Theme.CATEGORY_WIDTH &&
                        mouseY >= panelHeaderScreenY && mouseY <= panelHeaderScreenY + Theme.CATEGORY_HEIGHT && button == 0) {
                    draggingPanel = panel; dragOffsetX = panel.x - mouseX; dragOffsetY = panel.y - (mouseY - scrollOffset);
                    return true;
                }
            }
        } return super.mouseClicked(mouseX, mouseY, button);
    }

    public void startDraggingSlider(CategoryPanel panel, Setting<?> setting, double initialMouseX, int sliderAbsScreenX, int sliderPixelWidth) {
        this.draggingSliderSetting = setting; this.activeSliderPanel = panel;
        this.sliderOriginX = sliderAbsScreenX; this.sliderTotalWidth = sliderPixelWidth;
        updateSliderValue(initialMouseX);
    }

    @SuppressWarnings("unchecked")
    private void updateSliderValue(double screenMouseX) {
        if (draggingSliderSetting == null || !(draggingSliderSetting instanceof NumberSetting) || activeSliderPanel == null || sliderTotalWidth <= 0) return;
        NumberSetting numSet = (NumberSetting) draggingSliderSetting;
        double minVal = ((Number) numSet.getMin()).doubleValue(); double maxVal = ((Number) numSet.getMax()).doubleValue();
        double percent = MathHelper.clamp((screenMouseX - this.sliderOriginX) / (double) this.sliderTotalWidth, 0.0, 1.0);
        double rawNewValue = minVal + percent * (maxVal - minVal);
        if (numSet.getValue() instanceof Float) {
            NumberSetting<Float> fs = (NumberSetting<Float>) numSet; float step = fs.getStep() != null ? fs.getStep() : 0.001f;
            float val = (float)rawNewValue; if (step > 0) val = Math.round(val / step) * step;
            fs.setValue(MathHelper.clamp(val, fs.getMin(), fs.getMax()));
        } else if (numSet.getValue() instanceof Double) {
            NumberSetting<Double> ds = (NumberSetting<Double>) numSet; double step = ds.getStep() != null ? ds.getStep() : 0.001;
            double val = rawNewValue; if (step > 0) val = Math.round(val / step) * step;
            ds.setValue(MathHelper.clamp(val, ds.getMin(), ds.getMax()));
        } else if (numSet.getValue() instanceof Integer) {
            NumberSetting<Integer> is = (NumberSetting<Integer>) numSet; int step = is.getStep() != null ? is.getStep() : 1;
            int val = (int)Math.round(rawNewValue); if (step > 0) val = Math.round((float)val / step) * step;
            is.setValue(MathHelper.clamp(val, is.getMin(), is.getMax()));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScrollOffset += verticalAmount * SCROLL_SPEED;
        double minY = Double.MAX_VALUE; double maxY = Double.MIN_VALUE;
        boolean anyPanelVisible = false;
        for (CategoryPanel panel : categoryPanels) {
            if (searchQuery.isEmpty() || panel.matchesSearch(searchQuery)) {
                minY = Math.min(minY, panel.y); maxY = Math.max(maxY, panel.y + panel.getHeight());
                anyPanelVisible = true;
            }
        }
        if (!anyPanelVisible) { targetScrollOffset = 0; return true; }
        double totalContentSpan = maxY - minY;
        double minScrollAllowed = this.height - maxY - Theme.CATEGORY_HEIGHT;
        double maxScrollAllowed = -minY + Theme.CATEGORY_HEIGHT;
        if (totalContentSpan < this.height - (2*Theme.CATEGORY_HEIGHT)) {
            minScrollAllowed = Math.max(minScrollAllowed, 0); // Cap at 0 if all content fits.
            maxScrollAllowed = Math.min(maxScrollAllowed, 0); // If all fits, don't allow scrolling past 0.
        }
        targetScrollOffset = MathHelper.clamp(targetScrollOffset, minScrollAllowed, maxScrollAllowed);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (draggingPanel != null) { draggingPanel.snapToGrid(); draggingPanel = null; return true; }
            if (draggingSearchBar) { snapSearchBarToGrid(); draggingSearchBar = false; return true; }
            if (draggingSliderSetting != null) { draggingSliderSetting = null; activeSliderPanel = null; return true; }
        } return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void mouseMoved(double mouseX, double mouseY) { super.mouseMoved(mouseX, mouseY); }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            if (draggingPanel != null) {
                draggingPanel.x = (int) (mouseX + dragOffsetX); draggingPanel.y = (int) (mouseY - scrollOffset + dragOffsetY); return true;
            }
            if (draggingSearchBar) {
                searchX = (int) (mouseX + searchDragOffsetX); searchY = (int) (mouseY + searchDragOffsetY); return true;
            }
            if (draggingSliderSetting != null) { updateSliderValue(mouseX); return true; }
        } return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) return handleKeybindInput(keyCode);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (searchBarFocused) { searchBarFocused = false; targetGlowIntensity = 0.0f; return true; }
        }
        boolean isMac = MinecraftClient.IS_SYSTEM_MAC;
        boolean ctrlOrCmdMod = isMac ? (modifiers & GLFW.GLFW_MOD_SUPER) != 0 : Screen.hasControlDown();
        if ((keyCode == GLFW.GLFW_KEY_F && !(Screen.hasShiftDown() || ctrlOrCmdMod || Screen.hasAltDown())) || (keyCode == GLFW.GLFW_KEY_F && ctrlOrCmdMod)) {
            showSearch = !showSearch;
            if (!showSearch) { searchQuery = ""; searchBarFocused = false; targetGlowIntensity = 0.0f; }
            else {
                if (!searchBarFocused) targetGlowIntensity = 0.0f;
                searchBarFocused = true; if (glowIntensity < 0.5f) targetGlowIntensity = 1.0f;
            } return true;
        }
        if (showSearch && searchBarFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
                searchBarFocused = false; targetGlowIntensity = 0.0f; return true;
            }
        } return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showSearch && searchBarFocused) {
            if (chr != (char)167 && chr >= ' ' && chr != (char)127) { searchQuery += chr; return true; }
        } return super.charTyped(chr, modifiers);
    }

    private boolean handleKeybindInput(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE: bindingModule.setKeyBind(-1); bindingModule = null; break;
            case GLFW.GLFW_KEY_ENTER: case GLFW.GLFW_KEY_KP_ENTER: bindingModule = null; break;
            default:
                if (keyCode > 0 && keyCode < GLFW.GLFW_KEY_MENU || keyCode == GLFW.GLFW_KEY_KP_ENTER) bindingModule.setKeyBind(keyCode); // Include KP_ENTER
                else bindingModule.setKeyBind(-1);
                bindingModule = null; break;
        } return true;
    }

    @Override public boolean shouldPause() { return false; }
    @Override
    public boolean shouldCloseOnEsc() {
        if (bindingModule != null) return false;
        if (showSearch) {
            if (searchBarFocused) { searchBarFocused = false; targetGlowIntensity = 0.0f; return false; }
            showSearch = false; searchQuery = ""; searchBarFocused = false; targetGlowIntensity = 0.0f; return false;
        } return true;
    }

    public void setBindingModule(Module module) { this.bindingModule = module; }

    // New method for ModuleManager
    public boolean isBindingKey() {
        return this.bindingModule != null;
    }


    // --- Inner Classes (CategoryPanel, SettingValueFormatter, SettingClickHandler) ---
    private static class CategoryPanel {
        private final ModuleCategory category;
        private final List<Module> modules;
        public int x, y;
        private boolean expanded = true;
        private Module expandedModule = null;

        public CategoryPanel(ModuleCategory category, List<Module> modulesForThisCategory, int x, int y) {
            this.category = category;
            this.modules = new ArrayList<>(modulesForThisCategory);
            this.x = x; this.y = y;
        }

        public boolean matchesSearch(String query) {
            if (query.isEmpty()) return true; String lowerQuery = query.toLowerCase();
            if (category.getName().toLowerCase().contains(lowerQuery)) return true;
            for (Module module : this.modules) if (module.getName().toLowerCase().contains(lowerQuery)) return true;
            return false;
        }

        public void snapToGrid() {
            x = Math.round(x / (float)GRID_SIZE) * GRID_SIZE;
            y = Math.round(y / (float)GRID_SIZE) * GRID_SIZE;
        }

        public double getHeight() {
            double height = Theme.CATEGORY_HEIGHT;
            if (expanded) {
                if (!this.modules.isEmpty()) {
                    height += this.modules.size() * Theme.MODULE_HEIGHT;
                    if (this.modules.size() > 0) height += (this.modules.size() -1) * 1; // Separators between modules
                }
                if (this.expandedModule != null && !this.expandedModule.getSettings().isEmpty()) {
                    if (!this.modules.isEmpty() || this.expandedModule != null) height +=1; // Separator before settings
                    height += this.expandedModule.getSettings().size() * Theme.SETTING_HEIGHT;
                    if (this.expandedModule.getSettings().size() > 0) height += (this.expandedModule.getSettings().size() -1) * 1; // Separators between settings
                }
            }
            height += 1; // For bottom panel border
            return height;
        }


        public void render(DrawContext context, int mouseX, int mouseY, double scrollOffset, ClickGUIScreen clickGui, float frameDelta) {
            MinecraftClient client = MinecraftClient.getInstance(); int fontHeight = client.textRenderer.fontHeight;
            int panelHeaderScreenY = (int) (this.y + scrollOffset);
            drawCategoryHeader(context, client, panelHeaderScreenY, fontHeight, clickGui, frameDelta);
            if (expanded) {
                int contentTopScreenY = panelHeaderScreenY + Theme.CATEGORY_HEIGHT + 1;
                int finalContentBottomY = drawModulesAndSettings(context, client, contentTopScreenY, mouseX, mouseY, clickGui, fontHeight, frameDelta);
                context.fill(x, finalContentBottomY, x + Theme.CATEGORY_WIDTH, finalContentBottomY + 1, Theme.BORDER); // Final bottom border
            } else {
                context.fill(x, panelHeaderScreenY + Theme.CATEGORY_HEIGHT, x + Theme.CATEGORY_WIDTH, panelHeaderScreenY + Theme.CATEGORY_HEIGHT + 1, Theme.BORDER);
            }
        }

        private void drawCategoryHeader(DrawContext context, MinecraftClient client, int headerScreenY, int fontHeight, ClickGUIScreen clickGui, float frameDelta) {
            context.fill(x, headerScreenY, x + Theme.CATEGORY_WIDTH, headerScreenY + Theme.CATEGORY_HEIGHT, Theme.CATEGORY_BG);
            context.fill(x, headerScreenY, x + Theme.CATEGORY_WIDTH, headerScreenY + 1, Theme.BORDER);
            context.fill(x, headerScreenY, x + 1, headerScreenY + Theme.CATEGORY_HEIGHT +1 , Theme.BORDER);
            context.fill(x + Theme.CATEGORY_WIDTH - 1, headerScreenY, x + Theme.CATEGORY_WIDTH, headerScreenY + Theme.CATEGORY_HEIGHT +1, Theme.BORDER);
            String catName = category.getName(); String indicator = expanded ? "−" : "+";
            int indicatorWidth = client.textRenderer.getWidth(indicator) + Theme.PADDING;
            int availableTextWidth = Theme.CATEGORY_WIDTH - (Theme.PADDING * 2) - indicatorWidth;
            int textRenderX = x + Theme.PADDING; int textRenderY = headerScreenY + (Theme.CATEGORY_HEIGHT - fontHeight) / 2 + 1;
            MarqueeState marqueeState = clickGui.getMarqueeState("cat_header_" + category.ordinal() + "_" + this.hashCode());
            marqueeState.update(frameDelta, client.textRenderer.getWidth(catName), Math.max(1,availableTextWidth));
            clickGui.drawMarqueeText(context, client.textRenderer, catName, textRenderX, textRenderY, textRenderX, headerScreenY +1, Math.max(0,availableTextWidth), Theme.CATEGORY_HEIGHT-1, Theme.TEXT_PRIMARY, marqueeState, false);
            int actualIndicatorWidth = client.textRenderer.getWidth(indicator);
            context.drawText(client.textRenderer, indicator, x + Theme.CATEGORY_WIDTH - Theme.PADDING - actualIndicatorWidth, textRenderY, Theme.TEXT_SECONDARY, true);
        }

        private int drawModulesAndSettings(DrawContext context, MinecraftClient client, int contentTopScreenY,
                                           int mouseX, int mouseY, ClickGUIScreen clickGui,
                                           int fontHeight, float frameDelta) {
            int currentElementScreenY = contentTopScreenY;
            boolean needsTopContentSeparator = !this.modules.isEmpty() || (this.expandedModule != null && !this.expandedModule.getSettings().isEmpty() && this.modules.isEmpty());
            if(needsTopContentSeparator){
                context.fill(x + 1, currentElementScreenY - 1, x + Theme.CATEGORY_WIDTH -1, currentElementScreenY, Theme.SEPARATOR);
            }

            for (int i = 0; i < this.modules.size(); i++) {
                Module currentModuleInLoop = this.modules.get(i);
                currentElementScreenY = drawModuleEntry(context, client, currentModuleInLoop, currentElementScreenY, mouseX, mouseY, fontHeight, clickGui, frameDelta);

                if (this.expandedModule == currentModuleInLoop && !this.expandedModule.getSettings().isEmpty()) {
                    context.fill(x + 1, currentElementScreenY, x + Theme.CATEGORY_WIDTH - 1, currentElementScreenY + 1, Theme.SEPARATOR);
                    currentElementScreenY += 1;
                    currentElementScreenY = drawSettingsForModule(context, client, this.expandedModule, currentElementScreenY, mouseX, mouseY, clickGui, fontHeight, frameDelta);
                }

                if (i < this.modules.size() - 1) {
                    context.fill(x + 1, currentElementScreenY, x + Theme.CATEGORY_WIDTH - 1, currentElementScreenY + 1, Theme.SEPARATOR);
                    currentElementScreenY += 1;
                }
            }
            if(this.modules.isEmpty() && this.expandedModule != null && !this.expandedModule.getSettings().isEmpty()){
                currentElementScreenY = drawSettingsForModule(context, client, this.expandedModule,
                        currentElementScreenY, mouseX, mouseY, clickGui, fontHeight, frameDelta);
            }
            return currentElementScreenY;
        }

        private int drawModuleEntry(DrawContext context, MinecraftClient client, Module module, int moduleScreenY,
                                    int mouseX, int mouseY, int fontHeight, ClickGUIScreen clickGui, float frameDelta) {
            boolean isHovered = mouseX >= x && mouseX <= x + Theme.CATEGORY_WIDTH && mouseY >= moduleScreenY && mouseY < moduleScreenY + Theme.MODULE_HEIGHT;
            int bgColor = module.isEnabled() ? Theme.MODULE_ENABLED : (isHovered ? Theme.MODULE_HOVERED : Theme.MODULE_DEFAULT);
            context.fill(x + 1, moduleScreenY, x + Theme.CATEGORY_WIDTH - 1, moduleScreenY + Theme.MODULE_HEIGHT, bgColor);
            context.fill(x, moduleScreenY, x + 1, moduleScreenY + Theme.MODULE_HEIGHT, Theme.BORDER);
            context.fill(x + Theme.CATEGORY_WIDTH - 1, moduleScreenY, x + Theme.CATEGORY_WIDTH, moduleScreenY + Theme.MODULE_HEIGHT, Theme.BORDER);
            String moduleName = module.getName(); int keybindWidth = 0; String keyName = TeamClient.getInstance().getModuleManager().getKeyName(module.getKeyBind());
            if (module.getKeyBind() != -1 && !keyName.equalsIgnoreCase("None")) keybindWidth = client.textRenderer.getWidth("[" + keyName + "]") + Theme.PADDING;
            int availableTextWidth = Theme.CATEGORY_WIDTH - (Theme.PADDING * 2) - keybindWidth;
            int textRenderX = x + Theme.PADDING; int textRenderY = moduleScreenY + (Theme.MODULE_HEIGHT - fontHeight) / 2 + 1;
            int textColor = module.isEnabled() ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            MarqueeState marqueeState = clickGui.getMarqueeState("module_name_" + module.getName() + "_" + this.hashCode());
            marqueeState.update(frameDelta, client.textRenderer.getWidth(moduleName), Math.max(1,availableTextWidth));
            clickGui.drawMarqueeText(context, client.textRenderer, moduleName, textRenderX, textRenderY, textRenderX, moduleScreenY, Math.max(0, availableTextWidth), Theme.MODULE_HEIGHT, textColor, marqueeState, false);
            drawModuleKeybind(context, client, module, moduleScreenY, fontHeight);
            return moduleScreenY + Theme.MODULE_HEIGHT;
        }

        private void drawModuleKeybind(DrawContext context, MinecraftClient client, Module module, int moduleScreenY, int fontHeight) {
            String keyName = TeamClient.getInstance().getModuleManager().getKeyName(module.getKeyBind());
            if (module.getKeyBind() != -1 && !keyName.equalsIgnoreCase("None")) {
                String keybindText = "[" + keyName + "]"; int keybindWidth = client.textRenderer.getWidth(keybindText);
                int keybindTextColor = Theme.TEXT_SECONDARY;
                context.drawText(client.textRenderer, keybindText, x + Theme.CATEGORY_WIDTH - Theme.PADDING - keybindWidth, moduleScreenY + (Theme.MODULE_HEIGHT - fontHeight) / 2 + 1, keybindTextColor, true);
            }
        }

        private int drawSettingsForModule(DrawContext context, MinecraftClient client, Module settingsOwnerModule,
                                          int settingsTopScreenY, int mouseX, int mouseY,
                                          ClickGUIScreen clickGui, int fontHeight, float frameDelta) {
            int currentSettingScreenY = settingsTopScreenY;
            List<Setting<?>> settingsToDraw = settingsOwnerModule.getSettings();
            for (int i = 0; i < settingsToDraw.size(); i++) {
                Setting<?> setting = settingsToDraw.get(i);
                context.fill(x + 1, currentSettingScreenY, x + Theme.CATEGORY_WIDTH - 1, currentSettingScreenY + Theme.SETTING_HEIGHT, Theme.SETTING_BG);
                context.fill(x, currentSettingScreenY, x + 1, currentSettingScreenY + Theme.SETTING_HEIGHT, Theme.BORDER);
                context.fill(x + Theme.CATEGORY_WIDTH - 1, currentSettingScreenY, x + Theme.CATEGORY_WIDTH, currentSettingScreenY + Theme.SETTING_HEIGHT, Theme.BORDER);
                int textRenderY = currentSettingScreenY + (Theme.SETTING_HEIGHT - fontHeight) / 2 + 1; String settingNameText = setting.getName();
                int settingNameRenderX = x + Theme.PADDING * 2; int valueSideReservedWidth; String valueStrForWidthCalc = SettingValueFormatter.format(setting);
                if (setting instanceof NumberSetting) {
                    int approxSliderWidth = Theme.CATEGORY_WIDTH / 3; int valueTextDisplayWidth = client.textRenderer.getWidth(valueStrForWidthCalc);
                    valueSideReservedWidth = approxSliderWidth + valueTextDisplayWidth + Theme.PADDING * 2;
                } else { valueSideReservedWidth = client.textRenderer.getWidth(valueStrForWidthCalc) + Theme.PADDING; }
                valueSideReservedWidth = Math.min(Theme.CATEGORY_WIDTH - Theme.PADDING * 4, valueSideReservedWidth);
                int availableTextWidth = Theme.CATEGORY_WIDTH - (Theme.PADDING * 3) - valueSideReservedWidth; availableTextWidth = Math.max(10, availableTextWidth);
                MarqueeState marqueeState = clickGui.getMarqueeState(setting);
                marqueeState.update(frameDelta, client.textRenderer.getWidth(settingNameText), Math.max(1, availableTextWidth));
                clickGui.drawMarqueeText(context, client.textRenderer, settingNameText, settingNameRenderX, textRenderY, settingNameRenderX, currentSettingScreenY, Math.max(0, availableTextWidth), Theme.SETTING_HEIGHT, Theme.TEXT_SECONDARY, marqueeState, false);
                if (setting instanceof NumberSetting) {
                    NumberSetting<?> numSet = (NumberSetting<?>) setting; String valueText = SettingValueFormatter.format(numSet); int valueTextWidth = client.textRenderer.getWidth(valueText);
                    int sliderStartX = settingNameRenderX + availableTextWidth + Theme.PADDING;
                    int sliderRenderWidth = Theme.CATEGORY_WIDTH - (sliderStartX - x) - valueTextWidth - Theme.PADDING * 2;
                    sliderRenderWidth = Math.max(20, sliderRenderWidth);
                    if (sliderStartX + sliderRenderWidth + Theme.PADDING + valueTextWidth + Theme.PADDING > x + Theme.CATEGORY_WIDTH) {
                        sliderRenderWidth = (x + Theme.CATEGORY_WIDTH - Theme.PADDING) - sliderStartX - valueTextWidth - Theme.PADDING;
                        sliderRenderWidth = Math.max(10,sliderRenderWidth);
                    }
                    int sliderBarHeight = 4; int sliderBarScreenY = currentSettingScreenY + (Theme.SETTING_HEIGHT - sliderBarHeight) / 2;
                    context.fill(sliderStartX, sliderBarScreenY, sliderStartX + sliderRenderWidth, sliderBarScreenY + sliderBarHeight, Theme.SLIDER_BG);
                    double min = ((Number)numSet.getMin()).doubleValue(); double max = ((Number)numSet.getMax()).doubleValue(); double currentVal = ((Number)numSet.getValue()).doubleValue();
                    double percent = (max - min == 0) ? 0 : MathHelper.clamp((currentVal - min) / (max - min), 0.0, 1.0);
                    int fillWidth = (int) (sliderRenderWidth * percent);
                    context.fill(sliderStartX, sliderBarScreenY, sliderStartX + fillWidth, sliderBarScreenY + sliderBarHeight, Theme.SLIDER_FILL);
                    int knobWidth = 4; int knobRenderX = sliderStartX + fillWidth - knobWidth / 2;
                    knobRenderX = MathHelper.clamp(knobRenderX, sliderStartX, sliderStartX + sliderRenderWidth - knobWidth);
                    context.fill(knobRenderX, sliderBarScreenY - 1, knobRenderX + knobWidth, sliderBarScreenY + sliderBarHeight + 1, Theme.SLIDER_KNOB);
                    context.drawText(client.textRenderer, valueText, sliderStartX + sliderRenderWidth + Theme.PADDING, textRenderY, Theme.TEXT_PRIMARY, false);
                } else {
                    String valueDisplay = SettingValueFormatter.format(setting); int valueWidth = client.textRenderer.getWidth(valueDisplay);
                    context.drawText(client.textRenderer, valueDisplay, x + Theme.CATEGORY_WIDTH - Theme.PADDING - valueWidth, textRenderY, Theme.TEXT_PRIMARY, false);
                }
                currentSettingScreenY += Theme.SETTING_HEIGHT;
                if (i < settingsToDraw.size() - 1) {
                    context.fill(x + 1, currentSettingScreenY, x + Theme.CATEGORY_WIDTH - 1, currentSettingScreenY + 1, Theme.SEPARATOR);
                    currentSettingScreenY += 1;
                }
            }
            return currentSettingScreenY;
        }


        public boolean handleMouseClick(double absMouseX, double absMouseY, int button, ClickGUIScreen clickGui, double scrollOffset) {
            MinecraftClient client = MinecraftClient.getInstance();
            double headerTopScreenY = this.y + scrollOffset; double headerBottomScreenY = headerTopScreenY + Theme.CATEGORY_HEIGHT;
            if (absMouseX >= this.x && absMouseX <= this.x + Theme.CATEGORY_WIDTH && absMouseY >= headerTopScreenY && absMouseY <= headerBottomScreenY) {
                if (button == 1) { this.expanded = !this.expanded; if (!this.expanded) this.expandedModule = null; return true; }
                return false;
            }
            if (!this.expanded) return false;
            double currentElementTopScreenY = headerBottomScreenY + 1;
            for (Module module : this.modules) {
                double moduleBottomScreenY = currentElementTopScreenY + Theme.MODULE_HEIGHT;
                if (absMouseX >= this.x && absMouseX <= this.x + Theme.CATEGORY_WIDTH && absMouseY >= currentElementTopScreenY && absMouseY < moduleBottomScreenY) {
                    return handleModuleEntryClick(module, button, clickGui);
                }
                currentElementTopScreenY = moduleBottomScreenY;
                if (this.expandedModule == module) {
                    List<Setting<?>> settings = module.getSettings();
                    if (!settings.isEmpty()) {
                        currentElementTopScreenY += 1; // Separator before settings
                        for (int i = 0; i < settings.size(); i++) {
                            Setting<?> setting = settings.get(i); double settingBottomScreenY = currentElementTopScreenY + Theme.SETTING_HEIGHT;
                            if (absMouseX >= this.x && absMouseX <= this.x + Theme.CATEGORY_WIDTH && absMouseY >= currentElementTopScreenY && absMouseY < settingBottomScreenY) {
                                if (setting instanceof NumberSetting && button == 0) {
                                    NumberSetting<?> numSet = (NumberSetting<?>) setting; String settingNameText = setting.getName(); String valueText = SettingValueFormatter.format(numSet);
                                    int valueTextWidth = client.textRenderer.getWidth(valueText); int nameTextWidth = client.textRenderer.getWidth(settingNameText);
                                    int valueSideReservedWidth = Theme.CATEGORY_WIDTH / 3 + valueTextWidth + Theme.PADDING; valueSideReservedWidth = Math.min(Theme.CATEGORY_WIDTH - (Theme.PADDING * 4), valueSideReservedWidth);
                                    int actualNameAvailableWidth = Theme.CATEGORY_WIDTH - (Theme.PADDING * 3) - valueSideReservedWidth; actualNameAvailableWidth = Math.max(10, actualNameAvailableWidth);
                                    int sliderRenderX = this.x + Theme.PADDING * 2 + actualNameAvailableWidth + Theme.PADDING;
                                    int sliderMaxPossibleWidth = (this.x + Theme.CATEGORY_WIDTH - Theme.PADDING) - sliderRenderX - valueTextWidth - Theme.PADDING;
                                    int sliderActualPixelWidth = Math.max(20, sliderMaxPossibleWidth);
                                    if (sliderRenderX + sliderActualPixelWidth + Theme.PADDING + valueTextWidth + Theme.PADDING > this.x + Theme.CATEGORY_WIDTH) {
                                        sliderActualPixelWidth = (this.x + Theme.CATEGORY_WIDTH - Theme.PADDING) - sliderRenderX - valueTextWidth - Theme.PADDING;
                                        sliderActualPixelWidth = Math.max(10, sliderActualPixelWidth);
                                    }
                                    if (absMouseX >= sliderRenderX && absMouseX <= sliderRenderX + sliderActualPixelWidth) {
                                        clickGui.startDraggingSlider(this, setting, absMouseX, sliderRenderX, sliderActualPixelWidth); return true;
                                    }
                                }
                                SettingClickHandler.handle(setting, button); return true;
                            }
                            currentElementTopScreenY = settingBottomScreenY;
                            if (i < settings.size() - 1) currentElementTopScreenY += 1;
                        }
                    }
                }
                if (modules.indexOf(module) < modules.size() - 1 || (this.expandedModule == module && !module.getSettings().isEmpty())) {
                    currentElementTopScreenY += 1;
                }
            } return false;
        }

        private boolean handleModuleEntryClick(Module module, int button, ClickGUIScreen clickGui) {
            switch (button) {
                case 0: module.toggle(); return true;
                case 1: if (!module.getSettings().isEmpty()) this.expandedModule = (this.expandedModule == module) ? null : module; return true;
                case 2: clickGui.setBindingModule(module); return true;
            } return false;
        }
    }

    private static class SettingValueFormatter {
        public static String format(Setting<?> setting) {
            if (setting instanceof BooleanSetting) return ((BooleanSetting) setting).getValue() ? "On" : "Off";
            if (setting instanceof ModeSetting) return ((ModeSetting) setting).getValue();
            if (setting instanceof NumberSetting) return formatNumber((NumberSetting<?>) setting);
            return String.valueOf(setting.getValue());
        }
        private static String formatNumber(NumberSetting<?> setting) {
            Object value = setting.getValue();
            if (value instanceof Double || value instanceof Float) {
                double valAsDouble = ((Number) value).doubleValue();
                Number stepNum = (Number)setting.getStep(); double step = stepNum != null ? stepNum.doubleValue() : 0.01;
                if (step > 0 && step % 1 == 0 && Math.abs(valAsDouble) >=1) return String.format("%.0f", valAsDouble); // Integer-like
                if (Math.abs(valAsDouble) < 0.00001 && valAsDouble != 0) return String.format("%.5f", valAsDouble); // Very small numbers
                if (step < 0.01 || (step < 0.1 && Math.abs(valAsDouble) < 10 && valAsDouble != 0)) return String.format("%.2f", valAsDouble);
                return String.format("%.1f", valAsDouble);
            } return value.toString();
        }
    }
    private static class SettingClickHandler {
        public static void handle(Setting<?> setting, int button) {
            if (button == 0) {
                if (setting instanceof BooleanSetting) ((BooleanSetting)setting).toggle();
                else if (setting instanceof ModeSetting) ((ModeSetting)setting).cycle();
            } else if (button == 1) {
                if (setting instanceof ModeSetting) {
                    ModeSetting modeSetting = (ModeSetting) setting; int currentIndex = modeSetting.getIndex();
                    int newIndex = (currentIndex - 1 + modeSetting.getModes().size()) % modeSetting.getModes().size();
                    modeSetting.setIndex(newIndex);
                }
            }
        }
    }
}