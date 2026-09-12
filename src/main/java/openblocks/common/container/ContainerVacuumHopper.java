package openblocks.common.container;

import net.minecraft.inventory.IInventory;
import openblocks.common.tileentity.TileEntityVacuumHopper;
import openmods.container.ContainerInventoryProvider;

// 1.8.9 port of 1.12.2 ContainerVacuumHopper: verbatim (5-wide grid over the
// 10-slot inventory + player inventory at y=69 — same 176x151 shape).
public class ContainerVacuumHopper extends ContainerInventoryProvider<TileEntityVacuumHopper> {

	public ContainerVacuumHopper(IInventory playerInventory, TileEntityVacuumHopper hopper) {
		super(playerInventory, hopper);
		addInventoryGrid(44, 20, 5);
		addPlayerInventorySlots(69);
	}

}
