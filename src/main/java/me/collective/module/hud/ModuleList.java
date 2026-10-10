package me.collective.module.hud;

import me.collective.font.FontManager;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import me.collective.module.client.HUD;
import me.collective.render.RenderUtil;
import me.collective.setting.BooleanSetting;
import me.collective.setting.EnumSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleList extends HudModule {
    public enum SortMode {
        LENGTH,
        ALPHABET
    }

    public final BooleanSetting onlyBound = add(new BooleanSetting("OnlyBound", false));
    public final EnumSetting<SortMode> sort = add(new EnumSetting<>("Sort", SortMode.LENGTH));

    public ModuleList() {
        super("ModuleList", "Displays active client modules.", 4, 30);
    }

    @Override
    public void render(DrawContext context, float delta) {
        if (!isToggled()) return;

        List<Module> activeModules = ModuleManager.INSTANCE.getModules().stream()
                .filter(m -> m.isToggled() && !(m instanceof HudModule))
                .filter(m -> !onlyBound.getValue() || m.getKeybind().getValue() != -1)
                .collect(Collectors.toList());

        if (sort.getValue() == SortMode.LENGTH) {
            activeModules.sort((m1, m2) -> Float.compare(
                    FontManager.INSTANCE.getStringWidth(m2.getName()),
                    FontManager.INSTANCE.getStringWidth(m1.getName())
            ));
        } else {
            activeModules.sort(Comparator.comparing(Module::getName));
        }

        float curY = y;
        float maxWidth = 40;
        int fontH = FontManager.INSTANCE.getFontHeight();
        int idx = 0;
        boolean rightAlign = mc.getWindow() != null && x > (mc.getWindow().getScaledWidth() / 2f);

        for (Module m : activeModules) {
            String name = m.getName();
            float w = FontManager.INSTANCE.getStringWidth(name);
            maxWidth = Math.max(maxWidth, w);

            Color color = HUD.INSTANCE.getHudColor(idx);

            float textX = rightAlign ? (x + maxWidth - w) : x;
            FontManager.INSTANCE.drawStringWithShadow(context, name, textX, curY, color);
            curY += fontH + 1;
            idx++;
        }

        this.width = maxWidth;
        this.height = Math.max(fontH, curY - y);
    }
}
