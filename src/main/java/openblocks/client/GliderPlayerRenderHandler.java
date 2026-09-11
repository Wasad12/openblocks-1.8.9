package openblocks.client;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.common.entity.EntityHangGlider;
import org.lwjgl.opengl.GL11;

public class GliderPlayerRenderHandler {

	@SubscribeEvent
	public void onPlayerRenderPre(RenderLivingEvent.Pre evt) {
		if (evt.entity instanceof AbstractClientPlayer) {
			final AbstractClientPlayer player = (AbstractClientPlayer)evt.entity;
			if (EntityHangGlider.isGliderDeployed(player)) {
				player.limbSwing = 0f;
				player.prevLimbSwingAmount = 0f;
				player.limbSwingAmount = 0f;

				// RenderLivingEvent.Pre fires before ANY entity transform (world-origin matrix),
				// while the 1.12.2 hook fires at the end of applyRotations, i.e. the matrix there is
				// translate(entity) * corpse-rotations. Conjugate the 1.12.2 rotation into this
				// earlier point so the final composite matches 1.12.2 bit-for-bit:
				// T * Rc * R75 * Rc^-1 * T^-1, followed by vanilla T * Rc * ..., gives T * Rc * R75 * ...
				final float partialTicks = ClientTickHandler.renderTickTime;
				final float yaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
				GL11.glPushMatrix();
				GL11.glTranslated(evt.x, evt.y, evt.z);
				GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(75, -1, 0, 0);
				GL11.glRotatef(yaw - 180.0F, 0.0F, 1.0F, 0.0F);
				GL11.glTranslated(-evt.x, -evt.y, -evt.z);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRenderPost(RenderLivingEvent.Post evt) {
		if (evt.entity instanceof AbstractClientPlayer
				&& EntityHangGlider.isGliderDeployed((AbstractClientPlayer)evt.entity)) {
			GL11.glPopMatrix();
		}
	}

	private static float interpolateRotation(float prevRotation, float nextRotation, float modifier) {
		float rotation = nextRotation - prevRotation;

		while (rotation < -180.0F)
			rotation += 360.0F;

		while (rotation >= 180.0F) {
			rotation -= 360.0F;
		}

		return prevRotation + modifier * rotation;
	}
}
