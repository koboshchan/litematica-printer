package me.aleksilassila.litematica.printer.implementation.actions;

import me.aleksilassila.litematica.printer.actions.InteractAction;
import me.aleksilassila.litematica.printer.BlockHelper;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class InteractActionImpl extends InteractAction {
    private final boolean placement;

    public InteractActionImpl(PrinterPlacementContext context) {
        this(context, false);
    }

    public InteractActionImpl(PrinterPlacementContext context, boolean placement) {
        super(context);
        this.placement = placement;
    }

    @Override
    protected boolean shouldSneak(LocalPlayer player) {
        // The clicked block can change while PrepareAction waits in the queue.
        // Recheck at the click, but never suppress intentional interaction guides.
        return context.shouldSneak || placement && BlockHelper.interactiveBlocks.stream()
                .anyMatch(type -> type.isInstance(player.level()
                        .getBlockState(context.hitResult.getBlockPos()).getBlock()));
    }

    @Override
    protected void interact(Minecraft client, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (client.gameMode != null) {
            client.gameMode.useItemOn(player, hand, hitResult);

        }
    }
}
