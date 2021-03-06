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

package net.minecraftforge.fml.client;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.StartupQuery;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.options.ServerEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.ServerMetadata;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelSummary;

public class ClientHooks {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker CLIENTHOOKS = MarkerManager.getMarker("CLIENTHOOKS");
	// From FontRenderer.renderCharAtPos
	private static final String ALLOWED_CHARS = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
	private static final CharMatcher DISALLOWED_CHAR_MATCHER = CharMatcher.anyOf(ALLOWED_CHARS).negate();

	private static final Identifier iconSheet = new Identifier(ForgeVersion.MOD_ID, "textures/gui/icons.png");
	private static SetMultimap<String, Identifier> missingTextures = HashMultimap.create();
	private static Set<String> badTextureDomains = Sets.newHashSet();
	private static Table<String, String, Set<Identifier>> brokenTextures = HashBasedTable.create();

	@Nullable

	public static void processForgeListPingData(ServerMetadata packet, ServerEntry target) {
		if (packet.getForgeData() != null) {
			final Map<String, String> mods = packet.getForgeData().getRemoteModData();
			final Map<Identifier, Pair<String, Boolean>> remoteChannels = packet.getForgeData().getRemoteChannels();
			final int fmlver = packet.getForgeData().getFMLNetworkVersion();

			boolean fmlNetMatches = fmlver == FMLNetworkConstants.FMLNETVERSION;
			boolean channelsMatch = NetworkRegistry.checkListPingCompatibilityForClient(remoteChannels);
			AtomicBoolean result = new AtomicBoolean(true);
			ModList.get().forEachModContainer((modid, mc) -> mc.getCustomExtension(ExtensionPoint.DISPLAYTEST).ifPresent(ext ->
					result.compareAndSet(true, ext.getRight().test(mods.get(modid), true))));
			boolean modsMatch = result.get();

			final Map<String, String> extraServerMods = mods.entrySet().stream().
					filter(e -> !Objects.equals(FMLNetworkConstants.IGNORESERVERONLY, e.getValue())).
					filter(e -> !ModList.get().isLoaded(e.getKey())).
					collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			LOGGER.debug(CLIENTHOOKS, "Received FML ping data from server at {}: FMLNETVER={}, mod list is compatible : {}, channel list is compatible: {}, extra server mods: {}", target.address, fmlver, modsMatch, channelsMatch, extraServerMods);

			String extraReason = null;

			if (!extraServerMods.isEmpty()) {
				extraReason = "fml.menu.multiplayer.extraservermods";
			}
			if (!modsMatch) {
				extraReason = "fml.menu.multiplayer.modsincompatible";
			}
			if (!channelsMatch) {
				extraReason = "fml.menu.multiplayer.networkincompatible";
			}

			if (fmlver < FMLNetworkConstants.FMLNETVERSION) {
				extraReason = "fml.menu.multiplayer.serveroutdated";
			}
			if (fmlver > FMLNetworkConstants.FMLNETVERSION) {
				extraReason = "fml.menu.multiplayer.clientoutdated";
			}
			target.forgeData = new ExtendedServerListData("FML", extraServerMods.isEmpty() && fmlNetMatches && channelsMatch && modsMatch, mods.size(), extraReason);
		} else {
			target.forgeData = new ExtendedServerListData("VANILLA", NetworkRegistry.canConnectToVanillaServer(), 0, null);
		}

	}

	public static void drawForgePingInfo(MultiplayerScreen gui, ServerEntry target, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
		int idx;
		String tooltip;
		if (target.forgeData == null) {
			return;
		}
		switch (target.forgeData.type) {
		case "FML":
			if (target.forgeData.isCompatible) {
				idx = 0;
				tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.compatible", target.forgeData.numberOfMods);
			} else {
				idx = 16;
				if (target.forgeData.extraReason != null) {
					String extraReason = ForgeI18n.parseMessage(target.forgeData.extraReason);
					tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.incompatible.extra", extraReason);
				} else {
					tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.incompatible");
				}
			}
			break;
		case "VANILLA":
			if (target.forgeData.isCompatible) {
				idx = 48;
				tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.vanilla");
			} else {
				idx = 80;
				tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.vanilla.incompatible");
			}
			break;
		default:
			idx = 64;
			tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.unknown", target.forgeData.type);
		}

		MinecraftClient.getInstance().getTextureManager().bindTexture(iconSheet);
		DrawableHelper.blit(x + width - 18, y + 10, 16, 16, 0, idx, 16, 16, 256, 256);

		if (relativeMouseX > width - 15 && relativeMouseX < width && relativeMouseY > 10 && relativeMouseY < 26) {
			gui.setTooltip(tooltip);
		}

	}

	public static String fixDescription(String description) {
		return description.endsWith(":NOFML§r") ? description.substring(0, description.length() - 8) + "§r" : description;
	}

	static File getSavesDir() {
		return new File(MinecraftClient.getInstance().runDirectory, "saves");
	}

	public static void tryLoadExistingWorld(SelectWorldScreen selectWorldGUI, LevelSummary comparator) {
		try {
			MinecraftClient.getInstance().startIntegratedServer(comparator.getName(), comparator.getDisplayName(), null);
		} catch (StartupQuery.AbortedException e) {
			// ignore
		}
	}

