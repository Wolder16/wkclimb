package div.wkclimb.client;

import div.wkclimb.components.ClimberComponents;
import div.wkclimb.components.Grip;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class GripClientHandler {

    private static final double MOVE_ACCEL_ONE = 0.035;
    private static final double MOVE_ACCEL_TWO = 0.055;
    private static final double MAX_SPEED = 0.7;

    public static void tickClientMovement(ClientPlayerEntity player) {
        var comp = ClimberComponents.CLIMBER.get(player);
        if (!comp.isClimberModeEnabled() || !comp.hasAnyGrip()) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        Vec3d wishDir = Vec3d.ZERO;
        float yaw = player.getYaw(1.0f);
        double forward = 0, strafe = 0;

        if (mc.options.forwardKey.isPressed()) forward += 1;
        if (mc.options.backKey.isPressed()) forward -= 1;
        if (mc.options.leftKey.isPressed()) strafe += 1;
        if (mc.options.rightKey.isPressed()) strafe -= 1;

        if (forward != 0 || strafe != 0) {
            double rad = Math.toRadians(yaw);
            double x = strafe * Math.cos(rad) - forward * Math.sin(rad);
            double z = forward * Math.cos(rad) + strafe * Math.sin(rad);
            wishDir = new Vec3d(x, 0, z).normalize();
        }

        double yBoost = 0;
        if (mc.options.jumpKey.isPressed()) yBoost += 0.25;
        if (mc.options.sneakKey.isPressed()) yBoost -= 0.2;

        int gripCount = (comp.hasGrip(Hand.MAIN_HAND) ? 1 : 0) + (comp.hasGrip(Hand.OFF_HAND) ? 1 : 0);
        double accel = gripCount >= 2 ? MOVE_ACCEL_TWO : MOVE_ACCEL_ONE;

        Vec3d vel = player.getVelocity();
        vel = vel.add(wishDir.multiply(accel));

        if (yBoost != 0) {
            vel = vel.add(0, yBoost * 0.6, 0);
        }

        double horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (horizSpeed > MAX_SPEED) {
            double scale = MAX_SPEED / horizSpeed;
            vel = new Vec3d(vel.x * scale, vel.y, vel.z * scale);
        }

        player.setVelocity(vel);

        // Client-side prediction
        applyClientPredictionConstraints(player, comp);
    }

    private static void applyClientPredictionConstraints(ClientPlayerEntity player, div.wkclimb.components.ClimberComponent comp) {
        Vec3d attachOffset = new Vec3d(0, 1.4, 0);
        Vec3d attachPos = player.getPos().add(attachOffset);

        Grip left = comp.getLeftGrip();
        Grip right = comp.getRightGrip();

        for (int i = 0; i < 2; i++) {
            if (left.isValid()) constrainToAnchor(player, attachPos, left.anchor(), left.radius());
            if (right.isValid()) constrainToAnchor(player, attachPos, right.anchor(), right.radius());
        }
    }

    private static void constrainToAnchor(ClientPlayerEntity player, Vec3d attachPos, Vec3d anchor, double radius) {
        Vec3d delta = attachPos.subtract(anchor);
        double dist = delta.length();
        if (dist > radius) {
            Vec3d correction = delta.multiply((dist - radius) / dist);
            player.setPosition(player.getPos().subtract(correction));
        }
    }
}
