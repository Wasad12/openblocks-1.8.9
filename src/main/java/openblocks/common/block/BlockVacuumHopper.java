package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import openblocks.OpenBlocks;
import openblocks.common.tileentity.TileEntityVacuumHopper;
import openmods.gui.CommonGuiHandler;

// 1.8.9 port of 1.12.2 BlockVacuumHopper: same bounds (0.25-0.75 render box,
// ~full collision, 0.3-0.7 selection), ExtendedBlockState nozzle indicators,
// entity-collision intake, sneak-toggle + GUI. 1.8.9 adaptations: plain Block (no
// OpenBlock — hardness 1.0F like every OpenBlock), getRenderType MODEL, shower
// pattern for bounds (offset world-space collision box; min/max fields drive the
// raytrace, so setBlockBoundsBasedOnState carries the SELECTION box),
// IActivateAwareTile inlined (sneak + empty hand toggles, else GUI — same outcome
// as 1.12.2's activate-then-GUI fallthrough).
public class BlockVacuumHopper extends Block {

	private static final AxisAlignedBB SELECTION_AABB = new AxisAlignedBB(0.3, 0.3, 0.3, 0.7, 0.7, 0.7);
	private static final AxisAlignedBB COLLISION_AABB = new AxisAlignedBB(0.01, 0.01, 0.01, 0.99, 0.99, 0.99);

	public BlockVacuumHopper() {
		super(Material.rock);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.vacuum_hopper");
	}

	@Override
	protected BlockState createBlockState() {
		return new ExtendedBlockState(this,
				new IProperty[0],
				new net.minecraftforge.common.property.IUnlistedProperty[] { HopperOutputState.PROPERTY });
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
		// Mouse picking (collisionRayTrace) uses min/max fields — the 1.12.2
		// selection box (0.3-0.7). Collision below is world-space and separate.
		setBlockBounds(0.3F, 0.3F, 0.3F, 0.7F, 0.7F, 0.7F);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
		// 1.8.9 expects a WORLD-space box here (addCollisionBoxesToList adds it
		// directly with no offset — shower lesson).
		return COLLISION_AABB.offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) {
		return SELECTION_AABB.offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState oldState = (IExtendedBlockState)state;
			return oldState.withProperty(HopperOutputState.PROPERTY, HopperOutputState.computeState(world, pos));
		}

		return state;
	}

	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
		final TileEntityVacuumHopper te = getTile(world, pos);
		if (te != null) te.onEntityCollidedWithBlock(entity);
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
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && player.isSneaking()) {
			final ItemStack held = player.getHeldItem();
			if (held == null) {
				final TileEntityVacuumHopper te = getTile(world, pos);
				if (te != null) {
					te.toggleVacuum();
					return true;
				}
			}
		}
		if (world.isRemote) return true;
		player.openGui(OpenBlocks.instance, CommonGuiHandler.OPEN_MODS_TE_GUI, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		super.onNeighborBlockChange(world, pos, state, neighborBlock);
		if (!world.isRemote) {
			final TileEntityVacuumHopper te = getTile(world, pos);
			if (te != null) te.onNeighbourChanged(pos, neighborBlock);
		}
	}

	private static TileEntityVacuumHopper getTile(World world, BlockPos pos) {
		if (!world.isBlockLoaded(pos)) return null;
		final net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEntityVacuumHopper? (TileEntityVacuumHopper)te : null;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntityVacuumHopper createTileEntity(World world, IBlockState state) {
		return new TileEntityVacuumHopper();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		// drop inventory contents like vanilla containers (1.12.2 did this via the
		// OpenBlock break path; same outcome, vanilla 1.8.9 mechanism)
		final TileEntityVacuumHopper te = getTile(world, pos);
		if (te != null) te.dropContents(world, pos);
		super.breakBlock(world, pos, state);
	}
}
