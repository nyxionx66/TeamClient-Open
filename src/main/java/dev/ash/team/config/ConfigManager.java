package dev.ash.team.config;

import dev.ash.team.TeamClient;
import dev.ash.team.module.Module;
import dev.ash.team.module.setting.Setting;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;
    private final Map<String, Config<?>> configs = new HashMap<>();

    public ConfigManager() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve(TeamClient.MOD_ID).toFile();
        if (!configDir.exists() && !configDir.mkdirs()) {
            TeamClient.LOGGER.error("Failed to create config directory");
        }

        // Create modules directory
        File modulesDir = new File(configDir, "modules");
        if (!modulesDir.exists() && !modulesDir.mkdirs()) {
            TeamClient.LOGGER.error("Failed to create modules directory");
        }
    }

    public File getConfigDir() {
        return configDir;
    }

    public <T> void registerConfig(Config<T> config) {
        configs.put(config.getName(), config);
    }

    @SuppressWarnings("unchecked")
    public <T> Config<T> getConfig(String name) {
        return (Config<T>) configs.get(name);
    }

    public void save(String filename) {
        File configFile = new File(configDir, filename + ".json");

        try {
            if (!configFile.exists() && !configFile.createNewFile()) {
                TeamClient.LOGGER.error("Failed to create config file");
                return;
            }

            JsonObject root = new JsonObject();

            JsonObject modules = new JsonObject();
            for (Module module : TeamClient.getInstance().getModuleManager().getModules()) {
                JsonObject moduleObj = new JsonObject();

                moduleObj.addProperty("enabled", module.isEnabled());
                moduleObj.addProperty("keyBind", module.getKeyBind());

                JsonObject settings = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    Object value = setting.getValue();
                    if (value instanceof Boolean) {
                        settings.addProperty(setting.getName(), (Boolean) value);
                    } else if (value instanceof Number) {
                        settings.addProperty(setting.getName(), (Number) value);
                    } else if (value instanceof String) {
                        settings.addProperty(setting.getName(), (String) value);
                    }
                }

                moduleObj.add("settings", settings);
                modules.add(module.getName(), moduleObj);
            }

            root.add("modules", modules);

            JsonObject configsObj = new JsonObject();
            for (Config<?> config : configs.values()) {
                Object value = config.getValue();
                if (value instanceof Boolean) {
                    configsObj.addProperty(config.getName(), (Boolean) value);
                } else if (value instanceof Number) {
                    configsObj.addProperty(config.getName(), (Number) value);
                } else if (value instanceof String) {
                    configsObj.addProperty(config.getName(), (String) value);
                }
            }

            root.add("configs", configsObj);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }

            TeamClient.LOGGER.info("Saved config to {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            TeamClient.LOGGER.error("Failed to save config", e);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean load(String filename) {
        File configFile = new File(configDir, filename + ".json");

        if (!configFile.exists()) {
            TeamClient.LOGGER.error("Config file does not exist: {}", configFile.getAbsolutePath());
            return false;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                for (Module module : TeamClient.getInstance().getModuleManager().getModules()) {
                    if (modules.has(module.getName())) {
                        JsonObject moduleObj = modules.getAsJsonObject(module.getName());

                        if (moduleObj.has("enabled")) {
                            module.setEnabled(moduleObj.get("enabled").getAsBoolean());
                        }

                        if (moduleObj.has("keyBind")) {
                            module.setKeyBind(moduleObj.get("keyBind").getAsInt());
                        }

                        if (moduleObj.has("settings")) {
                            JsonObject settings = moduleObj.getAsJsonObject("settings");
                            for (Setting<?> setting : module.getSettings()) {
                                String name = setting.getName();
                                if (settings.has(name)) {
                                    JsonElement value = settings.get(name);
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isBoolean() && setting.getValue() instanceof Boolean) {
                                            ((Setting<Boolean>) setting).setValue(primitive.getAsBoolean());
                                        } else if (primitive.isNumber()) {
                                            if (setting.getValue() instanceof Integer) {
                                                ((Setting<Integer>) setting).setValue(primitive.getAsInt());
                                            } else if (setting.getValue() instanceof Double) {
                                                ((Setting<Double>) setting).setValue(primitive.getAsDouble());
                                            } else if (setting.getValue() instanceof Float) {
                                                ((Setting<Float>) setting).setValue(primitive.getAsFloat());
                                            }
                                        } else if (primitive.isString() && setting.getValue() instanceof String) {
                                            ((Setting<String>) setting).setValue(primitive.getAsString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (root.has("configs")) {
                JsonObject configsObj = root.getAsJsonObject("configs");
                for (Config<?> config : configs.values()) {
                    String name = config.getName();
                    if (configsObj.has(name)) {
                        JsonElement value = configsObj.get(name);
                        if (value.isJsonPrimitive()) {
                            JsonPrimitive primitive = value.getAsJsonPrimitive();
                            if (primitive.isBoolean() && config.getValue() instanceof Boolean) {
                                ((Config<Boolean>) config).setValue(primitive.getAsBoolean());
                            } else if (primitive.isNumber()) {
                                if (config.getValue() instanceof Integer) {
                                    ((Config<Integer>) config).setValue(primitive.getAsInt());
                                } else if (config.getValue() instanceof Double) {
                                    ((Config<Double>) config).setValue(primitive.getAsDouble());
                                } else if (config.getValue() instanceof Float) {
                                    ((Config<Float>) config).setValue(primitive.getAsFloat());
                                }
                            } else if (primitive.isString() && config.getValue() instanceof String) {
                                ((Config<String>) config).setValue(primitive.getAsString());
                            }
                        }
                    }
                }
            }

            TeamClient.LOGGER.info("Loaded config from {}", configFile.getAbsolutePath());
            return true;
        } catch (IOException | JsonParseException e) {
            TeamClient.LOGGER.error("Failed to load config", e);
            return false;
        }
    }

    public void reset() {
        for (Module module : TeamClient.getInstance().getModuleManager().getModules()) {
            module.setEnabled(false);
            module.setKeyBind(-1);

            for (Setting<?> setting : module.getSettings()) {
                @SuppressWarnings("unchecked")
                Setting<Object> s = (Setting<Object>) setting;
                s.setValue(s.getDefaultValue());
            }
        }

        for (Config<?> config : configs.values()) {
            @SuppressWarnings("unchecked")
            Config<Object> c = (Config<Object>) config;
            c.setValue(c.getDefaultValue());
        }
    }
}