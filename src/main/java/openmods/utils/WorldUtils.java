package openmods.utils;

import com.google.common.base.Preconditions;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import openblocks.OpenBlocks;

// 1.8.9 port of OpenModsLib WorldUtils: getWorld is routed through our sided proxy
// (the 1.12.2 version calls OpenMods.proxy; same pattern, our proxy).
public class WorldUtils {

	public static World getWorld(Side side, int dimensionId) {
		final World result;
		if (side == Side.SERVER) {
			result = OpenBlocks.proxy.getServerWorld(dimensionId);
		} else {
			result = OpenBlocks.proxy.getClientWorld();
			Preconditions.checkArgument(result.provider.getDimensionId() == dimensionId, "Invalid client dimension id %s", dimensionId);
		}

		Preconditions.checkNotNull(result, "Invalid world dimension %s", dimensionId);
		return result;
	}

	public static boolean isTileEntityValid(TileEntity te) {
		if (te.isInvalid()) return false;

		final World world = te.getWorld();
		return (world != null)? world.isBlockLoaded(te.getPos()) : false;
	}
}
