package me.collective.module.combat;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoArmor extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 1.0, 0.0, 5.0, 1.0));

    private int delayTimer = 0;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the strongest armor in your inventory.", Category.COMBAT);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        int[] screenSlots = { 5, 6, 7, 8 };

        for (int i = 0; i < slots.length; i++) {
            EquipmentSlot slot = slots[i];
            int screenSlot = screenSlots[i];

            ItemStack current = mc.player.getEquippedStack(slot);
            int bestInvSlot = -1;
            int bestScore = getScore(current);

            for (int invSlot = 0; invSlot < 36; invSlot++) {
                ItemStack stack = mc.player.getInventory().getStack(invSlot);
                if (stack.contains(DataComponentTypes.EQUIPPABLE)) {
                    var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
                    if (equippable != null && equippable.slot() == slot) {
                        int score = getScore(stack);
                        if (score > bestScore) {
                            bestScore = score;
                            bestInvSlot = invSlot;
                        }
                    }
                }
            }

            if (bestInvSlot != -1) {
                int fromSlot = bestInvSlot < 9 ? 36 + bestInvSlot : bestInvSlot;

                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);

                delayTimer = delay.getValue().intValue();
                return;
            }
        }
    }

    private int getScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.contains(DataComponentTypes.EQUIPPABLE)) return -1;
        String name = stack.getItem().toString().toLowerCase();
        if (name.contains("netherite")) return 40;
        if (name.contains("diamond")) return 30;
        if (name.contains("iron")) return 20;
        if (name.contains("chainmail")) return 15;
        if (name.contains("golden") || name.contains("gold")) return 12;
        if (name.contains("leather")) return 10;
        return 5;
    }
}
