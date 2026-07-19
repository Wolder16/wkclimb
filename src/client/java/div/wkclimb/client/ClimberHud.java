package div.wkclimb.client;

import div.wkclimb.components.ClimberComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ClimberHud {
    public static void render(DrawContext context, TextRenderer textRenderer, int scaledWidth, int scaledHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var comp = ClimberComponents.CLIMBER.get(client.player);
        if (!comp.isClimberModeEnabled()) return;

        int y = 10;
        int x = scaledWidth / 2 - 25;

        // Mode indicator
        context.drawTextWithShadow(textRenderer, Text.literal("§bCLIMB"), x, y, 0xFFFFFF);

        // Grip status
        String left = comp.getLeftGrip().isValid() ? "§aL" : "§7L";
        String right = comp.getRightGrip().isValid() ? "§aR" : "§7R";

        context.drawTextWithShadow(textRenderer, Text.literal(left + "  " + right), x + 50, y, 0xFFFFFF);
    }
}
