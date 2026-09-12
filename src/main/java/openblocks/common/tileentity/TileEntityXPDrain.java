package openblocks.common.tileentity;

import java.util.List;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;
import openblocks.OpenBlocks;
import openblocks.common.LiquidXpUtils;
import openmods.utils.EnchantmentUtils;

// 1.8.9 port of 1.12.2 TileEntityXPDrain. OpenTileEntity dropped (plain TileEntity);
// fluid capability replaced with the old facing-based IFluidHandler (our Tank speaks
// it, as does any 1.8.9 fluid block). Logic verbatim; pickup sound is "random.orb".
public class TileEntityXPDrain extends TileEntity implements ITickable {

	@Override
	public void update() {
		if (!getWorld().isRemote) {
			final List<EntityXPOrb> xpOrbsOnGrid = getXPOrbsOnGrid();
			final List<EntityPlayer> playersOnGrid = getPlayersOnGrid();

			if (!xpOrbsOnGrid.isEmpty() || !playersOnGrid.isEmpty()) {
				final BlockPos down = getPos().down();

				if (getWorld().isBlockLoaded(down)) {
					final TileEntity te = getWorld().getTileEntity(down);

					if (te instanceof IFluidHandler && !te.isInvalid()) {
						final IFluidHandler maybeHandler = (IFluidHandler)te;

						for (EntityXPOrb orb : xpOrbsOnGrid)
							tryConsumeOrb(maybeHandler, orb);

						for (EntityPlayer player : playersOnGrid)
							tryDrainPlayer(maybeHandler, player);
					}
				}
			}
		}
	}

	protected void tryDrainPlayer(IFluidHandler tank, EntityPlayer player) {
		int playerXP = EnchantmentUtils.getPlayerXP(player);
		if (playerXP <= 0) return;

		int maxDrainedXp = Math.min(4, playerXP);

		int xpAmount = LiquidXpUtils.xpToLiquidRatio(maxDrainedXp);
		FluidStack xpStack = new FluidStack(OpenBlocks.Fluids.xpJuice, xpAmount);

		int maxAcceptedLiquid = tank.fill(EnumFacing.UP, xpStack, false);

		// rounding down, so we only use as much as we can
		int acceptedXP = LiquidXpUtils.liquidToXpRatio(maxAcceptedLiquid);
		int acceptedLiquid = LiquidXpUtils.xpToLiquidRatio(acceptedXP);

		xpStack.amount = acceptedLiquid;
		int finallyAcceptedLiquid = tank.fill(EnumFacing.UP, xpStack, true);

		if (finallyAcceptedLiquid <= 0) return;

		if (getWorld().getTotalWorldTime() % 4 == 0) {
			getWorld().playSoundEffect(getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
					"random.orb", 0.1F, 0.5F * ((getWorld().rand.nextFloat() - getWorld().rand.nextFloat()) * 0.7F + 1.8F));
		}

		EnchantmentUtils.addPlayerXP(player, -acceptedXP);
	}

	protected void tryConsumeOrb(IFluidHandler tank, EntityXPOrb orb) {
		if (!orb.isDead) {
			int xpAmount = LiquidXpUtils.xpToLiquidRatio(orb.getXpValue());
			FluidStack xpStack = new FluidStack(OpenBlocks.Fluids.xpJuice, xpAmount);
			int filled = tank.fill(EnumFacing.UP, xpStack, false);
			if (filled == xpStack.amount) {
				tank.fill(EnumFacing.UP, xpStack, true);
				orb.setDead();
			}
		}
	}

	protected List<EntityPlayer> getPlayersOnGrid() {
		final BlockPos pos = getPos();
		return getWorld().getEntitiesWithinAABB(EntityPlayer.class,
				new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1));
	}

	protected List<EntityXPOrb> getXPOrbsOnGrid() {
		final BlockPos pos = getPos();
		return getWorld().getEntitiesWithinAABB(EntityXPOrb.class,
				new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.3, pos.getZ() + 1));
	}
}
