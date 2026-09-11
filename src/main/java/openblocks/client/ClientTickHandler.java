package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import openblocks.common.entity.EntityHangGlider;

public class ClientTickHandler {

	@SubscribeEvent
	public void onRenderTickStart(TickEvent.RenderTickEvent evt) {
		if (evt.phase == Phase.START) {
			final World world = Minecraft.getMinecraft().theWorld;
			if (world != null) EntityHangGlider.updateGliders(world);
		}
	}
}
