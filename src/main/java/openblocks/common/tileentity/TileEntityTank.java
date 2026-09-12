package openblocks.common.tileentity;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import openblocks.Config;
import openblocks.OpenBlocks;
import openblocks.client.renderer.tileentity.tank.ITankConnections;
import openblocks.client.renderer.tileentity.tank.ITankRenderFluidData;
import openblocks.client.renderer.tileentity.tank.TankRenderLogic;
import openblocks.common.LiquidXpUtils;

// 1.8.9 port of 1.12.2 TileEntityTank. Behavior preserved; lib/1.12-only APIs replaced:
// - SyncedTileEntity/SyncMap/SyncableTank/GenericTank -> plain FluidTank + NBT +
//   S35 description-packet sync (same SYNC/UPDATE throttling as the original).
// - IFluidHandler capability -> old facing-agnostic IFluidHandler (column logic verbatim).
// - Container interaction via FluidContainerRegistry (container->tank only, exactly like
//   1.12.2 tryEmptyItem, which never fills containers FROM the tank).
// - GenericTank.updateNeighbours result was unused by Tank logic -> dropped (documented).
public class TileEntityTank extends TileEntity implements ITickable, IFluidHandler {

	private final TankRenderLogic renderLogic;

	private boolean needsTankUpdate;

	private FluidTank tank = new FluidTank(getTankCapacity());

	private static final int SYNC_THRESHOLD = 8;
	private static final int UPDATE_THRESHOLD = 20;

	private boolean forceUpdate = true;

	private int ticksSinceLastSync = hashCode() % SYNC_THRESHOLD;

	private boolean needsSync;

	private int ticksSinceLastUpdate = hashCode() % UPDATE_THRESHOLD;

	private boolean needsUpdate;

	private FluidStack prevFluidStack;

	private int prevLuminosity;

	public TileEntityTank() {
		renderLogic = new TankRenderLogic(tank);
	}

	public double getFluidRatio() {
		return (double)tank.getFluidAmount() / (double)tank.getCapacity();
	}

	public static int getTankCapacity() {
		return FluidContainerRegistry.BUCKET_VOLUME * Config.bucketsPerTank;
	}

	public int getFluidLightLevel() {
		FluidStack stack = tank.getFluid();
		if (stack != null && stack.getFluid() != null) return stack.getFluid().getLuminosity(stack);
		return 0;
	}

	public ITankRenderFluidData getRenderFluidData() {
		return renderLogic.getTankRenderData();
	}

	public ITankConnections getTankConnections() {
		return renderLogic.getTankConnections();
	}

	public boolean accepts(FluidStack liquid) {
		if (liquid == null) return true;
		final FluidStack ownFluid = tank.getFluid();
		return ownFluid == null || ownFluid.isFluidEqual(liquid);
	}

	private boolean containsFluid(FluidStack liquid) {
		if (liquid == null) return false;
		final FluidStack ownFluid = tank.getFluid();
		return ownFluid != null && ownFluid.isFluidEqual(liquid);
	}

	public FluidTank getTank() {
		return tank;
	}

