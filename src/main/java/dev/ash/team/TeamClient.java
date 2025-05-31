package dev.ash.team;

import dev.ash.team.command.CommandManager;
import dev.ash.team.config.ConfigManager;
import dev.ash.team.event.EventBus;
// Assuming ClientTickEvent has Start and End inner classes or Phase enum
import dev.ash.team.module.ModuleManager;
// Import your ClickGUI screen and module if you intend to open it
import dev.ash.team.module.modules.render.ClickGUI;
import dev.ash.team.ui.clickgui.ClickGUIScreen; // Assuming this is your ClickGUI screen class
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamClient implements ClientModInitializer {
    // Keep existing constants
    public static final String MOD_ID = "team-client"; // Used for resource locations, configs etc.
    public static final String MOD_NAME = "Team Client"; // User-facing name for logs etc.
    public static final String VERSION = "1.0.0"; // Your mod's actual version for `fabric.mod.json`
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    // These can be the same or different for display purposes
    public static final String CLIENT_NAME = "TeamClient"; // Name displayed in GUIs, watermarks
    public static final String CLIENT_VERSION = "Alpha 0.1.2"; // Version displayed in GUIs

    private static TeamClient INSTANCE;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private CommandManager commandManager;

    // 'mc' field is fine here, or use MinecraftClient.getInstance() directly where needed
    public final MinecraftClient mc = MinecraftClient.getInstance();

    private KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        LOGGER.info("Initializing {} v{}...", MOD_NAME, VERSION); // Use MOD_NAME and VERSION for this log

        eventBus = new EventBus();
        // ConfigManager should ideally be initialized before ModuleManager
        // if modules rely on configs during their own initialization.
        configManager = new ConfigManager();
        moduleManager = new ModuleManager(); // ModuleManager will register modules
        commandManager = new CommandManager();

        // Initialize managers that might need EventBus or other managers
        // Example: moduleManager.initSubSystems(); if needed

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".menu", // Convention: key.yourmodid.action
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category." + MOD_ID // Convention: category.yourmodid
        ));

        // This listener is now primarily for keybindings or Fabric API specific tick logic
        // if your MixinMinecraft is handling the main Start/End ClientTickEvents.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (menuKey.wasPressed()) {
                LOGGER.info("{} GUI key pressed", CLIENT_NAME); // Use CLIENT_NAME for user-facing logs

                // Ensure client is available (it should be in this callback)
                if (client != null && moduleManager != null) {
                    ClickGUI clickGuiModule = moduleManager.getModule(ClickGUI.class);
                    if (clickGuiModule != null) {
                        // Toggle behavior for ClickGUI:
                        // If already open, close it. Otherwise, open it.
                        if (client.currentScreen instanceof ClickGUIScreen) {
                            client.setScreen(null); // Close the GUI
                        } else if (client.currentScreen == null) { // Only open if no other screen is active
                            // This assumes your ClickGUI module might have a method to get its screen instance,
                            // or you create a new instance of your ClickGUIScreen here.
                            client.setScreen(new ClickGUIScreen());
                        }
                    }
                }
            }

            // IMPORTANT:
            // If your MixinMinecraft is responsible for posting ClientTickEvent.Start and ClientTickEvent.End,
            // then DO NOT post another ClientTickEvent from here.
            // This would be redundant.

            // Example: (If MixinMinecraft posts the main tick events)
            // // if (client.player != null && client.world != null) {
            // //    // NO eventBus.post(new ClientTickEvent(...)) here;
            // // }
        });

        // Load configurations after modules have been registered and potentially had their
        // default settings established, so the config can override them.
        configManager.load("default"); // Or whatever your default config name is

        LOGGER.info("{} v{} initialized successfully.", MOD_NAME, VERSION);
    }

    public static TeamClient getInstance() {
        if (INSTANCE == null) {
            // This should ideally not happen if onInitializeClient ran correctly.
            // Consider throwing an IllegalStateException or logging a critical error.
            LOGGER.error("TeamClient INSTANCE is null! Initialization might have failed or getInstance() called too early.");
            // Fallback or error handling, though normally instance is set.
            // For safety, a temporary instance could be created, but that hides the root problem.
            // INSTANCE = new TeamClient(); // Avoid doing this without understanding why it's null
        }
        return INSTANCE;
    }

    public EventBus getEventBus() {
        if (eventBus == null) {
            LOGGER.warn("EventBus accessed before initialization!");
            eventBus = new EventBus(); // Lazy init as a fallback, but investigate why it's null
        }
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        if (moduleManager == null) {
            LOGGER.warn("ModuleManager accessed before initialization!");
            moduleManager = new ModuleManager(); // Lazy init
        }
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        if (configManager == null) {
            LOGGER.warn("ConfigManager accessed before initialization!");
            configManager = new ConfigManager(); // Lazy init
        }
        return configManager;
    }

    public CommandManager getCommandManager() {
        if (commandManager == null) {
            LOGGER.warn("CommandManager accessed before initialization!");
            commandManager = new CommandManager(); // Lazy init
        }
        return commandManager;
    }
}