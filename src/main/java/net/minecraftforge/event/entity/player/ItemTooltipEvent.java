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

package net.minecraftforge.event.entity.player;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ItemTooltipEvent extends PlayerEvent {
	private final TooltipContext flags;
	@Nonnull
	private final ItemStack itemStack;
	private final List<Text> toolTip;

	/**
	 * This event is fired in {@link ItemStack#getTooltip(EntityPlayer, ITooltipFlag)}, which in turn is called from it's respective GUIContainer.
	 * Tooltips are also gathered with a null entityPlayer during startup by {@link Minecraft#populateSearchTreeManager()}.
	 */
	public ItemTooltipEvent(@Nonnull ItemStack itemStack, @Nullable PlayerEntity entityPlayer, List<Text> list, TooltipContext flags) {
		super(entityPlayer);
		this.itemStack = itemStack;
		this.toolTip = list;
		this.flags = flags;
	}

	/**
	 * Use to determine if the advanced information on item tooltips is being shown, toggled by F3+H.
	 */
	public TooltipContext getFlags() {
		return flags;
	}

	/**
	 * The {@link ItemStack} with the tooltip.
	 */
	@Nonnull
	public ItemStack getItemStack() {
		return itemStack;
	}

	/**
	 * The {@link ItemStack} tooltip.
	 */
	public List<Text> getToolTip() {
		return toolTip;
	}

	/**
	 * This event is fired with a null player during startup when populating search trees for tooltips.
	 */
	@Override
	@Nullable
	public PlayerEntity getEntityPlayer() {
		return super.getEntityPlayer();
	}
}
