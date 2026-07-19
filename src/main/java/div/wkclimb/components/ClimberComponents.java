package div.wkclimb.components;

import div.wkclimb.WKClimb;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class ClimberComponents implements EntityComponentInitializer {
    public static final ComponentKey<ClimberComponent> CLIMBER =
            ComponentRegistryV3.INSTANCE.getOrCreate(
                    WKClimb.id("climber"),
                    ClimberComponent.class
            );

    public static void init() {
        // called from main
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                CLIMBER,
                ClimberComponent::new,
                RespawnCopyStrategy.ALWAYS_COPY
        );
    }
}
