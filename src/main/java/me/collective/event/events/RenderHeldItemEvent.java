package me.collective.event.events;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class RenderHeldItemEvent {
    private final AbstractClientPlayerEntity player;
    private final Hand hand;
    private final ItemStack item;
    private final MatrixStack matrices;
    private final OrderedRenderCommandQueue renderCommands;
    private final int light;

    public RenderHeldItemEvent(AbstractClientPlayerEntity player, Hand hand, ItemStack item, MatrixStack matrices, OrderedRenderCommandQueue renderCommands, int light) {
        this.player = player;
        this.hand = hand;
        this.item = item;
        this.matrices = matrices;
        this.renderCommands = renderCommands;
        this.light = light;
    }

    public AbstractClientPlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public ItemStack getItem() {
        return item;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public OrderedRenderCommandQueue getRenderCommands() {
        return renderCommands;
    }

    public int getLight() {
        return light;
    }
}
