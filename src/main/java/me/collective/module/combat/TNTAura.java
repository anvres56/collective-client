package me.collective.module.combat;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.Render3DEvent;
import me.collective.module.Module;
import me.collective.render.Renderer3D;
import me.collective.render.ShapeMode;
import me.collective.setting.BooleanSetting;
import me.collective.setting.ColorSetting;
import me.collective.setting.ModeSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.FindItemResult;
import me.collective.util.InventoryUtil;
import me.collective.util.PlacementUtils;
import me.collective.manager.FriendManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Порт TNTAura из ThunderHack.
 *
 * <p>Обкладывает цель обсидианом, ставит динамит над головой и поджигает
 * кремнем. Гриферство и дуэли.
 */
public class TNTAura extends Module {
    public final NumberSetting range = add(new NumberSetting("Радиус", 5.0, 2.0, 7.0, 0.5));
    public final NumberSetting blocksPerTick = add(new NumberSetting("Блоков/тик", 4.0, 1.0, 8.0, 1.0));
    public final NumberSetting placeDelay = add(new NumberSetting("Задержка", 3.0, 0.0, 10.0, 1.0));
    public final ModeSetting placeMode = add(new ModeSetting("Режим установки", "Normal", "Normal", "Strict"));
    public final ModeSetting rotate = add(new ModeSetting("Ротация", "Silent", "Silent", "None"));
    public final ModeSetting renderMode = add(new ModeSetting("Режим рендера", "Fade", "Fade", "Decrease"));
    public final ColorSetting fillColor = add(new ColorSetting("Заливка", new Color(255, 60, 60, 100)));
    public final ColorSetting lineColor = add(new ColorSetting("Линия", new Color(255, 60, 60, 255)));

    private final Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();
    private int delay = 0;

    public TNTAura() {
        super("TNTAura", "Обкладывает врага и взрывает динамитом.", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        renderPoses.clear();
        delay = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        FindItemResult tnt = InventoryUtil.findInHotbar(Items.TNT);
        if (!tnt.found()) {
            info("Нет динамита!");
            setToggled(false);
            return;
        }
        FindItemResult flint = InventoryUtil.findInHotbar(Items.FLINT_AND_STEEL);
        if (!flint.found()) {
            info("Нет зажигалки!");
            setToggled(false);
            return;
        }
        FindItemResult obby = InventoryUtil.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.NETHERITE_BLOCK);
        if (!obby.found()) {
            info("Нет обсидиана!");
            setToggled(false);
            return;
        }

        PlayerEntity target = getTarget();
        boolean strict = placeMode.process("Strict");
        boolean silent = rotate.process("Silent");

        // Обкладка обсидианом
        List<BlockPos> blocks = getBlocks(target);
        if (!blocks.isEmpty()) {
            if (delay > 0) {
                delay--;
            } else {
                int placed = 0;
                while (placed < blocksPerTick.getIntValue()) {
                    BlockPos bp = getSequentialPos(target);
                    if (bp == null) break;
                    if (PlacementUtils.placeBlock(bp, obby, silent, true, strict)) {
                        placed++;
                        delay = placeDelay.getIntValue();
                        renderPoses.put(bp, System.currentTimeMillis());
                    } else break;
                }
            }
        }

        // Динамит над головой + поджог
        if (target != null) {
            BlockPos head = BlockPos.ofFloored(target.getEntityPos()).up(2);
            if (PlacementUtils.canPlace(head, false)) {
                PlacementUtils.placeBlock(head, tnt, silent, true, strict);
            }
            BlockHitResult ignite = getIgniteResult(head);
            if (mc.world.getBlockState(head).getBlock() instanceof TntBlock && ignite != null) {
                InventoryUtil.swap(flint.slot(), true);
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND, ignite, me.collective.util.GrimUtils.nextSequence()));
                }
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.swapBack();
            }
            renderPoses.put(head, System.currentTimeMillis());
        }
    }

    private BlockPos getSequentialPos(PlayerEntity target) {
        for (BlockPos bp : getBlocks(target)) {
            if (PlacementUtils.canPlace(bp, false) && mc.world.isAir(bp)) return bp;
        }
        return null;
    }

    private List<BlockPos> getBlocks(PlayerEntity target) {
        List<BlockPos> blocks = new ArrayList<>();
        if (target == null) return blocks;
        BlockPos base = BlockPos.ofFloored(target.getEntityPos());
        BlockPos[] around = {base, base.north(), base.south(), base.east(), base.west(),
            base.north().east(), base.north().west(), base.south().east(), base.south().west()};
        for (BlockPos bp : around) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                blocks.add(bp.offset(dir));
                blocks.add(bp.offset(dir).up(1));
                Box up2 = new Box(bp.offset(dir).up(1));
                if (!up2.intersects(target.getBoundingBox())) blocks.add(bp.offset(dir).up(2));
                blocks.add(bp.offset(dir).down(1));
            }
            blocks.add(bp.down(1));
            blocks.add(bp.up(3));
        }
        blocks.sort(Comparator.comparing(b -> -mc.player.squaredDistanceTo(b.toCenterPos())));
        return blocks;
    }

    private BlockHitResult getIgniteResult(BlockPos pos) {
        if (mc.player == null) return null;
        double r = range.getFloatValue();
        if (mc.player.squaredDistanceTo(pos.toCenterPos()) > r * r) return null;
        return new BlockHitResult(pos.toCenterPos().add(0, -0.5, 0), Direction.DOWN, pos, false);
    }

    private PlayerEntity getTarget() {
        PlayerEntity best = null;
        double bestDist = range.getFloatValue();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            if (FriendManager.getInstance().isFriend(p)) continue;
            double d = mc.player.distanceTo(p);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!isToggled()) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<BlockPos, Long> e : new ArrayList<>(renderPoses.entrySet())) {
            BlockPos pos = e.getKey();
            long age = now - e.getValue();
            if (age > 500) {
                renderPoses.remove(pos);
                continue;
            }
            float k = 1f - age / 500f;
            Color fill = withAlpha(fillColor.getValue(), (int) (100 * k));
            if (renderMode.process("Fade")) {
                Color line = withAlpha(lineColor.getValue(), (int) (100 * k));
                Renderer3D.box(new Box(pos), fill, line, ShapeMode.Both, true);
            } else {
                float scale = 1f - age / 500f;
                Box b = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ())
                    .shrink(scale, scale, scale)
                    .offset(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5);
                Renderer3D.box(b, fill, lineColor.getValue(), ShapeMode.Both, true);
            }
        }
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
