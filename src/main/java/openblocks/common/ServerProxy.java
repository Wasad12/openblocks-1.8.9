package openblocks.common;

import net.minecraft.entity.player.EntityPlayer;
import openblocks.IOpenBlocksProxy;

public class ServerProxy implements IOpenBlocksProxy {

	@Override
	public void preInit() {}

	@Override
	public void init() {}

	@Override
	public void postInit() {}

	@Override
	public void registerRenderInformation() {}

	@Override
	public boolean isClientPlayer(EntityPlayer player) {
		return false;
	}
}
