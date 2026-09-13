package openblocks.client.model;

import java.util.Collections;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.common.block.FanRenderState;

/**
 * 1.8.9-native equivalent of the fan's share of 1.12.2's {@code openmods:eval}
 * model (see 1.12.2 {@code blockstates/fan.json}): the TESR renders the whole
 * head (yawed frame + spinning blades) every frame, so the in-world static model
 * must render nothing — otherwise an unrotated ghost of the fan would sit under
 * the rotated head. Returns an empty model when the {@link FanRenderState}
 * unlisted property says a live TE is present, the full static model everywhere
 * else (no-TE fallback, inventory parent chain). TankFrameModel pattern.
 */
public class FanBlockModel implements ISmartBlockModel {

	public static final net.minecraft.client.resources.model.ModelResourceLocation LOCATION =
			new net.minecraft.client.resources.model.ModelResourceLocation("openblocks:fan", "normal");

	private final IBakedModel base;
	private final IBakedModel empty;

	public FanBlockModel(IBakedModel base) {
		this.base = base;
		this.empty = new EmptyModel(base);
	}

	@Override
	public IBakedModel handleBlockState(IBlockState state) {
		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState extended = (IExtendedBlockState)state;
			if (extended.getUnlistedNames().contains(FanRenderState.PROPERTY)) {
				final Boolean hasTe = extended.getValue(FanRenderState.PROPERTY);
				if (Boolean.TRUE.equals(hasTe)) return empty;
			}
		}

		return base;
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

	/** Renders nothing; delegates particle/transform queries to the base model. */
	private static final class EmptyModel implements IBakedModel {
		private final IBakedModel base;

		EmptyModel(IBakedModel base) {
			this.base = base;
		}

		@Override
		public List<BakedQuad> getFaceQuads(EnumFacing facing) {
			return Collections.emptyList();
		}

		@Override
		public List<BakedQuad> getGeneralQuads() {
			return Collections.emptyList();
		}

		@Override
		public boolean isAmbientOcclusion() {
			return false;
		}

		@Override
		public boolean isGui3d() {
			return base.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
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

	/** Installs {@link FanBlockModel} over the freshly baked static model on every bake. */
	public static final class BakeHandler {
		@SubscribeEvent
		public void onModelBake(ModelBakeEvent event) {
			IBakedModel current = event.modelRegistry.getObject(LOCATION);
			if (current == null) return;
			IBakedModel inner = (current instanceof FanBlockModel)
					? ((FanBlockModel)current).base : current;
			event.modelRegistry.putObject(LOCATION, new FanBlockModel(inner));
		}
	}
}
