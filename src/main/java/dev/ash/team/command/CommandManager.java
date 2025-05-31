package dev.ash.team.command;

import dev.ash.team.TeamClient;
import dev.ash.team.command.commands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private final String prefix = ".";

    public CommandManager() {
        registerCommands();
    }

    private void registerCommands() {
        registerCommand(new HelpCommand());
        registerCommand(new ToggleCommand());
        registerCommand(new ModulesCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new KeybindCommand());

        TeamClient.LOGGER.info("Registered {} commands", commands.size());
    }

    public void registerCommand(Command command) {
        commands.add(command);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }

    public Command getCommand(String name) {
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }

            for (String alias : command.getAliases()) {
                if (alias.equalsIgnoreCase(name)) {
                    return command;
                }
            }
        }

        return null;
    }

    public boolean executeCommand(String message) {
        if (!message.startsWith(prefix)) {
            return false;
        }

        message = message.substring(prefix.length());

        String[] parts = message.split(" ");
        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        Command command = getCommand(commandName);
        if (command == null) {
            TeamClient.LOGGER.info("Unknown command: {}", commandName);
            return false;
        }

        boolean success = command.execute(args);
        if (!success) {
            TeamClient.LOGGER.info("Usage: {}", command.getSyntax());
        }

        return true;
    }
}