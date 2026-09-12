package openblocks.common.tileentity;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import openblocks.OpenBlocks;
import openblocks.client.gui.GuiVacuumHopper;
import openblocks.common.LiquidXpUtils;
import openblocks.common.container.ContainerVacuumHopper;
import openmods.api.IHasGui;
import openmods.api.INeighbourAwareTile;
import openmods.api.IValueProvider;
import openmods.inventory.GenericInventory;
import openmods.inventory.IInventoryProvider;
import openmods.inventory.ItemMover;
import openmods.inventory.TileEntityInventory;
import openmods.sync.ISyncListener;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;
import openmods.sync.SyncableBoolean;
import openmods.sync.SyncableSides;
import openmods.sync.SyncableTank;
import openmods.tileentity.SyncedTileEntity;
import openmods.utils.EnchantmentUtils;
import openmods.utils.InventoryUtils;
import openmods.utils.SidedInventoryAdapter;
import openmods.utils.SidedItemHandlerAdapter;
import openmods.utils.bitmap.BitMapUtils;
import openmods.utils.bitmap.IReadableBitMap;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IWriteableBitMap;

// 1.8.9 port of 1.12.2 TileEntityVacuumHopper: same 10-slot inventory, 5-level
// xpJuice tank, item/xp output side-maps, vacuumDisabled flag, suction physics,
// neighbour output and sneak-toggle. 1.8.9 adaptations: nullable stacks,
// EntityItem.getEntityItem() (getItem() does not exist), AABB.expand (no grow),
// 7-arg spawnParticle around the block center (no 4-arg overload),
// getTotalWorldTime() in place of OpenMods.proxy.getTicks (the lib call returns
// exactly that — read from source), old IFluidHandler drain-only on xp sides +
// item-handler cap (no fluid caps), canInsertStack via insertItem+simulate (no
// insertItemStacked in 1.8.9). Dropped: EntityItemProjectile selector branch
// (cannon entity, NOT STARTED — no such entities can exist; restore with Cannon),
// EnumHand/IActivateAwareTile (single-hand toggleVacuum called by the block),
// fixers/IncludeInterface (precedent).
public class TileEntityVacuumHopper extends SyncedTileEntity implements IInventoryProvider, IHasGui, INeighbourAwareTile, ITickable, IFluidHandler {

	public static final int TANK_CAPACITY = LiquidXpUtils.xpToLiquidRatio(EnchantmentUtils.getExperienceForLevel(5));

	public static final String OUTPUT_ITEMS = "items";
	public static final String OUTPUT_FLUIDS = "fluids";
	public static final String OUTPUT_BOTH = "both";

	private SyncableTank tank;
	public SyncableSides xpOutputs;
	public SyncableSides itemOutputs;
	public SyncableBoolean vacuumDisabled;

	private boolean needsTankUpdate;

	private final GenericInventory inventory = new TileEntityInventory(this, "vacuumhopper", true, 10);

	private final SidedInventoryAdapter sided = new SidedInventoryAdapter(inventory);

	private final SidedItemHandlerAdapter itemHandlerCapability = new SidedItemHandlerAdapter(inventory.getHandler());

	private Map<String, String> outputState = ImmutableMap.of();

	@Override
	protected void createSyncedFields() {
		tank = new SyncableTank(TANK_CAPACITY, OpenBlocks.Fluids.xpJuice);
		xpOutputs = new SyncableSides();
		itemOutputs = new SyncableSides();
		vacuumDisabled = new SyncableBoolean();
	}

	public TileEntityVacuumHopper() {
		sided.registerAllSlots(itemOutputs, false, true);

		itemHandlerCapability.registerAllSlots(itemOutputs, false, true);
	}

