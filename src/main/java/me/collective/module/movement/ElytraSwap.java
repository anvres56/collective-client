package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.KeybindSetting;
import me.collective.setting.ModeSetting;
import me.collective.util.FindItemResult;
import me.collective.util.InventoryUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

/**
 * Порт ElytraSwap из ThunderHack-Recode.
 *
 * <p>Режим Enable: при включении один раз меняет нагрудник на элитру (или
 * обратно) и самоотключается. Режим Bind: смена по кнопке, запуск фейерверка
 * по кнопке и авто-фейерверк при старте планирования.
 */
public class ElytraSwap extends Module {
    public static ElytraSwap INSTANCE;

    public final BooleanSetting delay = add(new BooleanSetting("Delay", false));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Enable", "Enable", "Bind"));
    public final KeybindSetting switchButton = add(new KeybindSetting("SwitchButton", -1));
    public final KeybindSetting fireWorkButton = add(new KeybindSetting("FireWorkButton", -1));
    public final BooleanSetting startFireWork = add(new BooleanSetting("StartFireWork", true));
    public final ModeSetting fireWorkMode = add(new ModeSetting("FireWorkMode", "Normal", "Silent", "Normal"));

    public static boolean swapping = false;
    private long lastSwitch = 0L;
    private long lastFirework = 0L;

    public ElytraSwap() {
        super("ElytraSwap", "Быстрая смена нагрудника на элитру и обратно + авто-фейерверк.", Category.MOVEMENT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mode.process("Enable")) swapChest(true);
    }

    @Override
    public void onUpdate() {
        if (!mode.process("Bind") || mc.player == null || mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        if (switchButton.getValue() != -1 && switchButton.isPressed() && now - lastSwitch >= 500) {
            lastSwitch = now;
            swapChest(false);
        }
        if (fireWorkButton.getValue() != -1 && fireWorkButton.isPressed()
            && now - lastFirework >= 500 && mc.player.isGliding()) {
            lastFirework = now;
            useFireWork();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof ClientCommandC2SPacket cmd)) return;
        if (cmd.getMode() != ClientCommandC2SPacket.Mode.START_FALL_FLYING) return;
        if (!mode.process("Bind") || !startFireWork.getValue()) return;
        useFireWork();
    }

    public void useFireWork() {
        if (mc.player == null || mc.interactionManager == null) return;
        boolean silent = fireWorkMode.process("Silent");

        FindItemResult hotbar = InventoryUtil.findInHotbar(Items.FIREWORK_ROCKET);
        if (hotbar.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return;
        }
        if (hotbar.isHotbar()) {
            if (silent) {
                InventoryUtil.swap(hotbar.slot(), true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InventoryUtil.swapBack();
            } else {
                mc.player.getInventory().selectedSlot = hotbar.slot();
                mc.interactionManager.syncSelectedSlot();
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
            sendSwing();
            return;
        }

        FindItemResult inv = InventoryUtil.find(Items.FIREWORK_ROCKET);
        if (inv.found() && inv.isMain()) {
            int syncId = mc.player.currentScreenHandler.syncId;
            mc.interactionManager.clickSlot(syncId, inv.slot(), mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendClose(syncId);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            sendSwing();
            if (silent) {
                mc.interactionManager.clickSlot(syncId, inv.slot(), mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                sendClose(syncId);
            }
            return;
        }

        info("У тебя нет фейерверков!");
    }

    public static int getChestPlateSlot() {
        Item[] items = {Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE,
            Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        for (Item item : items) {
            FindItemResult slot = InventoryUtil.find(item);
            if (slot.found()) return slot.slot();
        }
        return -1;
    }

    private void swapChest(boolean disable) {
        if (mc.player == null || mc.interactionManager == null) return;
        FindItemResult elytra = InventoryUtil.find(Items.ELYTRA);

        if (mc.player.getInventory().getStack(38).isOf(Items.ELYTRA)) {
            int slot = getChestPlateSlot();
            if (slot != -1) {
                if (delay.getValue()) {
                    new Thread(() -> {
                        swapping = true;
                        clickSlot(slot);
                        sleep(200);
                        clickSlot(6);
                        sleep(200);
                        clickSlot(slot);
                        sendClose(mc.player.currentScreenHandler.syncId);
                        swapping = false;
                    }).start();
                } else {
                    clickSlot(slot);
                    clickSlot(6);
                    clickSlot(slot);
                    sendClose(mc.player.currentScreenHandler.syncId);
                }
            } else {
                if (disable) disable("У тебя нет нагрудника!");
                else info("У тебя нет нагрудника!");
                return;
            }
        } else if (elytra.found()) {
            if (delay.getValue()) {
                new Thread(() -> {
                    swapping = true;
                    clickSlot(elytra.slot());
                    sleep(200);
                    clickSlot(6);
                    sleep(200);
                    clickSlot(elytra.slot());
                    sendClose(mc.player.currentScreenHandler.syncId);
                    if (startFireWork.getValue() && mc.player.fallDistance > 0 && mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    }
                    swapping = false;
                }).start();
            } else {
                clickSlot(elytra.slot());
                clickSlot(6);
                clickSlot(elytra.slot());
                sendClose(mc.player.currentScreenHandler.syncId);
                if (startFireWork.getValue() && mc.player.fallDistance > 0 && mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                }
            }
        } else {
            if (disable) disable("У тебя нет элитры!");
            else info("У тебя нет элитры!");
            return;
        }

        if (disable) disable("Свапнул! Отключаю..");
    }

    private void clickSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    private void sendClose(int syncId) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
        }
    }

    private void sendSwing() {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    private void disable(String msg) {
        info(msg);
        setToggled(false);
    }
}
