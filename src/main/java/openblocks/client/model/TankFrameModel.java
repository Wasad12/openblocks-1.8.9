package openblocks.client.model;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelPart;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.common.block.TankNeighbourState;

/**
 * 1.8.9-native equivalent of 1.12.2's {@code openmods:variantmodel} tank frame
 * (see 1.12.2 {@code blockstates/tank.json}): the 12 frame edges are baked once from
 * {@code tank_edge_*}.json (exact 1.12.2 geometry/UVs) and combined per-block from the
 * neighbour flags. The 12 visibility expressions are ported verbatim from the
 * {@code expansions} in 1.12.2's blockstate.
 *
 * <p>Installed over the static full-frame model at {@link ModelBakeEvent}; the static
 * model stays as fallback (bake failure) and as the inventory parent.</p>
 */
public class TankFrameModel implements ISmartBlockModel {

	public static final ModelResourceLocation LOCATION = new ModelResourceLocation("openblocks:tank", "normal");

	// edge name -> bit, same 12 names as 1.12.2 tank.json variants.
	private static final String[] EDGES = { "bs", "bn", "ts", "tn", "ne", "nw", "se", "sw", "be", "bw", "te", "tw" };

	private final IBakedModel base;

	private final Map<Integer, IBakedModel> cache = new HashMap<Integer, IBakedModel>();

	private IBakedModel[] pieces;

	public TankFrameModel(IBakedModel base) {
		this.base = base;
	}

	private static boolean edgeT(boolean a, boolean b, boolean d) {
		return (a == b) && !d;
	}

	private static boolean edgeB(boolean a, boolean b, boolean d) {
		return (!a && !b) || (a && b && !d);
	}

	private static boolean has(Set<String> n, String id) {
		return n.contains(id);
	}

	static int evaluate(Set<String> n) {
		final boolean t = has(n, "n_t"), b = has(n, "n_b");
		final boolean e = has(n, "n_e"), w = has(n, "n_w"), s = has(n, "n_s"), nn = has(n, "n_n");
		final boolean te = has(n, "n_te"), tw = has(n, "n_tw"), ts = has(n, "n_ts"), tn = has(n, "n_tn");
		final boolean be = has(n, "n_be"), bw = has(n, "n_bw"), bs = has(n, "n_bs"), bn = has(n, "n_bn");
		final boolean ne = has(n, "n_ne"), nw = has(n, "n_nw"), se = has(n, "n_se"), sw = has(n, "n_sw");

		final boolean[] visible = new boolean[] {
				edgeT(b, s, bs), // bs
				edgeT(b, nn, bn), // bn
				edgeB(t, s, ts), // ts
				edgeB(t, nn, tn), // tn
				edgeT(nn, e, ne), // ne
				edgeT(nn, w, nw), // nw
				edgeB(s, e, se), // se
				edgeB(s, w, sw), // sw
				edgeT(b, e, be), // be
				edgeT(b, w, bw), // bw
				edgeB(t, e, te), // te
				edgeB(t, w, tw), // tw
		};

		int mask = 0;
		for (int i = 0; i < visible.length; i++)
			if (visible[i]) mask |= (1 << i);
		return mask;
	}

	private static final Function<ResourceLocation, TextureAtlasSprite> TEXTURE_GETTER =
			new Function<ResourceLocation, TextureAtlasSprite>() {
				@Override
				public TextureAtlasSprite apply(ResourceLocation location) {
					return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
				}
			};

	private synchronized void ensurePieces() {
		if (pieces != null) return;
		try {
			final IBakedModel[] baked = new IBakedModel[EDGES.length];
			for (int i = 0; i < EDGES.length; i++) {
				final IModel model = ModelLoaderRegistry.getModel(
						new ResourceLocation("openblocks:block/tank_edge_" + EDGES[i]));
				baked[i] = model.bake(
						new SimpleModelState(ImmutableMap.<IModelPart, TRSRTransformation>of()),
						DefaultVertexFormats.BLOCK, TEXTURE_GETTER);
			}
			pieces = baked;
		} catch (Exception e) {
			// leave pieces null -> handleBlockState falls back to the static full model.
			pieces = null;
		}
	}

	@Override
	public IBakedModel handleBlockState(IBlockState state) {
		ensurePieces();
		if (pieces == null) return base;

		Set<String> flags = null;
		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState extended = (IExtendedBlockState)state;
			if (extended.getUnlistedNames().contains(TankNeighbourState.PROPERTY))
				flags = extended.getValue(TankNeighbourState.PROPERTY);
		}

		final int mask = (flags != null)? evaluate(flags) : (1 << EDGES.length) - 1;
		IBakedModel model = cache.get(mask);
		if (model == null) {
			final List<IBakedModel> parts = new ArrayList<IBakedModel>();
			for (int i = 0; i < EDGES.length; i++)
				if ((mask & (1 << i)) != 0) parts.add(pieces[i]);
			model = new CombinedModel(base, parts);
			cache.put(mask, model);
		}
		return model;
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

	/** Concatenates the visible piece quads (base contributes no quads — it is only
	 * the fallback model and the particle/transform delegate, so including its quads
	 * would double-render the full frame under the pieces). */
	private static final class CombinedModel implements IBakedModel {
		private final IBakedModel base;
		private final List<IBakedModel> parts;

		CombinedModel(IBakedModel base, List<IBakedModel> parts) {
			this.base = base;
			this.parts = parts;
		}

		@Override
		public List<BakedQuad> getFaceQuads(EnumFacing facing) {
			final List<BakedQuad> result = new ArrayList<BakedQuad>();
			for (IBakedModel part : parts)
				result.addAll(part.getFaceQuads(facing));
			return result;
		}

		@Override
		public List<BakedQuad> getGeneralQuads() {
			final List<BakedQuad> result = new ArrayList<BakedQuad>();
			for (IBakedModel part : parts)
				result.addAll(part.getGeneralQuads());
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
	}

	/** Installs {@link TankFrameModel} over the freshly baked static model on every bake. */
	public static final class BakeHandler {
		@SubscribeEvent
		public void onModelBake(ModelBakeEvent event) {
			IBakedModel current = event.modelRegistry.getObject(LOCATION);
			if (current == null) return;
			IBakedModel inner = (current instanceof TankFrameModel)
					? ((TankFrameModel)current).base : current;
			event.modelRegistry.putObject(LOCATION, new TankFrameModel(inner));
		}
	}
}
