package dev.ash.team.module.modules.combat;

import dev.ash.team.event.EventTarget;
import dev.ash.team.event.events.client.ClientTickEvent;
import dev.ash.team.module.Module;
import dev.ash.team.module.ModuleCategory;
import dev.ash.team.module.setting.BooleanSetting;
import dev.ash.team.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult; // For checking Block hits from raycast
import net.minecraft.util.hit.EntityHitResult; // For checking Entity hits from raycast
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {

    private final NumberSetting<Float> range = addSetting(new NumberSetting<>("Range", "Max distance", 4.5f, 1.0f, 7.0f, 0.1f));
    private final NumberSetting<Float> fov = addSetting(new NumberSetting<>("FOV", "Field of View", 90.0f, 5.0f, 180.0f, 5.0f));
    private final NumberSetting<Float> aimSpeedYaw = addSetting(new NumberSetting<>("SpeedYaw", "Horizontal aim speed %", 20.0f, 1.0f, 100.0f, 1.0f));
    private final NumberSetting<Float> aimSpeedPitch = addSetting(new NumberSetting<>("SpeedPitch", "Vertical aim speed %", 15.0f, 1.0f, 100.0f, 1.0f));
    private final BooleanSetting dynamicSpeed = addSetting(new BooleanSetting("DynamicSpeed", "Aim speed increases when further from target", true));
    private final NumberSetting<Float> maxRotationSpeed = addSetting(new NumberSetting<>("MaxRotSpeed", "Max deg/tick (0 for no limit)", 15.0f, 0.0f, 45.0f, 1.0f));

    private final BooleanSetting requireMouseDown = addSetting(new BooleanSetting("RequireClick", "Only on attack press", true));
    private final BooleanSetting targetPlayers = addSetting(new BooleanSetting("Players", "Target players", true));
    private final BooleanSetting targetMobs = addSetting(new BooleanSetting("Mobs", "Target hostile mobs", false));
    private final BooleanSetting targetAnimals = addSetting(new BooleanSetting("Animals", "Target animals", false));
    private final BooleanSetting targetInvisibles = addSetting(new BooleanSetting("Invisibles", "Target invisibles", false));
    private final BooleanSetting aimVertical = addSetting(new BooleanSetting("VerticalAim", "Assist pitch", true));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("ThroughWalls", "Target through walls (ignores raycast)", false));


    private final BooleanSetting predictMovement = addSetting(new BooleanSetting("PredictMovement", "Predict target movement", false));
    private final NumberSetting<Float> predictionFactor = addSetting(new NumberSetting<>("PredictionFactor", "How far to lead target (multiplier)", 0.15f, 0.0f, 1.0f, 0.01f));


    private Entity currentTarget = null;

    public AimAssist() {
        super("AimAssist", "Helps aiming at entities", ModuleCategory.COMBAT);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }

    @EventTarget
    public void onClientTick(ClientTickEvent.End event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) {
            currentTarget = null;
            return;
        }

        if (requireMouseDown.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            currentTarget = null;
            return;
        }

        findTarget();

        if (currentTarget != null && currentTarget.isAlive()) {
            aimAtTarget(currentTarget);
        } else {
            currentTarget = null;
        }
    }

    private void findTarget() {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerHeadPos = mc.player.getCameraPosVec(1f);

        List<LivingEntity> potentialTargets = mc.world.getEntitiesByClass(LivingEntity.class,
                mc.player.getBoundingBox().expand(range.getValue(), range.getValue(), range.getValue()),
                entity -> isValidTarget(entity, playerHeadPos)
        );

        if (potentialTargets.isEmpty()) {
            currentTarget = null;
            return;
        }

        currentTarget = potentialTargets.stream()
                .min(Comparator.comparingDouble(e -> getAngleToEntity((Entity) e, playerHeadPos, true))
                        .thenComparingDouble(e -> mc.player.squaredDistanceTo((Entity) e)))
                .orElse(null);

        if (currentTarget != null && getAngleToEntity(currentTarget, playerHeadPos, true) > fov.getValue() / 2.0f) {
            currentTarget = null;
        }
    }


    private boolean isValidTarget(Entity entity, Vec3d playerHeadPos) {
        if (entity == mc.player || !(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive()) {
            return false;
        }
        if (livingEntity.isRemoved() || livingEntity.isInvulnerable() || (livingEntity.isInvisible() && !targetInvisibles.getValue())) {
            return false;
        }
        // Use squaredDistanceTo for entity-entity comparison for performance,
        // but distanceTo for entity-Vec3d comparison (or manually calculate squared distance).
        if (mc.player.getPos().squaredDistanceTo(entity.getPos()) > range.getValue() * range.getValue()) {
            return false;
        }

        if (entity instanceof PlayerEntity && !targetPlayers.getValue()) return false;
        if (isHostileMob(entity) && !targetMobs.getValue()) return false;
        if (entity instanceof AnimalEntity && !isHostileMob(entity) && !targetAnimals.getValue()) return false;

        if (!throughWalls.getValue()) {
            Vec3d targetEyePos = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
            RaycastContext raycastContext = new RaycastContext(playerHeadPos, targetEyePos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player);
            HitResult hitResult = mc.world.raycast(raycastContext);

            if (hitResult.getType() != HitResult.Type.MISS) {
                // If it's an entity hit, check if it's our target entity
                if (hitResult instanceof EntityHitResult entityHitResult) {
                    if (entityHitResult.getEntity() != entity) {
                        return false; // Hit another entity first
                    }
                } else if (hitResult instanceof BlockHitResult) {
                    // If we hit a block before the target's distance, it's obstructed
                    // Use playerEyePos.squaredDistanceTo() for Vec3d comparisons for performance consistency
                    if (playerHeadPos.squaredDistanceTo(hitResult.getPos()) < playerHeadPos.squaredDistanceTo(targetEyePos) - 0.1) { // 0.1 buffer
                        return false; // Hit a block that's closer than the target
                    }
                } else {
                    // Unhandled HitResult type or some other logic might be needed if it's not Entity or Block
                    // For simplicity, if it's not MISS and not the entity itself, consider it blocked if closer
                    if (playerHeadPos.squaredDistanceTo(hitResult.getPos()) < playerHeadPos.squaredDistanceTo(targetEyePos) - 0.1){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isHostileMob(Entity entity) {
        return entity instanceof net.minecraft.entity.mob.HostileEntity ||
                (entity instanceof net.minecraft.entity.mob.SlimeEntity && !(entity instanceof net.minecraft.entity.passive.FrogEntity)) ||
                entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity ||
                entity instanceof net.minecraft.entity.boss.WitherEntity;
    }


    private double getAngleToEntity(Entity entity, Vec3d playerViewOrigin, boolean useEyeHeight) {
        Vec3d targetPos;
        if (useEyeHeight) {
            targetPos = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
        } else {
            // Correctly get center of bounding box for alternative aim point
            targetPos = entity.getBoundingBox().getCenter();
        }

        double deltaX = targetPos.x - playerViewOrigin.x;
        double deltaY = targetPos.y - playerViewOrigin.y;
        double deltaZ = targetPos.z - playerViewOrigin.z;

        double requiredYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        double distanceHorizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distanceHorizontal == 0) return 180; // Target is directly above or below, max angle difference for yaw
        double requiredPitch = -Math.toDegrees(Math.atan2(deltaY, distanceHorizontal));

        float playerCurrentYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        float playerCurrentPitch = MathHelper.wrapDegrees(mc.player.getPitch());

        double yawDiff = Math.abs(MathHelper.wrapDegrees(requiredYaw - playerCurrentYaw));
        double pitchDiff = Math.abs(MathHelper.wrapDegrees(requiredPitch - playerCurrentPitch));

        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void aimAtTarget(Entity target) {
        if (mc.player == null || !(target instanceof LivingEntity livingTarget)) return;

        Vec3d playerEyePos = mc.player.getCameraPosVec(1.0f);

        double targetX = livingTarget.getX();
        double targetY = livingTarget.getEyeY();
        double targetZ = livingTarget.getZ();

        if (predictMovement.getValue() && predictionFactor.getValue() > 0.001f) {
            Vec3d targetVelocity = livingTarget.getVelocity();
            float distanceToTarget = mc.player.distanceTo(livingTarget);
            float timeToTargetEst = distanceToTarget / 20.0f;
            float predictTicks = timeToTargetEst * 20.0f * predictionFactor.getValue();
            predictTicks = MathHelper.clamp(predictTicks, 0.5f, 5.0f);

            targetX += targetVelocity.x * predictTicks;
            targetZ += targetVelocity.z * predictTicks;
            // Less aggressive Y prediction to avoid overshooting due to gravity/jumping
            // targetY += targetVelocity.y * predictTicks * 0.3f;
        }
        Vec3d targetAimPos = new Vec3d(targetX, targetY, targetZ);

        double deltaX = targetAimPos.x - playerEyePos.x;
        double deltaY = targetAimPos.y - playerEyePos.y;
        double deltaZ = targetAimPos.z - playerEyePos.z;
        double distanceHorizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distanceHorizontal < 0.01) return; // Adjusted threshold slightly

        float targetYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, distanceHorizontal));

        float currentYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        float currentPitch = mc.player.getPitch(); // No wrap needed as it's clamped -90 to 90

        float yawDifference = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDifference = targetPitch - currentPitch;

        float yawSpeedMultiplier = aimSpeedYaw.getValue() / 100.0f;
        float pitchSpeedMultiplier = aimSpeedPitch.getValue() / 100.0f;

        if (dynamicSpeed.getValue()) {
            float angleToTargetVisual = (float) getAngleToEntity(target, playerEyePos, true);
            float fovVal = fov.getValue();

            if (angleToTargetVisual > fovVal * 0.25f) {
                float dynamicFactor = MathHelper.clamp(angleToTargetVisual / (fovVal * 0.5f), 1.0f, 2.5f);
                yawSpeedMultiplier *= dynamicFactor;
                pitchSpeedMultiplier *= dynamicFactor;
            } else if (angleToTargetVisual < fovVal * 0.05f) {
                yawSpeedMultiplier *= 0.7f;
                pitchSpeedMultiplier *= 0.7f;
            }
        }

        float yawStep = yawDifference * yawSpeedMultiplier;
        float pitchStep = pitchDifference * pitchSpeedMultiplier;

        float maxRot = maxRotationSpeed.getValue();
        if (maxRot > 0.001f) {
            yawStep = MathHelper.clamp(yawStep, -maxRot, maxRot);
            if (aimVertical.getValue()) {
                pitchStep = MathHelper.clamp(pitchStep, -maxRot, maxRot);
            }
        }

        mc.player.setYaw(currentYaw + yawStep);

        if (aimVertical.getValue()) {
            mc.player.setPitch(MathHelper.clamp(currentPitch + pitchStep, -90.0F, 90.0F));
        }
    }
}