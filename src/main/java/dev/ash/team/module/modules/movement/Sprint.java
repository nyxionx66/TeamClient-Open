package dev.ash.team.module.modules.movement;

import dev.ash.team.event.EventTarget;
import dev.ash.team.event.events.client.ClientTickEvent;
import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import dev.ash.team.module.setting.BooleanSetting;
import dev.ash.team.module.setting.ModeSetting;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Module that automatically sprints when moving.
 */
public class Sprint extends Module {
    private final ModeSetting sprintMode = addSetting(new ModeSetting("Mode", "Sprint mode", "Toggle", "Toggle", "Hold"));
    private final BooleanSetting multiDirection = addSetting(new BooleanSetting("MultiDirection", "Sprint in all directions", false));
    
    /**
     * Creates a new Sprint module.
     */
    public Sprint() {
        super("Sprint", "Automatically sprint when moving", ModuleCategory.MOVEMENT);
    }
    
    @Override
    @EventTarget
    public void onTick(ClientTickEvent event) {
        if (mc.player == null) {
            return;
        }
        
        ClientPlayerEntity player = mc.player;
        
        boolean shouldSprint = shouldSprint(player);
        
        if (shouldSprint && !player.isSprinting() && player.getHungerManager().getFoodLevel() > 6) {
            player.setSprinting(true);
        }
    }
    
    /**
     * Checks if the player should be sprinting.
     * 
     * @param player The player
     * @return true if the player should be sprinting, false otherwise
     */
    private boolean shouldSprint(ClientPlayerEntity player) {
        if (player.isUsingItem()) {
            return false;
        }
        
        if (player.isSneaking()) {
            return false;
        }
        
        if (player.horizontalCollision) {
            return false;
        }
        
        boolean isMoving = player.forwardSpeed > 0;
        
        if (multiDirection.getValue()) {
            isMoving = player.forwardSpeed != 0 || player.sidewaysSpeed != 0;
        }
        
        boolean holdModeSprinting = sprintMode.getValue().equals("Hold") && mc.options.sprintKey.isPressed();
        boolean toggleModeSprinting = sprintMode.getValue().equals("Toggle");
        
        return isMoving && (holdModeSprinting || toggleModeSprinting);
    }
}