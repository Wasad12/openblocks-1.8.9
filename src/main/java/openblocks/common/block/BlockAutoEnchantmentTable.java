package openblocks.common.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openblocks.OpenBlocks;
import openblocks.common.tileentity.TileEntityAutoEnchantmentTable;

// 1.8.9 port of 1.12.2 BlockAutoEnchantmentTable: same 0.75-height box, book
// particles and non-solid sides. 1.8.9 adaptations: plain Block (no OpenBlock),
// getRenderType MODEL (tank lesson — BlockContainer returns INVISIBLE... note this
// block has a TE but extends plain Block, so createTileEntity/hasTileEntity are
// overridden directly), shower-lesson bounds pattern, S35-era randomDisplayTick,
// onBlockActivated opens the GUI, neighbour changes dispatched to the TE.
public class BlockAutoEnchantmentTable extends Block {

	protected static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);

	public BlockAutoEnchantmentTable() {
		super(Material.rock);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.auto_enchantment_table");
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
		return AABB.offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) {
		return AABB.offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public int getRenderType() {
		return 3;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
		return false;
	}

	@Override
	public EnumWorldBlockLayer getBlockLayer() {
		return EnumWorldBlockLayer.SOLID;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, BlockPos pos, IBlockState state, Random rand) {
		super.randomDisplayTick(world, pos, state, rand);

		for (int x = -2; x <= 2; ++x) {
			for (int z = -2; z <= 2; ++z) {
				if (x > -2 && x < 2 && z == -1) z = 2;

				if (rand.nextInt(16) == 0) {
					for (int y = 0; y <= 1; ++y) {
						final BlockPos blockpos = pos.add(x, y, z);

						if (ForgeHooks.getEnchantPower(world, blockpos) > 0) {
							if (world.isAirBlock(pos.add(x / 2, 0, z / 2))) {
								world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
										pos.getX() + 0.5D,
										pos.getY() + 2.0D,
										pos.getZ() + 0.5D,
										x + rand.nextFloat() - 0.5D,
										y - rand.nextFloat() - 1.0F,
										z + rand.nextFloat() - 0.5D);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) return true;
		player.openGui(OpenBlocks.instance, openmods.gui.CommonGuiHandler.OPEN_MODS_TE_GUI, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		super.onNeighborBlockChange(world, pos, state, neighborBlock);
		if (!world.isRemote) {
			final TileEntityAutoEnchantmentTable te = getTile(world, pos);
			if (te != null) te.onNeighbourChanged(pos, neighborBlock);
		}
	}

	private static TileEntityAutoEnchantmentTable getTile(World world, BlockPos pos) {
		if (!world.isBlockLoaded(pos)) return null;
		final net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEntityAutoEnchantmentTable? (TileEntityAutoEnchantmentTable)te : null;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntityAutoEnchantmentTable createTileEntity(World world, IBlockState state) {
		return new TileEntityAutoEnchantmentTable();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		// drop inventory contents like vanilla containers (1.12.2 did this via the
		// OpenBlock break path; same outcome, vanilla 1.8.9 mechanism)
		final TileEntityAutoEnchantmentTable te = getTile(world, pos);
		if (te != null) te.dropContents(world, pos);
		super.breakBlock(world, pos, state);
	}
}
