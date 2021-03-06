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

package net.minecraftforge.items;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.container.Slot;
import net.minecraft.item.ItemStack;

public class SlotItemHandler extends Slot {
	private static Inventory emptyInventory = new BasicInventory(0);
	private final IItemHandler itemHandler;
	private final int index;

	public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
		super(emptyInventory, index, xPosition, yPosition);
		this.itemHandler = itemHandler;
		this.index = index;
	}

	@Override
	public boolean canInsert(@Nonnull ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		return itemHandler.isItemValid(index, stack);
	}

	@Override
	@Nonnull
	public ItemStack getStack() {
		return this.getItemHandler().getStackInSlot(index);
	}

	// Override if your IItemHandler does not implement IItemHandlerModifiable
	@Override
	public void setStack(@Nonnull ItemStack stack) {
		((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(index, stack);
		this.markDirty();
	}

	@Override
	public void onStackChanged(@Nonnull ItemStack p_75220_1_, @Nonnull ItemStack p_75220_2_) {

	}

	@Override
	public int getMaxStackAmount() {
		return this.itemHandler.getSlotLimit(this.index);
	}

	@Override
	public int getMaxStackAmount(@Nonnull ItemStack stack) {
		ItemStack maxAdd = stack.copy();
		int maxInput = stack.getMaxCount();
		maxAdd.setCount(maxInput);

		IItemHandler handler = this.getItemHandler();
		ItemStack currentStack = handler.getStackInSlot(index);
		if (handler instanceof IItemHandlerModifiable) {
			IItemHandlerModifiable handlerModifiable = (IItemHandlerModifiable) handler;

			handlerModifiable.setStackInSlot(index, ItemStack.EMPTY);

			ItemStack remainder = handlerModifiable.insertItem(index, maxAdd, true);

			handlerModifiable.setStackInSlot(index, currentStack);

			return maxInput - remainder.getCount();
		} else {
			ItemStack remainder = handler.insertItem(index, maxAdd, true);

			int current = currentStack.getCount();
			int added = maxInput - remainder.getCount();
			return current + added;
		}
	}

	@Override
	public boolean canTakeItems(PlayerEntity playerIn) {
		return !this.getItemHandler().extractItem(index, 1, true).isEmpty();
	}

	@Override
	@Nonnull
	public ItemStack takeStack(int amount) {
		return this.getItemHandler().extractItem(index, amount, false);
	}

	public IItemHandler getItemHandler() {
		return itemHandler;
	}
/* TODO Slot patches
    @Override
    public boolean isSameInventory(Slot other)
    {
        return other instanceof SlotItemHandler && ((SlotItemHandler) other).getItemHandler() == this.itemHandler;
    }*/
}
