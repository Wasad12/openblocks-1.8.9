package openblocks.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import openmods.Log;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.MethodEntry;
import openmods.network.rpc.RpcCall;
import openmods.network.rpc.RpcMethodRegistry;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.sync.ISyncMapProvider;
import openmods.sync.SyncMap;
import openmods.sync.SyncMapTile;

// 1.8.9 transport backing the ported OpenMods sync/RPC: the 1.12.2 FML embedded
// channels + data registries don't exist in 1.8.9, so the SAME call sites
// (RpcCallDispatcher.senders, SyncChannelHolder, SyncRpcTarget) run over two
// SimpleNetworkWrapper messages. Wire bytes of the sync protocol and the RPC
// target/arg codecs are unchanged — only the envelope is 1.8.9-native.
public class OpenBlocksChannel {

	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("openblocks");

	private static final int ID_RPC_CALL = 0;
	private static final int ID_SYNC_UPDATE = 1;

	public static void init() {
		INSTANCE.registerMessage(HandlerRpcCall.class, MessageRpcCall.class, ID_RPC_CALL, Side.SERVER);
		INSTANCE.registerMessage(HandlerSyncUpdate.class, MessageSyncUpdate.class, ID_SYNC_UPDATE, Side.CLIENT);
	}

	public static void sendRpcCall(RpcCall call) {
		try {
			final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
			if (call.target instanceof SyncRpcTarget) tmp.writeByte(1);
			else if (call.target instanceof TileEntityRpcTarget) tmp.writeByte(0);
			else throw new IllegalArgumentException("Unknown RPC target: " + call.target);
			call.target.writeToStream(tmp);
			tmp.writeVarIntToBuffer(call.method.id);
			call.method.writeArgs(tmp, call.args);
			INSTANCE.sendToServer(new MessageRpcCall(toBytes(tmp)));
		} catch (Exception e) {
			Log.warn(e, "Failed to send RPC call %s", call.method);
		}
	}

	public static void sendSyncUpdate(byte[] payload, EntityPlayerMP player) {
		INSTANCE.sendTo(new MessageSyncUpdate(payload), player);
	}

	private static byte[] toBytes(PacketBuffer buf) {
		final byte[] result = new byte[buf.readableBytes()];
		buf.readBytes(result);
		return result;
	}

	public static class MessageRpcCall implements IMessage {
		private byte[] data;

		@SuppressWarnings("unused")
		public MessageRpcCall() {}

		public MessageRpcCall(byte[] data) {
			this.data = data;
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			final int length = buf.readInt();
			data = new byte[length];
			buf.readBytes(data);
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(data.length);
			buf.writeBytes(data);
		}
	}

	public static class HandlerRpcCall implements IMessageHandler<MessageRpcCall, IMessage> {
		@Override
		public IMessage onMessage(final MessageRpcCall message, final MessageContext ctx) {
			ctx.getServerHandler().playerEntity.getServerForPlayer().addScheduledTask(new Runnable() {
				@Override
				public void run() {
					try {
						final PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(message.data));
						final int targetType = buf.readByte();
						final IRpcTarget target;
						if (targetType == 0) target = new TileEntityRpcTarget();
						else if (targetType == 1) target = new SyncRpcTarget.SyncTileEntityRpcTarget();
						else throw new IllegalArgumentException("Unknown RPC target type: " + targetType);

						target.readFromStreamStream(Side.SERVER, ctx.getServerHandler().playerEntity, buf);

						final int methodId = buf.readVarIntFromBuffer();
						final MethodEntry entry = RpcMethodRegistry.getEntry(methodId);
						if (entry == null) throw new IllegalArgumentException("Unknown RPC method id: " + methodId);

						final Object[] args = entry.readArgs(buf);
						entry.method.invoke(target.getTarget(), args);
						target.afterCall();
					} catch (Exception e) {
						Log.warn(e, "Failed to handle RPC call");
					}
				}
			});
			return null;
		}
	}

	public static class MessageSyncUpdate implements IMessage {
		private byte[] data;

		@SuppressWarnings("unused")
		public MessageSyncUpdate() {}

		public MessageSyncUpdate(byte[] data) {
			this.data = data;
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			final int length = buf.readInt();
			data = new byte[length];
			buf.readBytes(data);
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(data.length);
			buf.writeBytes(data);
		}
	}

	public static class HandlerSyncUpdate implements IMessageHandler<MessageSyncUpdate, IMessage> {
		@Override
		public IMessage onMessage(final MessageSyncUpdate message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(new Runnable() {
				@Override
				public void run() {
					try {
						final PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(message.data));
						final int ownerType = buf.readVarIntFromBuffer();
						if (ownerType != SyncMapTile.OWNER_TYPE)
							throw new IllegalArgumentException("Unknown sync map owner type: " + ownerType);

						final ISyncMapProvider provider = SyncMapTile.findOwner(Minecraft.getMinecraft().theWorld, buf);
						if (provider != null) {
							final SyncMap map = provider.getSyncMap();
							map.readUpdate(buf);
						}
					} catch (Exception e) {
						Log.warn(e, "Failed to handle sync update");
					}
				}
			});
			return null;
		}
	}

}
