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

package net.minecraftforge.server.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.dimension.DimensionType;

public class CommandDimensions {
	static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("dimensions")
				.requires(cs -> cs.hasPermissionLevel(0)) //permission
				.executes(ctx -> {
					ctx.getSource().sendFeedback(new TranslatableText("commands.forge.dimensions.list"), true);
					Map<String, List<String>> types = new HashMap<>();
					for (DimensionType dim : DimensionType.getAll()) {
						String key = dim.getModType() == null ? "Vanilla" : dim.getModType().getRegistryName().toString();
						types.computeIfAbsent(key, k -> new ArrayList<>()).add(DimensionType.getId(dim).toString());
					}

					types.keySet().stream().sorted().forEach(key -> {
						ctx.getSource().sendFeedback(new LiteralText(key + ": " + types.get(key).stream().sorted().collect(Collectors.joining(", "))), true);
					});
					return 0;
				});
	}
}
