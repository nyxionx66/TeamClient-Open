package dev.ash.team.module;

/**
 * Categories for modules.
 * Each module belongs to exactly one category.
 */
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc");
    
    private final String name;
    
    ModuleCategory(String name) {
        this.name = name;
    }
    
    /**
     * Gets the display name of the category.
     * 
     * @return The display name
     */
    public String getName() {
        return name;
    }
}