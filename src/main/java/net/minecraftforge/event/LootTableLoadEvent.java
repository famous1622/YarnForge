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

package net.minecraftforge.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.util.Identifier;
import net.minecraft.world.loot.LootSupplier;
import net.minecraft.world.loot.LootManager;

/**
 * Event fired when a LootTable json is loaded from json.
 * This event is fired whenever resources are loaded, or when the server starts.
 * This event will NOT be fired for LootTables loaded from the world folder, these are
 * considered configurations files and should not be modified by mods.
 * <p>
 * Canceling the event will make it load a empty loot table.
 */
@Cancelable
public class LootTableLoadEvent extends Event {
	private final Identifier name;
	private LootSupplier table;
	private LootManager lootTableManager;

	public LootTableLoadEvent(Identifier name, LootSupplier table, LootManager lootTableManager) {
		this.name = name;
		this.table = table;
		this.lootTableManager = lootTableManager;
	}

	public Identifier getName() {
		return this.name;
	}

	public LootSupplier getTable() {
		return this.table;
	}

	public void setTable(LootSupplier table) {
		this.table = table;
	}

	public LootManager getLootTableManager() {
		return this.lootTableManager;
	}
}
