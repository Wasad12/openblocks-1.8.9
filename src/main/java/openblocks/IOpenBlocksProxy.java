package openblocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public interface IOpenBlocksProxy {
	public void preInit();

	public void init();

	public void postInit();

	public void registerRenderInformation();

	public boolean isClientPlayer(EntityPlayer player);

	public int getParticleSettings();

	public void spawnLiquidSpray(World world, FluidStack fluid, double x, double y, double z, float scale, float gravity, Vec3 velocity);
}
