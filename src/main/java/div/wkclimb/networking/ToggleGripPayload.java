package div.wkclimb.networking;

import div.wkclimb.Wkclimb;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public record ToggleGripPayload(Hand hand) implements CustomPayload {
    public static final Identifier IDENTIFIER = Wkclimb.id("toggle_grip");
    public static final CustomPayload.Id<ToggleGripPayload> ID = new CustomPayload.Id<>(IDENTIFIER);

    // Correct in Minecraft 1.21: use PacketCodecs.enumConstant for enums
    public static final PacketCodec<RegistryByteBuf, ToggleGripPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.enumConstant(Hand.class), ToggleGripPayload::hand,
            ToggleGripPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
