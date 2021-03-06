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

package net.minecraftforge.client.event;

import javax.annotation.Nonnull;

import net.minecraftforge.eventbus.api.Cancelable;

import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;

/**
 * This event is called when an item is rendered in an item frame.
 * <p>
 * You can set canceled to do no further vanilla processing.
 */
@Cancelable
public class RenderItemInFrameEvent extends net.minecraftforge.eventbus.api.Event {
	private final ItemStack item;
	private final ItemFrameEntity entityItemFrame;
	private final ItemFrameEntityRenderer renderer;

	public RenderItemInFrameEvent(ItemFrameEntity itemFrame, ItemFrameEntityRenderer renderItemFrame) {
		item = itemFrame.getHeldItemStack();
		entityItemFrame = itemFrame;
		renderer = renderItemFrame;
	}

	@Nonnull
	public ItemStack getItem() {
		return item;
	}

	public ItemFrameEntity getEntityItemFrame() {
		return entityItemFrame;
	}

	public ItemFrameEntityRenderer getRenderer() {
		return renderer;
	}
}
