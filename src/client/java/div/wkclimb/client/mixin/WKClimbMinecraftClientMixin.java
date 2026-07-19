package div.wkclimb.client.mixin;

import div.wkclimb.client.WKClimbClient;
import div.wkclimb.components.ClimberComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class WKClimbMinecraftClientMixin {

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        var component = ClimberComponents.CLIMBER.get(player);
        if (component != null && component.isClimberModeEnabled()) {
            ci.cancel();
            WKClimbClient.sendToggleGrip(Hand.MAIN_HAND);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        var component = ClimberComponents.CLIMBER.get(player);
        if (component != null && component.isClimberModeEnabled()) {
            ci.cancel();
            WKClimbClient.sendToggleGrip(Hand.OFF_HAND);
        }
    }
}
