package openblocks.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.common.item.ItemTankBlock;
import openblocks.common.tileentity.TileEntityTank;

/**
 * 1.8.9-native equivalent of 1.12.2's inventory {@code textureditem} submodel for the
 * tank (see 1.12.2 {@code blockstates/tank.json} {@code inventory} variant): a filled
 * tank item shows its fluid as a box inside the frame. 1.8.9 has no item-override
 * system, so the fluid box is generated procedurally (winding/UVs mirror the TESR)
 * and cached per fluid + sixteenth-level, like 1.12.2's 17 {@code tank_fluid_N} models.
 *
 * <p>Installed over the static frame item model at {@link ModelBakeEvent} (the static
 * model stays for empty tanks).</p>
 */
public class TankItemModel implements ISmartItemModel {

	public static final ModelResourceLocation LOCATION = new ModelResourceLocation("openblocks:tank", "inventory");

	private final IBakedModel base;

	private static final Map<String, IBakedModel> cache = new HashMap<String, IBakedModel>();

	public TankItemModel(IBakedModel base) {
		this.base = base;
	}

	@Override
	public IBakedModel handleItemState(ItemStack stack) {
		final FluidStack fluid = ItemTankBlock.getTankFluid(stack);
		if (fluid == null) return base;

		final int capacity = TileEntityTank.getTankCapacity();
		final int level = Math.max(1, Math.min(16, Math.round(16.0f * fluid.amount / capacity)));
		final String key = FluidRegistry.getFluidName(fluid.getFluid()) + "#" + level;

		IBakedModel model = cache.get(key);
		if (model == null) {
			final TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
					.getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
			model = new FilledModel(base, buildFluidBox(sprite, level / 16.0f));
			cache.put(key, model);
		}
		return model;
	}

	private static void putVertex(UnpackedBakedQuad.Builder builder, double x, double y, double z, double u, double v, float nx, float ny, float nz) {
		builder.put(0, (float)x, (float)y, (float)z);
		builder.put(1, 1, 1, 1, 1);
		builder.put(2, (float)u, (float)v);
		builder.put(3, nx, ny, nz);
	}

	private static BakedQuad quad(EnumFacing face, double[] xs, double[] ys, double[] zs, double[] us, double[] vs) {
		// NOTE: 1.8.9 BakedQuad carries no sprite (added in 1.9) — texture comes purely
		// from atlas-absolute UVs, so there is no setTexture call (compiler-verified).
		final UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
		builder.setQuadTint(-1);
		builder.setQuadOrientation(face);
		final float nx = face.getFrontOffsetX(), ny = face.getFrontOffsetY(), nz = face.getFrontOffsetZ();
		for (int i = 0; i < 4; i++)
			putVertex(builder, xs[i], ys[i], zs[i], us[i], vs[i], nx, ny, nz);
		return builder.build();
	}

	// Full 0..1-footprint box of height h (frame border boxes never put faces exactly
	// on these planes, so no z-fighting — same geometry as 1.12.2 tank_fluid_N).
	static List<BakedQuad> buildFluidBox(TextureAtlasSprite sprite, double h) {
		if (h > 1) h = 1;
		final double uMin = sprite.getMinU(), uMax = sprite.getMaxU();
		final double vMin = sprite.getMinV(), vMax = sprite.getMaxV();

		final List<BakedQuad> quads = new ArrayList<BakedQuad>();
		// NORTH (z=0)
		quads.add(quad(EnumFacing.NORTH,
				new double[] { 1, 1, 0, 0 }, new double[] { 0, h, h, 0 }, new double[] { 0, 0, 0, 0 },
				new double[] { uMax, uMax, uMin, uMin }, new double[] { vMin, vMax, vMax, vMin }));
		// SOUTH (z=1)
		quads.add(quad(EnumFacing.SOUTH,
				new double[] { 1, 1, 0, 0 }, new double[] { 0, h, h, 0 }, new double[] { 1, 1, 1, 1 },
				new double[] { uMin, uMin, uMax, uMax }, new double[] { vMin, vMax, vMax, vMin }));
		// EAST (x=1)
		quads.add(quad(EnumFacing.EAST,
				new double[] { 1, 1, 1, 1 }, new double[] { 0, h, h, 0 }, new double[] { 0, 0, 1, 1 },
				new double[] { uMin, uMin, uMax, uMax }, new double[] { vMin, vMax, vMax, vMin }));
		// WEST (x=0)
		quads.add(quad(EnumFacing.WEST,
				new double[] { 0, 0, 0, 0 }, new double[] { 0, h, h, 0 }, new double[] { 1, 1, 0, 0 },
				new double[] { uMin, uMin, uMax, uMax }, new double[] { vMin, vMax, vMax, vMin }));
		// UP
		quads.add(quad(EnumFacing.UP,
				new double[] { 0, 1, 1, 0 }, new double[] { h, h, h, h }, new double[] { 1, 1, 0, 0 },
				new double[] { uMax, uMax, uMin, uMin }, new double[] { vMax, vMin, vMin, vMax }));
		// DOWN
		quads.add(quad(EnumFacing.DOWN,
				new double[] { 1, 1, 0, 0 }, new double[] { 0, 0, 0, 0 }, new double[] { 0, 1, 1, 0 },
				new double[] { uMax, uMin, uMin, uMax }, new double[] { vMin, vMin, vMax, vMax }));
		return quads;
	}

	@Override
	public List<BakedQuad> getFaceQuads(EnumFacing facing) {
		return base.getFaceQuads(facing);
	}

	@Override
	public List<BakedQuad> getGeneralQuads() {
		return base.getGeneralQuads();
	}

	@Override
	public boolean isAmbientOcclusion() {
		return base.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return base.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return base.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return base.getParticleTexture();
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return base.getItemCameraTransforms();
	}

	/** Frame quads from base + procedural fluid box in general quads. */
	private static final class FilledModel implements IFlexibleBakedModel {
		private final IBakedModel base;
		private final List<BakedQuad> fluid;

		FilledModel(IBakedModel base, List<BakedQuad> fluid) {
			this.base = base;
			this.fluid = fluid;
		}

		@Override
		public List<BakedQuad> getFaceQuads(EnumFacing facing) {
			return base.getFaceQuads(facing);
		}

		@Override
		public List<BakedQuad> getGeneralQuads() {
			final List<BakedQuad> result = new ArrayList<BakedQuad>(base.getGeneralQuads());
			result.addAll(fluid);
			return result;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return base.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return base.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return base.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return base.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return base.getItemCameraTransforms();
		}

		@Override
		public VertexFormat getFormat() {
			return DefaultVertexFormats.ITEM;
		}
	}

	/** Installs {@link TankItemModel} over the freshly baked frame item model on every bake. */
	public static final class BakeHandler {
		@SubscribeEvent
		public void onModelBake(ModelBakeEvent event) {
			cache.clear();
			IBakedModel current = event.modelRegistry.getObject(LOCATION);
			if (current == null) return;
			IBakedModel inner = (current instanceof TankItemModel)
					? ((TankItemModel)current).base : current;
			event.modelRegistry.putObject(LOCATION, new TankItemModel(inner));
		}
	}
}
