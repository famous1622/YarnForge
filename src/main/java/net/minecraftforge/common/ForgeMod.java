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

package net.minecraftforge.common;

import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IngredientNBT;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.common.crafting.conditions.AndCondition;
import net.minecraftforge.common.crafting.conditions.FalseCondition;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.OrCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;
import net.minecraftforge.common.crafting.conditions.TrueCondition;
import net.minecraftforge.common.data.ForgeBlockTagsProvider;
import net.minecraftforge.common.data.ForgeItemTagsProvider;
import net.minecraftforge.common.data.ForgeRecipeProvider;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.FMLWorldPersistenceHook;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.WorldPersistenceHooks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLModIdMappingEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.server.command.ConfigCommand;
import net.minecraftforge.server.command.ForgeCommand;
import net.minecraftforge.versions.forge.ForgeVersion;
import net.minecraftforge.versions.mcp.MCPVersion;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.data.DataGenerator;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.level.LevelProperties;

@Mod("forge")
public class ForgeMod implements WorldPersistenceHooks.WorldPersistenceHook {
	public static final String VERSION_CHECK_CAT = "version_checking";
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker FORGEMOD = MarkerManager.getMarker("FORGEMOD");
	//TODO: Remove all of these, use ForgeConfig instead
	@Deprecated
	public static int[] blendRanges = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34};
	@Deprecated
	public static boolean disableVersionCheck = false;
	@Deprecated
	public static boolean forgeLightPipelineEnabled = true;
	@Deprecated
	public static boolean zoomInMissingModelTextInGui = false;
	@Deprecated
	public static boolean disableStairSlabCulling = false; // Also known as the "DontCullStairsBecauseIUseACrappyTexturePackThatBreaksBasicBlockShapesSoICantTrustBasicBlockCulling" flag
	@Deprecated
	public static boolean alwaysSetupTerrainOffThread = false; // In WorldRenderer.setupTerrain, always force the chunk render updates to be queued to the thread
	@Deprecated
	public static boolean logCascadingWorldGeneration = true; // see Chunk#logCascadingWorldGeneration()
	@Deprecated
	public static boolean fixVanillaCascading = false; // There are various places in vanilla that cause cascading worldgen. Enabling this WILL change where blocks are placed to prevent this.
	// DO NOT contact Forge about worldgen not 'matching' vanilla if this flag is set.

	private static ForgeMod INSTANCE;

	public ForgeMod() {
		LOGGER.info(FORGEMOD, "Forge mod loading, version {}, for MC {} with MCP {}", ForgeVersion.getVersion(), MCPVersion.getMCVersion(), MCPVersion.getMCPVersion());
		INSTANCE = this;
		MinecraftForge.initialize();
		WorldPersistenceHooks.addHook(this);
		WorldPersistenceHooks.addHook(new FMLWorldPersistenceHook());
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::preInit);
		modEventBus.addListener(this::gatherData);
		modEventBus.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
		MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeConfig.clientSpec);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ForgeConfig.serverSpec);
		modEventBus.register(ForgeConfig.class);
		// Forge does not display problems when the remote is not matching.
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> "ANY", (remote, isServer) -> true));
		StartupMessageManager.addModMessage("Forge version " + ForgeVersion.getVersion());
	}

	public static ForgeMod getInstance() {
		return INSTANCE;
	}

	public void preInit(FMLCommonSetupEvent evt) {
		CapabilityItemHandler.register();
		CapabilityFluidHandler.register();
		CapabilityAnimation.register();
		CapabilityEnergy.register();
		MinecraftForge.EVENT_BUS.addListener(VillagerTradingManager::loadTrades);
		MinecraftForge.EVENT_BUS.register(MinecraftForge.INTERNAL_HANDLER);
		MinecraftForge.EVENT_BUS.register(this);

		VersionChecker.startVersionCheck();

        /*
         * We can't actually add any of these, because vanilla clients will choke on unknown argument types
         * So our custom arguments will not validate client-side, but they do still work
        ArgumentTypes.register("forge:enum", EnumArgument.class, new EnumArgument.Serializer());
        ArgumentTypes.register("forge:modid", ModIdArgument.class, new ArgumentSerializer<>(ModIdArgument::modIdArgument));
        ArgumentTypes.register("forge:structure_type", StructureArgument.class, new ArgumentSerializer<>(StructureArgument::structure));
        */
	}

	public void serverStarting(FMLServerStartingEvent evt) {
		new ForgeCommand(evt.getCommandDispatcher());
		ConfigCommand.register(evt.getCommandDispatcher());
	}

	public void serverStopping(FMLServerStoppingEvent evt) {
		WorldWorkerManager.clear();
	}

	@Override
	public CompoundTag getDataForWriting(WorldSaveHandler handler, LevelProperties info) {
		CompoundTag forgeData = new CompoundTag();
		CompoundTag dims = new CompoundTag();
		DimensionManager.writeRegistry(dims);
		if (!dims.isEmpty()) {
			forgeData.put("dims", dims);
		}
		return forgeData;
	}

	@Override
	public void readData(WorldSaveHandler handler, LevelProperties info, CompoundTag tag) {
		if (tag.containsKey("dims", 10)) {
			DimensionManager.readRegistry(tag.getCompound("dims"));
		}
	}

	public void mappingChanged(FMLModIdMappingEvent evt) {
	}

	@Override
	public String getModId() {
		return ForgeVersion.MOD_ID;
	}

	public void gatherData(GatherDataEvent event) {
		DataGenerator gen = event.getGenerator();

		if (event.includeServer()) {
			gen.install(new ForgeBlockTagsProvider(gen));
			gen.install(new ForgeItemTagsProvider(gen));
			gen.install(new ForgeRecipeProvider(gen));
		}
	}

	@SubscribeEvent //ModBus, can't use addListener due to nested genetics.
	public void registerRecipeSerialziers(RegistryEvent.Register<RecipeSerializer<?>> event) {
		CraftingHelper.register(AndCondition.Serializer.INSTANCE);
		CraftingHelper.register(FalseCondition.Serializer.INSTANCE);
		CraftingHelper.register(ItemExistsCondition.Serializer.INSTANCE);
		CraftingHelper.register(ModLoadedCondition.Serializer.INSTANCE);
		CraftingHelper.register(NotCondition.Serializer.INSTANCE);
		CraftingHelper.register(OrCondition.Serializer.INSTANCE);
		CraftingHelper.register(TrueCondition.Serializer.INSTANCE);
		CraftingHelper.register(TagEmptyCondition.Serializer.INSTANCE);

		CraftingHelper.register(new Identifier("forge", "compound"), CompoundIngredient.Serializer.INSTANCE);
		CraftingHelper.register(new Identifier("forge", "nbt"), IngredientNBT.Serializer.INSTANCE);
		CraftingHelper.register(new Identifier("minecraft", "item"), VanillaIngredientSerializer.INSTANCE);

		event.getRegistry().register(new ConditionalRecipe.Serializer<Recipe<?>>().setRegistryName(new Identifier("forge", "conditional")));

	}
}
