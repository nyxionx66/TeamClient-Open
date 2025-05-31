package dev.ash.team.command;

/**
 * Base class for all commands.
 * Commands are used to interact with the client via chat.
 */
public abstract class Command {
    private final String name;
    private final String description;
    private final String syntax;
    private final String[] aliases;
    
    /**
     * Creates a new command.
     * 
     * @param name The name of the command
     * @param description The description of the command
     * @param syntax The syntax of the command
     * @param aliases The aliases of the command
     */
    public Command(String name, String description, String syntax, String... aliases) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
        this.aliases = aliases;
    }
    
    /**
     * Executes the command.
     * 
     * @param args The arguments passed to the command
     * @return true if the command was executed successfully, false otherwise
     */
    public abstract boolean execute(String[] args);
    
    /**
     * Gets the name of the command.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the command.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the syntax of the command.
     * 
     * @return The syntax
     */
    public String getSyntax() {
        return syntax;
    }
    
    /**
     * Gets the aliases of the command.
     * 
     * @return The aliases
     */
    public String[] getAliases() {
        return aliases;
    }
}