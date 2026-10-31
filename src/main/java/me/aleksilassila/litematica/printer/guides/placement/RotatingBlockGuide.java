package me.aleksilassila.litematica.printer.guides.placement;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.world.level.block.state.BlockState;


import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RotatingBlockGuide extends GeneralPlacementGuide {
    public RotatingBlockGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    protected List<Direction> getPossibleSides() {
        Block block = state.targetState.getBlock();
        if (block instanceof WallSkullBlock || block instanceof WallSignBlock || block instanceof WallBannerBlock) {
            Optional<Direction> side = getProperty(state.targetState, BlockStateProperties.HORIZONTAL_FACING)
                    .map(Direction::getOpposite);
            return side.map(Collections::singletonList).orElseGet(Collections::emptyList);
        }

        return Collections.singletonList(Direction.DOWN);
    }

    @Override
    public boolean skipOtherGuides() {
        return true;
    }

    static float placementYaw(BlockState target) {
        // Wall variants have FACING, not ROTATION_16. Defaulting the latter to
        // zero made every wall sign, banner and skull look south.
        if (target.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return target.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite().toYRot();
        }
        int rotation = target.getValue(BlockStateProperties.ROTATION_16);
        if (target.getBlock() instanceof BannerBlock || target.getBlock() instanceof StandingSignBlock) {
            rotation = (rotation + 8) % 16;
        }
        return rotation * 22.5f;
    }

    @Override
    public PrinterPlacementContext getPlacementContext(LocalPlayer player) {
        PrinterPlacementContext base = super.getPlacementContext(player);
        if (base == null)
            return null;

        if (!Configs.ROTATE.getBooleanValue())
            return base;

        final float yaw = placementYaw(targetState);
        // Put the proposed rotation in the context itself so simulation,
        // PrepareAction and local prediction all use the same angle.
        return new PrinterPlacementContext(player, base.hitResult, base.getItemInHand(),
                base.requiredItemSlot, base.lookDirection, base.shouldSneak) {
            @Override
            public float getPlayerYaw() { return yaw; }
            @Override
            public float getPlayerPitch() { return 0; }
            @Override
            public boolean isRotationOverridden() { return true; }
        };
    }
}
