package me.aleksilassila.litematica.printer.implementation;

import javax.annotation.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

public class PrinterPlacementContext extends BlockPlaceContext
{
    public final @Nullable Direction lookDirection;
    public final boolean shouldSneak;
    public final BlockHitResult hitResult;
    public final int requiredItemSlot;

    public PrinterPlacementContext(Player player, BlockHitResult hitResult, ItemStack requiredItem,
                                   int requiredItemSlot)
    {
        this(player, hitResult, requiredItem, requiredItemSlot, null, false);
    }

    public PrinterPlacementContext(Player player, BlockHitResult hitResult, ItemStack requiredItem,
                                   int requiredItemSlot, @Nullable Direction lookDirection, boolean requiresSneaking)
    {
        super(player, InteractionHand.MAIN_HAND, requiredItem, hitResult);

        this.lookDirection = lookDirection;
        this.shouldSneak = requiresSneaking;
        this.hitResult = hitResult;
        this.requiredItemSlot = requiredItemSlot;
    }

    @Override
    public @NonNull Direction getNearestLookingDirection()
    {
        if (isRotationOverridden())
        {
            float origYaw = getPlayer().getYRot();
            float origPitch = getPlayer().getXRot();
            try
            {
                getPlayer().setYRot(getPlayerYaw());
                getPlayer().setXRot(getPlayerPitch());
                return super.getNearestLookingDirection();
            }
            finally
            {
                getPlayer().setYRot(origYaw);
                getPlayer().setXRot(origPitch);
            }
        }
        return lookDirection == null ? super.getNearestLookingDirection() : lookDirection;
    }

    @Override
    public @NonNull Direction[] getNearestLookingDirections()
    {
        if (isRotationOverridden())
        {
            float origYaw = getPlayer().getYRot();
            float origPitch = getPlayer().getXRot();
            try
            {
                getPlayer().setYRot(getPlayerYaw());
                getPlayer().setXRot(getPlayerPitch());
                return super.getNearestLookingDirections();
            }
            finally
            {
                getPlayer().setYRot(origYaw);
                getPlayer().setXRot(origPitch);
            }
        }
        return super.getNearestLookingDirections();
    }

    @Override
    public @NonNull Direction getNearestLookingVerticalDirection()
    {
        if (isRotationOverridden())
        {
            float origYaw = getPlayer().getYRot();
            float origPitch = getPlayer().getXRot();
            try
            {
                getPlayer().setYRot(getPlayerYaw());
                getPlayer().setXRot(getPlayerPitch());
                return super.getNearestLookingVerticalDirection();
            }
            finally
            {
                getPlayer().setYRot(origYaw);
                getPlayer().setXRot(origPitch);
            }
        }
        if (lookDirection != null && lookDirection.getOpposite() == super.getNearestLookingVerticalDirection())
        {
            return lookDirection;
        }
        return super.getNearestLookingVerticalDirection();
    }

    @Override
    public @NonNull Direction getHorizontalDirection()
    {
        if (isRotationOverridden())
        {
            float origYaw = getPlayer().getYRot();
            float origPitch = getPlayer().getXRot();
            try
            {
                getPlayer().setYRot(getPlayerYaw());
                getPlayer().setXRot(getPlayerPitch());
                return super.getHorizontalDirection();
            }
            finally
            {
                getPlayer().setYRot(origYaw);
                getPlayer().setXRot(origPitch);
            }
        }
        if (lookDirection == null || !lookDirection.getAxis().isHorizontal())
        {
            return super.getHorizontalDirection();
        }

        return lookDirection;
    }

    public float getPlayerYaw()
    {
        return getPlayer().getYRot();
    }

    public float getPlayerPitch()
    {
        return getPlayer().getXRot();
    }

    public boolean isRotationOverridden()
    {
        return false;
    }


    @Override
    public String toString()
    {
        return "PrinterPlacementContext{" +
                "lookDirection=" + lookDirection +
                ", requiresSneaking=" + shouldSneak +
                ", blockPos=" + hitResult.getBlockPos() +
                ", side=" + hitResult.getDirection() +
                // ", hitVec=" + hitResult +
                '}';
    }
}
