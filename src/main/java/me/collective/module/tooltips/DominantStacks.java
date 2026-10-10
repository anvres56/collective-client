package me.collective.module.tooltips;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Список «контейнер → доминирующий предмет».
 *
 * <p>Порт {@code TooltipsData} и списка {@code Tooltips.list} из модуля
 * Tooltips. Важная деталь оригинала: доминирующий считается по <b>числу
 * слотов</b>, а не по суммарному количеству предметов, и возвращается
 * {@code item.getDefaultStack()} — то есть один предмет без компонентов.
 *
 * <p>Список накапливается из пакетов обновления инвентаря, а не пересчитывается
 * наведением: в оригинале он обновляется на {@code InventoryS2CPacket} и
 * {@code ScreenHandlerSlotUpdateS2CPacket}.
 */
public final class DominantStacks {

    /** Контейнер и его доминирующий предмет. */
    public record Entry(ItemStack container, ItemStack dominant) {
    }

    private DominantStacks() {
    }

    /**
     * Пересчитывает список по содержимому контейнеров.
     *
     * <p>Порядок обхода сохраняется: контейнеры приходят из пакета, а
     * {@link Collections#synchronizedList} — так же, как в оригинале, потому
     * что список пишется из сетевого потока.
     */
    public static List<Entry> collect(List<ItemStack> candidates) {
        List<Entry> out = Collections.synchronizedList(new ArrayList<>());

        if (candidates == null) return out;

        for (ItemStack stack : candidates) {
            Entry entry = of(stack);
            if (entry != null) out.add(entry);
        }

        return out;
    }

    /**
     * Доминирующий предмет одного контейнера.
     *
     * @return запись или {@code null}, если это не контейнер или он пуст
     */
    public static Entry of(ItemStack stack) {
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return null;

        // счётчик ведётся по слотам: 1 + уже насчитано, количество не важно
        Map<Item, Integer> slots = new HashMap<>();
        List<ItemStack> contents = container.stream().toList();

        for (ItemStack content : contents) {
            slots.merge(content.getItem(), 1, Integer::sum);
        }

        if (slots.isEmpty()) return null;

        Map.Entry<Item, Integer> best = slots.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (best == null) return null;

        return new Entry(stack, best.getKey().getDefaultStack());
    }
}