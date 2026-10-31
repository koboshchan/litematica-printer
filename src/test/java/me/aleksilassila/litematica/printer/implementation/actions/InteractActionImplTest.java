package me.aleksilassila.litematica.printer.implementation.actions;

import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.BlockHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Arrays;
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
        for (Block support : BuiltInRegistries.BLOCK) {
            if (!BlockHelper.interactiveBlocks.stream().anyMatch(type -> type.isInstance(support))) {
                continue;
            }
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(support.defaultBlockState());
            assertTrue(placement.shouldSneak(player), support.toString());
            assertFalse(interaction.shouldSneak(player), support.toString());
        }
        for (Block support : new Block[]{Blocks.STONE, Blocks.COPPER_BULB.asList().getFirst(), Blocks.TRIAL_SPAWNER}) {
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(support.defaultBlockState());
            assertFalse(placement.shouldSneak(player), support.toString());
        }
    }

    @Test void everyVanillaUseOverrideIsCoveredWithoutSubclassInitialization() {
        for (Block block : BuiltInRegistries.BLOCK) {
            boolean overridesUse = false;
            for (Class<?> type = block.getClass(); type != Block.class; type = type.getSuperclass()) {
                overridesUse |= Arrays.stream(type.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals("useWithoutItem")
                                || method.getName().equals("useItemOn"));
            }
            if (overridesUse) {
                assertTrue(BlockHelper.interactiveBlocks.stream().anyMatch(type -> type.isInstance(block)),
                        () -> "Missing right-click handler for " + BuiltInRegistries.BLOCK.getKey(block));
            }
        }
    }
}
