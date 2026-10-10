package me.collective.module.player;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;

public class FastUse extends Module {
    public final BooleanSetting expOnly = add(new BooleanSetting("ExpOnly", false));
    public final BooleanSetting blocks = add(new BooleanSetting("Blocks", true));
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown", 0.0, 0.0, 4.0, 1.0));

    public FastUse() {
        super("FastUse", "Allows you to use items, place blocks, and throw exp bottles at high speeds.", Category.PLAYER);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        ItemStack offhand = mc.player.getOffHandStack();

        if (expOnly.getValue()) {
            if (!(stack.getItem() instanceof ExperienceBottleItem) && !(offhand.getItem() instanceof ExperienceBottleItem)) {
                return;
            }
        } else if (!blocks.getValue()) {
            if (stack.getItem() instanceof BlockItem || offhand.getItem() instanceof BlockItem) {
                return;
            }
        }

        if (mc.itemUseCooldown > cooldown.getValue().intValue()) {
            mc.itemUseCooldown = cooldown.getValue().intValue();
        }
    }
}
