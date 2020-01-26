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

package net.minecraftforge.items.wrapper;

import net.minecraftforge.items.IItemHandlerModifiable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class RecipeWrapper implements Inventory {

	protected final IItemHandlerModifiable inv;

	public RecipeWrapper(IItemHandlerModifiable inv) {
		this.inv = inv;
	}

	/**
	 * Returns the size of this inventory.  Must be equivalent to {@link #getHeight()} * {@link #getWidth()}.
	 */
	@Override
	public int getInvSize() {
		return inv.getSlots();
	}

	/**
	 * Returns the stack in this slot.  This stack should be a modifiable reference, not a copy of a stack in your inventory.
	 */
	@Override
	public ItemStack getInvStack(int slot) {
		return inv.getStackInSlot(slot);
	}

	/**
	 * Attempts to remove n items from the specified slot.  Returns the split stack that was removed.  Modifies the inventory.
	 */
	@Override
	public ItemStack takeInvStack(int slot, int count) {
		ItemStack stack = inv.getStackInSlot(slot);
		return stack.isEmpty() ? ItemStack.EMPTY : stack.split(count);
	}

	/**
	 * Sets the contents of this slot to the provided stack.
	 */
	@Override
	public void setInvStack(int slot, ItemStack stack) {
		inv.setStackInSlot(slot, stack);
	}

	/**
	 * Removes the stack contained in this slot from the underlying handler, and returns it.
	 */
	@Override
	public ItemStack removeInvStack(int index) {
		ItemStack s = getInvStack(index);
		if (s.isEmpty()) return ItemStack.EMPTY;
		setInvStack(index, ItemStack.EMPTY);
		return s;
	}

	@Override
	public boolean isInvEmpty() {
		for (int i = 0; i < inv.getSlots(); i++) {
			if (!inv.getStackInSlot(i).isEmpty()) return false;
		}
		return true;
	}

	@Override
	public boolean isValidInvStack(int slot, ItemStack stack) {
		return inv.isItemValid(slot, stack);
	}

	@Override
	public void clear() {
		for (int i = 0; i < inv.getSlots(); i++) {
			inv.setStackInSlot(i, ItemStack.EMPTY);
		}
	}

	//The following methods are never used by vanilla in crafting.  They are defunct as mods need not override them.
	@Override
	public int getInvMaxStackAmount() {
		return 0;
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUseInv(PlayerEntity player) {
		return false;
	}

	@Override
	public void onInvOpen(PlayerEntity player) {
	}

	@Override
	public void onInvClose(PlayerEntity player) {
	}

}