	private static ClientConnection getClientToServerNetworkManager() {
		return MinecraftClient.getInstance().getNetworkHandler() != null ? MinecraftClient.getInstance().getNetworkHandler().getConnection() : null;
	}

	public static void handleClientWorldClosing(ClientWorld world) {
		ClientConnection client = getClientToServerNetworkManager();
		// ONLY revert a non-local connection
		if (client != null && !client.isLocal()) {
			GameData.revertToFrozen();
		}
	}

	public static String stripSpecialChars(String message) {
		// We can't handle many unicode points in the splash renderer
		return DISALLOWED_CHAR_MATCHER.removeFrom(net.minecraft.util.ChatUtil.stripTextFormat(message));
	}

	public static void trackMissingTexture(Identifier resourceLocation) {
		badTextureDomains.add(resourceLocation.getNamespace());
		missingTextures.put(resourceLocation.getNamespace(), resourceLocation);
	}

	public static void trackBrokenTexture(Identifier resourceLocation, String error) {
		badTextureDomains.add(resourceLocation.getNamespace());
		Set<Identifier> badType = brokenTextures.get(resourceLocation.getNamespace(), error);
		if (badType == null) {
			badType = Sets.newHashSet();
			brokenTextures.put(resourceLocation.getNamespace(), MoreObjects.firstNonNull(error, "Unknown error"), badType);
		}
		badType.add(resourceLocation);
	}

	public static void logMissingTextureErrors() {
		if (missingTextures.isEmpty() && brokenTextures.isEmpty()) {
			return;
		}
		Logger logger = LogManager.getLogger("FML.TEXTURE_ERRORS");
		logger.error(Strings.repeat("+=", 25));
		logger.error("The following texture errors were found.");
		Map<String, NamespaceResourceManager> resManagers = ObfuscationReflectionHelper.getPrivateValue(ReloadableResourceManagerImpl.class, (ReloadableResourceManagerImpl) MinecraftClient.getInstance().getResourceManager(), "field_199014" + "_c");
		for (String resourceDomain : badTextureDomains) {
			Set<Identifier> missing = missingTextures.get(resourceDomain);
			logger.error(Strings.repeat("=", 50));
			logger.error("  DOMAIN {}", resourceDomain);
			logger.error(Strings.repeat("-", 50));
			logger.error("  domain {} is missing {} texture{}", resourceDomain, missing.size(), missing.size() != 1 ? "s" : "");
			NamespaceResourceManager fallbackResourceManager = resManagers.get(resourceDomain);
			if (fallbackResourceManager == null) {
				logger.error("    domain {} is missing a resource manager - it is probably a side-effect of automatic texture processing", resourceDomain);
			} else {
				List<ResourcePack> resPacks = fallbackResourceManager.packList;
				logger.error("    domain {} has {} location{}:", resourceDomain, resPacks.size(), resPacks.size() != 1 ? "s" : "");
				for (ResourcePack resPack : resPacks) {
					if (resPack instanceof ModFileResourcePack) {
						ModFileResourcePack modRP = (ModFileResourcePack) resPack;
						List<IModInfo> mods = modRP.getModFile().getModInfos();
						logger.error("      mod(s) {} resources at {}", mods.stream().map(IModInfo::getDisplayName).collect(Collectors.toList()), modRP.getModFile().getFilePath());
					} else if (resPack instanceof AbstractFileResourcePack) {
						logger.error("      resource pack at path {}", ((AbstractFileResourcePack) resPack).base.getPath());
					} else {
						logger.error("      unknown resourcepack type {} : {}", resPack.getClass().getName(), resPack.getName());
					}
				}
			}
			logger.error(Strings.repeat("-", 25));
			if (missingTextures.containsKey(resourceDomain)) {
				logger.error("    The missing resources for domain {} are:", resourceDomain);
				for (Identifier rl : missing) {
					logger.error("      {}", rl.getPath());
				}
				logger.error(Strings.repeat("-", 25));
			}
			if (!brokenTextures.containsRow(resourceDomain)) {
				logger.error("    No other errors exist for domain {}", resourceDomain);
			} else {
				logger.error("    The following other errors were reported for domain {}:", resourceDomain);
				Map<String, Set<Identifier>> resourceErrs = brokenTextures.row(resourceDomain);
				for (String error : resourceErrs.keySet()) {
					logger.error(Strings.repeat("-", 25));
					logger.error("    Problem: {}", error);
					for (Identifier rl : resourceErrs.get(error)) {
						logger.error("      {}", rl.getPath());
					}
				}
			}
			logger.error(Strings.repeat("=", 50));
		}
		logger.error(Strings.repeat("+=", 25));
	}

	public static void firePlayerLogin(ClientPlayerInteractionManager pc, ClientPlayerEntity player, ClientConnection networkManager) {
		MinecraftForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggedInEvent(pc, player, networkManager));
	}

	public static void firePlayerLogout(ClientPlayerInteractionManager pc, ClientPlayerEntity player) {
		MinecraftForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggedOutEvent(pc, player, player != null ? player.networkHandler != null ? player.networkHandler.getConnection() : null : null));
	}

	public static void firePlayerRespawn(ClientPlayerInteractionManager pc, ClientPlayerEntity oldPlayer, ClientPlayerEntity newPlayer, ClientConnection networkManager) {
		MinecraftForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.RespawnEvent(pc, oldPlayer, newPlayer, networkManager));
	}


}
