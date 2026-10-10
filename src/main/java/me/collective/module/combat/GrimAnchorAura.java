package me.collective.module.combat;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.EnumSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.FindItemResult;
import me.collective.util.GrimUtils;
import me.collective.util.InventoryUtil;
import me.collective.util.RotationUtils;
import me.collective.util.SwapManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GrimAnchorAura extends Module {
    private static final int ROTATION_PRIORITY = 40;

    public enum RotationMode {
        SILENT, CLIENT, BOTH
    }

    private enum ActionType {
        PLACE, CHARGE, EXPLODE
    }

    public final EnumSetting<RotationMode> rotationMode = add(new EnumSetting<>("Rotation", RotationMode.BOTH));
    public final NumberSetting targetRange = add(new NumberSetting("TargetRange", 10.0, 1.0, 15.0, 0.5));
    public final NumberSetting placeReach = add(new NumberSetting("Reach", 4.2, 1.0, 4.5, 0.1));
    public final NumberSetting turnSpeed = add(new NumberSetting("TurnSpeed", 65.0, 15.0, 180.0, 5.0));
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 0.65, 0.0, 0.95, 0.05));
    public final BooleanSetting targetCreative = add(new BooleanSetting("TargetCreative", true));
    public final BooleanSetting targetMobs = add(new BooleanSetting("TargetMobs", false));
    public final BooleanSetting testAlone = add(new BooleanSetting("TestAlone", false));
    public final BooleanSetting predict = add(new BooleanSetting("Predict", false));

    public final NumberSetting placeDelay = add(new NumberSetting("PlaceDelay", 1.0, 0.0, 5.0, 1.0));
    public final NumberSetting chargeDelay = add(new NumberSetting("ChargeDelay", 1.0, 0.0, 5.0, 1.0));
    public final NumberSetting breakDelay = add(new NumberSetting("BreakDelay", 1.0, 0.0, 5.0, 1.0));

    public final BooleanSetting checkDimension = add(new BooleanSetting("CheckDimension", true));
    public final BooleanSetting selfDamageSafety = add(new BooleanSetting("Safety", false));
    public final NumberSetting minSafeDistance = add(new NumberSetting("SafeDist", 3.0, 1.0, 6.0, 0.5));
    public final BooleanSetting replenish = add(new BooleanSetting("Replenish", false));

    private BlockPos currentAnchorPos;
    private Entity currentTarget;
    private int actionTimer;

    private ActionType pendingAction;
    private BlockHitResult pendingHit;
    private BlockPos lastPlacedAnchorPos;
    private int placedWaitTimer;
    private BlockPos lastChargedAnchorPos;
    private int chargedWaitTimer;
    private BlockPos lastExplodedAnchorPos;
    private int explodedWaitTimer;

    public GrimAnchorAura() {
        super("GrimAnchorAura", "Automated Respawn Anchor placement, charging, and exploding for GrimAC.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        this.currentAnchorPos = null;
        this.currentTarget = null;
        this.actionTimer = 0;
        this.lastPlacedAnchorPos = null;
        this.placedWaitTimer = 0;
        this.lastChargedAnchorPos = null;
        this.chargedWaitTimer = 0;
        this.lastExplodedAnchorPos = null;
        this.explodedWaitTimer = 0;
        cancelPending();
    }

    @Override
    public void onDisable() {
        RotationUtils.getInstance().release(this);
        cancelPending();
        this.currentAnchorPos = null;
        this.currentTarget = null;
        this.lastPlacedAnchorPos = null;
        this.placedWaitTimer = 0;
        this.lastChargedAnchorPos = null;
        this.chargedWaitTimer = 0;
        this.lastExplodedAnchorPos = null;
        this.explodedWaitTimer = 0;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (this.checkDimension.getValue() && mc.world.getRegistryKey() == World.NETHER) {
            cancelPending();
            RotationUtils.getInstance().release(this);
            return;
        }

        if (this.placedWaitTimer > 0) this.placedWaitTimer--;
        if (this.chargedWaitTimer > 0) this.chargedWaitTimer--;
        if (this.explodedWaitTimer > 0) this.explodedWaitTimer--;

        if (this.actionTimer > 0) {
            this.actionTimer--;
            if (this.pendingHit != null) {
                applyAim(this.pendingHit.getPos());
            }
            return;
        }

        this.currentTarget = findTarget();
        if (this.currentTarget == null && !this.testAlone.getValue()) {
            cancelPending();
            this.currentAnchorPos = null;
            RotationUtils.getInstance().release(this);
            return;
        }

        double reach = this.placeReach.getValue();

        BlockPos existingAnchor = findExistingAnchor(this.currentTarget, reach);
        if (existingAnchor != null) {
            this.currentAnchorPos = existingAnchor;
            this.lastPlacedAnchorPos = null;
            this.placedWaitTimer = 0;
            handleAnchorInteraction(existingAnchor, reach);
            return;
        }

        if (this.currentTarget == null) {
            cancelPending();
            this.currentAnchorPos = null;
            RotationUtils.getInstance().release(this);
            return;
        }

        if (this.lastPlacedAnchorPos != null && this.placedWaitTimer > 0) {
            if (this.pendingHit != null) {
                applyAim(this.pendingHit.getPos());
            }
            return;
        }

        BlockPos candidate = findPlacementPosition(this.currentTarget, reach);
        if (candidate == null) {
            cancelPending();
            this.currentAnchorPos = null;
            RotationUtils.getInstance().release(this);
            return;
        }

        this.currentAnchorPos = candidate;
        handlePlacement(candidate, reach);
    }

    private void handleAnchorInteraction(BlockPos anchorPos, double reach) {
        BlockState state = mc.world.getBlockState(anchorPos);
        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
            cancelPending();
            return;
        }

        int charges = state.get(RespawnAnchorBlock.CHARGES);

        if (charges == 0) {
            if (this.lastChargedAnchorPos != null && this.lastChargedAnchorPos.equals(anchorPos) && this.chargedWaitTimer > 0) {
                if (this.pendingHit != null) {
                    applyAim(this.pendingHit.getPos());
                }
                return;
            }

            FindItemResult glowstone = findGlowstone();
            if (!glowstone.found()) {
                cancelPending();
                return;
            }

            BlockHitResult hit = findBestInteractionHit(anchorPos, reach);
            if (hit == null) {
                cancelPending();
                return;
            }

            this.pendingAction = ActionType.CHARGE;
            this.pendingHit = hit;

            applyAim(hit.getPos());

            if (isAimed(hit, reach)) {
                BlockHitResult confirmed = confirmSentRaycast(hit, reach);
                if (confirmed != null && !SwapManager.getInstance().isActionConflictTick()) {
                    executeCharge(confirmed, anchorPos);
                }
            }
        } else {
            this.lastChargedAnchorPos = null;
            this.chargedWaitTimer = 0;

            if (this.lastExplodedAnchorPos != null && this.lastExplodedAnchorPos.equals(anchorPos) && this.explodedWaitTimer > 0) {
                if (this.pendingHit != null) {
                    applyAim(this.pendingHit.getPos());
                }
                return;
            }

            if (this.selfDamageSafety.getValue() && isUnsafeToExplode(anchorPos)) {
                cancelPending();
                return;
            }

            FindItemResult breakItem = findBreakItem();
            if (!breakItem.found()) {
                cancelPending();
                return;
            }

            BlockHitResult hit = findBestInteractionHit(anchorPos, reach);
            if (hit == null) {
                cancelPending();
                return;
            }

            this.pendingAction = ActionType.EXPLODE;
            this.pendingHit = hit;

            applyAim(hit.getPos());

            if (isAimed(hit, reach)) {
                BlockHitResult confirmed = confirmSentRaycast(hit, reach);
                if (confirmed != null && !SwapManager.getInstance().isActionConflictTick()) {
                    executeExplode(confirmed, anchorPos);
                }
            }
        }
    }

    private void handlePlacement(BlockPos placePos, double reach) {
        if (this.lastExplodedAnchorPos != null && this.lastExplodedAnchorPos.equals(placePos) && this.explodedWaitTimer > 0) {
            cancelPending();
            return;
        }

        FindItemResult anchor = findAnchor();
        if (!anchor.found()) {
            cancelPending();
            return;
        }

        BlockHitResult hit = findBestSupportHit(placePos, reach);
        if (hit == null) {
            cancelPending();
            return;
        }

        this.pendingAction = ActionType.PLACE;
        this.pendingHit = hit;

        applyAim(hit.getPos());

        if (isAimed(hit, reach)) {
            BlockHitResult confirmed = confirmSentRaycast(hit, reach);
            if (confirmed != null && !SwapManager.getInstance().isActionConflictTick()) {
                executePlace(confirmed, placePos);
            }
        }
    }

    private boolean isAimed(BlockHitResult hit, double reach) {
        return confirmSentRaycast(hit, reach) != null;
    }

    private void applyAim(Vec3d targetPos) {
        float[] rot = RotationUtils.getRotationsTo(mc.player.getEyePos(), targetPos);
        double maxSpeed = this.turnSpeed.getValue();
        double smooth = this.smoothness.getValue();

        if (this.rotationMode.getValue() == RotationMode.CLIENT || this.rotationMode.getValue() == RotationMode.BOTH) {
            float curYaw = mc.player.getYaw();
            float curPitch = mc.player.getPitch();

            float dy = MathHelper.wrapDegrees(rot[0] - curYaw);
            float dp = rot[1] - curPitch;

            double totalDist = Math.hypot(dy, dp);

            if (maxSpeed >= 180.0 && smooth <= 0.0) {
                mc.player.setYaw(curYaw + dy);
                mc.player.setPitch(MathHelper.clamp(rot[1], -90.0f, 90.0f));
            } else if (totalDist > 0.0001) {
                double decelRange = Math.max(16.0, maxSpeed * 0.40);
                double easePow = 0.50 + (smooth * 0.30);
                double speedFactor = totalDist < decelRange ? Math.pow(totalDist / decelRange, easePow) : 1.0;
                double desiredSpeed = Math.max(maxSpeed * speedFactor, 3.0);
                double step = Math.min(totalDist, Math.min(desiredSpeed, maxSpeed));

                double dirY = dy / totalDist;
                double dirP = dp / totalDist;

                double stepY = dirY * step;
                double stepP = dirP * step;

                double sens = mc.options.getMouseSensitivity().getValue();
                double f = sens * 0.6 + 0.2;
                double gcd = f * f * f * 8.0 * 0.15;
                if (gcd > 0.001) {
                    double qY = Math.round(stepY / gcd) * gcd;
                    double qP = Math.round(stepP / gcd) * gcd;
                    if (qY == 0 && Math.abs(dy) > gcd * 0.5) qY = Math.signum(dy) * gcd;
                    if (qP == 0 && Math.abs(dp) > gcd * 0.5) qP = Math.signum(dp) * gcd;
                    stepY = qY;
                    stepP = qP;
                }

                mc.player.setYaw(curYaw + (float) stepY);
                mc.player.setPitch(MathHelper.clamp(curPitch + (float) stepP, -90.0f, 90.0f));
            }
        }

        if (this.rotationMode.getValue() == RotationMode.SILENT || this.rotationMode.getValue() == RotationMode.BOTH) {
            RotationUtils rotUtils = RotationUtils.getInstance();
            if (this.rotationMode.getValue() == RotationMode.BOTH) {
                rotUtils.setRotationFullInstant(this, ROTATION_PRIORITY, mc.player.getYaw(), mc.player.getPitch());
            } else {
                rotUtils.setRotationSmooth(this, ROTATION_PRIORITY, rot[0], rot[1], maxSpeed, false);
            }
            rotUtils.keepWireFresh();
        } else {
            RotationUtils.getInstance().release(this);
        }
    }

    private void executePlace(BlockHitResult hit, BlockPos placePos) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            cancelPending();
            return;
        }

        BlockHitResult verified = confirmSentRaycast(hit, this.placeReach.getValue());
        if (verified == null) return;
        hit = verified;

        FindItemResult anchor = findAnchor();
        if (!anchor.found()) {
            cancelPending();
            return;
        }

        if (!SwapManager.getInstance().hold(this, anchor.slot(), SwapManager.Priority.PLACE, 2)) return;

        int seq = GrimUtils.nextSequence();
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, seq));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (this.predict.getValue()) {
            mc.world.setBlockState(placePos, Blocks.RESPAWN_ANCHOR.getDefaultState());
        }

        this.lastPlacedAnchorPos = placePos;
        this.placedWaitTimer = 10;
        this.actionTimer = this.placeDelay.getValue().intValue();
        cancelPending();
    }

    private void executeCharge(BlockHitResult hit, BlockPos anchorPos) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            cancelPending();
            return;
        }

        BlockHitResult verified = confirmSentRaycast(hit, this.placeReach.getValue());
        if (verified == null) return;
        hit = verified;

        FindItemResult glowstone = findGlowstone();
        if (!glowstone.found()) {
            cancelPending();
            return;
        }

        if (!SwapManager.getInstance().hold(this, glowstone.slot(), SwapManager.Priority.PLACE, 2)) return;

        int seq = GrimUtils.nextSequence();
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, seq));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (this.predict.getValue()) {
            mc.world.setBlockState(anchorPos, Blocks.RESPAWN_ANCHOR.getDefaultState().with(RespawnAnchorBlock.CHARGES, 1));
        }

        this.lastChargedAnchorPos = anchorPos;
        this.chargedWaitTimer = 10;
        this.actionTimer = this.chargeDelay.getValue().intValue();
        cancelPending();
    }

    private void executeExplode(BlockHitResult hit, BlockPos anchorPos) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            cancelPending();
            return;
        }

        BlockHitResult verified = confirmSentRaycast(hit, this.placeReach.getValue());
        if (verified == null) return;
        hit = verified;

        FindItemResult breakItem = findBreakItem();
        if (!breakItem.found()) {
            cancelPending();
            return;
        }

        if (!SwapManager.getInstance().hold(this, breakItem.slot(), SwapManager.Priority.PLACE, 2)) return;

        int seq = GrimUtils.nextSequence();
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, seq));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (this.predict.getValue()) {
            mc.world.setBlockState(anchorPos, Blocks.AIR.getDefaultState());
        }

        this.lastExplodedAnchorPos = anchorPos;
        this.explodedWaitTimer = 10;
        this.lastPlacedAnchorPos = null;
        this.placedWaitTimer = 0;
        this.currentAnchorPos = null;
        this.actionTimer = this.breakDelay.getValue().intValue();
        cancelPending();
    }

    private void cancelPending() {
        this.pendingAction = null;
        this.pendingHit = null;
    }

    private BlockHitResult findBestSupportHit(BlockPos placePos, double reach) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d eye = mc.player.getEyePos();
        double reachSq = reach * reach;

        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = placePos.offset(dir);
            if (neighbor.equals(this.lastExplodedAnchorPos) && this.explodedWaitTimer > 0) continue;

            BlockState state = mc.world.getBlockState(neighbor);
            if (state.isReplaceable() || state.getCollisionShape(mc.world, neighbor).isEmpty()) continue;
            if (state.isOf(Blocks.RESPAWN_ANCHOR) || !state.isSolidBlock(mc.world, neighbor)) continue;

            Direction clickFace = dir.getOpposite();
            if (!isFaceFacingEye(eye, neighbor, clickFace)) continue;

            Vec3d sample = getFaceSamplePoint(neighbor, clickFace);
            Vec3d dirVec = sample.subtract(eye);
            if (dirVec.lengthSquared() < 1.0E-6) continue;

            Vec3d end = eye.add(dirVec.normalize().multiply(reach + 0.1));
            BlockHitResult clip = mc.world.raycast(new RaycastContext(
                eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
            ));

            if (clip.getType() == HitResult.Type.BLOCK && clip.getBlockPos().equals(neighbor) && clip.getSide() == clickFace) {
                double d = eye.squaredDistanceTo(clip.getPos());
                if (d <= reachSq && d < bestDist) {
                    bestDist = d;
                    best = clip;
                }
            }
        }

        return best;
    }

    private BlockHitResult findBestInteractionHit(BlockPos anchorPos, double reach) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d eye = mc.player.getEyePos();
        double reachSq = reach * reach;

        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            if (!isFaceFacingEye(eye, anchorPos, side)) continue;

            Vec3d sample = getFaceSamplePoint(anchorPos, side);
            Vec3d dirVec = sample.subtract(eye);
            if (dirVec.lengthSquared() < 1.0E-6) continue;

            Vec3d end = eye.add(dirVec.normalize().multiply(reach + 0.1));
            BlockHitResult clip = mc.world.raycast(new RaycastContext(
                eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
            ));

            if (clip.getType() == HitResult.Type.BLOCK && clip.getBlockPos().equals(anchorPos) && clip.getSide() == side) {
                double d = eye.squaredDistanceTo(clip.getPos());
                if (d <= reachSq && d < bestDist) {
                    bestDist = d;
                    best = clip;
                }
            }
        }

        return best;
    }

    private BlockHitResult confirmSentRaycast(BlockHitResult expected, double reach) {
        if (mc.player == null || mc.world == null || expected == null) return null;

        RotationUtils rotUtils = RotationUtils.getInstance();
        Vec3d eye = mc.player.getEyePos();
        float yaw = (this.rotationMode.getValue() == RotationMode.CLIENT) ? mc.player.getYaw() : rotUtils.getSentYaw();
        float pitch = (this.rotationMode.getValue() == RotationMode.CLIENT) ? mc.player.getPitch() : rotUtils.getSentPitch();

        Vec3d look = RotationUtils.getRotationVector(pitch, yaw);
        Vec3d end = eye.add(look.multiply(reach + 0.1));
        BlockHitResult clip = mc.world.raycast(new RaycastContext(
            eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player
        ));

        if (clip != null && clip.getType() == HitResult.Type.BLOCK && clip.getBlockPos().equals(expected.getBlockPos())
            && eye.squaredDistanceTo(clip.getPos()) <= reach * reach) {
            if (this.pendingAction == ActionType.PLACE && expected.getSide() != null && clip.getSide() != expected.getSide()) {
                return null;
            }
            return clip;
        }

        return null;
    }

    private boolean isFaceFacingEye(Vec3d eye, BlockPos pos, Direction face) {
        return switch (face) {
            case DOWN -> eye.y < pos.getY();
            case UP -> eye.y > pos.getY() + 1.0;
            case NORTH -> eye.z < pos.getZ();
            case SOUTH -> eye.z > pos.getZ() + 1.0;
            case WEST -> eye.x < pos.getX();
            case EAST -> eye.x > pos.getX() + 1.0;
        };
    }

    private Vec3d getFaceSamplePoint(BlockPos pos, Direction face) {
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();

        return switch (face) {
            case DOWN -> new Vec3d(minX + 0.5, minY, minZ + 0.5);
            case UP -> new Vec3d(minX + 0.5, minY + 1.0, minZ + 0.5);
            case NORTH -> new Vec3d(minX + 0.5, minY + 0.5, minZ);
            case SOUTH -> new Vec3d(minX + 0.5, minY + 0.5, minZ + 1.0);
            case WEST -> new Vec3d(minX, minY + 0.5, minZ + 0.5);
            case EAST -> new Vec3d(minX + 1.0, minY + 0.5, minZ + 0.5);
        };
    }

    private boolean isUnsafeToExplode(BlockPos anchorPos) {
        if (this.testAlone.getValue() || !this.selfDamageSafety.getValue()) return false;

        boolean hasTotem = mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
            || mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        Vec3d center = anchorPos.toCenterPos();
        double playerDist = mc.player.getEyePos().distanceTo(center);

        if (hasTotem && playerDist >= 1.2) return false;

        double safeDist = this.minSafeDistance.getValue();
        if (playerDist < safeDist) {
            if (this.currentTarget != null) {
                double targetDist = this.currentTarget.getEyePos().distanceTo(center);
                if (playerDist <= targetDist - 0.5) return true;
            } else {
                return true;
            }
        }

        return !hasTotem && (mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= 6.0f && playerDist < 4.0;
    }

    private BlockPos findExistingAnchor(Entity target, double reach) {
        double reachSq = reach * reach;

        if (this.currentAnchorPos != null) {
            BlockState curState = mc.world.getBlockState(this.currentAnchorPos);
            if (curState.isOf(Blocks.RESPAWN_ANCHOR)
                && GrimUtils.closestEyeDistanceSqTo(mc.player, this.currentAnchorPos.toCenterPos()) <= reachSq) {
                return this.currentAnchorPos;
            }
        }

        BlockPos playerPos = mc.player.getBlockPos();
        int r = (int) Math.ceil(reach);
        BlockPos best = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = playerPos.add(x, y, z);
                    if (GrimUtils.closestEyeDistanceSqTo(mc.player, p.toCenterPos()) > reachSq) continue;

                    BlockState state = mc.world.getBlockState(p);
                    if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                        double d = target != null ? target.squaredDistanceTo(p.toCenterPos()) : mc.player.squaredDistanceTo(p.toCenterPos());
                        if (target == null || d <= 36.0) {
                            if (d < closestDist) {
                                closestDist = d;
                                best = p;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    private BlockPos findPlacementPosition(Entity target, double reach) {
        double reachSq = reach * reach;
        BlockPos targetPos = target.getBlockPos();

        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(targetPos);
        candidates.add(targetPos.up(1));
        candidates.add(targetPos.up(2));
        candidates.add(targetPos.north());
        candidates.add(targetPos.south());
        candidates.add(targetPos.east());
        candidates.add(targetPos.west());
        candidates.add(targetPos.down());

        candidates.sort(Comparator.comparingDouble(p -> target.squaredDistanceTo(p.toCenterPos())));

        for (BlockPos p : candidates) {
            if (GrimUtils.closestEyeDistanceSqTo(mc.player, p.toCenterPos()) > reachSq) continue;
            if (!mc.world.getBlockState(p).isReplaceable()) continue;

            Box box = new Box(p);
            if (mc.player.getBoundingBox().intersects(box)) continue;
            if (target.getBoundingBox().intersects(box)) continue;

            if (this.selfDamageSafety.getValue() && isUnsafeToExplode(p)) continue;

            BlockHitResult supportHit = findBestSupportHit(p, reach);
            if (supportHit != null) {
                return p;
            }
        }

        return null;
    }

    private Entity findTarget() {
        double rangeSq = this.targetRange.getValue() * this.targetRange.getValue();
        Entity best = null;
        double minDistance = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isSpectator() || player.isDead() || !player.isAlive()) continue;
            if (!this.targetCreative.getValue() && player.isCreative()) continue;

            double d = mc.player.squaredDistanceTo(player);
            if (d <= rangeSq && d < minDistance) {
                minDistance = d;
                best = player;
            }
        }

        if (best == null && this.targetMobs.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof LivingEntity living) || living instanceof PlayerEntity || !living.isAlive()) continue;
                double d = mc.player.squaredDistanceTo(entity);
                if (d <= rangeSq && d < minDistance) {
                    minDistance = d;
                    best = entity;
                }
            }
        }

        return best;
    }

    private FindItemResult findAnchor() {
        FindItemResult hotbar = InventoryUtil.findInHotbar(Items.RESPAWN_ANCHOR);
        if (hotbar.found()) return hotbar;

        if (this.replenish.getValue() && GrimUtils.inventoryClickClean()) {
            FindItemResult inv = InventoryUtil.find(Items.RESPAWN_ANCHOR);
            if (inv.found() && inv.slot() >= 9) {
                InventoryUtil.quickMove(inv.slot());
                return InventoryUtil.findInHotbar(Items.RESPAWN_ANCHOR);
            }
        }

        return hotbar;
    }

    private FindItemResult findGlowstone() {
        FindItemResult hotbar = InventoryUtil.findInHotbar(Items.GLOWSTONE);
        if (hotbar.found()) return hotbar;

        if (this.replenish.getValue() && GrimUtils.inventoryClickClean()) {
            FindItemResult inv = InventoryUtil.find(Items.GLOWSTONE);
            if (inv.found() && inv.slot() >= 9) {
                InventoryUtil.quickMove(inv.slot());
                return InventoryUtil.findInHotbar(Items.GLOWSTONE);
            }
        }

        return hotbar;
    }

    private FindItemResult findBreakItem() {
        if (mc.player == null) return new FindItemResult(-1, 0);

        int current = mc.player.getInventory().getSelectedSlot();
        ItemStack currentStack = mc.player.getInventory().getStack(current);
        if (!currentStack.isOf(Items.GLOWSTONE)) {
            return new FindItemResult(current, currentStack.getCount());
        }

        int preferredSlot = -1;
        int fallbackSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.GLOWSTONE)) {
                if (stack.isEmpty()
                    || stack.isIn(ItemTags.SWORDS)
                    || stack.isIn(ItemTags.AXES)
                    || stack.isOf(Items.TOTEM_OF_UNDYING)) {
                    preferredSlot = i;
                    break;
                }
                if (fallbackSlot == -1) {
                    fallbackSlot = i;
                }
            }
        }

        int targetSlot = preferredSlot != -1 ? preferredSlot : fallbackSlot;
        if (targetSlot != -1) {
            return new FindItemResult(targetSlot, mc.player.getInventory().getStack(targetSlot).getCount());
        }

        return new FindItemResult(-1, 0);
    }
}
