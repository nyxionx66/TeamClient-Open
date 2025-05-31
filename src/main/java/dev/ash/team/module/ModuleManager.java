package dev.ash.team.module;

import dev.ash.team.TeamClient;
import dev.ash.team.event.EventTarget;
import dev.ash.team.event.events.client.ClientTickEvent;
import dev.ash.team.event.events.input.KeyEvent;
import dev.ash.team.module.modules.combat.AimAssist; // Assuming you have this module
import dev.ash.team.module.modules.movement.Sprint;
import dev.ash.team.module.modules.player.FastPlace;
import dev.ash.team.module.modules.render.ClickGUI;
import dev.ash.team.module.modules.render.ArrayList; // Ensure ArrayList module is imported if not already

import dev.ash.team.ui.clickgui.ClickGUIScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class ModuleManager {
    private final Map<Class<? extends Module>, Module> modulesByClass = new HashMap<>();
    private final Map<String, Module> modulesByName = new HashMap<>();
    private final List<Module> modules = new java.util.ArrayList<>();

    public ModuleManager() {
        registerModules();
        TeamClient.getInstance().getEventBus().register(this); // Register ModuleManager itself for global events
    }

    private void registerModules() {
        // Movement
        registerModule(new Sprint());

        // Player
        registerModule(new FastPlace());

        // Combat
        registerModule(new AimAssist());


        // Render
        registerModule(new ClickGUI());
        registerModule(new ArrayList());


        // Misc, World etc. can be added similarly

        TeamClient.LOGGER.info("Registered {} modules", modules.size());
    }

    public void registerModule(Module module) {
        // Check if module with the same name already exists (case-insensitive)
        String lowerCaseName = module.getName().toLowerCase();
        if (modulesByName.containsKey(lowerCaseName)) {
            Module oldModule = modulesByName.get(lowerCaseName);
            TeamClient.LOGGER.warn("Module with name '{}' already exists. Unregistering old one: {}", module.getName(), oldModule.getClass().getSimpleName());
            unregisterModule(oldModule);
        }

        modulesByClass.put(module.getClass(), module);
        modulesByName.put(lowerCaseName, module);
        modules.add(module);

        // If the module is set to be enabled by default in its constructor, handle event bus registration
        if (module.isEnabledInitially()) { // Check a new flag
            module.setEnabledFromConstructor(true); // This will call onEnable and register
        }
    }

    public void unregisterModule(Module module) {
        if (module == null) return;

        // If module was enabled, call its onDisable to unregister from event bus
        if (module.isEnabled()) {
            module.setEnabled(false); // This will trigger onDisable
        }

        modulesByClass.remove(module.getClass());
        modulesByName.remove(module.getName().toLowerCase());
        modules.remove(module);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) modulesByClass.get(clazz);
    }

    public Module getModule(String name) {
        if (name == null) return null;
        return modulesByName.get(name.toLowerCase());
    }

    public List<Module> getModules() {
        // Return an unmodifiable list or a copy to prevent external modification
        return new java.util.ArrayList<>(modules);
    }

    public List<Module> getModulesByCategory(ModuleCategory category) {
        // Use streams for cleaner filtering
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        // Only process keybinds if no screen is open OR if the current screen is not our ClickGUI
        // (to avoid toggling modules while ClickGUI's keybind overlay is active)
        // OR if mc.currentScreen allows ingame keybinds (e.g. ChatScreen allows some)
        if (MinecraftClient.getInstance().currentScreen == null ||
                !(MinecraftClient.getInstance().currentScreen instanceof ClickGUIScreen &&
                        ((ClickGUIScreen)MinecraftClient.getInstance().currentScreen).isBindingKey())) { // Add isBindingKey() to ClickGUIScreen
            for (Module module : modules) {
                if (module.getKeyBind() == event.getKey() && module.getKeyBind() != -1) {
                    module.toggle();
                }
            }
        }
    }

    // This global onTick in ModuleManager might be redundant if modules handle their own ticks via EventBus registration
    // See previous discussion on this. If kept, it's fine. If removed, modules need to register for ClientTickEvent themselves.
    @EventTarget
    public void onTick(ClientTickEvent event) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick(event); // Call the module's specific onTick logic
            }
        }
    }


    public String getKeyName(int keyCode) {
        if (keyCode == -1 || keyCode == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE: return "ESC";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            // Add Mouse Buttons
            case GLFW.GLFW_MOUSE_BUTTON_1: return "MB1"; // Left Mouse
            case GLFW.GLFW_MOUSE_BUTTON_2: return "MB2"; // Right Mouse
            case GLFW.GLFW_MOUSE_BUTTON_3: return "MB3"; // Middle Mouse
            case GLFW.GLFW_MOUSE_BUTTON_4: return "MB4";
            case GLFW.GLFW_MOUSE_BUTTON_5: return "MB5";
            case GLFW.GLFW_MOUSE_BUTTON_6: return "MB6";
            case GLFW.GLFW_MOUSE_BUTTON_7: return "MB7";
            case GLFW.GLFW_MOUSE_BUTTON_8: return "MB8";

            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_KP_ENTER: return "NUM_ENTER";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PGDN";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            // Function keys
            default:
                if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
                    return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
                }
                // Number pad keys
                if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
                    return "NUM" + (keyCode - GLFW.GLFW_KEY_KP_0);
                }
                switch (keyCode) {
                    case GLFW.GLFW_KEY_KP_DECIMAL: return "NUM_DECIMAL";
                    case GLFW.GLFW_KEY_KP_DIVIDE: return "NUM_DIVIDE";
                    case GLFW.GLFW_KEY_KP_MULTIPLY: return "NUM_MULTIPLY";
                    case GLFW.GLFW_KEY_KP_SUBTRACT: return "NUM_SUBTRACT";
                    case GLFW.GLFW_KEY_KP_ADD: return "NUM_ADD";
                    case GLFW.GLFW_KEY_KP_EQUAL: return "NUM_EQUAL";
                }

                String keyName = GLFW.glfwGetKeyName(keyCode, 0); // Use 0 for scancode when it's not known/relevant
                // GLFW.glfwGetKeyScancode(keyCode) is for physical key location, not character
                if (keyName != null) {
                    return keyName.toUpperCase();
                }
                // Fallback for other keys if glfwGetKeyName returns null
                return "KEY_" + keyCode;
        }
    }

    public int getKeyCode(String keyName) {
        if (keyName == null || keyName.equalsIgnoreCase("NONE")) return -1;
        keyName = keyName.toUpperCase(); // Standardize for comparison

        switch (keyName) {
            case "ESC": return GLFW.GLFW_KEY_ESCAPE;
            case "TAB": return GLFW.GLFW_KEY_TAB;
            // Mouse buttons
            case "MB1": case "MOUSE0": case "LMB": return GLFW.GLFW_MOUSE_BUTTON_1;
            case "MB2": case "MOUSE1": case "RMB": return GLFW.GLFW_MOUSE_BUTTON_2;
            case "MB3": case "MOUSE2": case "MMB": return GLFW.GLFW_MOUSE_BUTTON_3;
            case "MB4": case "MOUSE3": return GLFW.GLFW_MOUSE_BUTTON_4;
            case "MB5": case "MOUSE4": return GLFW.GLFW_MOUSE_BUTTON_5;
            // ... add MB6, MB7, MB8 if needed

            case "LCTRL": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT": return GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT": return GLFW.GLFW_KEY_RIGHT_ALT;
            case "LSHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "ENTER": return GLFW.GLFW_KEY_ENTER;
            case "NUM_ENTER": return GLFW.GLFW_KEY_KP_ENTER;
            case "BACKSPACE": return GLFW.GLFW_KEY_BACKSPACE;
            case "DELETE": return GLFW.GLFW_KEY_DELETE;
            case "INSERT": return GLFW.GLFW_KEY_INSERT;
            case "HOME": return GLFW.GLFW_KEY_HOME;
            case "END": return GLFW.GLFW_KEY_END;
            case "PGUP": return GLFW.GLFW_KEY_PAGE_UP;
            case "PGDN": return GLFW.GLFW_KEY_PAGE_DOWN;
            case "CAPS": return GLFW.GLFW_KEY_CAPS_LOCK;
            case "SPACE": return GLFW.GLFW_KEY_SPACE;
            case "LEFT": return GLFW.GLFW_KEY_LEFT;
            case "RIGHT": return GLFW.GLFW_KEY_RIGHT;
            case "UP": return GLFW.GLFW_KEY_UP;
            case "DOWN": return GLFW.GLFW_KEY_DOWN;
            // Numpad numbers
            case "NUM0": return GLFW.GLFW_KEY_KP_0; case "NUM1": return GLFW.GLFW_KEY_KP_1;
            case "NUM2": return GLFW.GLFW_KEY_KP_2; case "NUM3": return GLFW.GLFW_KEY_KP_3;
            case "NUM4": return GLFW.GLFW_KEY_KP_4; case "NUM5": return GLFW.GLFW_KEY_KP_5;
            case "NUM6": return GLFW.GLFW_KEY_KP_6; case "NUM7": return GLFW.GLFW_KEY_KP_7;
            case "NUM8": return GLFW.GLFW_KEY_KP_8; case "NUM9": return GLFW.GLFW_KEY_KP_9;
            case "NUM_DECIMAL": return GLFW.GLFW_KEY_KP_DECIMAL;
            case "NUM_DIVIDE": return GLFW.GLFW_KEY_KP_DIVIDE;
            case "NUM_MULTIPLY": return GLFW.GLFW_KEY_KP_MULTIPLY;
            case "NUM_SUBTRACT": return GLFW.GLFW_KEY_KP_SUBTRACT;
            case "NUM_ADD": return GLFW.GLFW_KEY_KP_ADD;
            case "NUM_EQUAL": return GLFW.GLFW_KEY_KP_EQUAL;
            default:
                if (keyName.matches("F[1-9][0-9]?")) { // F1-F25
                    try {
                        int fNumber = Integer.parseInt(keyName.substring(1));
                        if (fNumber >= 1 && fNumber <= 25) {
                            return GLFW.GLFW_KEY_F1 + (fNumber - 1);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (keyName.length() == 1) { // Single characters A-Z, 0-9
                    char ch = keyName.charAt(0);
                    if (ch >= 'A' && ch <= 'Z') return GLFW.GLFW_KEY_A + (ch - 'A');
                    if (ch >= '0' && ch <= '9') return GLFW.GLFW_KEY_0 + (ch - '0');
                }
                if (keyName.startsWith("KEY_")) { // For fallback "KEY_XXX" names
                    try {
                        return Integer.parseInt(keyName.substring(4));
                    } catch (NumberFormatException ignored) {}
                }
                // Add more mappings if GLFW.glfwGetKeyName produces other common names
                // This is harder because glfwGetKeyName is layout-dependent for non-alphanumeric keys
                // For robust reverse mapping, one might need to iterate all GLFW_KEY_* constants
                // and check glfwGetKeyName against the input.
                return GLFW.GLFW_KEY_UNKNOWN; // Or -1 if unknown
        }
    }
}