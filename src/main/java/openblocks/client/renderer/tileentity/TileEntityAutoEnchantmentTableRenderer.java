package openblocks.client.renderer.tileentity;

import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import openblocks.common.tileentity.TileEntityAutoEnchantmentTable;

// 1.8.9 port of 1.12.2 TileEntityAutoEnchantmentTableRenderer (which itself mirrors
// vanilla's enchanting-table book): same floating/turning/paging book. 1.8.9
// adaptations: renderTileEntityAt signature (destroyStage instead of
// destroyProgress+alpha), MathHelper.floor_float (no fastFloor in 1.8.9).
public class TileEntityAutoEnchantmentTableRenderer extends TileEntitySpecialRenderer<TileEntityAutoEnchantmentTable> {

	private static final ResourceLocation TEXTURE_BOOK = new ResourceLocation("textures/entity/enchanting_table_book.png");

	private final ModelBook modelBook = new ModelBook();

	@Override
	public void renderTileEntityAt(TileEntityAutoEnchantmentTable table, double x, double y, double z, float partialTicks, int destroyStage) {
		TileEntityAutoEnchantmentTable.BookState te = table.bookState;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float)x + 0.5F, (float)y + 0.75F, (float)z + 0.5F);
		float f = te.tickCount + partialTicks;
		GlStateManager.translate(0.0F, 0.1F + MathHelper.sin(f * 0.1F) * 0.01F, 0.0F);
		float f1 = te.bookRotation - te.bookRotationPrev;

		while (f1 >= (float)Math.PI) {
			f1 -= ((float)Math.PI * 2F);
		}

		while (f1 < -(float)Math.PI) {
			f1 += ((float)Math.PI * 2F);
		}

		float f2 = te.bookRotationPrev + f1 * partialTicks;
		GlStateManager.rotate(-f2 * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(80.0F, 0.0F, 0.0F, 1.0F);
		bindTexture(TEXTURE_BOOK);
		float f3 = te.pageFlipPrev + (te.pageFlip - te.pageFlipPrev) * partialTicks + 0.25F;
		float f4 = te.pageFlipPrev + (te.pageFlip - te.pageFlipPrev) * partialTicks + 0.75F;
		f3 = (f3 - MathHelper.floor_float(f3)) * 1.6F - 0.3F;
		f4 = (f4 - MathHelper.floor_float(f4)) * 1.6F - 0.3F;

		if (f3 < 0.0F) {
			f3 = 0.0F;
		}

		if (f4 < 0.0F) {
			f4 = 0.0F;
		}

		if (f3 > 1.0F) {
			f3 = 1.0F;
		}

		if (f4 > 1.0F) {
			f4 = 1.0F;
		}

		float f5 = te.bookSpreadPrev + (te.bookSpread - te.bookSpreadPrev) * partialTicks;
		GlStateManager.enableCull();
		modelBook.render((Entity)null, f, f3, f4, f5, 0.0F, 0.0625F);
		GlStateManager.popMatrix();
	}
}
