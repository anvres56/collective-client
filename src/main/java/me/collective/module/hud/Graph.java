package me.collective.module.hud;

import me.collective.font.FontManager;
import me.collective.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.LinkedList;

public class Graph extends HudModule {
    private final LinkedList<Integer> fpsHistory = new LinkedList<>();
    private long lastUpdate = 0;

    public Graph() {
        super("Graph", "Draws an FPS performance graph.", 300, 100);
        this.width = 100;
        this.height = 40;
    }

    @Override
    public void render(DrawContext context, float delta) {
        if (!isToggled()) return;

        long now = System.currentTimeMillis();
        if (now - lastUpdate >= 100) {
            fpsHistory.addLast(mc.getCurrentFps());
            if (fpsHistory.size() > 50) {
                fpsHistory.removeFirst();
            }
            lastUpdate = now;
        }

        RenderUtil.drawRect(context, x, y, x + width, y + height, new Color(20, 20, 20, 180));
        RenderUtil.drawOutline(context, x, y, width, height, 1, new Color(110, 80, 230, 200).getRGB());

        FontManager.INSTANCE.drawStringWithShadow(context, "FPS: " + mc.getCurrentFps(), x + 4, y + 2, Color.WHITE);

        int max = 120;
        for (int fps : fpsHistory) {
            if (fps > max) max = fps;
        }

        float barWidth = width / 50.0f;
        int idx = 0;
        for (int fps : fpsHistory) {
            float h = (fps / (float) max) * (height - 14);
            float bx = x + idx * barWidth;
            float by = y + height - h;
            RenderUtil.drawRect(context, bx, by, bx + barWidth - 0.5f, y + height, new Color(110, 80, 230, 180).getRGB());
            idx++;
        }
    }
}