	@Override
	protected void onSyncMapCreate(SyncMap syncMap) {
		syncMap.addSyncListener(itemHandlerCapability.createSyncListener());

		syncMap.addUpdateListener(new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				if (changes.contains(xpOutputs) || changes.contains(itemOutputs)) {
					updateOutputStates();
					worldObj.markBlockRangeForRenderUpdate(pos, pos);
				}
			}

			private void updateOutputStates() {
				ImmutableMap.Builder<String, String> newOutputState = ImmutableMap.builder();
				for (EnumFacing side : EnumFacing.VALUES) {
					final boolean outputItems = itemOutputs.get(side);
					final boolean outputXp = xpOutputs.get(side);

					if (outputItems) {
						if (outputXp) {
							newOutputState.put(side.getName(), OUTPUT_BOTH);
						} else {
							newOutputState.put(side.getName(), OUTPUT_ITEMS);
						}
					} else if (outputXp) {
						newOutputState.put(side.getName(), OUTPUT_FLUIDS);
					}
				}

				outputState = newOutputState.build();
			}
		});
	}

	public IReadableBitMap<EnumFacing> getReadableXpOutputs() {
		return xpOutputs;
	}

	public IWriteableBitMap<EnumFacing> getWriteableXpOutputs() {
		return BitMapUtils.createRpcAdapter(createRpcProxy(xpOutputs, IRpcDirectionBitMap.class));
	}

	public IReadableBitMap<EnumFacing> getReadableItemOutputs() {
		return itemOutputs;
	}

	public IWriteableBitMap<EnumFacing> getWriteableItemOutputs() {
		return BitMapUtils.createRpcAdapter(createRpcProxy(itemOutputs, IRpcDirectionBitMap.class));
	}

	public IValueProvider<FluidStack> getFluidProvider() {
		return tank;
	}

	private final Predicate<Entity> entitySelector = new Predicate<Entity>() {
		@Override
		public boolean apply(Entity entity) {
			if (entity.isDead) return false;

			if (entity instanceof EntityItem) {
				ItemStack stack = ((EntityItem)entity).getEntityItem();
				return InventoryUtils.canInsertStack(inventory.getHandler(), stack);
			}

			if (entity instanceof EntityXPOrb) return tank.getSpace() > 0;

			return false;
		}
	};

	@Override
	public void update() {

		if (vacuumDisabled.get()) return;

		if (worldObj.isRemote) {
			spawnParticle(EnumParticleTypes.PORTAL, worldObj.rand.nextDouble() - 0.5, worldObj.rand.nextDouble() - 1.0, worldObj.rand.nextDouble() - 0.5);
		}

		final AxisAlignedBB searchBox = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).expand(3, 3, 3);
		List<Entity> interestingItems = worldObj.getEntitiesWithinAABB(Entity.class, searchBox, entitySelector);

		boolean needsSync = false;

		for (Entity entity : interestingItems) {
			double dx = (pos.getX() + 0.5D - entity.posX);
			double dy = (pos.getY() + 0.5D - entity.posY);
			double dz = (pos.getZ() + 0.5D - entity.posZ);

			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (distance < 1.1) {
				needsSync |= onEntityCollidedWithBlock(entity);
			} else {
				double var11 = 1.0 - distance / 15.0;

				if (var11 > 0.0D) {
					var11 *= var11;
					entity.motionX += dx / distance * var11 * 0.05;
					entity.motionY += dy / distance * var11 * 0.2;
					entity.motionZ += dz / distance * var11 * 0.05;
				}
			}

		}

		if (!worldObj.isRemote) {
			needsSync |= outputToNeighbors();
			if (needsSync) sync();
		}
	}

	private void spawnParticle(EnumParticleTypes type, double dx, double dy, double dz) {
		worldObj.spawnParticle(type,
				pos.getX() + 0.5 + dx,
				pos.getY() + 0.5 + dy,
				pos.getZ() + 0.5 + dz,
				0, 0, 0);
	}

	private boolean outputToNeighbors() {
		if (worldObj.getTotalWorldTime() % 10 == 0) {
			if (needsTankUpdate) {
				tank.updateNeighbours(worldObj, pos);
				needsTankUpdate = false;
			}

			tank.distributeToSides(50, worldObj, pos, xpOutputs.getValue());
			autoInventoryOutput();
			return true;
		}

		return false;
	}

	private void autoInventoryOutput() {
		final boolean outputSides = itemOutputs.getValue().isEmpty();
		if (outputSides) return;
		final ItemMover mover = new ItemMover(worldObj, pos).breakAfterFirstTry().randomizeSides().setSides(itemOutputs.getValue());
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			if (inventory.getStackInSlot(i) != null) {
				if (mover.pushFromSlot(inventory.getHandler(), i) > 0) break;
			}
		}
	}

	@Override
	public Object getServerGui(EntityPlayer player) {
		return new ContainerVacuumHopper(player.inventory, this);
	}

	@Override
	public Object getClientGui(EntityPlayer player) {
		return new GuiVacuumHopper(new ContainerVacuumHopper(player.inventory, this));
	}

	@Override
	public boolean canOpenGui(EntityPlayer player) {
		return true;
	}

	// 1.8.9 replacement for 1.12.2 onBlockActivated (IActivateAwareTile + EnumHand):
	// sneak + empty hand toggles the vacuum; called by the block (which opens the
	// GUI otherwise — same outcome as the 1.12.2 activate-then-fallthrough).
	public void toggleVacuum() {
		vacuumDisabled.toggle();
		sync();
	}

	public boolean onEntityCollidedWithBlock(Entity entity) {
		if (!worldObj.isRemote) {
			if (entity instanceof EntityItem && !entity.isDead) {
				final EntityItem item = (EntityItem)entity;
				final ItemStack toConsume = item.getEntityItem().copy();
				final ItemStack leftover = ItemHandlerHelper.insertItem(inventory.getHandler(), toConsume, false);
				setEntityItemStack(item, leftover);
				return true;
			} else if (entity instanceof EntityXPOrb) {
				if (tank.getSpace() > 0) {
					EntityXPOrb orb = (EntityXPOrb)entity;
					int xpAmount = LiquidXpUtils.xpToLiquidRatio(orb.getXpValue());
					FluidStack newFluid = new FluidStack(OpenBlocks.Fluids.xpJuice, xpAmount);
					tank.fill(newFluid, true);
					entity.setDead();
					return true;
				}
			}
		}

		return false;
	}

	// 1.8.9 equivalent of OpenModsLib ItemUtils.setEntityItemStack (kept local per
	// §19 — full ItemUtils pulls hashing deps; EntityItem.setEntityItemStack exists
	// in 1.8.9, null means fully consumed).
	private static void setEntityItemStack(EntityItem entity, ItemStack stack) {
		if (stack == null) {
			entity.setDead();
		} else {
			entity.setEntityItemStack(stack);
		}
	}

	@Override
	public IInventory getInventory() {
		return inventory;
	}

	/** Drop inventory contents into the world (called by the block on break). */
	public void dropContents(World world, BlockPos dropPos) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			final ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) {
				final net.minecraft.entity.item.EntityItem entity = new net.minecraft.entity.item.EntityItem(world, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack);
				world.spawnEntityInWorld(entity);
				inventory.setInventorySlotContents(i, null);
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		inventory.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		inventory.readFromNBT(tag);
	}

	@Override
	public void validate() {
		super.validate();
		this.needsTankUpdate = true;
	}

	@Override
	public void onNeighbourChanged(BlockPos neighbourPos, Block neighbourBlock) {
		this.needsTankUpdate = true;
	}

	public Map<String, String> getOutputState() {
		return outputState;
	}

	// Old (facing-aware) fluid handler: the 1.12.2 SidedFluidCapabilityWrapper gate
	// was drain-on-flagged-xp-sides (hopper OUTPUTS fluid), no fill.
	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
		return 0;
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
		if (from != null && !xpOutputs.get(from)) return null;
		return tank.drain(resource, doDrain);
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
		if (from != null && !xpOutputs.get(from)) return null;
		return tank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid) {
		return false;
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid) {
		return from == null || xpOutputs.get(from);
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing from) {
		return new FluidTankInfo[] { tank.getInfo() };
	}

	@Override
	public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return itemHandlerCapability.hasHandler(facing);

		return super.hasCapability(capability, facing);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return (T)itemHandlerCapability.getHandler(facing);

		return super.getCapability(capability, facing);
	}

}