	public NBTTagCompound getItemNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		tank.writeToNBT(nbt);
		return nbt;
	}

	@Override
	public void validate() {
		super.validate();
		needsTankUpdate = true;
		if (getWorld() != null && getWorld().isRemote) renderLogic.initialize(getWorld(), getPos());
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (getWorld() != null && getWorld().isRemote) renderLogic.invalidateConnections();
	}

	protected TileEntityTank getNeighourTank(BlockPos pos) {
		if (!getWorld().isBlockLoaded(pos)) return null;

		Chunk chunk = getWorld().getChunkFromBlockCoords(pos);
		TileEntity te = chunk.getTileEntity(pos, EnumCreateEntityType.CHECK);
		return (te instanceof TileEntityTank)? (TileEntityTank)te : null;
	}

	public void onNeighbourChanged(BlockPos neighbourPos, Block neighbourBlock) {
		forceUpdate = true;
		needsTankUpdate = true;
	}

	public void onBlockPlacedBy(EntityLivingBase placer, ItemStack stack) {
		NBTTagCompound itemTag = stack.getTagCompound();
		if (itemTag != null && itemTag.hasKey(openblocks.common.item.ItemTankBlock.TANK_TAG)) {
			tank.readFromNBT(itemTag.getCompoundTag(openblocks.common.item.ItemTankBlock.TANK_TAG));
		}
	}

	private static TileEntityTank getValidTank(final TileEntity neighbor) {
		return (neighbor instanceof TileEntityTank && !neighbor.isInvalid())? (TileEntityTank)neighbor : null;
	}

	private TileEntityTank getTankInDirection(EnumFacing direction) {
		final TileEntity neighbor = getWorld().getTileEntity(getPos().offset(direction));
		return getValidTank(neighbor);
	}

	public TileEntityTank getTankInDirection(int dx, int dy, int dz) {
		final TileEntity neighbor = getWorld().getTileEntity(getPos().add(dx, dy, dz));
		return getValidTank(neighbor);
	}

	public boolean onBlockActivated(EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (getWorld().isRemote) return true;

		final ItemStack heldItem = player.getHeldItem();
		if (heldItem != null) {
			final ItemStack result = tryEmptyItem(player, heldItem.copy());
			if (result != null) {
				if (!player.capabilities.isCreativeMode) {
					if (heldItem.stackSize == 1) {
						player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
					} else {
						heldItem.stackSize--;
						if (!player.inventory.addItemStackToInventory(result))
							player.dropPlayerItemWithRandomChoice(result, false);
					}
				}
				return true;
			}
		} else {
			return tryDrainXp(player);
		}

		return false;
	}

	protected boolean tryDrainXp(EntityPlayer player) {
		final FluidStack fluid = tank.getFluid();
		if (fluid != null && fluid.isFluidEqual(new FluidStack(OpenBlocks.Fluids.xpJuice, 0))) {
			int requiredXp = (int)Math.ceil(player.xpBarCap() * (1 - player.experience));
			int requiredXPJuice = LiquidXpUtils.xpToLiquidRatio(requiredXp);

			FluidStack drained = columnDrain(requiredXPJuice, false);
			if (drained != null) {
				int xp = LiquidXpUtils.liquidToXpRatio(drained.amount);
				if (xp > 0) {
					int actualDrain = LiquidXpUtils.xpToLiquidRatio(xp);
					player.addExperience(xp);
					columnDrain(actualDrain, true);
					return true;
				}
			}
		}

		return false;
	}

	// not using bucket-only helpers, since any registered container should work.
	// returns the emptied container, or null if nothing was transferred.
	// 1.12.2 transfers a bucket at a time; here the whole container content must fit,
	// so partial fills can never void fluid.
	protected ItemStack tryEmptyItem(EntityPlayer player, ItemStack container) {
		final FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(container);
		if (fluid == null || fluid.amount <= 0) return null;

		final FluidStack test = fluid.copy();
		final int fitted = fillColumn(test, false);
		if (fitted != fluid.amount) return null;

		fillColumn(fluid.copy(), true);
		getWorld().playSoundAtEntity(player, "random.splash", 1f, 1f);

		for (FluidContainerRegistry.FluidContainerData data : FluidContainerRegistry.getRegisteredFluidContainerData()) {
			if (data.filledContainer != null && data.filledContainer.getItem() == container.getItem()
					&& data.filledContainer.getItemDamage() == container.getItemDamage()
					&& data.fluid.isFluidEqual(fluid))
				return data.emptyContainer.copy();
		}

		return null;
	}

	private FluidStack columnDrain(int maxDrain, boolean doDrain) {
		if (maxDrain <= 0) return null;

		FluidStack contents = tank.getFluid();
		if (contents == null || contents.amount <= 0) return null;

		FluidStack needed = contents.copy();
		needed.amount = maxDrain;

		drainFromColumn(needed, doDrain);

		needed.amount = maxDrain - needed.amount;
		return needed;
	}

	@Override
	public void update() {
		ticksSinceLastSync++;
		ticksSinceLastUpdate++;

		if (Config.shouldTanksUpdate && !getWorld().isRemote && forceUpdate) {
			if (needsTankUpdate) {
				needsTankUpdate = false;
			}

			forceUpdate = false;

			FluidStack contents = tank.getFluid();
			if (contents != null && contents.amount > 0 && getPos().getY() > 0) {
				tryFillBottomTank(contents);
				contents = tank.getFluid();
			}

			if (contents != null && contents.amount > 0) {
				tryBalanceNeighbors(contents);
			}

			needsSync = true;
			markUpdated();
		}

		if (needsSync && !getWorld().isRemote && ticksSinceLastSync > SYNC_THRESHOLD) {
			needsSync = false;
			ticksSinceLastSync = 0;
			getWorld().markBlockForUpdate(getPos());
		}

		if (needsUpdate && ticksSinceLastUpdate > UPDATE_THRESHOLD) {
			needsUpdate = false;
			ticksSinceLastUpdate = 0;
			getWorld().notifyNeighborsOfStateChange(getPos(), getBlockType());
		}

		if (getWorld().isRemote) renderLogic.validateConnections(getWorld(), getPos());
	}

	private void tryGetNeighbor(List<TileEntityTank> result, FluidStack fluid, EnumFacing side) {
		TileEntityTank neighbor = getTankInDirection(side);
		if (neighbor != null && neighbor.accepts(fluid)) result.add(neighbor);
	}

	private void tryBalanceNeighbors(FluidStack contents) {
		List<TileEntityTank> neighbors = Lists.newArrayList();
		tryGetNeighbor(neighbors, contents, EnumFacing.NORTH);
		tryGetNeighbor(neighbors, contents, EnumFacing.SOUTH);
		tryGetNeighbor(neighbors, contents, EnumFacing.EAST);
		tryGetNeighbor(neighbors, contents, EnumFacing.WEST);

		final int count = neighbors.size();
		if (count == 0) return;

		int sum = contents.amount;
		for (TileEntityTank n : neighbors)
			sum += n.tank.getFluidAmount();

		final int suggestedAmount = sum / (count + 1);
		if (Math.abs(suggestedAmount - contents.amount) < Config.tankFluidUpdateThreshold) return; // Don't balance small amounts to reduce server load

		FluidStack suggestedStack = contents.copy();
		suggestedStack.amount = suggestedAmount;

		for (TileEntityTank n : neighbors) {
			int amount = n.tank.getFluidAmount();
			int diff = amount - suggestedAmount;
			if (diff != 1 && diff != 0 && diff != -1) {
				n.tank.setFluid(suggestedStack.copy());
				n.tankChanged();
				sum -= suggestedAmount;
				n.forceUpdate = true;
			} else {
				sum -= amount;
			}
		}

		FluidStack s = tank.getFluid();
		if (sum != s.amount) {
			s.amount = sum;
			tankChanged();
		}
	}

	private void notifyNeigbours() {
		needsUpdate = true;
	}

	private void tankChanged() {
		notifyNeigbours();
		markUpdated();
	}

	private void markUpdated() {
		markDirty();
		if (getWorld() != null && !getWorld().isRemote) getWorld().markBlockForUpdate(getPos());
	}

	private void markContentsUpdated() {
		notifyNeigbours();
		forceUpdate = true;
	}

	private void tryFillBottomTank(FluidStack fluid) {
		TileEntity te = getWorld().getTileEntity(getPos().down());
		if (te instanceof TileEntityTank) {
			int amount = ((TileEntityTank)te).internalFill(fluid, true);
			if (amount > 0) internalDrain(amount, true);
		}
	}

	private FluidStack internalDrain(int amount, boolean doDrain) {
		FluidStack drained = tank.drain(amount, doDrain);
		if (drained != null && doDrain) markContentsUpdated();
		return drained;
	}

	private void drainFromColumn(FluidStack needed, boolean doDrain) {
		if (!containsFluid(needed) || needed.amount <= 0) return;

		if (getPos().getY() < 255) {
			TileEntity te = getWorld().getTileEntity(getPos().up());
			if (te instanceof TileEntityTank) ((TileEntityTank)te).drainFromColumn(needed, doDrain);
		}

		if (needed.amount <= 0) return;

		FluidStack drained = internalDrain(needed.amount, doDrain);
		if (drained == null) return;

		needed.amount -= drained.amount;
	}

	private int internalFill(FluidStack resource, boolean doFill) {
		int amount = tank.fill(resource, doFill);
		if (amount > 0 && doFill) markContentsUpdated();
		return amount;
	}

	private int fillColumn(FluidStack resource, boolean doFill) {
		if (!accepts(resource) || resource.amount <= 0) return 0;

		final int startAmount = resource.amount;
		int amount = internalFill(resource, doFill);

		resource.amount -= amount;

		if (resource.amount > 0 && getPos().getY() < 255) {
			TileEntity te = getWorld().getTileEntity(getPos().up());
			if (te instanceof TileEntityTank) ((TileEntityTank)te).fillColumn(resource, doFill);
		}

		return startAmount - resource.amount;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tank.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		tank.readFromNBT(tag);
	}

	@Override
	public S35PacketUpdateTileEntity getDescriptionPacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new S35PacketUpdateTileEntity(getPos(), 1, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		final FluidStack prev = tank.getFluid() != null? tank.getFluid().copy() : null;
		readFromNBT(pkt.getNbtCompound());
		final FluidStack current = tank.getFluid();

		final boolean sameFluid = current == null? prev == null : current.isFluidEqual(prev);
		if (!sameFluid) {
			int luminosity = current != null && current.getFluid() != null? current.getFluid().getLuminosity(current) : 0;
			if (luminosity != prevLuminosity) {
				getWorld().checkLight(getPos());
				prevLuminosity = luminosity;
			}
		}

		renderLogic.updateFluid(current);
	}

	@Override
	public boolean hasFastRenderer() {
		return true;
	}

	// old (1.8.9) IFluidHandler, facing-agnostic: whole column acts as one tank,
	// mirroring the 1.12.2 capability wrapper.
	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
		if (resource == null) return 0;
		FluidStack copy = resource.copy();
		fillColumn(copy, doFill);
		return resource.amount - copy.amount;
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
		if (resource == null) return null;

		FluidStack needed = resource.copy();
		drainFromColumn(needed, doDrain);

		needed.amount = resource.amount - needed.amount;
		return needed;
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
		return columnDrain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(EnumFacing from, net.minecraftforge.fluids.Fluid fluid) {
		return fluid == null || accepts(new FluidStack(fluid, 0));
	}

	@Override
	public boolean canDrain(EnumFacing from, net.minecraftforge.fluids.Fluid fluid) {
		return true;
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing from) {
		return new FluidTankInfo[] { tank.getInfo() };
	}
}
