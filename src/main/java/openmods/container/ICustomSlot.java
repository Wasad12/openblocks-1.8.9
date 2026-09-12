package openmods.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

// 1.8.9 port of OpenModsLib ICustomSlot: ClickType doesn't exist in 1.8.9 (added
// in 1.9), so the click mode arrives as the raw drag-type int, like vanilla 1.8.9
// Container.slotClick(int, int, int, EntityPlayer).
public interface ICustomSlot {

	public ItemStack onClick(EntityPlayer player, int dragType, int clickType);

	public boolean canDrag();

	public boolean canTransferItemsOut();

	public boolean canTransferItemsIn();
}
