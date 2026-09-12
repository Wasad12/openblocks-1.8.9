package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import openblocks.OpenBlocks;
import openblocks.common.tileentity.TileEntityAutoAnvil;
import openmods.gui.CommonGuiHandler;

// 1.8.9 port of 1.12.2 BlockAutoAnvil: same anvil block (non-opaque, non-solid
// sides, GUI, neighbour dispatch, break drops). 1.8.9 adaptations: plain Block (no
// OpenBlock — hardness 1.0F like every OpenBlock), Material.anvil + Block.
// soundTypeAnvil (inner class + setStepSound on 1.8.9), getRenderType MODEL (tank
// lesson), TwoDirections → vanilla-style horizontal FACING with rotateY placement
// (same perpendicular long axis as ZN_YP/XP_YP), 1.8.9 onBlockPlaced hook.
public class BlockAutoAnvil extends Block {

	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

	public BlockAutoAnvil() {
		super(Material.anvil);
		setHardness(1.0F);
		setStepSound(Block.soundTypeAnvil);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.auto_anvil");
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	protected BlockState createBlockState() {
		return new BlockState(this, new IProperty[] { FACING });
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex();
	}

	@Override
	public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		// long axis perpendicular to the placer, like the vanilla anvil (and the
		// 1.12.2 TwoDirections orientations, which are the same two states)
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().rotateY());
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
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) return true;
		player.openGui(OpenBlocks.instance, CommonGuiHandler.OPEN_MODS_TE_GUI, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		super.onNeighborBlockChange(world, pos, state, neighborBlock);
		if (!world.isRemote) {
			final TileEntityAutoAnvil te = getTile(world, pos);
			if (te != null) te.onNeighbourChanged(pos, neighborBlock);
		}
	}

	private static TileEntityAutoAnvil getTile(World world, BlockPos pos) {
		if (!world.isBlockLoaded(pos)) return null;
		final net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEntityAutoAnvil? (TileEntityAutoAnvil)te : null;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntityAutoAnvil createTileEntity(World world, IBlockState state) {
		return new TileEntityAutoAnvil();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		// drop inventory contents like vanilla containers (1.12.2 did this via the
		// OpenBlock break path; same outcome, vanilla 1.8.9 mechanism)
		final TileEntityAutoAnvil te = getTile(world, pos);
		if (te != null) te.dropContents(world, pos);
		super.breakBlock(world, pos, state);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
	}
}
