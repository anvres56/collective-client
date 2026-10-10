package me.collective.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class AirPlaceExecutor {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private AirPlaceExecutor() {
    }

    public static void place(BlockHitResult hit, BlockPos placePos, Hand hand, BlockState predicted, Method method) {
        if (hit.isInsideBlock()) {
            airPlace(hit, predicted, method);
        } else {
            silentPlace(hit, placePos, predicted, hand, true);
        }
    }

    public static void airPlace(BlockHitResult hit, BlockState predicted, Method method) {
        airPlace(hit, hit.getBlockPos(), predicted, method);
    }

    public static void airPlace(BlockHitResult hit, BlockPos placePos, BlockState predicted, Method method) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        BlockHitResult wireHit = new BlockHitResult(hit.getPos(), hit.getSide(), hit.getBlockPos(), false);

        if (method == Method.Grim) {
            grimAirPlace(wireHit, placePos, predicted);
        } else {
            silentPlace(wireHit, placePos, predicted, Hand.MAIN_HAND, true);
        }
    }

    private static void grimAirPlace(BlockHitResult wireHit, BlockPos placePos, BlockState predicted) {
        ClientPlayNetworkHandler connection = mc.getNetworkHandler();
        if (connection == null || mc.world == null) return;

        try (PendingUpdateManager prediction = mc.world.getPendingUpdateManager().incrementSequence()) {
            connection.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            connection.sendPacket(new PlayerInteractBlockC2SPacket(
                Hand.OFF_HAND, wireHit, prediction.getSequence()));
            connection.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
            connection.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));

            if (predicted != null) {
                mc.world.setBlockState(placePos, predicted, 11);
            }
        }
    }

    public static void silentPlace(BlockHitResult hit, BlockPos placePos, BlockState predicted, Hand hand, boolean visualSwing) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        ClientPlayNetworkHandler connection = mc.getNetworkHandler();

        try (PendingUpdateManager prediction = mc.world.getPendingUpdateManager().incrementSequence()) {
            connection.sendPacket(new PlayerInteractBlockC2SPacket(hand, hit, prediction.getSequence()));

            if (predicted != null) {
                mc.world.setBlockState(placePos, predicted, 11);
            }
        }

        if (visualSwing && hand == Hand.MAIN_HAND) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            connection.sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public enum Method {
        Default,
        Grim
    }
}
