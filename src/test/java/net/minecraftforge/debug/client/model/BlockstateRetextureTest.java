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

package net.minecraftforge.debug.client.model;

import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

@Mod(BlockstateRetextureTest.MODID)
public class BlockstateRetextureTest {
	public static final String MODID = "forge_blockstate_retexture_test";
	static final boolean ENABLED = true;

	private static Identifier fenceName = new Identifier("minecraft", "oak_fence");
	private static ModelIdentifier fenceLocation = new ModelIdentifier(fenceName, "east=true,north=false,south=false,waterlogged=false,west=true");
	private static Identifier stoneName = new Identifier("minecraft", "stone");
	private static ModelIdentifier stoneLocation = new ModelIdentifier(stoneName, "");

	private static Function<Identifier, Sprite> textureGetter = location ->
	{
		assert location != null;
		return MinecraftClient.getInstance().getSpriteAtlas().getSprite(location.toString());
	};

	@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
	public static class ClientEvents {
		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onModelBakeEvent(ModelBakeEvent event) {
			if (!ENABLED) {
				return;
			}

			IModel<?> fence = ModelLoaderRegistry.getModelOrLogError(fenceLocation, "Error loading fence model");
			IModel<?> stone = ModelLoaderRegistry.getModelOrLogError(stoneLocation, "Error loading stone model");
			IModel<?> retexturedFence = fence.retexture(ImmutableMap.of("texture", "blocks/log_oak"));
			IModel<?> retexturedStone = stone.retexture(ImmutableMap.of("all", "blocks/diamond_block"));

			BakedModel fenceResult = retexturedFence.bake(event.getModelLoader(), textureGetter, new BasicState(fence.getDefaultState(), true), VertexFormats.POSITION_COLOR_UV_NORMAL);
			BakedModel stoneResult = retexturedStone.bake(event.getModelLoader(), textureGetter, new BasicState(stone.getDefaultState(), true), VertexFormats.POSITION_COLOR_UV_NORMAL);

			event.getModelRegistry().put(fenceLocation, fenceResult);
			event.getModelRegistry().put(stoneLocation, ModelLoaderRegistry.getMissingModel().bake(event.getModelLoader(), textureGetter, new BasicState(TRSRTransformation.identity(), false), VertexFormats.POSITION_COLOR_UV_NORMAL));
		}
	}
}
