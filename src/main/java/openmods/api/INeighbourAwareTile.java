package openmods.api;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;

public interface INeighbourAwareTile {

	public void onNeighbourChanged(BlockPos neighbourPos, Block neigbourBlock);
}
