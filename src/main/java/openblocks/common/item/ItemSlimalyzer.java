package openblocks.common.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import openblocks.OpenBlocks;

public class ItemSlimalyzer extends Item {

	// NOTE for future: world seed is not available on client side

	private static final String TAG_ACTIVE = "Active";

	public ItemSlimalyzer() {
		setUnlocalizedName("openblocks.slimalyzer");
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		// No property override: IItemPropertyGetter does not exist in 1.8.9
		// (VERIFIED absent via javap). The active/inactive model switch lives in
		// the ClientProxy mesh definition instead (glider pattern).
	}

	public static boolean isActive(ItemStack stack) {
		final NBTTagCompound itemTag = stack.getTagCompound();
		return itemTag != null && itemTag.getBoolean(TAG_ACTIVE);
	}

	private static NBTTagCompound getItemTag(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) {
			tag = new NBTTagCompound();
			stack.setTagCompound(tag);
		}
		return tag;
	}

	private static boolean isInSlimeChunk(World world, Entity entity) {
		if (world == null || entity == null) return false;

		final Chunk chunk = world.getChunkFromBlockCoords(entity.getPosition());
		return chunk.getRandomWithSeed(987234911L).nextInt(10) == 0;
	}

	private static boolean update(ItemStack stack, World world, Entity entity) {
		final boolean isActive = isActive(stack);
		final boolean isInSlimeChunk = isInSlimeChunk(world, entity);
		if (isActive != isInSlimeChunk) {
			if (isInSlimeChunk)
				world.playSoundEffect(entity.posX, entity.posY, entity.posZ,
						"openblocks:slimalyzer.signal", 1F, 1F);
			getItemTag(stack).setBoolean(TAG_ACTIVE, isInSlimeChunk);
			return true;
		}
		return false;
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if (!world.isRemote)
			update(stack, world, entity);
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		final World world = entityItem.worldObj;

		if (!world.isRemote) {
			final ItemStack stack = entityItem.getEntityItem();
			if (update(stack, world, entityItem))
				entityItem.setEntityItemStack(stack);
		}

		return false;
	}
}
