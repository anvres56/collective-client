package me.collective.module.tooltips;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

/**
 * Превью карты в тултипе.
 *
 * <p>Порт {@code HeightTooltipComponent}: размеры 108×110. Оригинал рисует
 * подложку {@code textures/map/map_background.png} с масштабом 1.5, затем саму
 * карту с масштабом 0.75 и сдвигом на 8 пикселей.
 *
 * <p>Геометрия и подложка перенесены как есть. Сама карта блитится готовой
 * текстурой, а не через {@code MapRenderer}: в 1.21.11 он принимает
 * {@code MapRenderState} и очередь render-команд, до которых нельзя дотянуться
 * из компонента тултипа.
 */
public class MapTooltipComponent implements TooltipComponent {

    private static final Identifier BACKGROUND = Identifier.ofVanilla("textures/map/map_background.png");

    public static final int WIDTH = 108;
    public static final int HEIGHT = 110;

    private final int id;
    private final MapState mapState;

    public MapTooltipComponent(int id, MapState mapState) {
        this.id = id;
        this.mapState = mapState;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return HEIGHT;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return WIDTH;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        var matrices = context.getMatrices();

        // подложка: масштаб 1.5, как в оригинале
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(1.5f);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, 0f, 0f, 64, 64, 64, 64);
        matrices.popMatrix();

        MinecraftClient mc = MinecraftClient.getInstance();
        var texture = mc.getMapTextureManager().getTextureId(new MapIdComponent(id), mapState);
        if (texture == null) return;

        // карта: масштаб 0.75 и сдвиг 8 пикселей
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(0.75f);
        matrices.translate(8f, 8f);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, 128, 128, 128, 128);
        matrices.popMatrix();
    }

    public int id() {
        return id;
    }

    public MapState mapState() {
        return mapState;
    }
}