package openmods.utils;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;

// 1.8.9 port of OpenModsLib NetUtils: only getPlayersWatchingBlock is needed by the
// ported sync code. 1.8.9's PlayerManager exposes isPlayerWatchingChunk (no direct
// watcher-set query), so the watcher set is built the same way by iteration.
// FML pipeline helpers dropped per §19 (transport uses SimpleNetworkWrapper).
public class NetUtils {

	public static Set<EntityPlayerMP> getPlayersWatchingBlock(WorldServer world, int blockX, int blockZ) {
		return getPlayersWatchingChunk(world, blockX >> 4, blockZ >> 4);
	}

	public static Set<EntityPlayerMP> getPlayersWatchingChunk(WorldServer world, int chunkX, int chunkZ) {
		final PlayerManager playerManager = world.getPlayerManager();

		final Set<EntityPlayerMP> playerList = Sets.newHashSet();

		for (Object o : world.playerEntities) {
			final EntityPlayerMP player = (EntityPlayerMP)o;
			if (playerManager.isPlayerWatchingChunk(player, chunkX, chunkZ)) playerList.add(player);
		}
		return playerList;
	}
}
