package me.collective.module.player;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.EnumSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.GrimUtils;
import me.collective.util.InventoryUtil;
import me.collective.util.SwapManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * GrimAirPlace: ставит блок туда, куда смотрит прицел, без реальной опоры.
 * Пакетная связка: START_DESTROY_BLOCK -> PlayerInteractBlock -> HandSwing -> START_DESTROY_BLOCK.
 * 1:1 порт из b2xy.
 */
public class GrimAirPlace extends Module {

    public final NumberSetting placeDelay = add(new NumberSetting("PlaceDelay", 1, 0, 5, 1));
    public final BooleanSetting customRange = add(new BooleanSetting("CustomRange", false));
    public final NumberSetting range = add(new NumberSetting("Range", 5.0, 0.0, 5.5, 0.1, (v) -> customRange.getValue()));

    private HitResult hitResult;
    private int delay;
    private boolean wasPressed;

    public GrimAirPlace() {
        super("GrimAirPlace", "Ставит блок туда, куда смотрит прицел (air place для Grim).", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        this.delay = 0;
        this.wasPressed = false;
    }

    @Override
    public void onDisable() {
        this.hitResult = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (this.delay < this.placeDelay.getIntValue()) this.delay++;

        double distance = this.customRange.getValue() ? this.range.getValue() : mc.player.getBlockInteractionRange();
        this.hitResult = mc.player.raycast(distance, 0.0f, false);

        if (!(this.hitResult instanceof BlockHitResult blockHit) || !placeable()) {
            this.wasPressed = false;
            return;
        }

        boolean isPressed = mc.options.useKey.isPressed();
        if (mc.currentScreen != null) {
            this.wasPressed = isPressed;
            return;
        }

        if (isPressed && !this.wasPressed && this.delay >= this.placeDelay.getIntValue()) {
            BlockPos targetPos = blockHit.getBlockPos();
            if (!mc.world.getBlockState(targetPos).isReplaceable()) {
                this.wasPressed = isPressed;
                return;
            }

            if (!SwapManager.getInstance().hold(this, mc.player.getInventory().selectedSlot, SwapManager.Priority.PLACE, 2)) return;

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(0, 0, 0), Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND, blockHit, GrimUtils.nextSequence()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(0, 0, 0), Direction.DOWN));

            this.delay = 0;
        }

        this.wasPressed = isPressed;
    }

    private boolean placeable() {
        if (mc.player == null) return false;
        Item item = mc.player.getMainHandStack().getItem();
        return item instanceof BlockItem || item instanceof SpawnEggItem;
    }

    public HitResult getHitResult() {
        return hitResult;
    }
}
