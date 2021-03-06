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

package net.minecraftforge.client.model;

import java.util.function.Predicate;

import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public interface ICustomModelLoader extends ISelectiveResourceReloadListener {
	@Override
	void apply(ResourceManager resourceManager);

	@Override
	default void onResourceManagerReload(ResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
		if (resourcePredicate.test(VanillaResourceType.MODELS)) {
			apply(resourceManager);
		}
	}

	/**
	 * Checks if given model should be loaded by this loader.
	 * Reading file contents is inadvisable, if possible decision should be made based on the location alone.
	 *
	 * @param modelLocation The path, either to an actual file or a {@link net.minecraft.client.renderer.model.ModelResourceLocation}.
	 */
	boolean accepts(Identifier modelLocation);

	/**
	 * @param modelLocation The model to (re)load, either path to an
	 *                      actual file or a {@link net.minecraft.client.renderer.model.ModelResourceLocation}.
	 */
	UnbakedModel loadModel(Identifier modelLocation) throws Exception;


	@Override
	default IResourceType getResourceType() {
		return VanillaResourceType.MODELS;
	}
}
