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

package net.minecraftforge.fml.event.lifecycle;

import java.util.function.Supplier;

import net.minecraftforge.fml.ModContainer;

import net.minecraft.client.Minecraft;

/**
 * This is the second of four commonly called events during mod lifecycle startup.
 * <p>
 * Called before {@link InterModEnqueueEvent}
 * Called after {@link FMLCommonSetupEvent}
 * <p>
 * Called on {@link net.minecraftforge.api.distmarker.Dist#CLIENT} - the game client.
 * <p>
 * Alternative to {@link FMLDedicatedServerSetupEvent}.
 * <p>
 * Do client only setup with this event, such as KeyBindings.
 * <p>
 * This is a parallel dispatch event.
 */
public class FMLClientSetupEvent extends ModLifecycleEvent {
	private final Supplier<Minecraft> minecraftSupplier;

	public FMLClientSetupEvent(Supplier<Minecraft> mc, ModContainer container) {
		super(container);
		this.minecraftSupplier = mc;
	}

	public Supplier<Minecraft> getMinecraftSupplier() {
		return minecraftSupplier;
	}
}
