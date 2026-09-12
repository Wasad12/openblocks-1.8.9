package openblocks.common.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidTank;
import openblocks.Config;
import openblocks.OpenBlocks;
import openblocks.common.item.ItemTankBlock;
import openblocks.common.tileentity.TileEntityTank;

// 1.8.9 port of 1.12.2 BlockTank. OpenBlock base replaced with plain BlockContainer
// (hardness 1.0F kept); ExtendedBlockState/VariantModelState frame-hiding replaced with
// a static full-frame model (see ARCHITECTURE.md).
public class BlockTank extends BlockContainer {

	public BlockTank() {
		super(Material.rock);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.tank");
	}

	private static TileEntityTank getTank(IBlockAccess world, BlockPos pos) {
		final TileEntity te = world.getTileEntity(pos);
		return (te instanceof TileEntityTank)? (TileEntityTank)te : null;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityTank();
	}

	@Override
	protected BlockState createBlockState() {
		return new ExtendedBlockState(this,
				new IProperty[0],
				new IUnlistedProperty[] { TankNeighbourState.PROPERTY });
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState extended = (IExtendedBlockState)state;
			return extended.withProperty(TankNeighbourState.PROPERTY,
					TankNeighbourState.computeFlags(world, pos));
		}
		return state;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		// 1.8.9 BlockContainer returns -1 (INVISIBLE); we have a static frame model + TESR.
		return 3;
	}

	@Override
	public EnumWorldBlockLayer getBlockLayer() {
		return EnumWorldBlockLayer.CUTOUT;
	}

	@Override
	public int getLightValue(IBlockAccess world, BlockPos pos) {
		if (!Config.tanksEmitLight) return 0;

		TileEntityTank tile = getTank(world, pos);
		return tile != null? tile.getFluidLightLevel() : 0;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		TileEntityTank tile = getTank(world, pos);
		return tile != null? tile.onBlockActivated(player, side, hitX, hitY, hitZ) : false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		TileEntityTank tile = getTank(world, pos);
		if (tile != null) tile.onBlockPlacedBy(placer, stack);
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		super.onNeighborBlockChange(world, pos, state, neighborBlock);
		TileEntityTank tile = getTank(world, pos);
		if (tile != null) tile.onNeighbourChanged(pos, neighborBlock);
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos) {
		ItemStack result = new ItemStack(this);
		TileEntityTank tile = getTank(world, pos);
		if (tile != null) {
			IFluidTank tank = tile.getTank();
			if (tank.getFluidAmount() > 0) {
				NBTTagCompound tankTag = tile.getItemNBT();
				if (tankTag.hasKey("Amount")) tankTag.setInteger("Amount", tank.getCapacity());

				NBTTagCompound nbt = getItemTag(result);
				nbt.setTag("tank", tankTag);
			}
		}
		return result;
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		List<ItemStack> drops = new ArrayList<ItemStack>();
		ItemStack stack = new ItemStack(this);

		NBTTagCompound tankTag = pendingDropTank;
		if (tankTag == null) {
			TileEntityTank tile = getTank(world, pos);
			if (tile != null && tile.getTank().getFluidAmount() > 0)
				tankTag = tile.getItemNBT();
		}

		if (tankTag != null && tankTag.hasKey("FluidName")) {
			NBTTagCompound itemTag = getItemTag(stack);
			itemTag.setTag("tank", tankTag);
		}

		drops.add(stack);
		return drops;
	}

	// Harvest-time tank-NBT stash. harvestBlock hands us the live TE directly, so
	// the drop no longer depends on a world TE lookup inside getDrops (which observed
	// an empty tank on the 1.8.9 break path even though the path provably calls
	// getDrops — see PORTING_LOG fix loop 6). Server thread is sequential, and the
	// field is always cleared in finally, so this cannot leak across harvests.
	private NBTTagCompound pendingDropTank;

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
		if (te instanceof TileEntityTank && ((TileEntityTank)te).getTank().getFluidAmount() > 0)
			pendingDropTank = ((TileEntityTank)te).getItemNBT();
		try {
			super.harvestBlock(world, player, pos, state, te);
		} finally {
			pendingDropTank = null;
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

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	@Override
	public int getComparatorInputOverride(World world, BlockPos pos) {
		TileEntityTank tile = getTank(world, pos);
		if (tile == null) return 0;
		double value = tile.getFluidRatio() * 15;
		if (value == 0) return 0;
		int trunc = MathHelper.floor_double(value);
		return Math.max(trunc, 1);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void getSubBlocks(Item item, CreativeTabs tab, List result) {
		if (tab == OpenBlocks.tabOpenBlocks || tab == CreativeTabs.tabAllSearch) {
			result.add(new ItemStack(this));

			if (tab == CreativeTabs.tabAllSearch && Config.displayAllFilledTanks) {
				final ItemStack emptyTank = new ItemStack(this);
				for (Fluid fluid : FluidRegistry.getRegisteredFluids().values())
					try {
						final ItemStack tankStack = emptyTank.copy();
						if (ItemTankBlock.fillTankItem(tankStack, fluid)) result.add(tankStack);
					} catch (Throwable t) {
						throw new RuntimeException(String.format("Failed to create item for fluid '%s'" +
								"Until this is fixed, you can bypass this code with config option 'tanks.displayAllFluids'",
								fluid.getName()), t);
					}
			}
		}
	}
}
