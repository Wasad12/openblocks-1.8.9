package openmods.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

// Imports adjusted (1.8.9: no util.math package); distance check via
// getDistanceSqToCenter (1.8.9 equivalent of 1.12.2 getDistanceSq); usability
// spelling. Logic verbatim.
public class TileEntityInventory extends GenericInventory {

	private final TileEntity owner;

	public TileEntityInventory(TileEntity owner, String name, boolean isInvNameLocalized, int size) {
		super(name, isInvNameLocalized, size);
		this.owner = owner;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return (owner.getWorld().getTileEntity(owner.getPos()) == owner)
				&& (player.getDistanceSqToCenter(owner.getPos()) <= 64.0D);
	}

}
