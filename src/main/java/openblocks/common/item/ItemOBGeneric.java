package openblocks.common.item;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import openblocks.OpenBlocks;

public class ItemOBGeneric extends Item {

	public static final int META_GLIDER_WING = 0;

	public ItemOBGeneric() {
		setHasSubtypes(true);
		setMaxDamage(0);
		setMaxStackSize(64);
		setUnlocalizedName("openblocks.generic");
		setCreativeTab(OpenBlocks.tabOpenBlocks);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		if (stack != null && stack.getItemDamage() == META_GLIDER_WING) return "item.openblocks.glider_wing";
		return super.getUnlocalizedName(stack);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		list.add(new ItemStack(item, 1, META_GLIDER_WING));
	}
}
