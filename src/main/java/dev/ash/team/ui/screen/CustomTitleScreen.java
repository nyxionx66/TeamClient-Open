package dev.ash.team.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
// Make sure TeamClient is imported if you use it for title/version
import dev.ash.team.TeamClient;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomTitleScreen extends Screen {
    private static final Random RANDOM = new Random();
    private List<FloatingParticle> particles; // Initialized in init
    private List<ModernButton> modernButtons = new ArrayList<>();
    private int tickCounter = 0;
    private int hoveredButtonIndex = -1; // Renamed from hoveredButton for clarity

    private final Screen parentScreen;


    // Default constructor, potentially called by Minecraft if it tries to create the screen without a parent
    public CustomTitleScreen() {
        this(null); // Delegate to the constructor that takes a parent
    }

    public CustomTitleScreen(Screen parent) {
        super(Text.literal("TeamClient Main Menu")); // This title is mostly for internal use/logging
        this.parentScreen = parent;
        // Particles and buttons are initialized in init() because width/height are needed
    }

    private void initializeParticles() {
        if (this.particles == null) {
            this.particles = new ArrayList<>();
        } else {
            this.particles.clear();
        }

        if (this.width <= 0 || this.height <= 0) return; // Screen not sized yet

        // Responsive particle count
        int particleCount = MathHelper.clamp((this.width * this.height) / 18000, 30, 70);
        for (int i = 0; i < particleCount; i++) {
            particles.add(new FloatingParticle(
                    RANDOM.nextFloat() * this.width,
                    RANDOM.nextFloat() * this.height,
                    (RANDOM.nextFloat() - 0.5f) * 1.0f, // Slower particle velocities
                    (RANDOM.nextFloat() - 0.5f) * 1.0f,
                    RANDOM.nextFloat() * 0.4f + 0.6f  // Brightness 0.6 to 1.0
            ));
        }
    }

    @Override
    protected void init() {
        super.init(); // Sets up this.client, this.textRenderer, this.width, this.height

        initializeParticles(); // Initialize particles after width/height are known

        this.clearChildren(); // Important to remove any vanilla/Screen buttons
        modernButtons.clear();

        // --- Improved Responsive Button Sizing ---
        int baseButtonWidth = 200; // Base width for a common resolution (e.g., 1080p width)
        int baseButtonHeight = 25; // Base height

        // Scale button size based on screen width, but with min/max caps
        float widthScaleFactor = Math.min(1.5f, Math.max(0.8f, (float)this.width / 1280.0f)); // Scale around 1280 width

        int buttonWidth = (int) (baseButtonWidth * widthScaleFactor);
        int buttonHeight = (int) (baseButtonHeight * widthScaleFactor * 0.8f); // Height can scale slightly less

        // Ensure minimum and maximum reasonable sizes
        buttonWidth = MathHelper.clamp(buttonWidth, 140, 280);
        buttonHeight = MathHelper.clamp(buttonHeight, 22, 40);

        int centerX = this.width / 2 - buttonWidth / 2;

        // Vertical positioning: try to center the block of buttons more effectively
        int numButtons = 4;
        int buttonSpacing = Math.max(10, buttonHeight / 2); // Spacing relative to button height
        int totalButtonBlockHeight = (buttonHeight * numButtons) + (buttonSpacing * (numButtons - 1));
        int startY = this.height / 2 - totalButtonBlockHeight / 2 + buttonHeight / 2; // Start a bit lower for title space


        modernButtons.add(new ModernButton(centerX, startY, buttonWidth, buttonHeight,
                "SINGLEPLAYER", 0xFF4A9EFF, () -> {
            if (this.client != null) this.client.setScreen(new SelectWorldScreen(this));
        }));

        startY += buttonHeight + buttonSpacing;
        modernButtons.add(new ModernButton(centerX, startY, buttonWidth, buttonHeight,
                "MULTIPLAYER", 0xFF00D4FF, () -> {
            if (this.client != null) this.client.setScreen(new MultiplayerScreen(this));
        }));

        startY += buttonHeight + buttonSpacing;
        modernButtons.add(new ModernButton(centerX, startY, buttonWidth, buttonHeight,
                "OPTIONS", 0xFF7B68EE, () -> {
            if (this.client != null && this.client.options != null)
                this.client.setScreen(new OptionsScreen(this, this.client.options));
        }));

        startY += buttonHeight + buttonSpacing;
        modernButtons.add(new ModernButton(centerX, startY, buttonWidth, buttonHeight,
                "QUIT GAME", 0xFFFF6B6B, () -> {
            if (this.client != null) this.client.scheduleStop();
        }));

        for (ModernButton modernBtn : modernButtons) {
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(""), // Invisible vanilla button
                            (vanillaButton) -> modernBtn.action.run()
                    )
                    .dimensions(modernBtn.x, modernBtn.y, modernBtn.width, modernBtn.height)
                    .build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;

        for (FloatingParticle particle : particles) {
            particle.tick(this.width, this.height);
        }

        int maxParticles = MathHelper.clamp((this.width * this.height) / 15000, 40, 100);
        if (tickCounter % 30 == 0 && particles.size() < maxParticles) {
            if (this.width > 0 && this.height > 0) {
                particles.add(new FloatingParticle(
                        RANDOM.nextFloat() * this.width,
                        RANDOM.nextFloat() * this.height,
                        (RANDOM.nextFloat() - 0.5f) * 1.0f,
                        (RANDOM.nextFloat() - 0.5f) * 1.0f,
                        RANDOM.nextFloat() * 0.4f + 0.6f
                ));
            }
        }

        for (ModernButton button : modernButtons) {
            button.tick();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client == null || this.textRenderer == null) return;

        renderGradientBackground(context);
        renderParticles(context, delta);

        hoveredButtonIndex = -1;
        for (int i = 0; i < modernButtons.size(); i++) {
            ModernButton button = modernButtons.get(i);
            if (button.isMouseOver(mouseX, mouseY)) {
                hoveredButtonIndex = i;
                button.setHovered(true);
            } else {
                button.setHovered(false);
            }
        }

        renderModernTitle(context);
        renderModernButtons(context, delta);
        renderVersionInfo(context);
    }

    private void renderGradientBackground(DrawContext context) {
        context.fillGradient(0, 0, this.width, this.height, 0xFF0A0A1A, 0xFF141424); // Slightly different dark gradient

        int patternSize = MathHelper.clamp(this.width / 12, 60, 120); // Responsive pattern size
        for (int x = 0; x < this.width + patternSize; x += patternSize) { // Extend beyond width/height for seamless tile
            for (int y = 0; y < this.height + patternSize; y += patternSize) {
                float timeOffset = tickCounter * 0.003f;
                // Using a combination of sin waves for a more complex, flowing pattern
                float alphaFactor1 = (float) (Math.sin(x * 0.008f + timeOffset + y * 0.003f) * 0.5 + 0.5);
                float alphaFactor2 = (float) (Math.cos(y * 0.008f - timeOffset + x * 0.002f) * 0.5 + 0.5);
                float combinedAlphaFactor = (alphaFactor1 * alphaFactor2); // Multiply for more variation

                int baseAlpha = 12; // Very subtle pattern
                int overlayColor = ((int) (baseAlpha * combinedAlphaFactor) << 24) | 0x252545; // Bluish/purplish overlay
                context.fill(x - patternSize / 2, y - patternSize / 2, x + patternSize / 2, y + patternSize/2, overlayColor);
            }
        }
    }

    private void renderParticles(DrawContext context, float delta) {
        for (FloatingParticle particle : particles) {
            float renderX = particle.prevX + (particle.x - particle.prevX) * delta;
            float renderY = particle.prevY + (particle.y - particle.prevY) * delta;

            // Particle color changes slowly over time rather than just position
            float hue = (float) ((tickCounter * 0.1f + particle.initialHueOffset) % 360) / 360f;
            int color = hsvToRgb(hue, 0.5f, particle.brightness * particle.alpha); // Slightly less saturated

            drawGlowingDot(context, (int) renderX, (int) renderY, color, particle.size * 0.7f);
        }
    }

    private void renderModernTitle(DrawContext context) {
        String title = TeamClient.CLIENT_NAME.toUpperCase(); // Use TeamClient constant
        int titleYPosition = this.height / 6; // Higher title

        MatrixStack matrices = context.getMatrices();
        matrices.push();

        float baseScale = this.width / 400f; // Scale based on width
        float scale = MathHelper.clamp(baseScale, 2.5f, 5.0f); // Clamp scale

        matrices.translate(this.width / 2f, titleYPosition, 0);
        matrices.scale(scale, scale, 1f);

        int titleWidth = this.textRenderer.getWidth(title);
        float time = tickCounter * 0.04f; // Slower glow animation
        float glowAmount = (float) (Math.sin(time) * 0.5 + 0.5); // Pulsates 0 to 1

        // Shadow for depth
        context.drawText(this.textRenderer, title, (int) (-titleWidth / 2 + 1.5f/scale), (int) (1.5f/scale), 0x40000000, false);


        // Main Title Text with inner "glow" by drawing a slightly darker version underneath
        // context.drawText(this.textRenderer, title, -titleWidth / 2, 0, applyGlowToColor(0xFFE0E0FF, glowAmount * 0.3f), false); // Main light text
        context.drawText(this.textRenderer, title, -titleWidth / 2, 0, 0xFFFFFFFF, false); // Solid white text


        // Subtle pulsing outline using accent color (or white)
        int outlineBaseColor = 0xFFCCCCCC; // Light grey outline
        int outlineFinalColor = applyGlowToColor(outlineBaseColor, glowAmount * 0.6f); // Pulsing alpha for outline
        float outlineOffset = 0.5f / scale * (1.0f + glowAmount * 0.5f); // Pulsing offset for outline

        context.drawText(this.textRenderer, title, (int) (-titleWidth / 2 - outlineOffset), 0, outlineFinalColor, false);
        context.drawText(this.textRenderer, title, (int) (-titleWidth / 2 + outlineOffset), 0, outlineFinalColor, false);
        context.drawText(this.textRenderer, title, -titleWidth / 2, (int) -outlineOffset, outlineFinalColor, false);
        context.drawText(this.textRenderer, title, -titleWidth / 2, (int) +outlineOffset, outlineFinalColor, false);


        matrices.pop();

        String subtitle = "Modern Gaming Experience";
        float subtitleScale = MathHelper.clamp(this.width / 900f, 0.8f, 1.2f);
        int subtitleWidth = this.textRenderer.getWidth(subtitle);

        matrices.push();
        matrices.translate(this.width/2f, titleYPosition + (this.textRenderer.fontHeight * scale * 0.5f) + 15 * subtitleScale, 0);
        matrices.scale(subtitleScale, subtitleScale, 1f);
        context.drawTextWithShadow(this.textRenderer, subtitle, -subtitleWidth/2, 0, 0xA0A0B0C0); // Lighter subtitle
        matrices.pop();
    }

    private int applyGlowToColor(int baseColor, float glowFactor){
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int alpha = (baseColor >> 24) & 0xFF;
        if(alpha == 0 && (r !=0 || g !=0 || b != 0) ) alpha = 255; // If opaque but alpha is 0 assume 255

        // For an additive-like glow, we might want to increase brightness or change color slightly
        // Here, just modulating alpha for simplicity with glowFactor
        return ((int)(alpha * glowFactor) << 24) | (r << 16) | (g << 8) | b;
    }


    private void renderModernButtons(DrawContext context, float delta) {
        for (int i = 0; i < modernButtons.size(); i++) {
            ModernButton button = modernButtons.get(i);
            renderModernButton(context, button, delta, i == hoveredButtonIndex);
        }
    }

    private void renderModernButton(DrawContext context, ModernButton button, float delta, boolean isHoveredExplicitly) {
        button.setHovered(isHoveredExplicitly);
        button.tick();

        float hoverProgress = button.getHoverProgress();
        float scaleEffect = 1.0f - hoverProgress * 0.02f; // Slight shrink on hover
        float textOffsetY = hoverProgress * -0.5f;      // Slight text lift

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(button.x + button.width / 2f, button.y + button.height / 2f, 0);
        matrices.scale(scaleEffect, scaleEffect, 1f);
        matrices.translate(-button.width / 2f, -button.height / 2f, 0);

        // Button Base
        int baseAlpha = 100 + (int) (hoverProgress * 70); // More opaque on hover: 100 -> 170
        int baseButtonColor = (baseAlpha << 24) | (button.baseColor & 0x00FFFFFF);
        context.fill(0, 0, button.width, button.height, baseButtonColor);

        // Accent line at the bottom that animates in width and brightness
        float lineProgress = ModernButton.easeOutCubic(hoverProgress); // Eased progress
        int accentLineWidth = (int) (button.width * lineProgress);
        // Make accent line brighter/more opaque as it appears
        int accentLineAlpha = (int) (255 * lineProgress);
        int accentLineColor = (accentLineAlpha << 24) | (button.accentColor & 0x00FFFFFF);

        // Center the animating line
        int lineStartX = (button.width - accentLineWidth) / 2;
        context.fill(lineStartX, button.height - 2, lineStartX + accentLineWidth, button.height, accentLineColor);


        String text = button.text;
        // Scale text slightly with button hover effect
        matrices.push();
        float textScaleFactor = 1.0f + hoverProgress * 0.03f;
        int textRenderWidth = this.textRenderer.getWidth(text);
        matrices.translate(button.width / 2f, (button.height / 2f) + textOffsetY, 0);
        matrices.scale(textScaleFactor, textScaleFactor, 1f);
        // Draw text centered after scaling
        context.drawTextWithShadow(this.textRenderer, text, -textRenderWidth / 2, -this.textRenderer.fontHeight / 2, 0xFFE0E0E0);
        matrices.pop(); // Pop text scale

        matrices.pop(); // Pop button scale
    }


    private void renderVersionInfo(DrawContext context) {
        // Use TeamClient constants for version info
        String versionText = TeamClient.CLIENT_NAME + " v" + TeamClient.CLIENT_VERSION;
        String mcVersionText = "Minecraft 1.20.5 (Fabric)"; // Or get dynamically if needed

        int versionY = this.height - this.textRenderer.fontHeight * 2 - 10;
        int mcVersionY = this.height - this.textRenderer.fontHeight - 8;

        context.drawTextWithShadow(this.textRenderer, versionText, 8, versionY, 0xFF707070);
        context.drawTextWithShadow(this.textRenderer, mcVersionText, 8, mcVersionY, 0xFF606060);
    }

    private void drawGlowingDot(DrawContext context, int x, int y, int baseColor, float coreSize) {
        int glowRadius = (int) (coreSize * 2.0f); // Reduced glow radius

        for (int i = glowRadius; i > 0; i -= 1) {
            float progress = (float) i / glowRadius;
            int alpha = (int) (Math.pow(1.0f - progress, 1.5) * 50); // Alpha falloff quadratic, less intense
            if (alpha <= 2) continue;
            int glowColor = (alpha << 24) | (baseColor & 0x00FFFFFF);
            context.fill(x - i, y - i, x + i + 1, y + i + 1, glowColor); // Simple square glow
        }

        int coreDrawSize = Math.max(1, (int) coreSize);
        int coreFinalColor = (0xFF << 24) | (baseColor & 0x00FFFFFF); // Full alpha for core
        context.fill(x - coreDrawSize / 2, y - coreDrawSize / 2,
                x + (coreDrawSize + 1) / 2, y + (coreDrawSize + 1) / 2, // Centering for odd/even
                coreFinalColor);
    }

    private int hsvToRgb(float h, float s, float v) {
        h = MathHelper.clamp(h, 0f, 1f);
        s = MathHelper.clamp(s, 0f, 1f);
        v = MathHelper.clamp(v, 0f, 1f);
        if (s == 0.0f) {
            int grey = (int) (v * 255.0f);
            return (0xFF << 24) | (grey << 16) | (grey << 8) | grey;
        }
        float hueSector = h * 6.0f;
        int sectorIndex = (int) Math.floor(hueSector);
        float fractionalSector = hueSector - sectorIndex;
        float p = v * (1.0f - s);
        float q = v * (1.0f - fractionalSector * s);
        float t = v * (1.0f - (1.0f - fractionalSector) * s);
        float r, g, b;
        switch (sectorIndex % 6) {
            default: case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
        }
        return (0xFF << 24) | ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonType) {
        if (buttonType == 0) { // Left click
            for (int i = 0; i < modernButtons.size(); i++) {
                ModernButton btn = modernButtons.get(i);
                if (btn.isMouseOver(mouseX, mouseY)) {
                    btn.action.run();
                    if (this.client != null && this.client.getSoundManager() != null) {
                        // Play sound via Screen.playSound if possible, or SoundManager
                        // For simplicity, this is omitted but you'd use client.getSoundManager().play(...)
                    }
                    return true; // Click was handled
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, buttonType); // Allow other handling if no custom button hit
    }

    @Override
    public void close() {
        if(this.client != null){
            if (this.parentScreen != null) {
                this.client.setScreen(this.parentScreen);
            }
            // No super.close() or mc.scheduleStop() here to prevent accidental game exit from main menu
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Main menu usually doesn't close on Esc.
    }


    private static class FloatingParticle {
        float x, y, prevX, prevY, vx, vy, size, brightness, alpha;
        float lifetime, maxLifetime;
        float initialHueOffset; // To give each particle a slightly different starting color phase

        FloatingParticle(float x, float y, float vx, float vy, float brightness) {
            this.x = x;
            this.y = y;
            this.prevX = x;
            this.prevY = y;
            this.vx = vx;
            this.vy = vy;
            this.size = RANDOM.nextFloat() * 1.8f + 0.7f; // Range 0.7 to 2.5
            this.brightness = MathHelper.clamp(brightness, 0.55f, 0.9f); // Brighter particles overall
            this.alpha = 0f;
            this.maxLifetime = RANDOM.nextFloat() * 9f + 8f; // Lifetime 8-17 seconds
            this.lifetime = RANDOM.nextFloat() * maxLifetime; // Start at a random point in its lifecycle for variety
            this.initialHueOffset = RANDOM.nextFloat() * 360f; // Random starting hue offset
        }

        void tick(int screenWidth, int screenHeight) {
            prevX = x;
            prevY = y;

            x += vx * 0.45f; // Fine-tuned speed factor
            y += vy * 0.45f;

            vx += (RANDOM.nextFloat() - 0.5f) * 0.08f; // More subtle drift
            vy += (RANDOM.nextFloat() - 0.5f) * 0.08f;

            vx = MathHelper.clamp(vx, -1.2f, 1.2f);
            vy = MathHelper.clamp(vy, -1.2f, 1.2f);

            lifetime += 1f / 20f; // Assume 20 TPS update rate

            float lifeRatio = lifetime / maxLifetime;
            if (lifeRatio < 0.2f) {
                alpha = MathHelper.lerp(lifeRatio / 0.2f, 0f, 1f);
            } else if (lifeRatio > 0.8f) {
                alpha = MathHelper.lerp((lifeRatio - 0.8f) / 0.2f, 1f, 0f);
            } else {
                alpha = 1f;
            }
            // Max alpha of particles reduced for a more "ethereal" background look
            alpha = MathHelper.clamp(alpha * 0.6f, 0f, 0.6f);


            float wrapOffset = size * 2;
            if (vx > 0 && x > screenWidth + wrapOffset) x = -wrapOffset;
            else if (vx < 0 && x < -wrapOffset) x = screenWidth + wrapOffset;
            if (vy > 0 && y > screenHeight + wrapOffset) y = -wrapOffset;
            else if (vy < 0 && y < -wrapOffset) y = screenHeight + wrapOffset;

            if (lifetime >= maxLifetime) {
                int edge = RANDOM.nextInt(4);
                float spawnPosOffset = size * 2 + RANDOM.nextInt(10);
                switch (edge) {
                    case 0: x = RANDOM.nextFloat() * screenWidth; y = -spawnPosOffset; break;
                    case 1: x = RANDOM.nextFloat() * screenWidth; y = screenHeight + spawnPosOffset; break;
                    case 2: x = -spawnPosOffset; y = RANDOM.nextFloat() * screenHeight; break;
                    case 3: x = screenWidth + spawnPosOffset; y = RANDOM.nextFloat() * screenHeight; break;
                }
                this.prevX = x;
                this.prevY = y;
                this.lifetime = 0;
                this.alpha = 0;
                this.brightness = RANDOM.nextFloat() * 0.4f + 0.6f;
                this.initialHueOffset = RANDOM.nextFloat() * 360f;
            }
        }
    }

    private static class ModernButton {
        int x, y, width, height;
        int baseColor;
        int accentColor;
        String text;
        Runnable action;
        private boolean hovered = false;
        private float hoverProgress = 0f;

        ModernButton(int x, int y, int width, int height, String text, int accentColor, Runnable action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.baseColor = 0x1A1C26; // A dark, slightly bluish base
            this.accentColor = accentColor; // Accent color for hover effects
            this.action = action;
        }

        void tick() {
            if (hovered) {
                hoverProgress = Math.min(1f, hoverProgress + 0.12f); // Slightly faster hover-in
            } else {
                hoverProgress = Math.max(0f, hoverProgress - 0.09f); // Slightly faster hover-out
            }
        }

        void setHovered(boolean hovered) {
            this.hovered = hovered;
        }

        float getHoverProgress() {
            return easeOutCubic(hoverProgress); // Apply easing here for all calculations based on it
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private static float easeOutCubic(float x) {
            return (float) (1 - Math.pow(1 - x, 3));
        }
    }
}