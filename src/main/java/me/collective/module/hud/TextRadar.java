package me.collective.module.hud;

import me.collective.font.FontManager;
import me.collective.render.RenderUtil;
import me.collective.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class TextRadar extends HudModule {
    public final NumberSetting range = add(new NumberSetting("Range", 50, 10, 150, 5));

    public TextRadar() {
        super("TextRadar", "Displays nearby players and their distance.", 4, 180);
    }

    @Override
    public void render(DrawContext context, float delta) {
        if (!isToggled() || mc.player == null || mc.world == null) return;

        List<PlayerEntity> players = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .filter(p -> mc.player.distanceTo(p) <= range.getIntValue())
                .sorted((p1, p2) -> Float.compare(mc.player.distanceTo(p1), mc.player.distanceTo(p2)))
                .collect(Collectors.toList());

        if (players.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        float curY = y;
        float maxWidth = 60;
        int fontH = FontManager.INSTANCE.getFontHeight();

        for (PlayerEntity p : players) {
            int dist = (int) mc.player.distanceTo(p);
            int hp = (int) (p.getHealth() + p.getAbsorptionAmount());
            String info = String.format("%s §7[%dm] §c%d❤", p.getName().getString(), dist, hp);
            float w = FontManager.INSTANCE.getStringWidth(info) + 6;
            maxWidth = Math.max(maxWidth, w);

            if (background.getValue()) {
                RenderUtil.drawRect(context, x, curY, x + w, curY + fontH + 2, new Color(20, 20, 20, 160));
            }

            FontManager.INSTANCE.drawStringWithShadow(context, info, x + 3, curY + 1, Color.WHITE);
            curY += fontH + 3;
        }

        this.width = maxWidth;
        this.height = Math.max(fontH, curY - y);
    }
}
