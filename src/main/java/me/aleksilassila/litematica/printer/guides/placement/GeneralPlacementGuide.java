package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.Printer;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    protected Vec3 getHitModifier(Direction validSide) {
        return new Vec3(0, 0, 0);
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
                    !neighborState.currentState.canBeReplaced())
                validSides.add(side);
        }

        for (Direction validSide : validSides) {
            if (!isInteractive(state.offset(validSide).currentState.getBlock())) {
                return Optional.of(validSide);
            }
        }

        if (!validSides.isEmpty()) {
            return Optional.of(validSides.getFirst());
        }

        if (Configs.PRINT_IN_AIR.getBooleanValue() && !getRequiresSupport()) {
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

    private Optional<Vec3> getHitVector(SchematicBlockState state) {
        return getValidSide(state).map(side -> Vec3.atCenterOf(state.blockPos)
                .add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5))
                .add(getHitModifier(side)));
    }

    @Nullable
    public PrinterPlacementContext getPlacementContext(LocalPlayer player) {
        try {
            Optional<Direction> validSide = getValidSide(state);
            Optional<Vec3> hitVec = getHitVector(state);
            Optional<ItemStack> requiredItem = getRequiredItem(player);
            int requiredSlot = getRequiredItemStackSlot(player);

            if (validSide.isEmpty() || hitVec.isEmpty() || requiredItem.isEmpty() || requiredSlot == -1)
                return null;

            Optional<Direction> lookDirection = getLookDirection();
            boolean requiresShift = getUseShift(state);

            Direction side = validSide.get();
            BlockPos clickPos = state.blockPos.relative(side);
            Direction hitSide = side.getOpposite();

            if (Configs.PRINT_IN_AIR.getBooleanValue() && !getRequiresSupport() && state.world.getBlockState(clickPos).canBeReplaced()) {
                Printer.printDebug("AirPlace triggered for {} at {}", targetState.getBlock(), state.blockPos);
                clickPos = state.blockPos;
                hitSide = side;

                if (lookDirection.isEmpty()) {
                    Vec3 diff = hitVec.get().subtract(player.getEyePosition());
                    double diffX = diff.x;
                    double diffY = diff.y;
                    double diffZ = diff.z;
                    double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
                    float yaw = (float) (Math.atan2(diffZ, diffX) * 180 / Math.PI) - 90;
                    float pitch = (float) -(Math.atan2(diffY, diffXZ) * 180 / Math.PI);

                    final BlockPos finalClickPos = clickPos;
                    final Direction finalHitSide = hitSide;
                    return new PrinterPlacementContext(player, new BlockHitResult(hitVec.get(), finalHitSide, finalClickPos, false),
                            requiredItem.get(), requiredSlot, null, requiresShift) {
                        @Override
                        public float getPlayerYaw() { return yaw; }
                        @Override
                        public float getPlayerPitch() { return pitch; }
                        @Override
                        public boolean isRotationOverridden() { return true; }
                    };
                }
            }

            BlockHitResult blockHitResult = new BlockHitResult(hitVec.get(), hitSide, clickPos, false);

            return new PrinterPlacementContext(player, blockHitResult, requiredItem.get(), requiredSlot,
                    lookDirection.orElse(null), requiresShift);
        } catch (Exception e) {
            Printer.logger.error("getPlacementContext(): Exception caught: {}", e.getMessage());
            //e.printStackTrace();
            return null;
        }
    }
}
