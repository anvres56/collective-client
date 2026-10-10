package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.InventoryUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * ElytraSwap: автоматически меняет элитры при низкой прочности (поэтапно, до 4 стадий —
 * из инвентаря через хотбар и обратно), плюс боевая защита: при ударе ставит нагрудник,
 * а после — элитры обратно. 1:1 порт из b2xy.
 */
public class ElytraSwap extends Module {

    public final NumberSetting durabilityThreshold = add(new NumberSetting("DurabilityThreshold", 10, 1, 100, 1));
    public final BooleanSetting onlyWhileFlying = add(new BooleanSetting("OnlyWhileFlying", false));
    public final BooleanSetting pauseInInventory = add(new BooleanSetting("PauseInInventory", true));
    public final NumberSetting swapCooldown = add(new NumberSetting("SwapCooldown", 100, 20, 200, 1));
    public final NumberSetting stageDelay = add(new NumberSetting("StageDelay", 8, 3, 20, 1));
    public final BooleanSetting notifySwap = add(new BooleanSetting("NotifySwap", true));

    public final BooleanSetting swapOnHit = add(new BooleanSetting("SwapOnHit", false));
    public final NumberSetting hitProtectionDuration = add(new NumberSetting("HitProtectionDuration", 60, 20, 200, 1, (v) -> swapOnHit.getValue()));
    public final BooleanSetting autoSwapBack = add(new BooleanSetting("AutoSwapBack", true, (v) -> swapOnHit.getValue()));
    public final BooleanSetting prioritizeNetherite = add(new BooleanSetting("PrioritizeNetherite", true, (v) -> swapOnHit.getValue()));

    private int cooldownTimer;
    private boolean needsSwap;
    private int swapStage;
    private int stageTimer;
    private int targetSlot;
    private int newElytraOriginalSlot;
    private int hotbarSlotUsed;
    private ItemStack hotbarOriginalItem = ItemStack.EMPTY;
    private boolean protectionActive;
    private int protectionTimer;
    private int lastHurtTime;
    private boolean needsChestplateSwap;
    private int chestplateSwapStage;
    private int chestplateSlot;
    private ItemStack storedElytra = ItemStack.EMPTY;

