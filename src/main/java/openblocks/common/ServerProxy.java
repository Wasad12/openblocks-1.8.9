package openblocks.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import openblocks.IOpenBlocksProxy;
import openblocks.OpenBlocks;

public class ServerProxy implements IOpenBlocksProxy {

	@Override
	public void preInit() {}

	@Override
	public void init() {
		net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(OpenBlocks.instance, new openmods.gui.CommonGuiHandler());
	}

	@Override
	public void postInit() {}

	@Override
	public void registerRenderInformation() {}

	@Override
	public boolean isClientPlayer(EntityPlayer player) {
		return false;
	}

	@Override
	public World getServerWorld(int dimensionId) {
		return net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
	}

	@Override
	public World getClientWorld() {
		return null;
	}

	@Override
	public int getParticleSettings() {
		return 2;
	}

	@Override
	public void spawnLiquidSpray(World world, FluidStack fluid, double x, double y, double z, float scale, float gravity, Vec3 velocity) {}
}
