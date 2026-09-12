package openmods.utils;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

// 1.8.9 port of OpenModsLib InventoryUtils: only tryGetHandler is needed by the
// ported feature code (merge helpers dropped per §19). Imports adjusted (1.8.9:
// no util.math package); logic verbatim — 1.8.9 HAS item-handler capabilities.
public class InventoryUtils {

	public static IItemHandler tryGetHandler(World world, BlockPos pos, EnumFacing side) {
		if (!world.isBlockLoaded(pos)) return null;
		final TileEntity te = world.getTileEntity(pos);

		return tryGetHandler(te, side);
	}

	public static IItemHandler tryGetHandler(TileEntity te, EnumFacing side) {
		if (te == null) return null;

		if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side))
			return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);

		if (te instanceof ISidedInventory)
			return new SidedInvWrapper((ISidedInventory)te, side);

		if (te instanceof IInventory)
			return new InvWrapper((IInventory)te);

		return null;
	}
}
