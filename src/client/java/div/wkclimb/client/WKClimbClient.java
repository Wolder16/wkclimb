package div.wkclimb.client;

import div.wkclimb.client.GripClientHandler;
import div.wkclimb.client.ClimberHud;
import div.wkclimb.components.ClimberComponents;
import div.wkclimb.networking.ToggleGripPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class WKClimbClient implements ClientModInitializer {
    public static KeyBinding toggleClimberModeKey;

    @Override
    public void onInitializeClient() {
        toggleClimberModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wkclimb.toggle_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.wkclimb"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleClimberModeKey.wasPressed()) {
                toggleClimberMode(client);
            }

            if (client.player != null) {
                GripClientHandler.tickClientMovement(client.player);
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.getWindow() != null) {
                ClimberHud.render(context, mc.textRenderer,
                        mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
            }
        });
    }

    private void toggleClimberMode(MinecraftClient client) {
        if (client.player == null) return;

        var component = ClimberComponents.CLIMBER.get(client.player);
        boolean newState = !component.isClimberModeEnabled();
        component.setClimberModeEnabled(newState);

        client.player.sendMessage(
                net.minecraft.text.Text.literal("Climber Mode: " + (newState ? "§aON" : "§cOFF")),
                true
        );
    }

    public static void sendToggleGrip(Hand hand) {
        ClientPlayNetworking.send(new ToggleGripPayload(hand));
    }
}
