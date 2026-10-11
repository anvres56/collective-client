package me.collective.module.hud;

import me.collective.font.FontManager;
import me.collective.module.client.HUD;
import me.collective.render.RenderUtil;
import me.collective.setting.BooleanSetting;
import me.collective.setting.StringSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class Watermark extends HudModule {
    public final StringSetting text = add(new StringSetting("Text", "Collective"));
    public final BooleanSetting version = add(new BooleanSetting("Version", true));

    public Watermark() {
        super("Watermark", "Displays the client watermark.", 4, 4);
    }

    @Override
    public void render(DrawContext context, float delta) {
        if (!isToggled()) return;

        String title = text.getValue() + (version.getValue() ? " §fv1.1" : "");
        this.width = FontManager.INSTANCE.getStringWidth(title) + 6;
        this.height = FontManager.INSTANCE.getFontHeight() + 4;

        if (background.getValue()) {
            RenderUtil.drawRect(context, x, y, x + width, y + height, new Color(20, 20, 20, 180));
            RenderUtil.drawOutline(context, x, y, width, height, 1, HUD.INSTANCE.getHudColor(0).getRGB());
        }

        FontManager.INSTANCE.drawStringWithShadow(context, title, x + 3, y + 2, HUD.INSTANCE.getHudColor(0));
    }
}
