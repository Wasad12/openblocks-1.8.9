package openblocks.client.renderer.tileentity;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import openblocks.client.model.ModelFan;
import openblocks.common.tileentity.TileEntityFan;
import org.lwjgl.opengl.GL11;

// OpenBlocks 1.8.X renderer (user-authorized exception to the no-1.8-branches rule,
// fan only, 2026-09-13) with one adaptation: blade speed follows 1.12.2 (45 deg/tick
// max) while ModelRenderer.rotateAngleZ takes RADIANS, so the 1.12.2 blade angle is
// converted here (1.8.X passed its 1-deg/tick value straight through, which read as
// ~57 deg/tick — near 1.12.2 speed by accident). Supersedes the baked-model + GL
// yaw approach (fix loop 1), whose detached-blades cause was never isolated.
public class TileEntityFanRenderer extends TileEntitySpecialRenderer<TileEntityFan> {

	private ModelFan model = new ModelFan();
	private static final ResourceLocation texture = new ResourceLocation("openblocks:textures/models/fan.png");

	@Override
	public void renderTileEntityAt(TileEntityFan fan, double x, double y, double z, float partialTick, int destroyProgress) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x + 0.5F, (float)y + 1.0f, (float)z + 0.5F);
		GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);

		GL11.glRotatef(fan != null? fan.getAngle() : 180, 0F, 1.0F, 0.0F);
		bindTexture(texture);
		model.render(partialTick, (float)Math.toRadians(fan != null? fan.getBladeRotation(partialTick) : 0));
		GL11.glPopMatrix();
	}

}
