package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import openblocks.common.entity.EntityHangGlider;

public class ClientTickHandler {

	// render partial ticks of the current frame (same value the entity renderer uses)
	public static float renderTickTime;

	@SubscribeEvent
	public void onRenderTickStart(TickEvent.RenderTickEvent evt) {
		if (evt.phase == Phase.START) {
			renderTickTime = evt.renderTickTime;
			final World world = Minecraft.getMinecraft().theWorld;
			if (world != null) EntityHangGlider.updateGliders(world);
		}
	}
}
