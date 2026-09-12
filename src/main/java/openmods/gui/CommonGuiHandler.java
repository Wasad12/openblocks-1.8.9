package openmods.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.api.IHasGui;

// Imports adjusted (1.8.9: no util.math package). OPEN_MODS_TE_GUI id (-1, same as
// 1.12.2 OpenBlock.OPEN_MODS_TE_GUI) kept as a local constant — our blocks don't
// extend OpenBlock, so the constant lives here instead.
public class CommonGuiHandler implements IGuiHandler {

	public static final int OPEN_MODS_TE_GUI = -1;

	protected final IGuiHandler wrappedHandler;

	public CommonGuiHandler(IGuiHandler wrappedHandler) {
		this.wrappedHandler = wrappedHandler;
	}

	public CommonGuiHandler() {
		this.wrappedHandler = null;
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		if (id != OPEN_MODS_TE_GUI) return wrappedHandler != null? wrappedHandler.getServerGuiElement(id, player, world, x, y, z) : null;
		else {
			TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
			if (tile instanceof IHasGui) return ((IHasGui)tile).getServerGui(player);
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}
}
