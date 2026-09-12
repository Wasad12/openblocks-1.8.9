package openblocks.client.model;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import openblocks.common.block.HopperOutputState;

/**
 * 1.8.9-native equivalent of 1.12.2's {@code openmods:variantmodel} vacuum hopper
 * (see 1.12.2 {@code blockstates/vacuum_hopper.json}): the static body model always
 * renders, plus one nozzle piece per side flagged in the output state. The 18
 * nozzle files ({@code vacuum_hopper_nozzle_<side>_<kind>}.json) are the 6 side
 * geometries with the {@code #texture} variable resolved per kind (1.8.9 variants
 * cannot override textures, so the combinations are pre-generated).
 * TankFrameModel pattern: baked once, combined per-block, cached per output map.
 *
 * <p>Installed over the static body model at {@link ModelBakeEvent}; the static
 * model stays as fallback (bake failure / no TE state) and as the inventory parent.</p>
 */
public class VacuumHopperModel implements ISmartBlockModel {

	public static final ModelResourceLocation LOCATION = new ModelResourceLocation("openblocks:vacuum_hopper", "normal");

	private static final String[] SIDES = { "down", "up", "north", "south", "west", "east" };

	private final IBakedModel base;

	private final Map<Map<String, String>, IBakedModel> cache = new HashMap<Map<String, String>, IBakedModel>();

	private Map<String, IBakedModel> pieces;

	public VacuumHopperModel(IBakedModel base) {
		this.base = base;
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
			final Map<String, IBakedModel> baked = new HashMap<String, IBakedModel>();
			for (String side : SIDES) {
				for (String kind : new String[] { "items", "fluids", "both" }) {
					final IModel model = ModelLoaderRegistry.getModel(
							new ResourceLocation("openblocks:block/vacuum_hopper_nozzle_" + side + "_" + kind));
					baked.put(side + ":" + kind, model.bake(
							new SimpleModelState(ImmutableMap.<IModelPart, TRSRTransformation>of()),
							DefaultVertexFormats.BLOCK, TEXTURE_GETTER));
				}
			}
			pieces = baked;
		} catch (Exception e) {
			// leave pieces null -> handleBlockState falls back to the static body model.
			pieces = null;
		}
	}

	@Override
	public IBakedModel handleBlockState(IBlockState state) {
		ensurePieces();
		if (pieces == null) return base;

		Map<String, String> outputs = ImmutableMap.of();
		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState extended = (IExtendedBlockState)state;
			if (extended.getUnlistedNames().contains(HopperOutputState.PROPERTY)) {
				final Map<String, String> value = extended.getValue(HopperOutputState.PROPERTY);
				if (value != null) outputs = value;
			}
		}

		IBakedModel model = cache.get(outputs);
		if (model == null) {
			final List<IBakedModel> parts = new ArrayList<IBakedModel>();
			for (Map.Entry<String, String> entry : outputs.entrySet()) {
				final IBakedModel piece = pieces.get(entry.getKey() + ":" + entry.getValue());
				if (piece != null) parts.add(piece);
			}
			model = new CombinedModel(base, parts);
			cache.put(outputs, model);
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

	/** Body quads plus the visible nozzle pieces (unlike the tank frame, the base
	 * contributes geometry here — it is the hopper body, not just a fallback). */
	private static final class CombinedModel implements IBakedModel {
		private final IBakedModel base;
		private final List<IBakedModel> parts;

		CombinedModel(IBakedModel base, List<IBakedModel> parts) {
			this.base = base;
			this.parts = parts;
		}

		@Override
		public List<BakedQuad> getFaceQuads(EnumFacing facing) {
			final List<BakedQuad> result = new ArrayList<BakedQuad>(base.getFaceQuads(facing));
			for (IBakedModel part : parts)
				result.addAll(part.getFaceQuads(facing));
			return result;
		}

		@Override
		public List<BakedQuad> getGeneralQuads() {
			final List<BakedQuad> result = new ArrayList<BakedQuad>(base.getGeneralQuads());
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

	/** Installs {@link VacuumHopperModel} over the freshly baked static model on every bake. */
	public static final class BakeHandler {
		@SubscribeEvent
		public void onModelBake(ModelBakeEvent event) {
			IBakedModel current = event.modelRegistry.getObject(LOCATION);
			if (current == null) return;
			IBakedModel inner = (current instanceof VacuumHopperModel)
					? ((VacuumHopperModel)current).base : current;
			event.modelRegistry.putObject(LOCATION, new VacuumHopperModel(inner));
		}
	}
}
