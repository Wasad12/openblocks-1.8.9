package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import openblocks.OpenBlocks;

// 1.8.9 port of 1.12.2 BlockXPShower. OpenBlock.FourDirections/SURFACE-mode/lib
// orientation replaced with a vanilla horizontal FACING (= direction of the tank,
// set from the clicked face) + POWERED bit (same 0x8 meta layout as 1.12.2).
// Model/AABB authored facing north; rotated per facing (vanilla stairs pattern).
public class BlockXPShower extends Block {

	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyBool POWERED = PropertyBool.create("powered");

	public BlockXPShower() {
		super(Material.rock);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.xp_shower");
		setDefaultState(blockState.getBaseState()
				.withProperty(FACING, EnumFacing.NORTH)
				.withProperty(POWERED, false));
	}

	@Override
	protected BlockState createBlockState() {
		return new BlockState(this, new IProperty[] { FACING, POWERED });
	}

	private static final int MASK_POWERED = 0x8;

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState()
				.withProperty(FACING, EnumFacing.getHorizontal(meta & 7))
				.withProperty(POWERED, (meta & MASK_POWERED) != 0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex()
				| (state.getValue(POWERED)? MASK_POWERED : 0);
	}

	// NOTE: 1.8.9 has no getStateForPlacement (added in 1.9); the placement hook is
	// onBlockPlaced, with the same arguments.
	@Override
	public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		final EnumFacing tankSide = (facing.getAxis() == EnumFacing.Axis.Y)? EnumFacing.NORTH : facing.getOpposite();
		return getDefaultState().withProperty(FACING, tankSide).withProperty(POWERED, false);
	}

	private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new AxisAlignedBB(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
	}

	private static AxisAlignedBB unionForFacing(EnumFacing facing) {
		// Directional half-boxes matching the rendered arm per blockstate rotation
		// (authored-north arm x7-9/y7-9/z0-9; east y90, south y180, west y270).
		// Previous code returned full-length axis boxes (0-16) for both directions
		// on each axis, so the selection outline extended through the tank side.
		switch (facing) {
			case EAST:
				return box(7, 7, 7, 16, 9, 9);
			case SOUTH:
				return box(7, 7, 7, 9, 9, 16);
			case WEST:
				return box(0, 7, 7, 9, 9, 9);
			case NORTH:
			default:
				return box(7, 7, 0, 9, 9, 9);
		}
	}

	private static EnumFacing facingOf(IBlockAccess world, BlockPos pos) {
		final IBlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof BlockXPShower) return state.getValue(FACING);
		return EnumFacing.NORTH;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
		// 1.8.9 expects a WORLD-space box here (addCollisionBoxesToList adds it
		// directly with no offset). Returning the local box made the shower
		// non-solid (box stuck at origin) — entities passed through.
		return unionForFacing(state.getValue(FACING)).offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
		// Mouse picking (collisionRayTrace) uses min/max fields, NOT the boxes
		// above — without this the ray used a full cube and never matched the
		// thin rendered arm, so aiming at the shower selected the tank behind.
		final AxisAlignedBB b = unionForFacing(facingOf(world, pos));
		setBlockBounds((float)b.minX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.maxZ);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) {
		return unionForFacing(facingOf(world, pos)).offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean isFullCube() {
		return false;
	}

	@Override
	public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
		switch (side) {
			case NORTH:
			case SOUTH:
			case EAST:
			case WEST:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		super.onBlockAdded(world, pos, state);
		updateRedstone(world, pos, state);
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		updateRedstone(world, pos, state);
		super.onNeighborBlockChange(world, pos, state, neighborBlock);
	}

	private static void updateRedstone(World world, BlockPos blockPos, IBlockState state) {
		if (world.isRemote) return;
		boolean isPowered = world.isBlockIndirectlyGettingPowered(blockPos) > 0;
		if (state.getValue(POWERED) != isPowered)
			world.setBlockState(blockPos, state.withProperty(POWERED, isPowered), 3);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public openblocks.common.tileentity.TileEntityXPShower createTileEntity(World world, IBlockState state) {
		return new openblocks.common.tileentity.TileEntityXPShower();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		updateRedstone(world, pos, state);
	}
}
