package me.collective.gui.proxy;

import me.collective.font.FontManager;
import me.collective.proxy.ProxyEntry;
import me.collective.proxy.ProxyManager;
import me.collective.render.BlurEngine;
import me.collective.render.RenderUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Выбор прокси: список, добавление одной строкой, удаление. С блюром.
 * Формат: [socks5://|http://][user:pass@]host:port
 */
public class ProxyScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget proxyField;
    private int selected = -1;
    private int scroll = 0;
    private static final int ROW_H = 22;
    private static final int VISIBLE_ROWS = 7;

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 340;

    public ProxyScreen(Screen parent) {
        super(Text.literal("Proxy"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = 36;
        int listH = VISIBLE_ROWS * ROW_H;

        proxyField = new TextFieldWidget(this.textRenderer, panelX + 10, panelY + 66 + listH, 220, 20, Text.literal("Прокси"));
        proxyField.setMaxLength(128);
        proxyField.setPlaceholder(Text.literal("socks5://user:pass@host:port"));
        addDrawableChild(proxyField);

        ButtonWidget addButton = ButtonWidget.builder(Text.literal("Добавить"), b -> {
            if (ProxyManager.INSTANCE.addLine(proxyField.getText())) {
                proxyField.setText("");
            }
        }).dimensions(panelX + 236, panelY + 66 + listH, 94, 20).build();
        addDrawableChild(addButton);

        int btnY = panelY + 92 + listH;
        addDrawableChild(ButtonWidget.builder(Text.literal("Выбрать"), b -> {
            ProxyManager.INSTANCE.setActive(selected);
        }).dimensions(panelX + 10, btnY, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Прямое"), b -> {
            ProxyManager.INSTANCE.setDirect();
        }).dimensions(panelX + 96, btnY, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"), b -> {
            ProxyManager.INSTANCE.remove(selected);
            selected = -1;
        }).dimensions(panelX + 182, btnY, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
            .dimensions(panelX + 258, btnY, 72, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        List<float[]> blur = new ArrayList<>();
        blur.add(new float[]{0, 0, this.width, this.height});
        BlurEngine.INSTANCE.renderBlur(context, blur, 10f);
        RenderUtil.drawRect(context, 0, 0, this.width, this.height, new Color(0, 0, 0, 130));

        int listH = VISIBLE_ROWS * ROW_H;
        RenderUtil.drawRect(context, panelX - 8, panelY - 8, panelX + PANEL_W + 8,
            panelY + 120 + listH, new Color(12, 12, 18, 220));

        FontManager.INSTANCE.drawStringWithShadow(context, "Proxy",
            panelX + 10, panelY + 6, Color.WHITE);
        ProxyEntry active = ProxyManager.INSTANCE.getActive();
        FontManager.INSTANCE.drawStringWithShadow(context,
            "Активно: " + (active == null ? "прямое" : active.displayName()),
            panelX + 10, panelY + 24, new Color(170, 170, 170));

        List<ProxyEntry> list = ProxyManager.INSTANCE.getProxies();
        if (selected >= list.size()) selected = list.size() - 1;
        int maxScroll = Math.max(0, list.size() - VISIBLE_ROWS);
        if (scroll > maxScroll) scroll = maxScroll;

        RenderUtil.drawRect(context, panelX + 10, panelY + 44, panelX + PANEL_W - 10,
            panelY + 44 + listH, new Color(0, 0, 0, 120));

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            ProxyEntry entry = list.get(idx);
            int ry = panelY + 44 + i * ROW_H;
            boolean hovered = mouseX >= panelX + 10 && mouseX <= panelX + PANEL_W - 10
                && mouseY >= ry && mouseY <= ry + ROW_H;
            if (idx == selected) {
                RenderUtil.drawRect(context, panelX + 10, ry, panelX + PANEL_W - 10,
                    ry + ROW_H, new Color(130, 80, 240, 70));
            } else if (hovered) {
                RenderUtil.drawRect(context, panelX + 10, ry, panelX + PANEL_W - 10,
                    ry + ROW_H, new Color(255, 255, 255, 15));
            }
            boolean isActive = active != null && entry.equals(active);
            FontManager.INSTANCE.drawStringWithShadow(context,
                (isActive ? "● " : "○ ") + entry.displayName(),
                panelX + 16, ry + 7, isActive ? new Color(110, 255, 130) : Color.WHITE);
        }

        if (list.isEmpty()) {
            FontManager.INSTANCE.drawStringWithShadow(context, "Пусто — добавь прокси ниже",
                panelX + 16, panelY + 50, new Color(150, 150, 150));
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int listH = VISIBLE_ROWS * ROW_H;
        List<ProxyEntry> list = ProxyManager.INSTANCE.getProxies();
        if (click.button() == 0) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int idx = scroll + i;
                if (idx >= list.size()) break;
                int ry = panelY + 44 + i * ROW_H;
                if (click.x() >= panelX + 10 && click.x() <= panelX + PANEL_W - 10
                    && click.y() >= ry && click.y() <= ry + ROW_H) {
                    selected = idx;
                    if (doubled) ProxyManager.INSTANCE.setActive(idx);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, ProxyManager.INSTANCE.getProxies().size() - VISIBLE_ROWS);
        if (verticalAmount < 0 && scroll < maxScroll) {
            scroll++;
            return true;
        } else if (verticalAmount > 0 && scroll > 0) {
            scroll--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
        else super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
