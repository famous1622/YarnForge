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

package net.minecraftforge.fml.network;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.registries.ClearableRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.container.Container;
import net.minecraft.container.ContainerType;
import net.minecraft.container.NameableContainerProvider;
import net.minecraft.network.Packet;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.server.network.packet.HandshakeC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

public class NetworkHooks {
	private static final Logger LOGGER = LogManager.getLogger();
	// internal tracking map for custom dimensions received from servers for use on client.
	private static Int2ObjectMap<DimensionType> trackingMap = new Int2ObjectOpenHashMap<>();

	public static String getFMLVersion(final String ip) {
		return ip.contains("\0") ? Objects.equals(ip.split("\0")[1], FMLNetworkConstants.NETVERSION) ? FMLNetworkConstants.NETVERSION : ip.split("\0")[1] : FMLNetworkConstants.NOVERSION;
	}

	public static ConnectionType getConnectionType(final Supplier<ClientConnection> connection) {
		return ConnectionType.forVersionFlag(connection.get().channel().attr(FMLNetworkConstants.FML_NETVERSION).get());
	}

	public static Packet<?> getEntitySpawningPacket(Entity entity) {
		return FMLNetworkConstants.playChannel.toVanillaPacket(new FMLPlayMessages.SpawnEntity(entity), NetworkDirection.PLAY_TO_CLIENT);
	}

	public static boolean onCustomPayload(final ICustomPacket<?> packet, final ClientConnection manager) {
		return NetworkRegistry.findTarget(packet.getName()).
				map(ni -> ni.dispatch(packet.getDirection(), packet, manager)).orElse(Boolean.FALSE);
	}

	public static void registerServerLoginChannel(ClientConnection manager, HandshakeC2SPacket packet) {
		manager.channel().attr(FMLNetworkConstants.FML_NETVERSION).set(packet.getFMLVersion());
		FMLHandshakeHandler.registerHandshake(manager, NetworkDirection.LOGIN_TO_CLIENT);
	}

	public synchronized static void registerClientLoginChannel(ClientConnection manager) {
		manager.channel().attr(FMLNetworkConstants.FML_NETVERSION).set(FMLNetworkConstants.NOVERSION);
		FMLHandshakeHandler.registerHandshake(manager, NetworkDirection.LOGIN_TO_SERVER);
	}

	public synchronized static void sendMCRegistryPackets(ClientConnection manager, String direction) {
		final Set<Identifier> resourceLocations = NetworkRegistry.buildChannelVersions().keySet().stream().
				filter(rl -> !Objects.equals(rl.getNamespace(), "minecraft")).
				collect(Collectors.toSet());
		FMLMCRegisterPacketHandler.INSTANCE.addChannels(resourceLocations, manager);
		FMLMCRegisterPacketHandler.INSTANCE.sendRegistry(manager, NetworkDirection.valueOf(direction));
	}

	public synchronized static void sendDimensionDataPacket(ClientConnection manager, ServerPlayerEntity player) {
		// don't send vanilla dims
		if (player.dimension.isVanilla()) return;
		// don't sent to local - we already have a valid dim registry locally
		if (manager.isLocal()) return;
		FMLNetworkConstants.playChannel.sendTo(new FMLPlayMessages.DimensionInfoMessage(player.dimension), manager, NetworkDirection.PLAY_TO_CLIENT);
	}

	public static void handleClientLoginSuccess(ClientConnection manager) {
		if (manager == null || manager.channel() == null) {
			throw new NullPointerException("ARGH! Network Manager is null (" + manager != null ? "CHANNEL" : "MANAGER" + ")");
		}
		if (getConnectionType(() -> manager) == ConnectionType.VANILLA) {
			LOGGER.info("Connected to a vanilla server. Catching up missing behaviour.");
			ConfigTracker.INSTANCE.loadDefaultServerConfigs();
		} else {
			LOGGER.info("Connected to a modded server.");
		}
	}

	public static boolean tickNegotiation(ServerLoginNetworkHandler netHandlerLoginServer, ClientConnection networkManager, ServerPlayerEntity player) {
		return FMLHandshakeHandler.tickLogin(networkManager);
	}

