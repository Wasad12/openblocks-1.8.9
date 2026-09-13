package openblocks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IUnlistedProperty;
import openblocks.common.tileentity.TileEntityFan;

// 1.8.9 replacement for the fan's share of 1.12.2 VariantModelState/EvalModelState:
// carries TE presence as an unlisted blockstate property, read by the FanBlockModel
// smart model (which renders nothing in-world — the TESR draws the rotated head —
// and the full static model everywhere else). HopperOutputState pattern.
public class FanRenderState implements IUnlistedProperty<Boolean> {

	public static final FanRenderState PROPERTY = new FanRenderState();

	private FanRenderState() {}

	@Override
	public String getName() {
		return "fan_has_te";
	}

	@Override
	public boolean isValid(Boolean value) {
		return value != null;
	}

	@Override
	public Class<Boolean> getType() {
		return Boolean.class;
	}

	@Override
	public String valueToString(Boolean value) {
		return value.toString();
	}

	public static Boolean computeState(IBlockAccess world, BlockPos pos) {
		final TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEntityFan? Boolean.TRUE : Boolean.FALSE;
	}
}
