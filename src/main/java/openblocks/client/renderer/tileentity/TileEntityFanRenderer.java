package openblocks.client.renderer.tileentity;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelPart;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.client.model.animation.FastTESR;
import openblocks.common.tileentity.TileEntityFan;
import org.lwjgl.opengl.GL11;

// 1.8.9 port of 1.12.2 TileEntityFanRenderer: same structure (frame pass, blades
// pass, region render cache, full lighting path via the block model renderer) but
// the openmods:eval model states are replaced with direct GL transforms (see
// ARCHITECTURE.md): the verbatim 1.12.2 frame model yawed by -angle about Y
// through the block center, the verbatim blades model offset +0.171875 Y and spun
// by bladeRotation about Z through the ring-center pivot. Both models are baked
// lazily (hopper pattern — their textures are stitched explicitly) because no
// 1.8.9 blockstate can reference two models for one block.
public class TileEntityFanRenderer extends FastTESR<TileEntityFan> {

	/** Blades Y offset: the 1.12.2 inventory composition AND the blade_spin offset_y. */
	private static final float BLADES_OFFSET_Y = 0.171875f;

	/** Blade spin pivot (block-local): authored quad center raised by the offset. */
	private static final float PIVOT_X = 0.5f;
	private static final float PIVOT_Y = 0.5f + BLADES_OFFSET_Y;
	private static final float PIVOT_Z = 7.0f / 16.0f;

	protected static BlockRendererDispatcher blockRenderer;

	private static IBakedModel frameModel;
	private static IBakedModel bladesModel;

	private static final Function<ResourceLocation, TextureAtlasSprite> TEXTURE_GETTER =
			new Function<ResourceLocation, TextureAtlasSprite>() {
				@Override
				public TextureAtlasSprite apply(ResourceLocation location) {
					return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
				}
			};

	private static synchronized void ensureModels() {
		if (frameModel != null && bladesModel != null) return;
		try {
			final IModel frame = ModelLoaderRegistry.getModel(new ResourceLocation("openblocks:block/fan_frame"));
			final IModel blades = ModelLoaderRegistry.getModel(new ResourceLocation("openblocks:block/fan_blades"));
			final SimpleModelState identity = new SimpleModelState(ImmutableMap.<IModelPart, TRSRTransformation>of());
			frameModel = frame.bake(identity, DefaultVertexFormats.BLOCK, TEXTURE_GETTER);
			bladesModel = blades.bake(identity, DefaultVertexFormats.BLOCK, TEXTURE_GETTER);
		} catch (Exception e) {
			// leave null -> this frame renders nothing; retried next frame.
			frameModel = null;
			bladesModel = null;
		}
	}

	@Override
	public void renderTileEntityFast(TileEntityFan te, double x, double y, double z, float partialTicks, int destroyStage, WorldRenderer renderer) {
		if (te.isInvalid()) return;
		ensureModels();
		if (frameModel == null || bladesModel == null) return;
		if (blockRenderer == null) blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();

		final BlockPos pos = te.getPos();
		final IBlockAccess world = MinecraftForgeClient.getRegionRenderCache(te.getWorld(), pos);
		final IBlockState state = world.getBlockState(pos);

		GL11.glPushMatrix();
		// base_rotate: whole head yaws by -angle about Y through the block center.
		GL11.glTranslatef((float)(x + 0.5), (float)y, (float)(z + 0.5));
		GL11.glRotatef(-te.getAngle(), 0, 1, 0);
		GL11.glTranslatef((float)-(x + 0.5), (float)-y, (float)-(z + 0.5));
		renderer.setTranslation(x - pos.getX(), y - pos.getY(), z - pos.getZ());
		blockRenderer.getBlockModelRenderer().renderModel(world, frameModel, state, pos, renderer, false);

		// blade_spin: blades ride +0.171875 Y, spinning about Z through the pivot.
		GL11.glTranslatef(0, BLADES_OFFSET_Y, 0);
		GL11.glTranslatef((float)(x + PIVOT_X), (float)(y + PIVOT_Y), (float)(z + PIVOT_Z));
		GL11.glRotatef(te.getBladeRotation(partialTicks), 0, 0, 1);
		GL11.glTranslatef((float)-(x + PIVOT_X), (float)-(y + PIVOT_Y), (float)-(z + PIVOT_Z));
		blockRenderer.getBlockModelRenderer().renderModel(world, bladesModel, state, pos, renderer, false);

		renderer.setTranslation(0, 0, 0);
		GL11.glPopMatrix();
	}
}
