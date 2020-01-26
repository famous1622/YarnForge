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

import javax.annotation.Nonnull;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event.HasResult;

import net.minecraft.item.ItemUsageContext;

/**
 * This event is fired when a player attempts to use a Hoe on a block, it
 * can be canceled to completely prevent any further processing.
 * <p>
 * You can also set the result to ALLOW to mark the event as processed
 * and damage the hoe.
 * <p>
 * setResult(ALLOW) is the same as the old setHandled();
 */
@Cancelable
@HasResult
public class UseHoeEvent extends PlayerEvent {
	private final ItemUsageContext context;

	public UseHoeEvent(ItemUsageContext context) {
		super(context.getPlayer());
		this.context = context;
	}

	@Nonnull
	public ItemUsageContext getContext() {
		return context;
	}
}
