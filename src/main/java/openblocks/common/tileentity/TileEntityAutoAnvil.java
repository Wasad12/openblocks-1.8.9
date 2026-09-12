package openblocks.common.tileentity;

import com.google.common.base.Optional;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import openblocks.OpenBlocks;
import openblocks.client.gui.GuiAutoAnvil;
import openblocks.common.LiquidXpUtils;
import openblocks.common.container.ContainerAutoAnvil;
import openblocks.common.tileentity.TileEntityAutoAnvil.AutoSlots;
import openmods.api.IHasGui;
import openmods.api.INeighbourAwareTile;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.gui.misc.IConfigurableGuiSlots;
import openmods.inventory.GenericInventory;
import openmods.inventory.IInventoryProvider;
import openmods.inventory.ItemMover;
import openmods.inventory.TileEntityInventory;
import openmods.sync.SyncMap;
import openmods.sync.SyncableFlags;
import openmods.sync.SyncableSides;
import openmods.sync.SyncableTank;
import openmods.tileentity.SyncedTileEntity;
import openmods.utils.EnchantmentUtils;
import openmods.utils.MiscUtils;
import openmods.utils.SidedInventoryAdapter;
import openmods.utils.SidedItemHandlerAdapter;
import openmods.utils.VanillaAnvilLogic;
import openmods.utils.bitmap.BitMapUtils;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;
import openmods.utils.bitmap.IWriteableBitMap;

// 1.8.9 port of 1.12.2 TileEntityAutoAnvil: same slots/tank/sides sync fields,
// movers, 40-tick repair cycle and enchant gating. 1.8.9 adaptations (same set as
// TileEntityAutoEnchantmentTable): nullable stacks, old IFluidHandler instead of
// fluid capabilities (fill-only on xp sides), item-handler caps kept, fixers
// dropped. Sound: no SoundEvents/playSoundAtBlock helper on 1.8.9 — inline
// playSoundEffect with the vanilla 1.8.9 "random.anvil_use" string.
public class TileEntityAutoAnvil extends SyncedTileEntity implements IInventoryProvider, IHasGui, IConfigurableGuiSlots<AutoSlots>, INeighbourAwareTile, ITickable, IFluidHandler {

	protected static final int TOTAL_COOLDOWN = 40;
	public static final int MAX_STORED_LEVELS = 45;
	public static final int TANK_CAPACITY = LiquidXpUtils.getLiquidForLevel(MAX_STORED_LEVELS);

	private int cooldown = 0;

	private boolean needsTankUpdate;

	/**
	 * The 3 slots in the inventory
	 */
	public enum Slots {
		tool,
		modifier,
		output
	}

	/**
	 * The keys of the things that can be auto injected/extracted
	 */
	public enum AutoSlots {
		tool,
		modifier,
		output,
		xp
	}

	/**
	 * The shared/syncable objects
	 */
	private SyncableSides toolSides;
	private SyncableSides modifierSides;
	private SyncableSides outputSides;
	private SyncableSides xpSides;
	private SyncableTank tank;
	private SyncableFlags automaticSlots;

	private final GenericInventory inventory = new TileEntityInventory(this, "autoanvil", true, 3) {
		@Override
		public boolean isItemValidForSlot(int i, ItemStack itemstack) {
			return i != 2 && super.isItemValidForSlot(i, itemstack);
		}
	};

	private final SidedInventoryAdapter slotSides = new SidedInventoryAdapter(inventory);

	private final SidedItemHandlerAdapter itemHandlerCapability = new SidedItemHandlerAdapter(inventory.getHandler());

	public TileEntityAutoAnvil() {
		slotSides.registerSlot(Slots.tool, toolSides, true, false);
		slotSides.registerSlot(Slots.modifier, modifierSides, true, false);
		slotSides.registerSlot(Slots.output, outputSides, false, true);

		itemHandlerCapability.registerSlot(Slots.tool, toolSides, true, false);
		itemHandlerCapability.registerSlot(Slots.modifier, modifierSides, true, false);
		itemHandlerCapability.registerSlot(Slots.output, outputSides, false, true);
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

	// Old (facing-aware) fluid handler: same side gating as the 1.12.2
	// SidedFluidCapabilityWrapper (fill on flagged xp sides, no drain).
	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
		if (from != null && !xpSides.get(from)) return 0;
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid) {
		return from == null || xpSides.get(from);
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid) {
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing from) {
		return new FluidTankInfo[] { tank.getInfo() };
	}

	@Override
	protected void createSyncedFields() {
		toolSides = new SyncableSides();
		modifierSides = new SyncableSides();
		outputSides = new SyncableSides();
		xpSides = new SyncableSides();
		tank = new SyncableTank(TANK_CAPACITY, OpenBlocks.Fluids.xpJuice);
		automaticSlots = SyncableFlags.create(AutoSlots.values().length);
	}

	@Override
	protected void onSyncMapCreate(SyncMap syncMap) {
		syncMap.addSyncListener(itemHandlerCapability.createSyncListener());
	}

