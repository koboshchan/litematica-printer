package me.aleksilassila.litematica.printer.implementation;

import me.aleksilassila.litematica.printer.actions.PrepareAction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrinterPlacementContextTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private PrinterPlacementContext context(float yaw, float pitch, boolean sneak) {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(player.level()).thenReturn(level);
        when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        float[] rotation = {13, 27};
        when(player.getYRot()).thenAnswer(i -> rotation[0]);
        when(player.getXRot()).thenAnswer(i -> rotation[1]);
        when(player.getViewYRot(anyFloat())).thenAnswer(i -> rotation[0]);
        when(player.getViewXRot(anyFloat())).thenAnswer(i -> rotation[1]);
        doAnswer(i -> { rotation[0] = i.getArgument(0); return null; }).when(player).setYRot(anyFloat());
        doAnswer(i -> { rotation[1] = i.getArgument(0); return null; }).when(player).setXRot(anyFloat());
        when(player.getDirection()).thenAnswer(i -> Direction.fromYRot(rotation[0]));
        return new PrinterPlacementContext(player,
                new BlockHitResult(new Vec3(0.5, 1, 0.5), Direction.UP, BlockPos.ZERO, false),
                ItemStack.EMPTY, 0, null, sneak) {
            @Override public float getPlayerYaw() { return yaw; }
            @Override public float getPlayerPitch() { return pitch; }
            @Override public boolean isRotationOverridden() { return true; }
        };
    }

    @Test void allAirplaceFacingsUseSimulatedRotationAndRestorePlayer() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PrinterPlacementContext ctx = context(facing.toYRot(), 0, false);
            assertEquals(facing, ctx.getHorizontalDirection());
            assertEquals(facing, ctx.getNearestLookingDirection());
            assertEquals(facing, ctx.getNearestLookingDirections()[0]);
            assertEquals(facing.getOpposite(), Blocks.FURNACE.getStateForPlacement(ctx)
                    .getValue(BlockStateProperties.HORIZONTAL_FACING));
            PrepareAction action = new PrepareAction(ctx);
            assertEquals(facing.toYRot(), action.yaw);
            assertEquals(0, action.pitch);
            assertEquals(13, ctx.getPlayer().getYRot());
            assertEquals(27, ctx.getPlayer().getXRot());
        }
    }

    @Test void verticalAirplaceFacingMatchesPitch() {
        assertEquals(Direction.UP, context(0, -90, false).getNearestLookingDirection());
        assertEquals(Direction.DOWN, context(0, 90, false).getNearestLookingDirection());
    }

    @Test void allSixteenSkullRotationsUseContextYaw() {
        for (int step = 0; step < 16; step++) {
            PrinterPlacementContext ctx = context(step * 22.5f, 0, false);
            assertEquals(step, Blocks.SKELETON_SKULL.getStateForPlacement(ctx)
                    .getValue(BlockStateProperties.ROTATION_16));
            assertEquals(step * 22.5f, new PrepareAction(ctx).yaw);
        }
    }

    @Test void secondaryUseAndAirTargetMatchSimulation() {
        PrinterPlacementContext ctx = context(90, 0, true);
        assertTrue(ctx.isSecondaryUseActive());
        assertEquals(BlockPos.ZERO, ctx.getClickedPos());
        assertTrue(ctx.replacingClickedOnBlock());
    }
}
