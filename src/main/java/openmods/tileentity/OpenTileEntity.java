package openmods.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;

// 1.8.9 port of OpenModsLib OpenTileEntity: same TE plumbing + RPC proxy seams.
// Dropped per §19: orientation/front/back helpers (need OpenBlock), sound helpers
// (no 1.8.9 SoundEvent hooks in this path), createServerRpcProxy (unused by ported
// features), inventory-callback helpers (unused). Imports adjusted (1.8.9 packages).
public abstract class OpenTileEntity extends TileEntity implements IRpcTargetProvider {

	/** Place for TE specific setup. Called once upon creation */
	public void setup() {}

	public boolean isAddedToWorld() {
		return worldObj != null;
	}

	protected TileEntity getTileEntity(BlockPos blockPos) {
		return (worldObj != null && worldObj.isBlockLoaded(blockPos))? worldObj.getTileEntity(blockPos) : null;
	}

	public TileEntity getTileInDirection(net.minecraft.util.EnumFacing direction) {
		return getTileEntity(pos.offset(direction));
	}

	public boolean isAirBlock(net.minecraft.util.EnumFacing direction) {
		return worldObj != null && worldObj.isAirBlock(getPos().offset(direction));
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	public void openGui(Object instance, EntityPlayer player) {
		player.openGui(instance, -1, worldObj, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public IRpcTarget createRpcTarget() {
		return new TileEntityRpcTarget(this);
	}

	public <T> T createProxy(final IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(this, mainIntf, extraIntf);
		return RpcCallDispatcher.instance().createProxy(createRpcTarget(), sender, mainIntf, extraIntf);
	}

	public <T> T createClientRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final IPacketSender sender = RpcCallDispatcher.instance().senders.client;
		return createProxy(sender, mainIntf, extraIntf);
	}

	public void markUpdated() {
		worldObj.markChunkDirty(pos, this);
	}

	public boolean isValid(EntityPlayer player) {
		return (worldObj.getTileEntity(pos) == this) && (player.getDistanceSqToCenter(pos) <= 64.0D);
	}
}
