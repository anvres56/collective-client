package me.collective.gui.alt;

import me.collective.account.Alt;
import me.collective.account.AltManager;
import me.collective.font.FontManager;
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
 * Alt Manager: список ников, добавление, вход, удаление. С блюром.
 */
public class AltScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget nickField;
    private ButtonWidget addButton;
    private ButtonWidget loginButton;
    private ButtonWidget deleteButton;
    private ButtonWidget backButton;

    private int selected = -1;
    private int scroll = 0;
    private static final int ROW_H = 22;
    private static final int VISIBLE_ROWS = 8;

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 320;

    public AltScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = 40;
        int listH = VISIBLE_ROWS * ROW_H;

        nickField = new TextFieldWidget(this.textRenderer, panelX + 10, panelY + 66 + listH, 190, 20, Text.literal("Ник"));
        nickField.setMaxLength(16);
        nickField.setPlaceholder(Text.literal("Ник..."));
        addDrawableChild(nickField);

        addButton = ButtonWidget.builder(Text.literal("Добавить"), b -> addFromField()).dimensions(panelX + 206, panelY + 66 + listH, 104, 20).build();
        addDrawableChild(addButton);

        int btnY = panelY + 92 + listH;
        loginButton = ButtonWidget.builder(Text.literal("Войти"), b -> loginSelected())
            .dimensions(panelX + 10, btnY, 97, 20).build();
        addDrawableChild(loginButton);

        deleteButton = ButtonWidget.builder(Text.literal("Удалить"), b -> {
            List<Alt> alts = AltManager.INSTANCE.getAlts();
            if (selected >= 0 && selected < alts.size()) {
                AltManager.INSTANCE.remove(alts.get(selected));
                selected = -1;
            }
        }).dimensions(panelX + 113, btnY, 97, 20).build();
        addDrawableChild(deleteButton);

        backButton = ButtonWidget.builder(Text.literal("Назад"), b -> close())
            .dimensions(panelX + 216, btnY, 94, 20).build();
        addDrawableChild(backButton);
    }

    private void addFromField() {
        if (AltManager.INSTANCE.add(nickField.getText().trim())) {
            nickField.setText("");
        }
    }

    private void loginSelected() {
        List<Alt> alts = AltManager.INSTANCE.getAlts();
        if (selected >= 0 && selected < alts.size()) {
            AltManager.INSTANCE.login(alts.get(selected));
        }
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

        FontManager.INSTANCE.drawStringWithShadow(context, "Alt Manager",
            panelX + 10, panelY + 6, Color.WHITE);
        FontManager.INSTANCE.drawStringWithShadow(context,
            "Текущий: " + AltManager.INSTANCE.currentName(),
            panelX + 10, panelY + 24, new Color(170, 170, 170));

        List<Alt> alts = AltManager.INSTANCE.getAlts();
        if (selected >= alts.size()) selected = alts.size() - 1;
        int maxScroll = Math.max(0, alts.size() - VISIBLE_ROWS);
        if (scroll > maxScroll) scroll = maxScroll;

        RenderUtil.drawRect(context, panelX + 10, panelY + 44, panelX + PANEL_W - 10,
            panelY + 44 + listH, new Color(0, 0, 0, 120));

        String current = AltManager.INSTANCE.currentName();
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scroll + i;
            if (idx >= alts.size()) break;
            Alt alt = alts.get(idx);
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
            boolean isCurrent = alt.name().equalsIgnoreCase(current);
            FontManager.INSTANCE.drawStringWithShadow(context,
                (isCurrent ? "▶ " : "  ") + alt.name(),
                panelX + 16, ry + 7, isCurrent ? new Color(110, 255, 130) : Color.WHITE);
        }

        if (alts.isEmpty()) {
            FontManager.INSTANCE.drawStringWithShadow(context, "Пусто — добавь ник ниже",
                panelX + 16, panelY + 50, new Color(150, 150, 150));
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int listH = VISIBLE_ROWS * ROW_H;
        List<Alt> alts = AltManager.INSTANCE.getAlts();
        if (click.button() == 0) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int idx = scroll + i;
                if (idx >= alts.size()) break;
                int ry = panelY + 44 + i * ROW_H;
                if (click.x() >= panelX + 10 && click.x() <= panelX + PANEL_W - 10
                    && click.y() >= ry && click.y() <= ry + ROW_H) {
                    if (doubled || idx == selected) {
                        selected = idx;
                        loginSelected();
                        return true;
                    }
                    selected = idx;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, AltManager.INSTANCE.getAlts().size() - VISIBLE_ROWS);
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
        if (input.key() == GLFW.GLFW_KEY_ENTER && nickField.isFocused()) {
            addFromField();
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
