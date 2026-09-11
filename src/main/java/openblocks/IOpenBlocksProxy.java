package openblocks;

import net.minecraft.entity.player.EntityPlayer;

public interface IOpenBlocksProxy {
	public void preInit();

	public void init();

	public void postInit();

	public void registerRenderInformation();

	public boolean isClientPlayer(EntityPlayer player);
}
