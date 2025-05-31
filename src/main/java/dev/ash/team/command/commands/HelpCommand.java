package dev.ash.team.command.commands;

import dev.ash.team.TeamClient;
import dev.ash.team.command.Command;
import dev.ash.team.command.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Command that shows help information for all commands.
 */
public class HelpCommand extends Command {
    /**
     * Creates a new HelpCommand.
     */
    public HelpCommand() {
        super("help", "Shows help information for all commands", ".help [command]", "?");
    }

    @Override
    public boolean execute(String[] args) {
        CommandManager commandManager = TeamClient.getInstance().getCommandManager();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (args.length == 0) {
            // Show all commands
            assert mc.player != null;
            mc.player.sendMessage(Text.literal("§7--- §6Team Client Commands §7---"), false);
            for (Command command : commandManager.getCommands()) {
                mc.player.sendMessage(Text.literal("§6" + commandManager.getPrefix() + command.getName()
                        + "§7: " + command.getDescription()), false);
            }
            mc.player.sendMessage(Text.literal("§7Use §6" + commandManager.getPrefix()
                    + "help <command> §7for more information about a command"), false);
        } else {
            // Show help for a specific command
            Command command = commandManager.getCommand(args[0]);
            if (command == null) {
                assert mc.player != null;
                mc.player.sendMessage(Text.literal("§cUnknown command: " + args[0]), false);
                return false;
            }

            assert mc.player != null;
            mc.player.sendMessage(Text.literal("§7--- §6" + command.getName() + " §7---"), false);
            mc.player.sendMessage(Text.literal("§7Description: §6" + command.getDescription()), false);
            mc.player.sendMessage(Text.literal("§7Syntax: §6" + command.getSyntax()), false);

            if (command.getAliases().length > 0) {
                mc.player.sendMessage(Text.literal("§7Aliases: §6"
                        + String.join("§7, §6", command.getAliases())), false);
            }

        }
        return true;
    }
}