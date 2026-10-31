package me.aleksilassila.litematica.printer.implementation.actions;

import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InteractActionImplTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void staleSupportsAreRecheckedWithoutBreakingIntentionalInteractions() {
        LocalPlayer player = mock(LocalPlayer.class);
        Level level = mock(Level.class);
        when(player.level()).thenReturn(level);
        when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        PrinterPlacementContext context = new PrinterPlacementContext(player,
                new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false), ItemStack.EMPTY, 0);
        InteractActionImpl placement = new InteractActionImpl(context, true);
        InteractActionImpl interaction = new InteractActionImpl(context);
        assertFalse(placement.shouldSneak(player));
        for (Block support : new Block[]{Blocks.CHEST, Blocks.FURNACE, Blocks.OAK_DOOR,
                Blocks.BARREL, Blocks.CRAFTER, Blocks.STONE_BUTTON}) {
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(support.defaultBlockState());
            assertTrue(placement.shouldSneak(player), support.toString());
            assertFalse(interaction.shouldSneak(player), support.toString());
        }
    }
}
