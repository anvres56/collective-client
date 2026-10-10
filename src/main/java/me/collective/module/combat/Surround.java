package me.collective.module.combat;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.FindItemResult;
import me.collective.util.GrimUtils;
import me.collective.util.InventoryUtil;
import me.collective.util.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Surround extends Module {
    public final BooleanSetting doubleHeight = add(new BooleanSetting("DoubleHeight", false));
    public final BooleanSetting onlyOnGround = add(new BooleanSetting("OnlyOnGround", true));
    public final BooleanSetting center = add(new BooleanSetting("Center", true));
    public final NumberSetting blocksPerTick = add(new NumberSetting("BlocksPerTick", 2.0, 1.0, 5.0, 1.0));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0.0, 0.0, 5.0, 1.0));

    private int delayTimer = 0;

    public Surround() {
        super("Surround", "Surrounds your feet with blast-resistant blocks to protect against crystals.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        delayTimer = 0;
        if (mc.player != null && center.getValue() && mc.player.isOnGround()) {
            double cx = Math.floor(mc.player.getX()) + 0.5;
            double cz = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPosition(cx, mc.player.getY(), cz);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        FindItemResult obsidian = InventoryUtil.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.NETHERITE_BLOCK);
        if (!obsidian.found()) return;

        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> targetPositions = new ArrayList<>();

        targetPositions.add(playerPos.north());
        targetPositions.add(playerPos.south());
        targetPositions.add(playerPos.east());
        targetPositions.add(playerPos.west());

        if (doubleHeight.getValue()) {
            targetPositions.add(playerPos.north().up());
            targetPositions.add(playerPos.south().up());
            targetPositions.add(playerPos.east().up());
            targetPositions.add(playerPos.west().up());
        }

        int placed = 0;
        int maxPlaced = blocksPerTick.getValue().intValue();

        for (BlockPos pos : targetPositions) {
            if (placed >= maxPlaced) break;
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;

            // Don't place if player is inside the block
            Box blockBox = new Box(pos);
            if (mc.player.getBoundingBox().intersects(blockBox)) continue;

            if (placeBlock(pos, obsidian.slot())) {
                placed++;
            }
        }

        if (placed > 0) {
            delayTimer = delay.getValue().intValue();
        }
    }

    private boolean placeBlock(BlockPos pos, int slot) {
        if (mc.player == null || mc.world == null) return false;

        // Find existing neighbor face to click against
        Direction placeSide = null;
        BlockPos neighborPos = null;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            BlockState state = mc.world.getBlockState(neighbor);
            if (!state.isReplaceable()) {
                placeSide = side.getOpposite();
                neighborPos = neighbor;
                break;
            }
        }

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        InventoryUtil.swap(slot);

        if (neighborPos != null && placeSide != null) {
            Vec3d hitVec = new Vec3d(
                neighborPos.getX() + 0.5 + placeSide.getOffsetX() * 0.5,
                neighborPos.getY() + 0.5 + placeSide.getOffsetY() * 0.5,
                neighborPos.getZ() + 0.5 + placeSide.getOffsetZ() * 0.5
            );
            BlockHitResult hitResult = new BlockHitResult(hitVec, placeSide, neighborPos, false);

            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND, hitResult, GrimUtils.nextSequence()
            ));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        } else {
            // Grim Air place fallback
            BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, BlockPos.ORIGIN, Direction.DOWN
            ));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND, hitResult, GrimUtils.nextSequence()
            ));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, BlockPos.ORIGIN, Direction.DOWN
            ));
        }

        InventoryUtil.swap(prevSlot);
        return true;
    }
}
