package openblocks.common.block;

import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IUnlistedProperty;
import openblocks.common.tileentity.TileEntityVacuumHopper;

// 1.8.9 replacement for 1.12.2 VariantModelState on the vacuum hopper: carries the
// TE's per-side output map (side name -> "items"/"fluids"/"both", same ids as the
// 1.12.2 blockstate variants) as an unlisted blockstate property, read by the
// VacuumHopperModel smart nozzle model. TankNeighbourState pattern.
public class HopperOutputState implements IUnlistedProperty<Map<String, String>> {

	public static final HopperOutputState PROPERTY = new HopperOutputState();

	private HopperOutputState() {}

	@Override
	public String getName() {
		return "hopper_outputs";
	}

	@Override
	public boolean isValid(Map<String, String> value) {
		return value != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Map<String, String>> getType() {
		return (Class<Map<String, String>>)(Class<?>)Map.class;
	}

	@Override
	public String valueToString(Map<String, String> value) {
		return value.toString();
	}

	public static Map<String, String> computeState(IBlockAccess world, BlockPos pos) {
		final TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileEntityVacuumHopper)
			return ((TileEntityVacuumHopper)te).getOutputState();

		return com.google.common.collect.ImmutableMap.of();
	}
}
