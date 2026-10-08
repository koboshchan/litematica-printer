package me.aleksilassila.litematica.printer.actions;

import me.aleksilassila.litematica.printer.Printer;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.world.phys.BlockHitResult;

abstract public class InteractAction extends Action {
    public final PrinterPlacementContext context;

    public InteractAction(PrinterPlacementContext context) {
        this.context = context;
    }

    protected abstract void interact(Minecraft client, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult);

    @Override
    public void send(Minecraft client, LocalPlayer player) {
        // Input is refreshed every tick, so sneak must be sent at the actual click,
        // not just in PrepareAction several ticks earlier. Local prediction also
        // checks the player's shift flag, not only the input packet.
        Input originalInput = player.input.keyPresses;
        boolean originalShift = player.isShiftKeyDown();
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        Input clickInput = new Input(originalInput.forward(), originalInput.backward(),
                originalInput.left(), originalInput.right(), originalInput.jump(),
                context.shouldSneak, originalInput.sprint());
        try {
            player.input.keyPresses = clickInput;
            player.setShiftKeyDown(context.shouldSneak);
            player.connection.send(new ServerboundPlayerInputPacket(clickInput));
            if (Configs.ROTATE.getBooleanValue()) {
                PrepareAction rotation = new PrepareAction(context);
                float yaw = rotation.modifyYaw ? rotation.yaw : originalYaw;
                float pitch = rotation.modifyPitch ? rotation.pitch : originalPitch;
                player.setYRot(yaw);
                player.setXRot(pitch);
                player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch,
                        player.onGround(), player.horizontalCollision));
            }
            interact(client, player, InteractionHand.MAIN_HAND, context.hitResult);
        } finally {
            player.setYRot(originalYaw);
            player.setXRot(originalPitch);
            player.input.keyPresses = originalInput;
            player.setShiftKeyDown(originalShift);
            player.connection.send(new ServerboundPlayerInputPacket(originalInput));
        }
        Printer.printDebug("InteractAction.send: Blockpos: {} Side: {} HitPos: {}", context.getClickedPos(), context.getClickedFace(), context.getClickLocation());
    }

    @Override
    public String toString() {
        return "InteractAction{" +
                "context=" + context +
                '}';
    }
}
