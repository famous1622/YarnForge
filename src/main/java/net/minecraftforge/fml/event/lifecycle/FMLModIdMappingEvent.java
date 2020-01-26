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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.util.Identifier;

/**
 * Called whenever the ID mapping might have changed. If you register for this event, you
 * will be called back whenever the client or server loads an ID set. This includes both
 * when the ID maps are loaded from disk, as well as when the ID maps revert to the initial
 * state.
 * <p>
 * Note: you cannot change the IDs that have been allocated, but you might want to use
 * this event to update caches or other in-mod artifacts that might be impacted by an ID
 * change.
 * <p>
 * Fired on the {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}
 */
public class FMLModIdMappingEvent extends ModLifecycleEvent {
	public final boolean isFrozen;
	private final Map<Identifier, ImmutableList<ModRemapping>> remaps;
	private final ImmutableSet<Identifier> keys;

	public FMLModIdMappingEvent(Map<Identifier, Map<Identifier, Integer[]>> remaps, boolean isFrozen) {
		super(null);
		this.isFrozen = isFrozen;
		this.remaps = Maps.newHashMap();
		remaps.forEach((name, rm) ->
		{
			List<ModRemapping> tmp = Lists.newArrayList();
			rm.forEach((key, value) -> tmp.add(new ModRemapping(name, key, value[0], value[1])));
			tmp.sort(Comparator.comparingInt(o -> o.newId));
			this.remaps.put(name, ImmutableList.copyOf(tmp));
		});
		this.keys = ImmutableSet.copyOf(this.remaps.keySet());
	}

	public ImmutableSet<Identifier> getRegistries() {
		return this.keys;
	}

	public ImmutableList<ModRemapping> getRemaps(Identifier registry) {
		return this.remaps.get(registry);
	}

	public class ModRemapping {
		public final Identifier registry;
		public final Identifier key;
		public final int oldId;
		public final int newId;

		private ModRemapping(Identifier registry, Identifier key, int oldId, int newId) {
			this.registry = registry;
			this.key = key;
			this.oldId = oldId;
			this.newId = newId;
		}
	}
}