	@Override
	public void update() {
		if (!worldObj.isRemote) {
			// if we should auto-drink liquid, do it!
			if (automaticSlots.get(AutoSlots.xp)) {
				if (needsTankUpdate) {
					tank.updateNeighbours(worldObj, pos);
					needsTankUpdate = false;
				}

				tank.fillFromSides(100, worldObj, pos, xpSides.getValue());
			}

			final ItemMover mover = new ItemMover(worldObj, pos).breakAfterFirstTry().randomizeSides().setMaxSize(1);

			if (shouldAutoOutput() && hasOutput()) {
				mover.setSides(outputSides.getValue()).pushFromSlot(inventory.getHandler(), Slots.output.ordinal());
			}

			if (shouldAutoInputTool() && !hasTool()) {
				mover.setSides(toolSides.getValue()).pullToSlot(inventory.getHandler(), Slots.tool.ordinal());
			}

			if (shouldAutoInputModifier()) {
				mover.setSides(modifierSides.getValue()).pullToSlot(inventory.getHandler(), Slots.modifier.ordinal());

			}

			if (cooldown-- < 0 && !hasOutput()) {
				repairItem();
				cooldown = TOTAL_COOLDOWN;
			}

			if (tank.isDirty()) sync();
		}
	}

	private void repairItem() {
		final VanillaAnvilLogic helper = new VanillaAnvilLogic(getStack(Slots.tool), getStack(Slots.modifier), false, Optional.<String> absent());

		final ItemStack output = helper.getOutputStack();
		if (output != null) {
			int levelCost = helper.getLevelCost();
			int xpCost = EnchantmentUtils.getExperienceForLevel(levelCost);
			int liquidXpCost = LiquidXpUtils.xpToLiquidRatio(xpCost);

			FluidStack drained = tank.drain(liquidXpCost, false);

			if (drained != null && drained.amount == liquidXpCost) {
				tank.drain(liquidXpCost, true);
				removeModifiers(helper.getModifierCost());
				setStack(Slots.tool, null);
				setStack(Slots.output, output);
				worldObj.playSoundEffect(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "random.anvil_use", 0.3f, 1f);
			}
		}
	}

	private void removeModifiers(int modifierCost) {
		if (modifierCost > 0) {
			ItemStack modifierStack = getStack(Slots.modifier);
			if (modifierStack != null) {
				modifierStack.stackSize -= modifierCost;
				if (modifierStack.stackSize <= 0) setStack(Slots.modifier, null);
				else markDirty();
			}
		} else {
			setStack(Slots.modifier, null);
		}
	}

	@Override
	public boolean canOpenGui(EntityPlayer player) {
		return true;
	}

	@Override
	public Object getServerGui(EntityPlayer player) {
		return new ContainerAutoAnvil(player.inventory, this);
	}

	@Override
	public Object getClientGui(EntityPlayer player) {
		return new GuiAutoAnvil(new ContainerAutoAnvil(player.inventory, this));
	}

	public IValueProvider<FluidStack> getFluidProvider() {
		return tank;
	}

	private boolean shouldAutoInputModifier() {
		return automaticSlots.get(AutoSlots.modifier);
	}

	public boolean shouldAutoOutput() {
		return automaticSlots.get(AutoSlots.output);
	}

	private boolean hasTool() {
		return getStack(Slots.tool) != null;
	}

	private boolean shouldAutoInputTool() {
		return automaticSlots.get(AutoSlots.tool);
	}

	private boolean hasOutput() {
		return getStack(Slots.output) != null;
	}

	private ItemStack getStack(Slots slot) {
		return inventory.getStackInSlot(slot.ordinal());
	}

	private void setStack(Slots slot, ItemStack stack) {
		inventory.setInventorySlotContents(slot.ordinal(), stack);
	}

	@Override
	public IInventory getInventory() {
		return slotSides;
	}

	/** Drop inventory contents into the world (called by the block on break). */
	public void dropContents(World world, BlockPos dropPos) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			final ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) {
				final EntityItem entity = new EntityItem(world, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack);
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
		inventory.readFromNBT(tag, false);
	}

	private SyncableSides selectSlotMap(AutoSlots slot) {
		switch (slot) {
			case modifier:
				return modifierSides;
			case output:
				return outputSides;
			case tool:
				return toolSides;
			case xp:
				return xpSides;
			default:
				throw MiscUtils.unhandledEnum(slot);
		}
	}

	@Override
	public IValueProvider<Set<EnumFacing>> createAllowedDirectionsProvider(AutoSlots slot) {
		return selectSlotMap(slot);
	}

	@Override
	public IWriteableBitMap<EnumFacing> createAllowedDirectionsReceiver(AutoSlots slot) {
		SyncableSides dirs = selectSlotMap(slot);
		return BitMapUtils.createRpcAdapter(createRpcProxy(dirs, IRpcDirectionBitMap.class));
	}

	@Override
	public IValueProvider<Boolean> createAutoFlagProvider(AutoSlots slot) {
		return BitMapUtils.singleBitProvider(automaticSlots, slot.ordinal());
	}

	@Override
	public IValueReceiver<Boolean> createAutoSlotReceiver(AutoSlots slot) {
		IRpcIntBitMap bits = createRpcProxy(automaticSlots, IRpcIntBitMap.class);
		return BitMapUtils.singleBitReceiver(bits, slot.ordinal());
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

}
