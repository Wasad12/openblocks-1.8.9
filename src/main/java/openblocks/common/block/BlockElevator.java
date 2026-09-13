package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import openblocks.OpenBlocks;
import openblocks.api.IElevatorBlock;

// 1.8.9 port of 1.12.2 BlockElevator, reduced to the user-requested single white
// elevator: no COLOR property (always WHITE), no orientation (1.12.2 maps a single
// variant), no dye recolor, no tints, no custom item. Plain Block (no OpenBlock —
// hardness 1.0F like every OpenBlock), getRenderType MODEL (tank lesson).
public class BlockElevator extends Block implements IElevatorBlock {

	public BlockElevator() {
		// 1.8.9 Block takes the map color in the constructor (no per-state hook
		// pre-1.9); snow white matches 1.12.2's per-state white.
		super(Material.rock, MapColor.snowColor);
		setHardness(1.0F);
		setCreativeTab(OpenBlocks.tabOpenBlocks);
		setUnlocalizedName("openblocks.elevator");
	}

	@Override
	public int getRenderType() {
		return 3;
	}

	@Override
	public EnumDyeColor getColor(World world, BlockPos pos, IBlockState state) {
		return EnumDyeColor.WHITE;
	}

	@Override
	public PlayerRotation getRotation(World world, BlockPos pos, IBlockState state) {
		return PlayerRotation.NONE;
	}
}
