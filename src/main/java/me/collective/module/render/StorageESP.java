package me.collective.module.render;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class StorageESP extends Module {
    public final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    public final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", true));
    public final BooleanSetting enderChests = add(new BooleanSetting("EnderChests", true));
    public final BooleanSetting barrels = add(new BooleanSetting("Barrels", true));
    public final BooleanSetting chatAlert = add(new BooleanSetting("ChatAlert", true));

    private final Set<BlockPos> alertedPositions = new HashSet<>();
    private int tickTimer = 0;

    public StorageESP() {
        super("StorageESP", "Detects and highlights nearby storage containers (Chests, Shulkers, Barrels) for base hunting.", Category.RENDER);
    }

    @Override
    public void onDisable() {
        alertedPositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        tickTimer++;
        if (tickTimer < 20) return; // scan once per second
        tickTimer = 0;

        if (chatAlert.getValue()) {
            for (BlockEntity be : mc.world.getBlockEntities()) {
                BlockPos pos = be.getPos();
                if (alertedPositions.contains(pos)) continue;

                String type = null;
                if (chests.getValue() && be instanceof ChestBlockEntity) type = "Chest";
                else if (shulkers.getValue() && be instanceof ShulkerBoxBlockEntity) type = "Shulker Box";
                else if (enderChests.getValue() && be instanceof EnderChestBlockEntity) type = "Ender Chest";
                else if (barrels.getValue() && be instanceof BarrelBlockEntity) type = "Barrel";

                if (type != null) {
                    alertedPositions.add(pos);
                    double dist = mc.player.getEyePos().distanceTo(pos.toCenterPos());
                    if (dist > 8.0) { // only alert if not right next to player
                        mc.player.sendMessage(Text.literal(String.format("§7[§6StorageESP§7] §e%s §7found at §aX: %d, Y: %d, Z: %d §7(§f%.0fm§7)",
                            type, pos.getX(), pos.getY(), pos.getZ(), dist)), false);
                    }
                }
            }
        }
    }
}
