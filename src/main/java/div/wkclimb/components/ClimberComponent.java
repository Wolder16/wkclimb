package div.wkclimb.components;

import div.wkclimb.WKClimb;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class ClimberComponent implements ComponentV3, AutoSyncedComponent {
    private final PlayerEntity provider;

    private boolean climberModeEnabled = false;
    private Grip leftGrip = Grip.EMPTY;
    private Grip rightGrip = Grip.EMPTY;
    private int regrabCooldownTicks = 0;

    public ClimberComponent(PlayerEntity provider) {
        this.provider = provider;
    }

    public boolean isClimberModeEnabled() {
        return climberModeEnabled;
    }

    public void setClimberModeEnabled(boolean enabled) {
        this.climberModeEnabled = enabled;
        if (!enabled) {
            releaseAllGrips();
        }
        markDirty();
    }

    public Grip getLeftGrip() {
        return leftGrip;
    }

    public Grip getRightGrip() {
        return rightGrip;
    }

    public boolean hasAnyGrip() {
        return leftGrip.isValid() || rightGrip.isValid();
    }

    public boolean hasGrip(Hand hand) {
        return (hand == Hand.MAIN_HAND ? leftGrip : rightGrip).isValid();
    }

    public Grip getGrip(Hand hand) {
        return hand == Hand.MAIN_HAND ? leftGrip : rightGrip;
    }

    public void toggleGrip(Hand hand, PlayerEntity player) {
        if (!climberModeEnabled) return;

        Grip current = getGrip(hand);
        if (current.isValid()) {
            releaseGrip(hand);
        } else {
            attemptGrab(hand, player);
        }
    }

    private void releaseGrip(Hand hand) {
        if (hand == Hand.MAIN_HAND) leftGrip = Grip.EMPTY;
        else rightGrip = Grip.EMPTY;
        markDirty();
    }

    private void releaseAllGrips() {
        leftGrip = Grip.EMPTY;
        rightGrip = Grip.EMPTY;
        markDirty();
    }

    private void attemptGrab(Hand hand, PlayerEntity player) {
        World world = player.getWorld();
        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVector();
        double reach = 2.5;
        Vec3d end = start.add(look.multiply(reach));

        var hitResult = world.raycast(new net.minecraft.world.RaycastContext(
                start, end,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hitResult.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) return;

        BlockPos pos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();

        if (side != Direction.UP) return;
        if (!world.getBlockState(pos.up()).isAir()) return;

        var state = world.getBlockState(pos);
        if (!state.isSolidBlock(world, pos)) return;

        Vec3d hitPos = hitResult.getPos();
        Vec3d local = hitPos.subtract(Vec3d.of(pos));

        double playerX = player.getX();
        double playerZ = player.getZ();
        double blockCenterX = pos.getX() + 0.5;
        double blockCenterZ = pos.getZ() + 0.5;

        double dx = playerX - blockCenterX;
        double dz = playerZ - blockCenterZ;

        Direction edgeSide = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        double edgeEps = 0.18;
        boolean nearEdge = switch (edgeSide) {
            case NORTH -> local.z <= edgeEps;
            case SOUTH -> local.z >= (1 - edgeEps);
            case WEST -> local.x <= edgeEps;
            case EAST -> local.x >= (1 - edgeEps);
            default -> false;
        };
        if (!nearEdge) return;

        double y = pos.getY() + 1.0;
        double x = hitPos.x;
        double z = hitPos.z;

        switch (edgeSide) {
            case NORTH -> z = pos.getZ();
            case SOUTH -> z = pos.getZ() + 1.0;
            case WEST -> x = pos.getX();
            case EAST -> x = pos.getX() + 1.0;
        }

        Vec3d anchor = switch (edgeSide) {
            case NORTH -> new Vec3d(x, y, z + 0.015);
            case SOUTH -> new Vec3d(x, y, z - 0.015);
            case WEST -> new Vec3d(x + 0.015, y, z);
            case EAST -> new Vec3d(x - 0.015, y, z);
            default -> new Vec3d(x, y, z);
        };

        double radius = 1.0;
        double distToAnchor = player.getEyePos().distanceTo(anchor);
        if (distToAnchor > 2.3) return;

        Grip other = (hand == Hand.MAIN_HAND) ? rightGrip : leftGrip;
        if (other.isValid()) {
            double distBetween = anchor.distanceTo(other.anchor());
            if (distBetween > (radius + other.radius() + 0.25)) return;
        }

        Grip newGrip = new Grip(pos, edgeSide, anchor, radius);

        if (hand == Hand.MAIN_HAND) leftGrip = newGrip;
        else rightGrip = newGrip;

        regrabCooldownTicks = 0;
        markDirty();
    }

    public void tick(PlayerEntity player) {
        if (regrabCooldownTicks > 0) regrabCooldownTicks--;

        if (leftGrip.isValid() && !isGripStillValid(leftGrip, player)) {
            leftGrip = Grip.EMPTY;
            markDirty();
        }
        if (rightGrip.isValid() && !isGripStillValid(rightGrip, player)) {
            rightGrip = Grip.EMPTY;
            markDirty();
        }

        if (hasAnyGrip()) {
            applyGripConstraints(player);
        }
    }

    private boolean isGripStillValid(Grip grip, PlayerEntity player) {
        World world = player.getWorld();
        if (grip.blockPos() == null) return false;
        var state = world.getBlockState(grip.blockPos());
        if (!state.isSolidBlock(world, grip.blockPos())) return false;
        if (!world.getBlockState(grip.blockPos().up()).isAir()) return false;
        double dist = player.getEyePos().distanceTo(grip.anchor());
        return dist < grip.radius() * 1.7;
    }

    private void applyGripConstraints(PlayerEntity player) {
        Vec3d attachOffset = new Vec3d(0, 1.4, 0);
        Vec3d attachPos = player.getPos().add(attachOffset);

        Vec3d velocity = player.getVelocity();

        int iterations = 3;
        for (int i = 0; i < iterations; i++) {
            if (leftGrip.isValid()) {
                velocity = applySingleConstraint(velocity, attachPos, leftGrip.anchor(), leftGrip.radius(), player);
            }
            if (rightGrip.isValid()) {
                velocity = applySingleConstraint(velocity, attachPos, rightGrip.anchor(), rightGrip.radius(), player);
            }
        }

        player.setVelocity(velocity);
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity) {
            player.fallDistance = 0;
        }
    }

    private Vec3d applySingleConstraint(Vec3d velocity, Vec3d attachPos, Vec3d anchor, double radius, PlayerEntity player) {
        Vec3d delta = attachPos.subtract(anchor);
        double dist = delta.length();
        if (dist > radius) {
            Vec3d correction = delta.multiply((dist - radius) / dist);
            player.move(net.minecraft.entity.MovementType.SELF, correction.multiply(-1));
            return velocity.multiply(0.9);
        }
        return velocity;
    }

    private void markDirty() {
        ClimberComponents.CLIMBER.sync(provider);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.climberModeEnabled = tag.getBoolean("climberMode");
        this.leftGrip = tag.contains("leftGrip", NbtElement.COMPOUND_TYPE) ? Grip.fromNbt(tag.getCompound("leftGrip")) : Grip.EMPTY;
        this.rightGrip = tag.contains("rightGrip", NbtElement.COMPOUND_TYPE) ? Grip.fromNbt(tag.getCompound("rightGrip")) : Grip.EMPTY;
        this.regrabCooldownTicks = tag.getInt("regrabCooldown");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("climberMode", this.climberModeEnabled);
        tag.put("leftGrip", this.leftGrip.toNbt());
        tag.put("rightGrip", this.rightGrip.toNbt());
        tag.putInt("regrabCooldown", this.regrabCooldownTicks);
    }
}
