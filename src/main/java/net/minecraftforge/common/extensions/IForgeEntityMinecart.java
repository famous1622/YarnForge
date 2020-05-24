/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.common.extensions;

import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.IMinecartCollisionHandler;

public interface IForgeEntityMinecart
{
    public static float DEFAULT_MAX_SPEED_AIR_LATERAL = 0.4f;
    public static float DEFAULT_MAX_SPEED_AIR_VERTICAL = -1.0f;
    public static double DEFAULT_AIR_DRAG = 0.95f;
    public static IMinecartCollisionHandler COLLISIONS = null;

    default AbstractMinecartEntity getMinecart() {
        return (AbstractMinecartEntity)this;
    }

    /**
     * Gets the current global Minecart Collision handler if none
     * is registered, returns null
     * @return The collision handler or null
     */
    default IMinecartCollisionHandler getCollisionHandler() {
        return COLLISIONS;
    }

    /**
     * Internal, returns the current spot to look for the attached rail.
     */
    default BlockPos getCurrentRailPosition()
    {
        int x = MathHelper.floor(getMinecart().getX());
        int y = MathHelper.floor(getMinecart().getY());
        int z = MathHelper.floor(getMinecart().getZ());
        BlockPos pos = new BlockPos(x, y - 1, z);
        if (getMinecart().world.getBlockState(pos).matches(BlockTags.field_15463)) pos = pos.down();
        return pos;
    }

    double getMaxSpeedWithRail();

    /**
     * Moved to allow overrides.
     * This code handles minecart movement and speed capping when on a rail.
     */
    void moveMinecartOnRail(BlockPos pos);

    /**
     * This function returns an ItemStack that represents this cart.
     * This should be an ItemStack that can be used by the player to place the cart,
     * but is not necessary the item the cart drops when destroyed.
     * @return An ItemStack that can be used to place the cart.
     */
    default ItemStack getCartItem()
    {
        switch (getMinecart().getMinecartType())
        {
            case field_7679: return new ItemStack(Items.field_8063);
            case field_7678:   return new ItemStack(Items.field_8388);
            case field_7675:     return new ItemStack(Items.field_8069);
            case field_7677:  return new ItemStack(Items.field_8836);
            case field_7681: return new ItemStack(Items.field_8220);
            default:      return new ItemStack(Items.field_8045);
        }
    }

    /**
     * Returns true if this cart can currently use rails.
     * This function is mainly used to gracefully detach a minecart from a rail.
     * @return True if the minecart can use rails.
     */
    boolean canUseRail();

    /**
     * Set whether the minecart can use rails.
     * This function is mainly used to gracefully detach a minecart from a rail.
     * @param use Whether the minecart can currently use rails.
     */
    void setCanUseRail(boolean use);

    /**
     * Return false if this cart should not call onMinecartPass() and should ignore Powered Rails.
     * @return True if this cart should call onMinecartPass().
     */
    default boolean shouldDoRailFunctions() {
        return true;
    }

    /**
     * Returns true if this cart is self propelled.
     * @return True if powered.
     */
    default boolean isPoweredCart() {
        return getMinecart().getMinecartType() == AbstractMinecartEntity.Type.field_7679;
    }

    /**
     * Returns true if this cart can be ridden by an Entity.
     * @return True if this cart can be ridden.
     */
    default boolean canBeRidden() {
        return getMinecart().getMinecartType() == AbstractMinecartEntity.Type.field_7674;
    }

    /**
     * Returns the carts max speed when traveling on rails. Carts going faster
     * than 1.1 cause issues with chunk loading. Carts cant traverse slopes or
     * corners at greater than 0.5 - 0.6. This value is compared with the rails
     * max speed and the carts current speed cap to determine the carts current
     * max speed. A normal rail's max speed is 0.4.
     *
     * @return Carts max speed.
     */
    default float getMaxCartSpeedOnRail() {
        return 1.2f;
    }

    /**
     * Returns the current speed cap for the cart when traveling on rails. This
     * functions differs from getMaxCartSpeedOnRail() in that it controls
     * current movement and cannot be overridden. The value however can never be
     * higher than getMaxCartSpeedOnRail().
     *
     * @return
     */
    float getCurrentCartSpeedCapOnRail();
    void setCurrentCartSpeedCapOnRail(float value);
    float getMaxSpeedAirLateral();
    void setMaxSpeedAirLateral(float value);
    float getMaxSpeedAirVertical();
    void setMaxSpeedAirVertical(float value);
    double getDragAir();
    void setDragAir(double value);

    default double getSlopeAdjustment() {
        return 0.0078125D;
    }

    /**
     * Called from Detector Rails to retrieve a redstone power level for comparators.
     */
    default int getComparatorLevel() {
        return -1;
    }
}
