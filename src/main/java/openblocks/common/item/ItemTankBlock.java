package openblocks.common.item;

import com.google.common.base.Strings;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import openblocks.OpenBlocks;
import openblocks.common.tileentity.TileEntityTank;

// 1.8.9 port of 1.12.2 ItemTankBlock. Fluid-handler/texture capabilities and the
// `level` item-override (both 1.9+ systems) dropped; NBT, tooltip, filled name,
// creative listing and fillTankItem kept.
public class ItemTankBlock extends ItemBlock {

	public static final String TANK_TAG = "tank";

	public ItemTankBlock(Block block) {
		super(block);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void getSubItems(Item item, CreativeTabs tab, List result) {
		block.getSubBlocks(item, tab, result);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addInformation(ItemStack stack, EntityPlayer player, List result, boolean advanced) {
		FluidTank fakeTank = readTank(stack);
		FluidStack fluidStack = fakeTank.getFluid();
		if (fluidStack != null && fluidStack.amount > 0) {
			float percent = Math.max(100.0f / fakeTank.getCapacity() * fluidStack.amount, 1);
			result.add(String.format("%d mB (%.0f%%)", fluidStack.amount, percent));
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		final FluidTank fakeTank = readTank(stack);
		final FluidStack fluidStack = fakeTank.getFluid();
		final String unlocalizedName = getUnlocalizedName();

		if (fluidStack != null && fluidStack.amount > 0) {
			final String fluidName = fluidStack.getLocalizedName();
			if (!Strings.isNullOrEmpty(fluidName))
				return StatCollector.translateToLocalFormatted(unlocalizedName + ".filled.name", fluidName);
		}

		return super.getItemStackDisplayName(stack);
	}

	public static boolean fillTankItem(ItemStack result, Fluid fluid) {
		if (result == null || !(result.getItem() instanceof ItemTankBlock)) return false;
		final int tankCapacity = TileEntityTank.getTankCapacity();
		FluidStack stack = FluidRegistry.getFluidStack(fluid.getName(), tankCapacity);
		if (stack == null) return false;

		FluidTank tank = new FluidTank(tankCapacity);
		tank.setFluid(stack);

		saveTank(result, tank);
		return true;
	}

	private static FluidTank readTank(ItemStack stack) {
		FluidTank tank = new FluidTank(TileEntityTank.getTankCapacity());

		final NBTTagCompound itemTag = stack.getTagCompound();
		if (itemTag != null && itemTag.hasKey(TANK_TAG)) {
			tank.readFromNBT(itemTag.getCompoundTag(TANK_TAG));
			return tank;
		}

		return tank;
	}

	private static void saveTank(ItemStack container, FluidTank tank) {
		if (tank.getFluidAmount() > 0) {
			NBTTagCompound itemTag = getItemTag(container);

			NBTTagCompound tankTag = new NBTTagCompound();
			tank.writeToNBT(tankTag);
			itemTag.setTag(TANK_TAG, tankTag);
		} else {
			container.setTagCompound(null);
		}
	}

	private static NBTTagCompound getItemTag(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) {
			tag = new NBTTagCompound();
			stack.setTagCompound(tag);
		}
		return tag;
	}
}
