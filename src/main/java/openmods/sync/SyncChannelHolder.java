package openmods.sync;

import io.netty.buffer.Unpooled;
import java.util.Collection;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import openblocks.common.network.OpenBlocksChannel;

// 1.8.9 port of OpenModsLib SyncChannelHolder: same singleton + sendPayloadToPlayers
// seam (SyncMapServer calls it verbatim), routed over OpenBlocksChannel
// (SimpleNetworkWrapper) instead of the 1.12.2 FML embedded channel.
public class SyncChannelHolder {

	public static final SyncChannelHolder INSTANCE = new SyncChannelHolder();

	public void sendPayloadToPlayers(PacketBuffer payload, Collection<EntityPlayerMP> players) {
		final byte[] data = new byte[payload.readableBytes()];
		payload.getBytes(payload.readerIndex(), data);
		for (EntityPlayerMP player : players)
			OpenBlocksChannel.sendSyncUpdate(data, player);
	}

	public static PacketBuffer wrapPayload(byte[] data) {
		return new PacketBuffer(Unpooled.wrappedBuffer(data));
	}

	public static void ensureLoaded() {}
}
