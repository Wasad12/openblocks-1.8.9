package openblocks.common.item;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import openblocks.Config;
import openblocks.OpenBlocks;

public class ItemSpongeOnAStick extends Item {

	public ItemSpongeOnAStick() {
		setUnlocalizedName("openblocks.sponge_on_a_stick");
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setMaxStackSize(1);
		setMaxDamage(Config.spongeMaxDamage);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return soakUp(world, pos, player, stack);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		soakUp(world, player.getPosition(), player, stack);
		return stack;
	}

	private static boolean soakUp(World world, BlockPos pos, EntityPlayer player, ItemStack stack) {
		boolean absorbedAnything = false;
		boolean hitLava = false;
		int damage = stack.getItemDamage();

		for (int x = -Config.spongeStickRange; x <= Config.spongeStickRange; x++) {
			for (int y = -Config.spongeStickRange; y <= Config.spongeStickRange; y++) {
				for (int z = -Config.spongeStickRange; z <= Config.spongeStickRange; z++) {
					final BlockPos targetPos = pos.add(x, y, z);

					Material material = world.getBlockState(targetPos).getBlock().getMaterial();
					if (material.isLiquid()) {
						absorbedAnything = true;
						hitLava |= material == Material.lava;
						world.setBlockToAir(targetPos);
						if (++damage >= Config.spongeMaxDamage) break;
					}

				}
			}
		}

		if (hitLava) {
			stack.stackSize = 0;
			player.setFire(6);
		}

		if (absorbedAnything) {
			stack.damageItem(1, player);
			return true;
		}

		return false;
	}
}
