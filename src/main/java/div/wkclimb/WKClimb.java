package div.wkclimb;

import div.wkclimb.components.ClimberComponents;
import div.wkclimb.networking.ToggleGripPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WKClimb implements ModInitializer {
    public static final String MOD_ID = "wkclimb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing wkclimb (White Knuckle climbing mechanics)");

        // Register networking
        PayloadTypeRegistry.playC2S().register(ToggleGripPayload.ID, ToggleGripPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ToggleGripPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                div.wkclimb.components.ClimberComponent component = ClimberComponents.CLIMBER.get(player);
                component.toggleGrip(payload.hand(), player);
            });
        });

        // Tick logic
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                div.wkclimb.components.ClimberComponent component = ClimberComponents.CLIMBER.get(player);
                component.tick(player);
            }
        });

        ClimberComponents.init();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
