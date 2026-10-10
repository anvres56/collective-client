package me.collective.util.printer;

import me.collective.util.GrimUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Исполнитель установки блоков (порт из BepHax NEW-SRC).
 *
 * Адаптация под 1.21.11: {@code BlockStatePredictionHandler} удалён из клиента,
 * поэтому вместо {@code prediction.currentSequence()} используем собственный
 * монотонный счётчик {@link GrimUtils#nextSequence()} (семантика сохранена,
 * сервер игнорирует последовательность для ACK).
 */
public final class AirPlaceExecutor {
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
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().getNetworkHandler() != null) {
            BlockHitResult wireHit = new BlockHitResult(hit.getPos(), hit.getSide(), hit.getBlockPos(), false);
            if (method == Method.Grim) {
                grimAirPlace(wireHit, predicted);
            } else {
                silentPlace(wireHit, hit.getBlockPos(), predicted, Hand.MAIN_HAND, true);
            }
        }
    }

    private static void grimAirPlace(BlockHitResult wireHit, BlockState predicted) {
        ClientPlayNetworkHandler connection = MinecraftClient.getInstance().getNetworkHandler();

        // 1.21.11: BlockStatePredictionHandler нет — шлём пакеты напрямую с собственным sequence.
        connection.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        connection.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, wireHit, GrimUtils.nextSequence()));
        connection.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
        connection.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        if (predicted != null) {
            MinecraftClient.getInstance().world.setBlockState(wireHit.getBlockPos(), predicted, 11);
        }
    }

    public static void silentPlace(BlockHitResult hit, BlockPos placePos, BlockState predicted, Hand hand, boolean visualSwing) {
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworkHandler connection = MinecraftClient.getInstance().getNetworkHandler();

            // 1.21.11: BlockStatePredictionHandler удалён — sequence берём из GrimUtils.
            connection.sendPacket(new PlayerInteractBlockC2SPacket(hand, hit, GrimUtils.nextSequence()));
            if (predicted != null) {
                MinecraftClient.getInstance().world.setBlockState(placePos, predicted, 11);
            }

            if (visualSwing && hand == Hand.MAIN_HAND) {
                MinecraftClient.getInstance().player.swingHand(Hand.MAIN_HAND);
            } else {
                connection.sendPacket(new HandSwingC2SPacket(hand));
            }
        }
    }

    public enum Method {
        Default,
        Grim;
    }
}