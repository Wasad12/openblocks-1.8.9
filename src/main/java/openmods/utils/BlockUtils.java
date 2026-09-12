package openmods.utils;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

// 1.8.9 port of OpenModsLib BlockUtils: only the neighbour-tile lookups needed by
// the ported feature code. Imports adjusted (1.8.9: no util.math package).
public class BlockUtils {

	public static TileEntity getTileInDirection(TileEntity tile, EnumFacing direction) {
		final BlockPos offset = tile.getPos().offset(direction);
		return tile.getWorld().getTileEntity(offset);
	}

	public static TileEntity getTileInDirection(World world, BlockPos coord, EnumFacing direction) {
		return world.getTileEntity(coord.offset(direction));
	}

	public static TileEntity getTileInDirectionSafe(World world, BlockPos coord, EnumFacing direction) {
		BlockPos n = coord.offset(direction);
		return world.isBlockLoaded(n)? world.getTileEntity(n) : null;
	}
}