    public ElytraSwap() {
        super("ElytraSwap", "Автоматически меняет элитры при низкой прочности и надевает нагрудник при ударе.", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        resetSwapState();
    }

    @Override
    public void onDisable() {
        resetSwapState();
    }

    private void resetSwapState() {
        this.cooldownTimer = 0;
        this.needsSwap = false;
        this.swapStage = 0;
        this.stageTimer = 0;
        this.targetSlot = -1;
        this.newElytraOriginalSlot = -1;
        this.hotbarSlotUsed = -1;
        this.hotbarOriginalItem = ItemStack.EMPTY;
        this.protectionActive = false;
        this.protectionTimer = 0;
        this.lastHurtTime = 0;
        this.needsChestplateSwap = false;
        this.chestplateSwapStage = 0;
        this.chestplateSlot = -1;
        this.storedElytra = ItemStack.EMPTY;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (this.swapOnHit.getValue()) {
            this.handleCombatProtection();
        }
        if (this.cooldownTimer > 0) {
            this.cooldownTimer--;
            return;
        }
        if (this.pauseInInventory.getValue() && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            this.resetSwapState();
            return;
        }
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (this.protectionActive) {
            return;
        }
        if (!chestItem.getItem().equals(Items.ELYTRA)) {
            return;
        }
        if (this.onlyWhileFlying.getValue() && !mc.player.isGliding()) {
            return;
        }
        if (this.needsSwap) {
            this.processSwapStages();
            return;
        }
        int currentDurability = chestItem.getMaxDamage() - chestItem.getDamage();
        if (currentDurability <= this.durabilityThreshold.getIntValue()) {
            this.initiateSwap();
        }
    }

    private void initiateSwap() {
        int bestSlot = -1;
        int bestDurability = this.durabilityThreshold.getIntValue();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.getItem().equals(Items.ELYTRA)) {
                continue;
            }
            int durability = stack.getMaxDamage() - stack.getDamage();
            if (durability <= bestDurability) {
                continue;
            }
            bestDurability = durability;
            bestSlot = i;
        }
        if (bestSlot == -1) {
            return;
        }
        this.targetSlot = bestSlot;
        this.needsSwap = true;
        this.swapStage = 1;
        this.stageTimer = 0;
    }

    private void processSwapStages() {
        this.stageTimer++;
        if (this.stageTimer < this.stageDelay.getIntValue()) {
            return;
        }
        switch (this.swapStage) {
            case 1 -> {
                this.newElytraOriginalSlot = this.targetSlot;
                if (this.targetSlot >= 9) {
                    int hotbarSlot = -1;
                    if (this.hotbarSlotUsed != -1 && this.hotbarSlotUsed < 9) {
                        hotbarSlot = this.hotbarSlotUsed;
                    } else {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.player.getInventory().getStack(i);
                            if (!stack.isEmpty() && this.isEssentialItem(stack)) {
                                continue;
                            }
                            hotbarSlot = i;
                            break;
                        }
                        if (hotbarSlot == -1) {
                            hotbarSlot = 0;
                        }
                    }
                    this.hotbarOriginalItem = mc.player.getInventory().getStack(hotbarSlot).copy();
                    this.hotbarSlotUsed = hotbarSlot;
                    InventoryUtil.move().from(this.targetSlot).toHotbar(hotbarSlot);
                    this.targetSlot = hotbarSlot;
                    this.swapStage = 2;
                    this.stageTimer = 0;
                } else {
                    this.hotbarSlotUsed = this.targetSlot;
                    this.hotbarOriginalItem = ItemStack.EMPTY;
                    this.swapStage = 2;
                    this.stageTimer = 0;
                }
            }
            case 2 -> {
                ItemStack toEquip = mc.player.getInventory().getStack(this.targetSlot);
                if (!toEquip.getItem().equals(Items.ELYTRA)) {
                    this.resetSwapState();
                    return;
                }
                if (toEquip.getItem().equals(Items.LEATHER_LEGGINGS)
                    || toEquip.getItem().equals(Items.CHAINMAIL_LEGGINGS)
                    || toEquip.getItem().equals(Items.IRON_LEGGINGS)
                    || toEquip.getItem().equals(Items.GOLDEN_LEGGINGS)
                    || toEquip.getItem().equals(Items.DIAMOND_LEGGINGS)
                    || toEquip.getItem().equals(Items.NETHERITE_LEGGINGS)) {
                    this.resetSwapState();
                    return;
                }
                InventoryUtil.swap(this.targetSlot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.swapBack();
                this.swapStage = 3;
                this.stageTimer = 0;
            }
            case 3 -> {
                if (this.newElytraOriginalSlot >= 9) {
                    InventoryUtil.move().fromHotbar(this.targetSlot).to(this.newElytraOriginalSlot);
                    if (!this.hotbarOriginalItem.isEmpty()) {
                        this.swapStage = 4;
                        this.stageTimer = 0;
                        return;
                    }
                }
                if (this.notifySwap.getValue()) {
                    ItemStack newChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                    if (newChest.getItem().equals(Items.ELYTRA)) {
                        int newDurability = newChest.getMaxDamage() - newChest.getDamage();
                        sendClientMessage("Элитры заменены, прочность " + newDurability);
                    }
                }
                this.needsSwap = false;
                this.swapStage = 0;
                this.stageTimer = 0;
                this.targetSlot = -1;
                this.cooldownTimer = this.swapCooldown.getIntValue();
            }
            case 4 -> {
                if (this.stageTimer < 3) {
                    this.stageTimer++;
                    return;
                }
                for (int i = 9; i < 36; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!ItemStack.areItemsEqual(stack, this.hotbarOriginalItem)) {
                        continue;
                    }
                    InventoryUtil.move().from(i).toHotbar(this.hotbarSlotUsed);
                    break;
                }
                this.needsSwap = false;
                this.swapStage = 0;
                this.stageTimer = 0;
                this.targetSlot = -1;
                this.cooldownTimer = this.swapCooldown.getIntValue();
            }
        }
    }

    private boolean isEssentialItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.equals(Items.TOTEM_OF_UNDYING)
            || item.equals(Items.GOLDEN_APPLE)
            || item.equals(Items.ENCHANTED_GOLDEN_APPLE)
            || item.equals(Items.ENDER_PEARL)
            || item.equals(Items.CHORUS_FRUIT);
    }

    private void handleCombatProtection() {
        if (mc.player == null) return;
        ItemStack chestItem;
        if (mc.player.hurtTime > 0 && mc.player.hurtTime > this.lastHurtTime) {
            this.lastHurtTime = mc.player.hurtTime;
            chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestItem.getItem().equals(Items.ELYTRA) && !this.protectionActive) {
                int bestChestplate = this.findBestChestplate();
                if (bestChestplate != -1) {
                    this.storedElytra = chestItem.copy();
                    this.chestplateSlot = bestChestplate;
                    this.needsChestplateSwap = true;
                    this.chestplateSwapStage = 1;
                    this.stageTimer = 0;
                    this.protectionActive = true;
                    this.protectionTimer = this.hitProtectionDuration.getIntValue();
                    if (this.notifySwap.getValue()) {
                        sendClientMessage("Удар! Надеваю нагрудник для защиты.");
                    }
                }
            } else if (this.protectionActive) {
                this.protectionTimer = this.hitProtectionDuration.getIntValue();
            }
        }
        if (mc.player.hurtTime < this.lastHurtTime) {
            this.lastHurtTime = mc.player.hurtTime;
        }
        if (this.needsChestplateSwap) {
            this.processChestplateSwap();
            return;
        }
        if (this.protectionActive && !this.needsChestplateSwap) {
            this.protectionTimer--;
            if (this.protectionTimer <= 0 && this.autoSwapBack.getValue()) {
                chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                if (!chestItem.getItem().equals(Items.ELYTRA) && !this.storedElytra.isEmpty()) {
                    int elytraSlot = this.findStoredElytra();
                    if (elytraSlot != -1) {
                        this.chestplateSlot = elytraSlot;
                        this.needsChestplateSwap = true;
                        this.chestplateSwapStage = 1;
                        this.stageTimer = 0;
                        if (this.notifySwap.getValue()) {
                            sendClientMessage("Защита окончена, возвращаю элитры.");
                        }
                    } else {
                        this.protectionActive = false;
                        this.storedElytra = ItemStack.EMPTY;
                    }
                } else {
                    this.protectionActive = false;
                    this.storedElytra = ItemStack.EMPTY;
                }
            }
        }
    }

    private void processChestplateSwap() {
        this.stageTimer++;
        if (this.stageTimer < this.stageDelay.getIntValue()) {
            return;
        }
        switch (this.chestplateSwapStage) {
            case 1 -> {
                if (this.chestplateSlot >= 9) {
                    int hotbarSlot = 0;
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (!stack.isEmpty() && this.isEssentialItem(stack)) {
                            continue;
                        }
                        hotbarSlot = i;
                        break;
                    }
                    InventoryUtil.move().from(this.chestplateSlot).toHotbar(hotbarSlot);
                    this.chestplateSlot = hotbarSlot;
                }
                this.chestplateSwapStage = 2;
                this.stageTimer = 0;
            }
            case 2 -> {
                ItemStack toEquip = mc.player.getInventory().getStack(this.chestplateSlot);
                if (!this.isChestplateItem(toEquip)) {
                    this.needsChestplateSwap = false;
                    this.chestplateSwapStage = 0;
                    return;
                }
                if (toEquip.getItem().equals(Items.LEATHER_LEGGINGS)
                    || toEquip.getItem().equals(Items.CHAINMAIL_LEGGINGS)
                    || toEquip.getItem().equals(Items.IRON_LEGGINGS)
                    || toEquip.getItem().equals(Items.GOLDEN_LEGGINGS)
                    || toEquip.getItem().equals(Items.DIAMOND_LEGGINGS)
                    || toEquip.getItem().equals(Items.NETHERITE_LEGGINGS)) {
                    this.needsChestplateSwap = false;
                    this.chestplateSwapStage = 0;
                    return;
                }
                InventoryUtil.swap(this.chestplateSlot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.swapBack();
                this.chestplateSwapStage = 3;
                this.stageTimer = 0;
            }
            case 3 -> {
                this.needsChestplateSwap = false;
                this.chestplateSwapStage = 0;
                this.stageTimer = 0;
                this.chestplateSlot = -1;
                ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                if (chestItem.getItem().equals(Items.ELYTRA)) {
                    this.protectionActive = false;
                    this.storedElytra = ItemStack.EMPTY;
                }
            }
        }
    }

    private int findBestChestplate() {
        int bestSlot = -1;
        int bestValue = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            int value = this.getChestplateValue(stack);
            if (value <= bestValue) {
                continue;
            }
            bestValue = value;
            bestSlot = i;
        }
        return bestSlot;
    }

    private int getChestplateValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem().equals(Items.NETHERITE_CHESTPLATE)) {
            return (this.prioritizeNetherite.getValue() ? 1000 : 400) + (stack.getMaxDamage() - stack.getDamage());
        }
        if (stack.getItem().equals(Items.DIAMOND_CHESTPLATE)) {
            return 300 + (stack.getMaxDamage() - stack.getDamage());
        }
        if (stack.getItem().equals(Items.IRON_CHESTPLATE)) {
            return 200 + (stack.getMaxDamage() - stack.getDamage());
        }
        if (stack.getItem().equals(Items.CHAINMAIL_CHESTPLATE)) {
            return 150 + (stack.getMaxDamage() - stack.getDamage());
        }
        if (stack.getItem().equals(Items.GOLDEN_CHESTPLATE)) {
            return 100 + (stack.getMaxDamage() - stack.getDamage());
        }
        if (stack.getItem().equals(Items.LEATHER_CHESTPLATE)) {
            return 50 + (stack.getMaxDamage() - stack.getDamage());
        }
        return 0;
    }

    private boolean isChestplateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item.equals(Items.ELYTRA)
            || item.equals(Items.NETHERITE_CHESTPLATE)
            || item.equals(Items.DIAMOND_CHESTPLATE)
            || item.equals(Items.IRON_CHESTPLATE)
            || item.equals(Items.GOLDEN_CHESTPLATE)
            || item.equals(Items.CHAINMAIL_CHESTPLATE)
            || item.equals(Items.LEATHER_CHESTPLATE);
    }

    private int findStoredElytra() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.getItem().equals(Items.ELYTRA)) continue;
            if (Math.abs(stack.getDamage() - this.storedElytra.getDamage()) > 5) continue;
            return i;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.ELYTRA)) return i;
        }
        return -1;
    }

    private void sendClientMessage(String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("§7[§6ElytraSwap§7] §f" + msg), false);
        }
    }
}
