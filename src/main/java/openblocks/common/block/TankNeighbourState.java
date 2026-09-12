package openblocks.common.block;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.property.IUnlistedProperty;
import openblocks.common.tileentity.TileEntityTank;

// 1.8.9 replacement for 1.12.2 NeighbourMap + VariantModelState: carries the same 18
// neighbour flags ("n_t", "n_b", ... — same ids and same accepts() rule) as an unlisted
// blockstate property, read by the smart frame model. No lib model loader involved.
public class TankNeighbourState implements IUnlistedProperty<Set<String>> {

	public static final TankNeighbourState PROPERTY = new TankNeighbourState();

	private TankNeighbourState() {}

	@Override
	public String getName() {
		return "tank_neighbours";
	}

	@Override
	public boolean isValid(Set<String> value) {
		return value != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Set<String>> getType() {
		return (Class<Set<String>>)(Class<?>)Set.class;
	}

	@Override
	public String valueToString(Set<String> value) {
		return value.toString();
	}

	private static void testNeighbour(ImmutableSet.Builder<String> result, IBlockAccess world, FluidStack ownFluid, int x, int y, int z, String id) {
		final TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		if (te instanceof TileEntityTank && ((TileEntityTank)te).accepts(ownFluid)) result.add(id);
	}

	public static ImmutableSet<String> computeFlags(IBlockAccess world, BlockPos pos) {
		final TileEntity te = world.getTileEntity(pos);
		final FluidStack ownFluid = (te instanceof TileEntityTank)? ((TileEntityTank)te).getTank().getFluid() : null;

		final int x = pos.getX();
		final int y = pos.getY();
		final int z = pos.getZ();

		final ImmutableSet.Builder<String> neighbours = ImmutableSet.builder();
		testNeighbour(neighbours, world, ownFluid, x + 0, y + 1, z + 0, "n_t");
		testNeighbour(neighbours, world, ownFluid, x + 0, y - 1, z + 0, "n_b");

		testNeighbour(neighbours, world, ownFluid, x + 1, y + 0, z + 0, "n_e");
		testNeighbour(neighbours, world, ownFluid, x - 1, y + 0, z + 0, "n_w");
		testNeighbour(neighbours, world, ownFluid, x + 0, y + 0, z + 1, "n_s");
		testNeighbour(neighbours, world, ownFluid, x + 0, y + 0, z - 1, "n_n");

		testNeighbour(neighbours, world, ownFluid, x + 1, y + 1, z + 0, "n_te");
		testNeighbour(neighbours, world, ownFluid, x - 1, y + 1, z + 0, "n_tw");
		testNeighbour(neighbours, world, ownFluid, x + 0, y + 1, z + 1, "n_ts");
		testNeighbour(neighbours, world, ownFluid, x + 0, y + 1, z - 1, "n_tn");

		testNeighbour(neighbours, world, ownFluid, x + 1, y - 1, z + 0, "n_be");
		testNeighbour(neighbours, world, ownFluid, x - 1, y - 1, z + 0, "n_bw");
		testNeighbour(neighbours, world, ownFluid, x + 0, y - 1, z + 1, "n_bs");
		testNeighbour(neighbours, world, ownFluid, x + 0, y - 1, z - 1, "n_bn");

		testNeighbour(neighbours, world, ownFluid, x - 1, y + 0, z - 1, "n_nw");
		testNeighbour(neighbours, world, ownFluid, x - 1, y + 0, z + 1, "n_sw");
		testNeighbour(neighbours, world, ownFluid, x + 1, y + 0, z + 1, "n_se");
		testNeighbour(neighbours, world, ownFluid, x + 1, y + 0, z - 1, "n_ne");

		return neighbours.build();
	}
}
