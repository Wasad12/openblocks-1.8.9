package openblocks.common.tileentity;

import java.util.List;
import java.util.Random;
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
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.oredict.OreDictionary;
import openblocks.OpenBlocks;
import openblocks.client.gui.GuiAutoEnchantmentTable;
import openblocks.common.LiquidXpUtils;
import openblocks.common.container.ContainerAutoEnchantmentTable;
import openblocks.common.tileentity.TileEntityAutoEnchantmentTable.AutoSlots;
import openblocks.rpc.ILevelChanger;
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
import openmods.sync.SyncableEnum;
import openmods.sync.SyncableFlags;
import openmods.sync.SyncableInt;
import openmods.sync.SyncableSides;
import openmods.sync.SyncableTank;
import openmods.tileentity.SyncedTileEntity;
import openmods.utils.EnchantmentUtils;
import openmods.utils.MiscUtils;
import openmods.utils.SidedInventoryAdapter;
import openmods.utils.SidedItemHandlerAdapter;
import openmods.utils.VanillaEnchantLogic;
import openmods.utils.VanillaEnchantLogic.Level;
import openmods.utils.bitmap.BitMapUtils;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;
import openmods.utils.bitmap.IWriteableBitMap;

// 1.8.9 port of 1.12.2 TileEntityAutoEnchantmentTable: same slots/tank/sides sync
// fields, update logic, enchant gating and book math. 1.8.9 adaptations: nullable
// stacks (no isEmpty/EMPTY/getCount/shrink — null/stackSize), MathHelper.clamp_*,
// getClosestPlayer without the watch flag, old IFluidHandler instead of fluid
// capabilities (side-gated via the same xpSides flags), javax.annotation and
// @RegisterFixer dropped. Everything else verbatim.
public class TileEntityAutoEnchantmentTable extends SyncedTileEntity implements IInventoryProvider, IHasGui, IConfigurableGuiSlots<AutoSlots>, ILevelChanger, INeighbourAwareTile, ITickable, IFluidHandler {

	private static final String TAG_SEED = "Seed";

	public static final int MAX_STORED_LEVELS = 30;
	public static final int TANK_CAPACITY = LiquidXpUtils.getLiquidForLevel(MAX_STORED_LEVELS);

	public static enum Slots {
		tool,
		output,
		lapis
	}

	public static enum AutoSlots {
		toolInput,
		lapisInput,
		output,
		xp
	}

	private SyncableTank tank;
	private SyncableSides inputSides;
	private SyncableSides lapisSides;
	private SyncableSides outputSides;
	private SyncableSides xpSides;
	private SyncableFlags automaticSlots;

	private SyncableInt powerLimit;
	private SyncableInt availablePower;
	private SyncableEnum<VanillaEnchantLogic.Level> selectedLevel;

	private long seed;

	private static final int POWER_CHECK_PERIOD = 20;

	private int powerCheckCountdown = 0;

	private boolean needsTankUpdate;

