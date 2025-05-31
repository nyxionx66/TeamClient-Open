package dev.ash.team.command.commands;

import dev.ash.team.TeamClient;
import dev.ash.team.command.Command;
import dev.ash.team.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class KeybindCommand extends Command {
    public KeybindCommand() {
        super("keybind", "Sets a module's keybind", ".keybind <module> <key>", "bind", "kb");
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length != 2) {
            return false;
        }

        String moduleName = args[0];
        String keyName = args[1].toUpperCase();

        Module module = TeamClient.getInstance().getModuleManager().getModule(moduleName);
        if (module == null) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cModule not found: " + moduleName), false);
            return false;
        }

        int keyCode = TeamClient.getInstance().getModuleManager().getKeyCode(keyName);
        if (keyCode == -1 && !keyName.equalsIgnoreCase("NONE")) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cInvalid key: " + keyName), false);
            return false;
        }

        module.setKeyBind(keyCode);

        String keyDisplay = TeamClient.getInstance().getModuleManager().getKeyName(keyCode);
        MinecraftClient.getInstance().player.sendMessage(
                Text.literal("§7Set keybind for §6" + module.getName() + " §7to §6" + keyDisplay), false);

        return true;
    }
}