package div.wkclimb.components;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Grip anchor data (immutable record).
 */
public record Grip(
        BlockPos blockPos,
        Direction edgeSide,
        Vec3d anchor,
        double radius
) {
    public static final Grip EMPTY = new Grip(BlockPos.ORIGIN, Direction.NORTH, Vec3d.ZERO, 0);

    public boolean isValid() {
        return radius > 0 && blockPos != null && anchor != null;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("x", blockPos.getX());
        tag.putInt("y", blockPos.getY());
        tag.putInt("z", blockPos.getZ());
        tag.putInt("edgeSide", edgeSide.getId());
        tag.putDouble("anchorX", anchor.x);
        tag.putDouble("anchorY", anchor.y);
        tag.putDouble("anchorZ", anchor.z);
        tag.putDouble("radius", radius);
        return tag;
    }

    public static Grip fromNbt(NbtCompound tag) {
        if (!tag.contains("x") || !tag.contains("radius") || tag.getDouble("radius") <= 0) {
            return EMPTY;
        }
        BlockPos pos = new BlockPos(
                tag.getInt("x"),
                tag.getInt("y"),
                tag.getInt("z")
        );
        Direction side = Direction.byId(tag.getInt("edgeSide"));
        Vec3d anchor = new Vec3d(
                tag.getDouble("anchorX"),
                tag.getDouble("anchorY"),
                tag.getDouble("anchorZ")
        );
        double radius = tag.getDouble("radius");
        return new Grip(pos, side, anchor, radius);
    }
}
