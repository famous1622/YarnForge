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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation.Type;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceManager;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;

/**
 * A model that can be rendered in multiple {@link BlockRenderLayer}.
 */
public final class MultiLayerModel implements UnbakedModel {
	public static final MultiLayerModel INSTANCE = new MultiLayerModel(ImmutableMap.of());
	private static final Logger LOGGER = LogManager.getLogger();
	private final ImmutableMap<Optional<BlockRenderLayer>, ModelIdentifier> models;

	public MultiLayerModel(ImmutableMap<Optional<BlockRenderLayer>, ModelIdentifier> models) {
		this.models = models;
	}

	private static ImmutableMap<Optional<BlockRenderLayer>, BakedModel> buildModels(ImmutableMap<Optional<BlockRenderLayer>, ModelIdentifier> models, ModelBakeSettings sprite, VertexFormat format, ModelLoader bakery, Function<Identifier, Sprite> spriteGetter) {
		ImmutableMap.Builder<Optional<BlockRenderLayer>, BakedModel> builder = ImmutableMap.builder();
		for (Optional<BlockRenderLayer> key : models.keySet()) {
			UnbakedModel model = ModelLoaderRegistry.getModelOrLogError(models.get(key), "Couldn't load MultiLayerModel dependency: " + models.get(key));
			builder.put(key, model.bake(bakery, spriteGetter, new ModelStateComposition(sprite.getState(), model.getDefaultState(), sprite.isUvLocked()), format));
		}
		return builder.build();
	}

	@Override
	public Collection<Identifier> getModelDependencies() {
		return ImmutableList.copyOf(models.values());
	}

	@Override
	public Collection<Identifier> getTextureDependencies(Function<Identifier, UnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public BakedModel bake(ModelLoader bakery, Function<Identifier, Sprite> spriteGetter, ModelBakeSettings sprite, VertexFormat format) {
		UnbakedModel missing = ModelLoaderRegistry.getMissingModel();
		return new MultiLayerBakedModel(
				buildModels(models, sprite, format, bakery, spriteGetter),
				missing.bake(bakery, spriteGetter, new BasicState(missing.getDefaultState(), sprite.isUvLocked()), format),
				PerspectiveMapWrapper.getTransforms(sprite.getState())
		);
	}

	@Override
	public MultiLayerModel process(ImmutableMap<String, String> customData) {
		ImmutableMap.Builder<Optional<BlockRenderLayer>, ModelIdentifier> builder = ImmutableMap.builder();
		for (String key : customData.keySet()) {
			if ("base".equals(key)) {
				builder.put(Optional.empty(), getLocation(customData.get(key)));
			}
			for (BlockRenderLayer layer : BlockRenderLayer.values()) {
				if (layer.toString().equals(key)) {
					builder.put(Optional.of(layer), getLocation(customData.get(key)));
				}
			}
		}
		ImmutableMap<Optional<BlockRenderLayer>, ModelIdentifier> models = builder.build();
		if (models.isEmpty()) return INSTANCE;
		return new MultiLayerModel(models);
	}

	private ModelIdentifier getLocation(String json) {
		JsonElement e = new JsonParser().parse(json);
		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
			return new ModelIdentifier(e.getAsString());
		}
		LOGGER.fatal("Expect ModelResourceLocation, got: {}", json);
		return new ModelIdentifier("builtin/missing", "missing");
	}

	public enum Loader implements ICustomModelLoader {
		INSTANCE;

		@Override
		public void apply(ResourceManager resourceManager) {
		}

		@Override
		public boolean accepts(Identifier modelLocation) {
			return modelLocation.getNamespace().equals(ForgeVersion.MOD_ID) && (
					modelLocation.getPath().equals("multi-layer") ||
							modelLocation.getPath().equals("models/block/multi-layer") ||
							modelLocation.getPath().equals("models/item/multi-layer"));
		}

		@Override
		public UnbakedModel loadModel(Identifier modelLocation) {
			return MultiLayerModel.INSTANCE;
		}
	}

	private static final class MultiLayerBakedModel implements BakedModel {
		private final ImmutableMap<Optional<BlockRenderLayer>, BakedModel> models;
		private final ImmutableMap<Type, TRSRTransformation> cameraTransforms;
		private final BakedModel base;
		private final BakedModel missing;

		public MultiLayerBakedModel(ImmutableMap<Optional<BlockRenderLayer>, BakedModel> models, BakedModel missing, ImmutableMap<Type, TRSRTransformation> cameraTransforms) {
			this.models = models;
			this.cameraTransforms = cameraTransforms;
			this.missing = missing;
			this.base = models.getOrDefault(Optional.empty(), missing);
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
			BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
			if (layer == null) {
				ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
				for (BakedModel model : models.values()) {
					builder.addAll(model.getQuads(state, side, rand));
				}
				return builder.build();
			}
			// assumes that child model will handle this state properly. FIXME?
			return models.getOrDefault(Optional.of(layer), missing).getQuads(state, side, rand);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return base.useAmbientOcclusion();
		}

		@Override
		public boolean isAmbientOcclusion(BlockState state) {
			return base.isAmbientOcclusion(state);
		}

		@Override
		public boolean hasDepthInGui() {
			return base.hasDepthInGui();
		}

		@Override
		public boolean isBuiltin() {
			return base.isBuiltin();
		}

		@Override
		public Sprite getSprite() {
			return base.getSprite();
		}

		@Override
		public Pair<? extends BakedModel, Matrix4f> handlePerspective(Type cameraTransformType) {
			return PerspectiveMapWrapper.handlePerspective(this, cameraTransforms, cameraTransformType);
		}

		@Override
		public ModelItemPropertyOverrideList getItemPropertyOverrides() {
			return ModelItemPropertyOverrideList.EMPTY;
		}
	}
}
