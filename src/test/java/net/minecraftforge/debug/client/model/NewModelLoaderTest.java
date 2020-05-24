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

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.geometry.ISimpleModelGeometry;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;
import net.minecraftforge.debug.client.model.NewModelLoaderTest.TestLoader;
import net.minecraftforge.debug.client.model.NewModelLoaderTest.TestModel;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

@Mod(NewModelLoaderTest.MODID)
public class NewModelLoaderTest
{
    public static final String MODID = "new_model_loader_test";
    public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MODID);

    public static RegistryObject<Block> obj_block = BLOCKS.register("obj_block", () ->
            new Block(Block.Settings.of(Material.WOOD).strength(10)) {
                @Override
                protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
                {
                    builder.add(Properties.HORIZONTAL_FACING);
                }

                @Nullable
                @Override
                public BlockState getPlacementState(ItemPlacementContext context)
                {
                    return getDefaultState().with(
                            Properties.HORIZONTAL_FACING, context.getPlayerFacing()
                    );
                }

                @Override
                public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, EntityContext context)
                {
                    return Block.createCuboidShape(2,2,2,14,14,14);
                }
            }
    );

    public static RegistryObject<Item> obj_item = ITEMS.register("obj_block", () ->
            new BlockItem(obj_block.get(), new Item.Settings().group(ItemGroup.MISC)) {
                @Override
                public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity)
                {
                    return armorType == EquipmentSlot.field_6169;
                }
            }
    );

    public static RegistryObject<Item> custom_transforms = ITEMS.register("custom_transforms", () ->
            new Item(new Item.Settings().group(ItemGroup.MISC))
    );

    public static RegistryObject<Item> custom_vanilla_loader = ITEMS.register("custom_vanilla_loader", () ->
            new Item(new Item.Settings().group(ItemGroup.MISC))
    );

    public static RegistryObject<Item> custom_loader = ITEMS.register("custom_loader", () ->
            new Item(new Item.Settings().group(ItemGroup.MISC))
    );

    public NewModelLoaderTest()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        modEventBus.addListener(this::clientSetup);
    }

    public void clientSetup(FMLClientSetupEvent event)
    {
        ModelLoaderRegistry.registerLoader(new Identifier(MODID, "custom_loader"), new TestLoader());
    }

    static class TestLoader implements IModelLoader<TestModel>
    {
        @Override
        public void apply(ResourceManager resourceManager)
        {
        }

        @Override
        public TestModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
        {
            return new TestModel();
        }
    }

    static class TestModel implements ISimpleModelGeometry<TestModel>
    {
        @Override
        public void addQuads(IModelConfiguration owner, IModelBuilder<?> modelBuilder, ModelLoader bakery, Function<net.minecraft.client.util.SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, Identifier modelLocation)
        {
            Sprite texture = spriteGetter.apply(owner.resolveTexture("particle"));

            BakedQuadBuilder builder = new BakedQuadBuilder();

            builder.setTexture(texture);
            builder.setQuadOrientation(Direction.field_11036);

            putVertex(builder, 0,1,0.5f, texture.getFrameU(0), texture.getFrameV(0), 1, 1, 1);
            putVertex(builder, 0,0,0.5f, texture.getFrameU(0), texture.getFrameV(16), 1, 1, 1);
            putVertex(builder, 1,0,0.5f, texture.getFrameU(16), texture.getFrameV(16), 1, 1, 1);
            putVertex(builder, 1,1,0.5f, texture.getFrameU(16), texture.getFrameV(0), 1, 1, 1);

            modelBuilder.addGeneralQuad(builder.build());
        }

        private void putVertex(BakedQuadBuilder builder, int x, float y, float z, float u, float v, float red, float green, float blue)
        {
            ImmutableList<VertexFormatElement> elements = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getElements();
            for(int i=0;i<elements.size();i++)
            {
                switch(elements.get(i).getType())
                {
                    case field_1633:
                        builder.put(i, x, y, z);
                        break;
                    case field_1636:
                        if (elements.get(i).getIndex() == 0)
                            builder.put(i, u, v);
                        else
                            builder.put(i);
                        break;
                    case field_1632:
                        builder.put(i, red, green, blue, 1.0f);
                        break;
                    default:
                        builder.put(i);
                        break;
                }
            }
        }

        @Override
        public Collection<net.minecraft.client.util.SpriteIdentifier> getTextures(IModelConfiguration owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            return Collections.singleton(owner.resolveTexture("particle"));
        }
    }
}
