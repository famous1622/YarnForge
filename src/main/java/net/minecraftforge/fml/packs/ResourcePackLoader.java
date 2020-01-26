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

package net.minecraftforge.fml.packs;


import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

import net.minecraft.resource.ResourcePackCreator;
import net.minecraft.resource.ResourcePackContainer;
import net.minecraft.resource.ResourcePackContainerManager;

public class ResourcePackLoader {
	private static Map<ModFile, ModFileResourcePack> modResourcePacks;
	private static ResourcePackContainerManager<?> resourcePackList;

	public static Optional<ModFileResourcePack> getResourcePackFor(String modId) {
		return Optional.ofNullable(ModList.get().getModFileById(modId)).
				map(ModFileInfo::getFile).map(mf -> modResourcePacks.get(mf));
	}

	public static <T extends ResourcePackContainer> void loadResourcePacks(ResourcePackContainerManager<T> resourcePacks, BiFunction<Map<ModFile, ? extends ModFileResourcePack>, BiConsumer<? super ModFileResourcePack, T>, IPackInfoFinder> packFinder) {
		resourcePackList = resourcePacks;
		modResourcePacks = ModList.get().getModFiles().stream().
				filter(mf -> !Objects.equals(mf.getModLoader(), "minecraft")).
				map(mf -> new ModFileResourcePack(mf.getFile())).
				collect(Collectors.toMap(ModFileResourcePack::getModFile, Function.identity()));
		resourcePacks.addCreator(new LambdaFriendlyPackFinder(packFinder.apply(modResourcePacks, ModFileResourcePack::setPackInfo)));
	}

	public interface IPackInfoFinder<T extends ResourcePackContainer> {
		void addPackInfosToMap(Map<String, T> packList, ResourcePackContainer.Factory<T> factory);
	}

	// SO GROSS - DON'T @ me bro
	@SuppressWarnings("unchecked")
	private static class LambdaFriendlyPackFinder implements ResourcePackCreator {
		private IPackInfoFinder wrapped;

		private LambdaFriendlyPackFinder(final IPackInfoFinder wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public <T extends ResourcePackContainer> void registerContainer(Map<String, T> packList, ResourcePackContainer.Factory<T> factory) {
			wrapped.addPackInfosToMap(packList, factory);
		}
	}
}
