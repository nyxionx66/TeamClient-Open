package dev.ash.team.command.commands;

import dev.ash.team.TeamClient;
import dev.ash.team.command.Command;
import dev.ash.team.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Command that toggles a module.
 */
public class ToggleCommand extends Command {
    /**
     * Creates a new ToggleCommand.
     */
    public ToggleCommand() {
        super("toggle", "Toggles a module", ".toggle <module>", "t");
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 1) {
            return false;
        }

        String moduleName = args[0];
        Module module = TeamClient.getInstance().getModuleManager().getModule(moduleName);

        if (module == null) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cModule not found: " + moduleName), false);
            return false;
        }

        module.toggle();
        MinecraftClient.getInstance().player.sendMessage(
                Text.literal("§7Module §6" + module.getName() + " §7is now "
                        + (module.isEnabled() ? "§aenabled" : "§cdisabled")), false);

        return true;
    }
}