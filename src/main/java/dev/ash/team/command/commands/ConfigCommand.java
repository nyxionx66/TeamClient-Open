package dev.ash.team.command.commands;

import dev.ash.team.TeamClient;
import dev.ash.team.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Manages configuration files", ".config <save/load/reset> [filename]", "cfg");
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) {
            return false;
        }

        String action = args[0].toLowerCase();
        MinecraftClient mc = MinecraftClient.getInstance();

        switch (action) {
            case "save":
                if (args.length < 2) {
                    mc.player.sendMessage(Text.literal("§cPlease specify a filename"), false);
                    return false;
                }
                String saveFilename = args[1];
                TeamClient.getInstance().getConfigManager().save(saveFilename);
                mc.player.sendMessage(Text.literal("§aConfiguration saved to " + saveFilename), false);
                return true;

            case "load":
                if (args.length < 2) {
                    mc.player.sendMessage(Text.literal("§cPlease specify a filename"), false);
                    return false;
                }
                String loadFilename = args[1];

                if (TeamClient.getInstance().getConfigManager().load(loadFilename)) {
                    mc.player.sendMessage(Text.literal("§aConfiguration loaded from " + loadFilename), false);
                } else {
                    mc.player.sendMessage(Text.literal("§cFailed to load configuration: " + loadFilename), false);
                }
                return true;

            case "reset":
                TeamClient.getInstance().getConfigManager().reset();
                mc.player.sendMessage(Text.literal("§aConfiguration reset to defaults"), false);
                return true;

            default:
                return false;
        }
    }
}