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

package net.minecraftforge.fml.common.network;

import java.util.Iterator;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.network.Packet;
import net.minecraft.network.SplitterHandler;
import net.minecraft.network.SizePrepender;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.network.NetworkSide;

public class PacketLoggingHandler {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void register(ClientConnection manager) {
		ChannelPipeline pipeline = manager.channel().pipeline();
		final NetworkSide direction = manager.getDirection();
		if (manager.isLocal()) {
			pipeline.addBefore("packet_handler", "splitter", new SimpleChannelInboundHandler<Packet<?>>() {
				String prefix = (direction == NetworkSide.SERVERBOUND ? "SERVER: C->S" : "CLIENT: S->C");

				@Override
				protected void channelRead0(ChannelHandlerContext ctx, Packet<?> msg) throws Exception {
					PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
					msg.write(buf);
					LOGGER.debug("{} {}:\n{}", prefix, msg.getClass().getSimpleName(), ByteBufUtils.getContentDump(buf));
					ctx.fireChannelRead(msg);
				}
			});
			pipeline.addBefore("splitter", "prepender", new ChannelOutboundHandlerAdapter() {
				String prefix = (direction == NetworkSide.SERVERBOUND ? "SERVER: S->C" : "CLIENT: C->S");

				@Override
				public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
					if (msg instanceof Packet<?>) {
						PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
						((Packet<?>) msg).write(buf);
						LOGGER.debug("{} {}:\n{}", prefix, msg.getClass().getSimpleName(), ByteBufUtils.getContentDump(buf));
					}
					ctx.write(msg, promise);
				}
			});
		} else {
			pipeline.replace("splitter", "splitter", new SplitterHandler() {
				String prefix = (direction == NetworkSide.SERVERBOUND ? "SERVER: C->S" : "CLIENT: S->C");

				@Override
				protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> output) throws Exception {
					super.decode(context, input, output);
					Iterator<Object> itr = output.iterator();
					while (itr.hasNext()) {
						ByteBuf pkt = (ByteBuf) itr.next();
						pkt.markReaderIndex();
						LOGGER.debug("{}:\n{}", prefix, ByteBufUtils.getContentDump(pkt));
						pkt.resetReaderIndex();
					}
				}
			});
			pipeline.replace("prepender", "prepender", new SizePrepender() {
				String prefix = (direction == NetworkSide.SERVERBOUND ? "SERVER: S->C" : "CLIENT: C->S");

				@Override
				protected void encode(ChannelHandlerContext context, ByteBuf input, ByteBuf output) throws Exception {
					input.markReaderIndex();
					LOGGER.debug("{}:\n{}", prefix, ByteBufUtils.getContentDump(input));
					input.resetReaderIndex();
					super.encode(context, input, output);
				}
			});
		}
	}
}
