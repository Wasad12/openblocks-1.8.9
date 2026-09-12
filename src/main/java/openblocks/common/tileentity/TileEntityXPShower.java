package openblocks.common.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Vec3;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidHandler;
import openblocks.OpenBlocks;
import openblocks.common.LiquidXpUtils;
import openblocks.common.block.BlockXPShower;
import openblocks.common.entity.EntityXPOrbNoFly;

// 1.8.9 port of 1.12.2 TileEntityXPShower. SyncedTileEntity/SyncableBoolean/
// GenericTank replaced: plain boolean spray flag synced via S35 description packet
// (sent on change), plain FluidTank buffer with an xpJuice-only intake guard.
public class TileEntityXPShower extends TileEntity implements ITickable {

	private static final int DRAIN_PER_CYCLE = 100;

	private static final int ORB_SPAWN_FREQUENCY = 3;

	private FluidTank bufferTank = new FluidTank(FluidContainerRegistry.BUCKET_VOLUME);

	private boolean particleSpawnerActive;

	private int particleSpawnTimer = 0;

	@Override
	public void update() {
		if (!getWorld().isRemote) {
			trySpawnXpOrbs();
		} else {
			trySpawnParticles();
		}
	}

	private EnumFacing getBack() {
		final IBlockState state = getWorld().getBlockState(getPos());
		if (state.getBlock() instanceof BlockXPShower) return state.getValue(BlockXPShower.FACING);
		return EnumFacing.NORTH;
	}

	private int fillFromSide(int maxDrain, EnumFacing side) {
		final FluidStack current = bufferTank.getFluid();
		if (current != null && !current.isFluidEqual(new FluidStack(OpenBlocks.Fluids.xpJuice, 0))) return 0;

		final TileEntity neighbor = getWorld().getTileEntity(getPos().offset(side));
		if (!(neighbor instanceof IFluidHandler) || neighbor.isInvalid()) return 0;

		final IFluidHandler handler = (IFluidHandler)neighbor;
		final FluidStack test = handler.drain(side.getOpposite(), maxDrain, false);
		if (test == null || test.amount <= 0) return 0;
		if (!test.isFluidEqual(new FluidStack(OpenBlocks.Fluids.xpJuice, 0))) return 0;

		final FluidStack drained = handler.drain(side.getOpposite(), maxDrain, true);
		if (drained == null || drained.amount <= 0) return 0;

		return bufferTank.fill(drained, true);
	}

	private void trySpawnXpOrbs() {
		boolean hasSpawnedParticle = false;
		if (getWorld().getTotalWorldTime() % ORB_SPAWN_FREQUENCY == 0 && isPowered()) {
			fillFromSide(DRAIN_PER_CYCLE, getBack());

			final int amountInTank = bufferTank.getFluidAmount();

			if (amountInTank > 0) {
				final int xpInTank = LiquidXpUtils.liquidToXpRatio(amountInTank);
				final int xpInOrb = EntityXPOrb.getXPSplit(xpInTank);
				final int toDrain = LiquidXpUtils.xpToLiquidRatio(xpInOrb);

				if (toDrain > 0) {
					bufferTank.drain(toDrain, true);
					hasSpawnedParticle = true;

					final BlockPos p = getPos();
					getWorld().spawnEntityInWorld(new EntityXPOrbNoFly(getWorld(), p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5, xpInOrb));
				}
			}
		}

		if (particleSpawnerActive != hasSpawnedParticle) {
			particleSpawnerActive = hasSpawnedParticle;
			markDirty();
			getWorld().markBlockForUpdate(getPos());
		}
	}

	private boolean isPowered() {
		final IBlockState state = getWorld().getBlockState(getPos());
		return state.getBlock() instanceof BlockXPShower && state.getValue(BlockXPShower.POWERED);
	}

	private void trySpawnParticles() {
		final int particleLevel = OpenBlocks.proxy.getParticleSettings();
		if (particleLevel == 0 || (particleLevel == 1 && getWorld().rand.nextInt(3) == 0)) {
			particleSpawnTimer = particleSpawnerActive? 10 : particleSpawnTimer - 1;

			if (particleSpawnTimer > 0) {
				final BlockPos p = getPos();
				Vec3 vec = new Vec3(
						(getWorld().rand.nextDouble() - 0.5) * 0.05,
						0,
						(getWorld().rand.nextDouble() - 0.5) * 0.05);
				OpenBlocks.proxy.spawnLiquidSpray(getWorld(),
						new FluidStack(OpenBlocks.Fluids.xpJuice, 1),
						p.getX() + 0.5d, p.getY() + 0.4d, p.getZ() + 0.5d, 0.4f, 0.7f, vec);
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		bufferTank.writeToNBT(tag);
		tag.setBoolean("spray", particleSpawnerActive);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		bufferTank.readFromNBT(tag);
		particleSpawnerActive = tag.getBoolean("spray");
	}

	@Override
	public S35PacketUpdateTileEntity getDescriptionPacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new S35PacketUpdateTileEntity(getPos(), 1, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
}
