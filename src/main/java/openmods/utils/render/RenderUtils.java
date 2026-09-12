package openmods.utils.render;

import net.minecraft.client.renderer.GlStateManager;

// 1.8.9 port of OpenModsLib RenderUtils: only the setColor shapes needed by the
// ported GUI code. Logic verbatim.
public class RenderUtils {

	public static void setColor(int rgb) {
		final float r = (float)((rgb >> 16) & 0xFF) / 255;
		final float g = (float)((rgb >> 8) & 0xFF) / 255;
		final float b = (float)((rgb >> 0) & 0xFF) / 255;
		GlStateManager.color(r, g, b);
	}

	public static void setColor(int rgb, float alpha) {
		final float r = (float)((rgb >> 16) & 0xFF) / 255;
		final float g = (float)((rgb >> 8) & 0xFF) / 255;
		final float b = (float)((rgb >> 0) & 0xFF) / 255;

		GlStateManager.color(r, g, b, alpha);
	}
}
