package me.aleksilassila.litematica.printer.guides.placement;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RotatingBlockGuideTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void wallVariantsFaceAwayFromTheSupportInEveryDirection() {
        for (Block block : new Block[]{Blocks.OAK_WALL_SIGN, Blocks.WALL_BANNER.white(),
                Blocks.SKELETON_WALL_SKULL}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                assertEquals(facing.getOpposite().toYRot(), RotatingBlockGuide.placementYaw(
                        block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)));
            }
        }
    }

    @Test void standingVariantsKeepAllSixteenRotations() {
        for (int rotation = 0; rotation < 16; rotation++) {
            assertEquals(rotation * 22.5f, RotatingBlockGuide.placementYaw(
                    Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, rotation)));
            for (Block block : new Block[]{Blocks.OAK_SIGN, Blocks.BANNER.white()}) {
                assertEquals((rotation + 8) % 16 * 22.5f, RotatingBlockGuide.placementYaw(
                        block.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, rotation)));
            }
        }
    }
}
