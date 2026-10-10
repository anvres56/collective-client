package me.collective.util.sort;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Геометрия контейнеров: вторая половина двойного сундука, число слотов,
 * признак «это шалкер» и идентификатор сервера для папок с данными.
 */
public final class ContainerGeometry {
    private ContainerGeometry() {
    }

    /**
     * Вторая половина двойного сундука, если она есть и совпадает по типу.
     * Для одинарных сундуков, бочек и разнотипных половин — null.
     */
    @Nullable
    public static BlockPos otherHalf(World level, BlockPos pos) {
        if (level == null) return null;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof ChestBlock) && !(block instanceof TrappedChestBlock)) return null;

        if (!state.contains(Properties.CHEST_TYPE) || !state.contains(Properties.HORIZONTAL_FACING)) return null;
        if (state.get(Properties.CHEST_TYPE) == ChestType.SINGLE) return null;

        Direction connected = ChestBlock.getFacing(state);
        Block otherBlock = level.getBlockState(pos.offset(connected)).getBlock();
        if (!(otherBlock instanceof ChestBlock)) return null;

        // Половины обычного и зачарованного сундука не образуют двойной сундук
        return (block instanceof TrappedChestBlock) == (otherBlock instanceof TrappedChestBlock)
            ? pos.offset(connected)
            : null;
    }

    /** Меньшая из двух половин — чтобы индекс не дублировал двойные сундуки. */
    public static BlockPos canonical(World level, BlockPos pos) {
        BlockPos other = otherHalf(level, pos);
        return other == null || pos.compareTo(other) <= 0 ? pos : other;
    }

    public static boolean isContainerBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block instanceof TrappedChestBlock || block instanceof BarrelBlock;
    }

    public static boolean isShulkerItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isIn(ItemTags.SHULKER_BOXES);
    }

    /** 54 слота у двойного сундука, 27 у одинарного. */
    public static int slotCount(World level, BlockPos pos) {
        return otherHalf(level, pos) != null ? 54 : 27;
    }

    /**
     * Число слотов контейнера. Если блок загружен — считаем точно,
     * иначе оцениваем по типу и занятым стопкам (чанк мог выгрузиться).
     */
    public static int slotCount(World level, BlockPos canonical, String containerType, int usedStacks) {
        return level != null && level.isPosLoaded(canonical) && isContainerBlock(level.getBlockState(canonical))
            ? slotCount(level, canonical)
            : estimatedSlots(containerType, usedStacks);
    }

    public static int estimatedSlots(String containerType, int usedStacks) {
        if (!"chest".equals(containerType) && !"copper_chest".equals(containerType)) return 27;
        return usedStacks > 27 ? 54 : 27;
    }

    /** Идентификатор сервера: папка с данными не должна смешивать серверы. */
    public static String serverId() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return "unknown";
        if (mc.getCurrentServerEntry() != null) return sanitizeFileName(mc.getCurrentServerEntry().address);
        return mc.isInSingleplayer() && mc.getServer() != null
            ? "singleplayer_" + sanitizeFileName(mc.getServer().getSaveProperties().getLevelName())
            : "unknown";
    }

    /** Имя папки: спецсимволы и пробелы заменены, регистр сброшен. */
    public static String sanitizeFileName(String name) {
        return name != null && !name.isEmpty()
            ? name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\s+", "_").toLowerCase()
            : "unknown";
    }
}
