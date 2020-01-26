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

package net.minecraftforge.debug.block;

//@Mod(MaterialFogColorTest.MODID)
//@Mod.EventBusSubscriber
public class MaterialFogColorTest {
	static final String MODID = "fog_color_in_material_test";

    /*
    static
    {
        FluidRegistry.enableUniversalBucket();
    }

    static int color = 0xFFd742f4; // <-- change value for testing

    public static final Fluid SLIME = new Fluid("slime", new ResourceLocation(MODID, "slime_still"), new ResourceLocation(MODID, "slime_flow"), new ResourceLocation(MODID, "slime_overlay")) {
        @Override
        public int getColor()
        {
            return color;
        }
    };

    //@ObjectHolder("slime")
    //public static final BlockFluidBase SLIME_BLOCK = null;
    @ObjectHolder("test_fluid")
    public static final Block FLUID_BLOCK = null;
    @ObjectHolder("test_fluid")
    public static final Item FLUID_ITEM = null;

    private static final ResourceLocation RES_LOC = new ResourceLocation(MODID, "slime");
    private static final ResourceLocation testFluidRegistryName = new ResourceLocation(MODID, "test_fluid");

    @SubscribeEvent
    public void preInit(FMLCommonSetupEvent event)
    {
        FluidRegistry.registerFluid(SLIME);
        FluidRegistry.addBucketForFluid(SLIME);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().register((new BlockFluidClassic(SLIME, Material.WATER)).setRegistryName(RES_LOC).setUnlocalizedName(RES_LOC.toString()));
        Fluid fluid = new Fluid("fog_test", new ResourceLocation("blocks/water_still"), new ResourceLocation("blocks/water_flow"), new ResourceLocation("blocks/water_overlay"));
        FluidRegistry.registerFluid(fluid);
        Block fluidBlock = new BlockFluidClassic(fluid, Material.WATER)
        {
            @Override
            public Vec3d getFogColor(World world, BlockPos pos, BlockState state, Entity entity, Vec3d originalColor, float partialTicks)
            {
                return new Vec3d(0.6F, 0.1F, 0.0F);
            }
        };
        event.getRegistry().register(fluidBlock.setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setUnlocalizedName(MODID + ".test_fluid").setRegistryName(testFluidRegistryName));
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().register(new ItemBlock(FLUID_BLOCK).setRegistryName(testFluidRegistryName));
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = MODID)
    public static class ClientEventHandler
    {
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event)
        {
            ModelResourceLocation fluidLocation = new ModelResourceLocation(testFluidRegistryName, "fluid");
            ModelLoader.registerItemVariants(FLUID_ITEM);
            ModelLoader.setCustomMeshDefinition(FLUID_ITEM, stack -> fluidLocation);
            ModelLoader.setCustomStateMapper(FLUID_BLOCK, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(BlockState state)
                {
                    return fluidLocation;
                }
            });
        }
    }
    */
}
