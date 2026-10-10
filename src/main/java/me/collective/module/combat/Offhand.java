package me.collective.module.combat;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.EnumSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.FindItemResult;
import me.collective.util.InventoryUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class Offhand extends Module {
    public enum Mode {
        TOTEM, CRYSTAL, GAPPLE
    }

    public final EnumSetting<Mode> defaultItem = add(new EnumSetting<>("Default", Mode.TOTEM));
    public final NumberSetting healthThreshold = add(new NumberSetting("Health", 14.0, 1.0, 36.0, 1.0));
    public final BooleanSetting elytraCheck = add(new BooleanSetting("ElytraProtect", true));
    public final BooleanSetting fallCheck = add(new BooleanSetting("FallProtect", true));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0.0, 0.0, 10.0, 1.0));

    private int timer = 0;

    public Offhand() {
        super("Offhand", "Automatically manages your offhand item (Totem/Crystal/Gapple).", Category.COMBAT);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        Item target = getTargetItem();
        if (mc.player.getOffHandStack().isOf(target)) {
            return;
        }

        FindItemResult result = InventoryUtil.find(target);
        if (result.found()) {
            InventoryUtil.moveToOffhand(result.slot());
            timer = delay.getValue().intValue();
        } else if (target != Items.TOTEM_OF_UNDYING) {
            // Fallback to totem if target item not found
            FindItemResult totem = InventoryUtil.find(Items.TOTEM_OF_UNDYING);
            if (totem.found() && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                InventoryUtil.moveToOffhand(totem.slot());
                timer = delay.getValue().intValue();
            }
        }
    }

    private Item getTargetItem() {
        if (mc.player == null) return Items.TOTEM_OF_UNDYING;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        boolean lowHp = hp <= healthThreshold.getValue().floatValue();
        boolean gliding = elytraCheck.getValue() && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && mc.player.isGliding();
        boolean falling = fallCheck.getValue() && mc.player.fallDistance > 3.0f;

        if (lowHp || gliding || falling) {
            return Items.TOTEM_OF_UNDYING;
        }

        return switch (defaultItem.getValue()) {
            case TOTEM -> Items.TOTEM_OF_UNDYING;
            case CRYSTAL -> Items.END_CRYSTAL;
            case GAPPLE -> Items.ENCHANTED_GOLDEN_APPLE;
        };
    }
}
