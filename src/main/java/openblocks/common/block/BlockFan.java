package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import openblocks.OpenBlocks;
import openblocks.common.tileentity.TileEntityFan;

// 1.8.9 port of 1.12.2 BlockFan: same 0.2-0.8 column, non-opaque, top-of-solid
// placement, TE-driven head angle. 1.8.9 adaptations: plain Block (no OpenBlock —
// hardness 1.0F like every OpenBlock), getRenderType MODEL (tank lesson), static
// bounds (no per-state geometry — the head yaw lives in the TESR, not the state),
// ExtendedBlockState carrying only FanRenderState (the orientation property and
// EvalModelState are eval-system plumbing, which has no 1.8.9 counterpart — see
// ARCHITECTURE.md), IPlaceAwareTile/IAddAwareTile/IActivateAwareTile inlined
// (hopper pattern).
public class BlockFan extends Block {

	public BlockFan() {
		super(Material.circuits);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.fan");
		setBlockBounds(0.2F, 0.0F, 0.2F, 0.8F, 1.0F, 0.8F);
	}

	@Override
	protected BlockState createBlockState() {
		return new ExtendedBlockState(this,
				new IProperty[0],
				new IUnlistedProperty[] { FanRenderState.PROPERTY });
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState oldState = (IExtendedBlockState)state;
			return oldState.withProperty(FanRenderState.PROPERTY, FanRenderState.computeState(world, pos));
		}

		return state;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
		// 1.8.9 expects a WORLD-space box here (addCollisionBoxesToList adds it
		// directly with no offset — shower lesson).
		return new AxisAlignedBB(0.2, 0.0, 0.2, 0.8, 1.0, 0.8).offset(pos.getX(), pos.getY(), pos.getZ());
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
	public boolean isFullCube() {
		return false;
	}

	@Override
	public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
		// 1.12.2 OpenBlock.isOnTopOfSolidBlock: side == UP, block below solid
		// (top face). isSideSolid 3-arg form, same call the lib makes.
		return side == EnumFacing.UP && world.isSideSolid(pos.down(), EnumFacing.UP, false);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		final TileEntityFan te = getTile(world, pos);
		if (te != null) te.onBlockPlacedBy(state, placer, stack);
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		super.onBlockAdded(world, pos, state);
		final TileEntityFan te = getTile(world, pos);
		if (te != null) te.onAdded();
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		super.onNeighborBlockChange(world, pos, state, neighborBlock);
		if (!world.isRemote) {
			final TileEntityFan te = getTile(world, pos);
			if (te != null) te.onNeighbourChanged(pos, neighborBlock);
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		final TileEntityFan te = getTile(world, pos);
		if (te != null) return te.onBlockActivated(player);
		return false;
	}

	private static TileEntityFan getTile(World world, BlockPos pos) {
		if (!world.isBlockLoaded(pos)) return null;
		final net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEntityFan? (TileEntityFan)te : null;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntityFan createTileEntity(World world, IBlockState state) {
		return new TileEntityFan();
	}
}
