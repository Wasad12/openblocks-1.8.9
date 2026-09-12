package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;
import openblocks.OpenBlocks;

// 1.8.9 port of 1.12.2 BlockXPDrain. OpenBlock.FourDirections replaced with plain
// Block: the plate is fully symmetric, so 1.12.2's orientation variants all render
// the same model — no facing state needed. Hardness 1.0F like OpenBlock's default.
public class BlockXPDrain extends Block {

	private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0);

	public BlockXPDrain() {
		super(Material.glass);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.xp_drain");
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
	public EnumWorldBlockLayer getBlockLayer() {
		return EnumWorldBlockLayer.CUTOUT;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
		return AABB;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) {
		return AABB.offset(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public openblocks.common.tileentity.TileEntityXPDrain createTileEntity(World world, IBlockState state) {
		return new openblocks.common.tileentity.TileEntityXPDrain();
	}
}
