package openmods.utils;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.IFluidHandler;

// 1.8.9 port of OpenModsLib CompatibilityUtils: 1.8.9 has no fluid capabilities,
// so a fluid handler is simply a TE implementing the old IFluidHandler (same
// convention the Tank port uses).
public class CompatibilityUtils {

	public static IFluidHandler getFluidHandler(TileEntity te, EnumFacing side) {
		if (te == null) return null;
		if (te instanceof IFluidHandler) return (IFluidHandler)te;

		return null;
	}

	public static boolean isFluidHandler(TileEntity te, EnumFacing side) {
		return te != null && (te instanceof IFluidHandler);
	}

}