	/**
	 * Request to open a GUI on the client, from the server
	 * <p>
	 * Refer to {@link net.minecraftforge.fml.ExtensionPoint#GUIFACTORY} for how to provide a function to consume
	 * these GUI requests on the client.
	 * <p>
	 * The {@link IInteractionObject#getGuiID()} is treated as a {@link ResourceLocation}.
	 * It should refer to a valid modId namespace, to trigger opening on the client.
	 * The namespace is directly used to lookup the modId in the client side.
	 *
	 * @param player            The player to open the GUI for
	 * @param containerSupplier A supplier of container properties including the registry name of the container
	 */
	public static void openGui(ServerPlayerEntity player, NameableContainerProvider containerSupplier) {
		openGui(player, containerSupplier, buf -> {
		});
	}

	/**
	 * Request to open a GUI on the client, from the server
	 * <p>
	 * Refer to {@link net.minecraftforge.fml.ExtensionPoint#GUIFACTORY} for how to provide a function to consume
	 * these GUI requests on the client.
	 * <p>
	 * The {@link IInteractionObject#getGuiID()} is treated as a {@link ResourceLocation}.
	 * It should refer to a valid modId namespace, to trigger opening on the client.
	 * The namespace is directly used to lookup the modId in the client side.
	 *
	 * @param player            The player to open the GUI for
	 * @param containerSupplier A supplier of container properties including the registry name of the container
	 * @param pos               A block pos, which will be encoded into the auxillary data for this request
	 */
	public static void openGui(ServerPlayerEntity player, NameableContainerProvider containerSupplier, BlockPos pos) {
		openGui(player, containerSupplier, buf -> buf.writeBlockPos(pos));
	}

	/**
	 * Request to open a GUI on the client, from the server
	 * <p>
	 * Refer to {@link net.minecraftforge.fml.ExtensionPoint#GUIFACTORY} for how to provide a function to consume
	 * these GUI requests on the client.
	 * <p>
	 * The {@link IInteractionObject#getGuiID()} is treated as a {@link ResourceLocation}.
	 * It should refer to a valid modId namespace, to trigger opening on the client.
	 * The namespace is directly used to lookup the modId in the client side.
	 * The maximum size for #extraDataWriter is 32600 bytes.
	 *
	 * @param player            The player to open the GUI for
	 * @param containerSupplier A supplier of container properties including the registry name of the container
	 * @param extraDataWriter   Consumer to write any additional data the GUI needs
	 */
	public static void openGui(ServerPlayerEntity player, NameableContainerProvider containerSupplier, Consumer<PacketByteBuf> extraDataWriter) {
		if (player.world.isClient) return;
		player.method_14247();
		player.incrementContainerSyncId();
		int openContainerId = player.containerSyncId;
		PacketByteBuf extraData = new PacketByteBuf(Unpooled.buffer());
		extraDataWriter.accept(extraData);
		extraData.readerIndex(0); // reset to beginning in case modders read for whatever reason

		PacketByteBuf output = new PacketByteBuf(Unpooled.buffer());
		output.writeVarInt(extraData.readableBytes());
		output.writeBytes(extraData);

		if (output.readableBytes() > 32600 || output.readableBytes() < 1) {
			throw new IllegalArgumentException("Invalid PacketBuffer for openGui, found " + output.readableBytes() + " bytes");
		}
		Container c = containerSupplier.createMenu(openContainerId, player.inventory, player);
		ContainerType<?> type = c.getType();
		FMLPlayMessages.OpenContainer msg = new FMLPlayMessages.OpenContainer(type, openContainerId, containerSupplier.getDisplayName(), output);
		FMLNetworkConstants.playChannel.sendTo(msg, player.networkHandler.getConnection(), NetworkDirection.PLAY_TO_CLIENT);

		player.container = c;
		player.container.addListener(player);
		MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, c));
	}

	public static DimensionType getDummyDimType(final int dimension) {
		return trackingMap.computeIfAbsent(dimension, id -> DimensionType.byRawId(id));
	}

	static void addCachedDimensionType(final DimensionType dimensionType, final Identifier dimName) {
		trackingMap.put(dimensionType.getRawId(), dimensionType);
		final ClearableRegistry<DimensionType> dimtypereg = (ClearableRegistry<DimensionType>) Registry.DIMENSION;
		dimtypereg.set(dimensionType.getRawId(), dimName, dimensionType);
	}
}
