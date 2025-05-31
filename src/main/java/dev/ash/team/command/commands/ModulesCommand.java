package dev.ash.team.command.commands;

import dev.ash.team.TeamClient;
import dev.ash.team.command.Command;
import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

public class ModulesCommand extends Command {
    public ModulesCommand() {
        super("modules", "Lists all available modules", ".modules [category]", "mods", "ml");
    }

    @Override
    public boolean execute(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (args.length == 0) {
            // Show all categories
            mc.player.sendMessage(Text.literal("§7--- §6Module Categories §7---"), false);
            for (ModuleCategory category : ModuleCategory.values()) {
                List<Module> modules = TeamClient.getInstance().getModuleManager().getModulesByCategory(category);
                mc.player.sendMessage(Text.literal("§6" + category.getName() + " §7(" + modules.size() + "): "
                        + formatModuleList(modules)), false);
            }
            mc.player.sendMessage(Text.literal("§7Use §6.modules <category> §7to view specific modules"), false);
            return true;
        } else {
            // Show modules in specific category
            String categoryName = args[0].toUpperCase();
            try {
                ModuleCategory category = ModuleCategory.valueOf(categoryName);
                List<Module> modules = TeamClient.getInstance().getModuleManager().getModulesByCategory(category);

                mc.player.sendMessage(Text.literal("§7--- §6" + category.getName() + " Modules §7---"), false);
                for (Module module : modules) {
                    mc.player.sendMessage(Text.literal("§6" + module.getName()
                            + " §7- " + module.getDescription()
                            + (module.isEnabled() ? " §a[ON]" : " §c[OFF]")), false);
                }
                return true;
            } catch (IllegalArgumentException e) {
                mc.player.sendMessage(Text.literal("§cInvalid category: " + categoryName), false);
                return false;
            }
        }
    }

    private String formatModuleList(List<Module> modules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            sb.append("§");
            sb.append(module.isEnabled() ? "a" : "7");
            sb.append(module.getName());
            if (i < modules.size() - 1) {
                sb.append("§7, ");
            }
        }
        return sb.toString();
    }
}