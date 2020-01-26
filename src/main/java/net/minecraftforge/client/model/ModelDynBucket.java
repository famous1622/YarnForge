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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.vecmath.Quat4f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation.Type;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.util.PngFile;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class ModelDynBucket implements UnbakedModel {
	public static final ModelIdentifier LOCATION = new ModelIdentifier(new Identifier(ForgeVersion.MOD_ID, "dynbucket"), "inventory");
	public static final UnbakedModel MODEL = new ModelDynBucket();
	private static final Logger LOGGER = LogManager.getLogger();
	// minimal Z offset to prevent depth-fighting
	private static final float NORTH_Z_COVER = 7.496f / 16f;
	private static final float SOUTH_Z_COVER = 8.504f / 16f;
	private static final float NORTH_Z_FLUID = 7.498f / 16f;
	private static final float SOUTH_Z_FLUID = 8.502f / 16f;
	@Nullable
	private final Identifier baseLocation;
	@Nullable
	private final Identifier liquidLocation;
	@Nullable
	private final Identifier coverLocation;
	@Nullable
	private final Fluid fluid;

	private final boolean flipGas;
	private final boolean tint;

	public ModelDynBucket() {
		this(null, null, null, null, false, true);
	}

	public ModelDynBucket(@Nullable Identifier baseLocation, @Nullable Identifier liquidLocation, @Nullable Identifier coverLocation, @Nullable Fluid fluid, boolean flipGas, boolean tint) {
		this.baseLocation = baseLocation;
		this.liquidLocation = liquidLocation;
		this.coverLocation = coverLocation;
		this.fluid = fluid;
		this.flipGas = flipGas;
		this.tint = tint;
	}

	@Nullable
	protected static Resource getResource(Identifier resourceLocation) {
		try {
			return MinecraftClient.getInstance().getResourceManager().getResource(resourceLocation);
		} catch (IOException ignored) {
			return null;
		}
	}

	@Nullable
	protected static PngFile getSizeInfo(Resource resource) {
		try {
			return new PngFile(resource.toString(), resource.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<Identifier> getTextureDependencies(Function<Identifier, UnbakedModel> modelGetter, Set<String> missingTextureErrors) {
		ImmutableSet.Builder<Identifier> builder = ImmutableSet.builder();

		if (baseLocation != null) {
			builder.add(baseLocation);
		}
		if (liquidLocation != null) {
			builder.add(liquidLocation);
		}
		if (coverLocation != null) {
			builder.add(coverLocation);
		}
		if (fluid != null) {
			builder.add(fluid.getAttributes().getStillTexture());
		}

		return builder.build();
	}

	@Override
	public Collection<Identifier> getModelDependencies() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public BakedModel bake(ModelLoader bakery, Function<Identifier, Sprite> spriteGetter, ModelBakeSettings sprite, VertexFormat format) {
		IModelState state = sprite.getState();
		ImmutableMap<Type, TRSRTransformation> transformMap = PerspectiveMapWrapper.getTransforms(state);

		// if the fluid is lighter than air, will manipulate the initial state to be rotated 180° to turn it upside down
		if (flipGas && fluid != null && fluid.getAttributes().isLighterThanAir()) {
			sprite = new ModelStateComposition(state, TRSRTransformation.blockCenterToCorner(new TRSRTransformation(null, new Quat4f(0, 0, 1, 0), null, null)));
			state = sprite.getState();
		}

		TRSRTransformation transform = state.apply(Optional.empty()).orElse(TRSRTransformation.identity());
		Sprite fluidSprite = null;
		Sprite particleSprite = null;
		ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

		if (fluid != null) {
			fluidSprite = spriteGetter.apply(fluid.getAttributes().getStillTexture());
		}

		Random random = new Random();
		if (baseLocation != null) {
			// build base (insidest)
			BakedModel model = (new ItemLayerModel(ImmutableList.of(baseLocation))).bake(bakery, spriteGetter, sprite, format);
			random.setSeed(42);
			builder.addAll(model.getQuads(null, null, random));
			particleSprite = model.getSprite();
		}
		if (liquidLocation != null && fluidSprite != null) {
			Sprite liquid = spriteGetter.apply(liquidLocation);
			// build liquid layer (inside)
			builder.addAll(ItemTextureQuadConverter.convertTexture(format, transform, liquid, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, tint ? fluid.getAttributes().getColor() : 0xFFFFFFFF, 1));
			builder.addAll(ItemTextureQuadConverter.convertTexture(format, transform, liquid, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, tint ? fluid.getAttributes().getColor() : 0xFFFFFFFF, 1));
			particleSprite = fluidSprite;
		}
		if (coverLocation != null) {
			// cover (the actual item around the other two)
			Sprite cover = spriteGetter.apply(coverLocation);
			builder.add(ItemTextureQuadConverter.genQuad(format, transform, 0, 0, 16, 16, NORTH_Z_COVER, cover, Direction.NORTH, 0xFFFFFFFF, 2));
			builder.add(ItemTextureQuadConverter.genQuad(format, transform, 0, 0, 16, 16, SOUTH_Z_COVER, cover, Direction.SOUTH, 0xFFFFFFFF, 2));
			if (particleSprite == null) {
				particleSprite = cover;
			}
		}

		return new BakedDynBucket(bakery, this, builder.build(), particleSprite, format, Maps.immutableEnumMap(transformMap), Maps.newHashMap(), transform.isIdentity());
	}

	/**
	 * Sets the fluid in the model.
	 * "fluid" - Name of the fluid in the fluid registry.
	 * "flipGas" - If "true" the model will be flipped upside down if the fluid is lighter than air. If "false" it won't.
	 * "applyTint" - If "true" the model will tint the fluid quads according to the fluid's base color.
	 * <p/>
	 * If the fluid can't be found, water is used.
	 */
	@Override
	public ModelDynBucket process(ImmutableMap<String, String> customData) {
		Identifier fluidName = new Identifier(customData.get("fluid"));
		Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidName);

		if (fluid == null) fluid = this.fluid;

		boolean flip = flipGas;
		if (customData.containsKey("flipGas")) {
			String flipStr = customData.get("flipGas");
			if (flipStr.equals("true")) {
				flip = true;
			} else if (flipStr.equals("false")) {
				flip = false;
			} else {
				throw new IllegalArgumentException(String.format("DynBucket custom data \"flipGas\" must have value 'true' or 'false' (was '%s')", flipStr));
			}
		}

		boolean tint = this.tint;
		if (customData.containsKey("applyTint")) {
			String string = customData.get("applyTint");
			switch (string) {
			case "true":
				tint = true;
				break;
			case "false":
				tint = false;
				break;
			default:
				throw new IllegalArgumentException(String.format("DynBucket custom data \"applyTint\" must have value 'true' or 'false' (was '%s')", string));
			}
		}

		// create new model with correct liquid
		return new ModelDynBucket(baseLocation, liquidLocation, coverLocation, fluid, flip, tint);
	}

	/**
	 * Allows to use different textures for the model.
	 * There are 3 layers:
	 * base - The empty bucket/container
	 * fluid - A texture representing the liquid portion. Non-transparent = liquid
	 * cover - An overlay that's put over the liquid (optional)
	 * <p/>
	 * If no liquid is given a hardcoded variant for the bucket is used.
	 */
	@Override
	public ModelDynBucket retexture(ImmutableMap<String, String> textures) {

		Identifier base = baseLocation;
		Identifier liquid = liquidLocation;
		Identifier cover = coverLocation;

		if (textures.containsKey("base")) {
			base = new Identifier(textures.get("base"));
		}
		if (textures.containsKey("fluid")) {
			liquid = new Identifier(textures.get("fluid"));
		}
		if (textures.containsKey("cover")) {
			cover = new Identifier(textures.get("cover"));
		}

		return new ModelDynBucket(base, liquid, cover, fluid, flipGas, tint);
	}

	public enum LoaderDynBucket implements ICustomModelLoader {
		INSTANCE;

		@Override
		public boolean accepts(Identifier modelLocation) {
			return modelLocation.getNamespace().equals(ForgeVersion.MOD_ID) && modelLocation.getPath().contains("forgebucket");
		}

		@Override
		public UnbakedModel loadModel(Identifier modelLocation) {
			return MODEL;
		}

		@Override
		public void apply(ResourceManager resourceManager) {
			// no need to clear cache since we create a new model instance
		}

		public void register(SpriteAtlasTexture map) {
			// only create these textures if they are not added by a resource pack

			Resource res;
			if (getResource(new Identifier(ForgeVersion.MOD_ID, "textures/items/bucket_cover.png")) == null) {
				Identifier bucketCover = new Identifier(ForgeVersion.MOD_ID, "items/bucket_cover");
				BucketCoverSprite bucketCoverSprite = new BucketCoverSprite(bucketCover);
//                map.setTextureEntry(bucketCoverSprite);
			}

			if (getResource(new Identifier(ForgeVersion.MOD_ID, "textures/items/bucket_base.png")) == null) {
				Identifier bucketBase = new Identifier(ForgeVersion.MOD_ID, "items/bucket_base");
				BucketBaseSprite bucketBaseSprite = new BucketBaseSprite(bucketBase);
//                map.setTextureEntry(bucketBaseSprite);
			}
		}
	}

	private static final class BucketBaseSprite extends Sprite {
		private final Identifier bucket = new Identifier("item/bucket");
		private final ImmutableList<Identifier> dependencies = ImmutableList.of(bucket);

		private BucketBaseSprite(Identifier res) {
			super(res, getSizeInfo(getResource(new Identifier("textures/item/bucket.png"))), getResource(new Identifier("textures/item/bucket.png")).getMetadata(AnimationResourceMetadata.READER));
		}

        /* TODO Custom TAS
        @Override
        public boolean hasCustomLoader(@Nonnull IResourceManager manager, @Nonnull ResourceLocation location)
        {
            return true;
        }

        @Override
        public Collection<ResourceLocation> getDependencies()
        {
            return dependencies;
        }

        @Override
        public boolean load(@Nonnull IResourceManager manager, @Nonnull ResourceLocation location, @Nonnull Function<ResourceLocation, TextureAtlasSprite> textureGetter)
        {
            final TextureAtlasSprite sprite = textureGetter.apply(bucket);
            // TODO custom sprites are gonna be a PITA, these are final
            width = sprite.getIconWidth();
            height = sprite.getIconHeight();
            // TODO No easy way to dump pixels of one sprite into another without n^2 for loop, investigate patch?
            final int[][] pixels = sprite.getFrameTextureData(0);
            this.clearFramesTextureData();
            this.framesTextureData.add(pixels);
            return false;
        }*/
	}

	/**
	 * Creates a bucket cover sprite from the vanilla resource.
	 */
	private static final class BucketCoverSprite extends Sprite {
		private final Identifier bucket = new Identifier("item/bucket");
		private final Identifier bucketCoverMask = new Identifier(ForgeVersion.MOD_ID, "item/vanilla_bucket_cover_mask");
		private final ImmutableList<Identifier> dependencies = ImmutableList.of(bucket, bucketCoverMask);

		private BucketCoverSprite(Identifier res) {
			super(res, getSizeInfo(getResource(new Identifier("textures/item/bucket.png"))), getResource(new Identifier("textures/item/bucket.png")).getMetadata(AnimationResourceMetadata.READER));
		}

        /* TODO Custom TAS
        @Override
        public boolean hasCustomLoader(@Nonnull IResourceManager manager, @Nonnull ResourceLocation location)
        {
            return true;
        }

        @Override
        public Collection<ResourceLocation> getDependencies()
        {
            return dependencies;
        }

        @Override
        public boolean load(@Nonnull IResourceManager manager, @Nonnull ResourceLocation location, @Nonnull Function<ResourceLocation, TextureAtlasSprite> textureGetter)
        {
            final TextureAtlasSprite sprite = textureGetter.apply(bucket);
            final TextureAtlasSprite alphaMask = textureGetter.apply(bucketCoverMask);
            width = sprite.getIconWidth();
            height = sprite.getIconHeight();
            final int[][] pixels = new int[Minecraft.getMinecraft().gameSettings.mipmapLevels + 1][];
            pixels[0] = new int[width * height];

            try (
                 IResource empty = getResource(new ResourceLocation("textures/items/bucket_empty.png"));
                 IResource mask = getResource(new ResourceLocation(ForgeVersion.MOD_ID, "textures/items/vanilla_bucket_cover_mask.png"))
            ) {
                // use the alpha mask if it fits, otherwise leave the cover texture blank
                if (empty != null && mask != null && Objects.equals(empty.getPackName(), mask.getPackName()) &&
                        alphaMask.getIconWidth() == width && alphaMask.getIconHeight() == height)
                {
                    final int[][] oldPixels = sprite.getFrameTextureData(0);
                    final int[][] alphaPixels = alphaMask.getFrameTextureData(0);

                    for (int p = 0; p < width * height; p++)
                    {
                        final int alphaMultiplier = alphaPixels[0][p] >>> 24;
                        final int oldPixel = oldPixels[0][p];
                        final int oldPixelAlpha = oldPixel >>> 24;
                        final int newAlpha = oldPixelAlpha * alphaMultiplier / 0xFF;
                        pixels[0][p] = (oldPixel & 0xFFFFFF) + (newAlpha << 24);
                    }
                }
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to close resource", e);
            }

            this.clearFramesTextureData();
            this.framesTextureData.add(pixels);
            return false;
        }*/
	}

	private static final class BakedDynBucketOverrideHandler extends ModelItemPropertyOverrideList {
		private final ModelLoader bakery;

		private BakedDynBucketOverrideHandler(ModelLoader bakery) {
			this.bakery = bakery;
		}

		@Override
		public BakedModel apply(BakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable LivingEntity entity) {
			return FluidUtil.getFluidContained(stack)
					.map(fluidStack -> {
						BakedDynBucket model = (BakedDynBucket) originalModel;

						Fluid fluid = fluidStack.getFluid();
						String name = fluid.getRegistryName().toString();

						if (!model.cache.containsKey(name)) {
							UnbakedModel parent = model.parent.process(ImmutableMap.of("fluid", name));
							Function<Identifier, Sprite> textureGetter;
							textureGetter = location -> MinecraftClient.getInstance().getSpriteAtlas().getSprite(location.toString());

							BakedModel bakedModel = parent.bake(bakery, textureGetter, new SimpleModelState(model.transforms), model.format);
							model.cache.put(name, bakedModel);
							return bakedModel;
						}

						return model.cache.get(name);
					})
					// not a fluid item apparently
					.orElse(originalModel); // empty bucket
		}
	}

	// the dynamic bucket is based on the empty bucket
	private static final class BakedDynBucket extends BakedItemModel {
		private final ModelDynBucket parent;
		private final Map<String, BakedModel> cache; // contains all the baked models since they'll never change
		private final VertexFormat format;

		BakedDynBucket(ModelLoader bakery,
					   ModelDynBucket parent,
					   ImmutableList<BakedQuad> quads,
					   Sprite particle,
					   VertexFormat format,
					   ImmutableMap<Type, TRSRTransformation> transforms,
					   Map<String, BakedModel> cache,
					   boolean untransformed) {
			super(quads, particle, transforms, new BakedDynBucketOverrideHandler(bakery), untransformed);
			this.format = format;
			this.parent = parent;
			this.cache = cache;
		}
	}

}