	private final GenericInventory inventory = new TileEntityInventory(this, "autoenchant", true, 3) {
		final List<ItemStack> lapis = OreDictionary.getOres("gemLapis");

		@Override
		public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
			if (slot == Slots.tool.ordinal()) return itemstack != null && itemstack.isItemEnchantable();
			if (slot == Slots.lapis.ordinal()) {
				if (itemstack == null) return false;
				for (ItemStack ore : lapis)
					if (OreDictionary.itemMatches(ore, itemstack, false)) return true;

				return false;
			}
			return false;
		}
	};

	private final SidedInventoryAdapter slotSides = new SidedInventoryAdapter(inventory);

	private final SidedItemHandlerAdapter itemHandlerCapability = new SidedItemHandlerAdapter(inventory.getHandler());

	private static final Random bookRand = new Random();

	private static final Random seedGenerator = new Random();

	/**
	 * grotesque book turning stuff taken from the main enchantment table
	 */
	public class BookState {

		public int tickCount;
		public float pageFlip;
		public float pageFlipPrev;
		public float flipT;
		public float flipA;
		public float bookSpread;
		public float bookSpreadPrev;
		public float bookRotation;
		public float bookRotationPrev;
		public float tRot;

		public void handleBookRotation() {
			this.bookSpreadPrev = this.bookSpread;
			this.bookRotationPrev = this.bookRotation;
			EntityPlayer entityplayer = worldObj.getClosestPlayer(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 3.0D);

			if (entityplayer != null) {
				double d0 = entityplayer.posX - (pos.getX() + 0.5F);
				double d1 = entityplayer.posZ - (pos.getZ() + 0.5F);
				this.tRot = (float)MathHelper.atan2(d1, d0);
				this.bookSpread += 0.1F;

				if (this.bookSpread < 0.5F || bookRand.nextInt(40) == 0) {
					float f1 = this.flipT;

					while (true) {
						this.flipT += bookRand.nextInt(4) - bookRand.nextInt(4);

						if (f1 != this.flipT) {
							break;
						}
					}
				}
			} else {
				this.tRot += 0.02F;
				this.bookSpread -= 0.1F;
			}

			while (this.bookRotation >= (float)Math.PI) {
				this.bookRotation -= ((float)Math.PI * 2F);
			}

			while (this.bookRotation < -(float)Math.PI) {
				this.bookRotation += ((float)Math.PI * 2F);
			}

			while (this.tRot >= (float)Math.PI) {
				this.tRot -= ((float)Math.PI * 2F);
			}

			while (this.tRot < -(float)Math.PI) {
				this.tRot += ((float)Math.PI * 2F);
			}

			float f2 = this.tRot - this.bookRotation;

			while (f2 >= (float)Math.PI) {
				f2 -= ((float)Math.PI * 2F);
			}

			while (f2 < -(float)Math.PI) {
				f2 += ((float)Math.PI * 2F);
			}

			this.bookRotation += f2 * 0.4F;
			this.bookSpread = MathHelper.clamp_float(this.bookSpread, 0.0F, 1.0F);
			++this.tickCount;
			this.pageFlipPrev = this.pageFlip;
			float f = (this.flipT - this.pageFlip) * 0.4F;
			f = MathHelper.clamp_float(f, -0.2F, 0.2F);
			this.flipA += (f - this.flipA) * 0.9F;
			this.pageFlip += this.flipA;
		}
	}

	public final BookState bookState = new BookState();

	public TileEntityAutoEnchantmentTable() {
		slotSides.registerSlot(Slots.tool, inputSides, true, false);
		slotSides.registerSlot(Slots.lapis, lapisSides, true, false);
		slotSides.registerSlot(Slots.output, outputSides, false, true);

		itemHandlerCapability.registerSlot(Slots.tool, inputSides, true, false);
		itemHandlerCapability.registerSlot(Slots.lapis, lapisSides, true, false);
		itemHandlerCapability.registerSlot(Slots.output, outputSides, false, true);

		this.seed = seedGenerator.nextLong();
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
		tank = new SyncableTank(TANK_CAPACITY, OpenBlocks.Fluids.xpJuice);
		inputSides = new SyncableSides();
		outputSides = new SyncableSides();
		xpSides = new SyncableSides();
		lapisSides = new SyncableSides();
		powerLimit = new SyncableInt(1);
		availablePower = new SyncableInt();
		selectedLevel = new SyncableEnum<VanillaEnchantLogic.Level>(VanillaEnchantLogic.Level.L1);
		automaticSlots = SyncableFlags.create(AutoSlots.values().length);
	}

	@Override
	protected void onSyncMapCreate(SyncMap syncMap) {
		syncMap.addSyncListener(itemHandlerCapability.createSyncListener());
	}

	@Override
	public void update() {
		bookState.handleBookRotation();

		if (!worldObj.isRemote) {
			if (automaticSlots.get(AutoSlots.xp)) {
				if (needsTankUpdate) {
					tank.updateNeighbours(worldObj, pos);
					needsTankUpdate = false;
				}

				tank.fillFromSides(80, worldObj, pos, xpSides.getValue());
			}

			if (powerCheckCountdown-- <= 0) {
				powerCheckCountdown = POWER_CHECK_PERIOD;
				final int power = (int)EnchantmentUtils.getPower(worldObj, getPos());
				availablePower.set(power);
			}

			final ItemMover mover = new ItemMover(worldObj, pos).breakAfterFirstTry().randomizeSides().setMaxSize(1);

			if (shouldAutoOutput() && hasStack(Slots.output)) {
				mover.setSides(outputSides.getValue()).pushFromSlot(inventory.getHandler(), Slots.output.ordinal());
			}

			if (shouldAutoInputTool() && hasSpace(Slots.tool)) {
				mover.setSides(inputSides.getValue()).pullToSlot(inventory.getHandler(), Slots.tool.ordinal());
			}

			if (shouldAutoInputLapis() && hasSpace(Slots.lapis)) {
				mover.setSides(lapisSides.getValue()).pullToSlot(inventory.getHandler(), Slots.lapis.ordinal());
			}

			tryEnchantItem();

			sync();
		}
	}

	private void tryEnchantItem() {
		final ItemStack tool = getStack(Slots.tool);
		if (tool == null || !tool.isItemEnchantable()) return;

		final ItemStack lapis = getStack(Slots.lapis);
		if (lapis == null) return;

		if (hasStack(Slots.output)) return;

		final int power = Math.min(availablePower.get(), powerLimit.get());
		if (power <= 0) return;

		final VanillaEnchantLogic logic = new VanillaEnchantLogic(seed);
		if (!logic.setup(tool, selectedLevel.get(), power)) return;

		if (lapis.stackSize < logic.getLapisCost()) return;

		final int levelsRequirement = logic.getLevelRequirement();
		final int availableXp = LiquidXpUtils.liquidToXpRatio(tank.getFluidAmount());
		final int availableLevels = EnchantmentUtils.getLevelForExperience(availableXp);
		if (availableLevels < levelsRequirement) return;

		final int xpCost = EnchantmentUtils.getExperienceForLevel(levelsRequirement) - EnchantmentUtils.getExperienceForLevel(levelsRequirement - logic.getLevelCost());
		final int liquidXpCost = LiquidXpUtils.xpToLiquidRatio(xpCost);
		final FluidStack drainedXp = tank.drain(liquidXpCost, false);
		if (drainedXp == null || drainedXp.amount < xpCost) return;

		setStack(Slots.output, logic.enchant());

		setStack(Slots.tool, null);
		decrementStack(Slots.lapis, logic.getLapisCost());
		tank.drain(liquidXpCost, true);

		this.seed = seedGenerator.nextLong();
	}

	private boolean shouldAutoInputLapis() {
		return automaticSlots.get(AutoSlots.lapisInput);
	}

	private boolean shouldAutoInputTool() {
		return automaticSlots.get(AutoSlots.toolInput);
	}

	private boolean shouldAutoOutput() {
		return automaticSlots.get(AutoSlots.output);
	}

	private boolean hasStack(Slots slot) {
		return getStack(slot) != null;
	}

	private boolean hasSpace(Slots slot) {
		final ItemStack stackInSlot = getStack(slot);
		return stackInSlot == null || stackInSlot.stackSize < stackInSlot.getMaxStackSize();
	}

	public void setStack(Slots slot, ItemStack stack) {
		inventory.setInventorySlotContents(slot.ordinal(), stack);
	}

	private void decrementStack(Slots slot, int amount) {
		ItemStack stack = getStack(slot);
		stack.stackSize -= amount;
		if (stack.stackSize <= 0) setStack(slot, null);
		else markDirty();
	}

	private ItemStack getStack(Slots slot) {
		return inventory.getStackInSlot(slot);
	}

	@Override
	public Object getServerGui(EntityPlayer player) {
		return new ContainerAutoEnchantmentTable(player.inventory, this);
	}

	@Override
	public Object getClientGui(EntityPlayer player) {
		return new GuiAutoEnchantmentTable(new ContainerAutoEnchantmentTable(player.inventory, this));
	}

	@Override
	public boolean canOpenGui(EntityPlayer player) {
		return true;
	}

	public IValueProvider<FluidStack> getFluidProvider() {
		return tank;
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
		tag.setLong(TAG_SEED, seed);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		seed = tag.getLong(TAG_SEED);
		inventory.readFromNBT(tag, false);
	}

	private SyncableSides selectSlotMap(AutoSlots slot) {
		switch (slot) {
			case toolInput:
				return inputSides;
			case output:
				return outputSides;
			case xp:
				return xpSides;
			case lapisInput:
				return lapisSides;
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
	public void changePowerLimit(int powerLimit) {
		this.powerLimit.set(powerLimit);
		sync();
	}

	@Override
	public void changeLevel(Level level) {
		this.selectedLevel.set(level);
		sync();
	}

	public IValueProvider<Integer> getLevelProvider() {
		return powerLimit;
	}

	public IValueProvider<Integer> getAvailablePowerProvider() {
		return availablePower;
	}

	public IValueProvider<Level> getSelectedLevelProvider() {
		return selectedLevel;
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
