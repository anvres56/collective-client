package me.collective.scripting;

import me.collective.module.hud.HudModule;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BiConsumer;

public class ScriptHudElement extends HudModule {
    private BiConsumer<DrawContext, Float> renderAction;

    public ScriptHudElement(String name, String description, float defaultX, float defaultY) {
        super(name, description, defaultX, defaultY);
        this.width = 100;
        this.height = 18;
    }

    public void onRender(BiConsumer<DrawContext, Float> renderAction) {
        this.renderAction = renderAction;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(DrawContext context, float delta) {
        if (renderAction != null) {
            renderAction.accept(context, delta);
        }
    }
}
