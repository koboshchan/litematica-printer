package me.aleksilassila.litematica.printer.guides.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import me.aleksilassila.litematica.printer.Printer;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * An old school guide where there are defined specific conditions
 * for player state depending on the block being placed.
 */
public class GeneralPlacementGuide extends PlacementGuide {
    public GeneralPlacementGuide(SchematicBlockState state) {
        super(state);
    }

    protected List<Direction> getPossibleSides() {
        return Arrays.asList(Direction.values());
    }

    protected Optional<Direction> getLookDirection() {
        return Optional.empty();
    }

    protected boolean getRequiresSupport() {
        return false;
    }

    protected boolean getRequiresExplicitShift() {
        return false;
    }

    protected Vec3d getHitModifier(Direction validSide) {
        return new Vec3d(0, 0, 0);
    }

    private Optional<Direction> getValidSide(SchematicBlockState state) {
        List<Direction> sides = getPossibleSides();

        List<Direction> validSides = new ArrayList<>();
        for (Direction side : sides) {
            SchematicBlockState neighborState = state.offset(side);

            if (getProperty(neighborState.currentState, SlabBlock.TYPE).orElse(null) == SlabType.DOUBLE) {
                validSides.add(side);
                continue;
            }

            if (canBeClicked(neighborState.world, neighborState.blockPos) && // Handle unclickable grass for example
                    !neighborState.currentState.isReplaceable())
                validSides.add(side);
        }

        for (Direction validSide : validSides) {
            if (!isInteractive(state.offset(validSide).currentState.getBlock())) {
                return Optional.of(validSide);
            }
        }

        if (!validSides.isEmpty()) {
            return Optional.of(validSides.get(0));
        }

        if (Configs.AIR_PLACE.getBooleanValue() && !getRequiresSupport()) {
            return Optional.of(Direction.UP);
        }

        return Optional.empty();
    }

    protected boolean getUseShift(SchematicBlockState state) {
        if (getRequiresExplicitShift())
            return true;

        Direction clickSide = getValidSide(state).orElse(null);
        if (clickSide == null)
            return false;
        return isInteractive(state.offset(clickSide).currentState.getBlock());
    }

    private Optional<Vec3d> getHitVector(SchematicBlockState state) {
        return getValidSide(state).map(side -> Vec3d.ofCenter(state.blockPos)
                .add(Vec3d.of(side.getVector()).multiply(0.5))
                .add(getHitModifier(side)));
    }

    @Nullable
    public PrinterPlacementContext getPlacementContext(ClientPlayerEntity player) {
        try {
            Optional<Direction> validSide = getValidSide(state);
            Optional<Vec3d> hitVec = getHitVector(state);
            Optional<ItemStack> requiredItem = getRequiredItem(player);
            int requiredSlot = getRequiredItemStackSlot(player);

            if (validSide.isEmpty() || hitVec.isEmpty() || requiredItem.isEmpty() || requiredSlot == -1)
                return null;

            Optional<Direction> lookDirection = getLookDirection();
            boolean requiresShift = getUseShift(state);

            Direction side = validSide.get();
            BlockPos clickPos = state.blockPos.offset(side);
            Direction hitSide = side.getOpposite();

            if (Configs.AIR_PLACE.getBooleanValue() && !getRequiresSupport() && state.world.getBlockState(clickPos).isReplaceable()) {
                Printer.printDebug("AirPlace triggered for {} at {}", targetState.getBlock(), state.blockPos);
                clickPos = state.blockPos;
                hitSide = side;

                if (lookDirection.isEmpty()) {
                    Vec3d diff = hitVec.get().subtract(player.getEyePos());
                    double diffX = diff.x;
                    double diffY = diff.y;
                    double diffZ = diff.z;
                    double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
                    float yaw = (float) (Math.atan2(diffZ, diffX) * 180 / Math.PI) - 90;
                    float pitch = (float) -(Math.atan2(diffY, diffXZ) * 180 / Math.PI);

                    return new PrinterPlacementContext(player, new BlockHitResult(hitVec.get(), hitSide, clickPos, false),
                            requiredItem.get(), requiredSlot, null, requiresShift) {
                        @Override
                        public float getPlayerYaw() { return yaw; }
                        @Override
                        public float getPlayerPitch() { return pitch; }
                    };
                }
            }

            BlockHitResult blockHitResult = new BlockHitResult(hitVec.get(), hitSide,
                    clickPos, false);

            return new PrinterPlacementContext(player, blockHitResult, requiredItem.get(), requiredSlot,
                    lookDirection.orElse(null), requiresShift);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
