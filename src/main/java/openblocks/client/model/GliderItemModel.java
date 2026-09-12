package openblocks.client.model;

import java.util.Collections;
import java.util.List;
import javax.vecmath.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.common.entity.EntityHangGlider;
import openblocks.common.item.ItemHangGlider;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 1.8.9-native equivalent of 1.12.2's {@code openmods:perspective-aware} model for the hang
 * glider (see 1.12.2 {@code blockstates/hang_glider.json}): while a glider is deployed, the
 * held item is invisible in hand perspectives but stays visible in the GUI and inventory.
 *
 * <p>Installed over the normal baked model at {@link ModelBakeEvent}. The item mesh definition
 * always resolves to the normal model; all deployed/folded switching happens here, where both
 * the stack (via {@link ISmartItemModel}) and the render context (via
 * {@link IPerspectiveAwareModel}) are visible — neither alone can see both.</p>
 */
public class GliderItemModel implements ISmartItemModel, IPerspectiveAwareModel {

	public static final ModelResourceLocation NORMAL =
			new ModelResourceLocation("openblocks:hang_glider", "inventory");

	private final IBakedModel inner;

	public GliderItemModel(IBakedModel inner) {
		this.inner = inner;
	}

	@Override
	public IBakedModel handleItemState(ItemStack stack) {
		// Deployed: return this, so the perspective-aware branch below hides the item in
		// hands but keeps it in the GUI. Folded: the plain baked model (today's behavior).
		// Player-scoped aliveness (same lookup the body-tilt uses), NOT stack identity:
		// 1.8.9's hook only receives the stack, and identity proved unreliable in survival
		// (creative hid, survival didn't — same code, so the objects must differ by mode).
		if (stack == null || !(stack.getItem() instanceof ItemHangGlider)) return inner;
		final EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
		if (thePlayer == null) return inner;
		return EntityHangGlider.getGliderFor(thePlayer) != null? this : inner;
	}

	@Override
	public Pair<? extends IFlexibleBakedModel, Matrix4f> handlePerspective(
			ItemCameraTransforms.TransformType cameraTransformType) {
		// Only reached for a deployed glider (see handleItemState).
		if (cameraTransformType == ItemCameraTransforms.TransformType.THIRD_PERSON
				|| cameraTransformType == ItemCameraTransforms.TransformType.FIRST_PERSON) {
			return Pair.<IFlexibleBakedModel, Matrix4f>of(new HiddenModel(inner), new Matrix4f());
		}
		if (inner instanceof IPerspectiveAwareModel) {
			return ((IPerspectiveAwareModel)inner).handlePerspective(cameraTransformType);
		}
		return Pair.<IFlexibleBakedModel, Matrix4f>of(asFlexible(inner), new Matrix4f());
	}

	@Override
	public List<BakedQuad> getFaceQuads(EnumFacing facing) {
		return inner.getFaceQuads(facing);
	}

	@Override
	public List<BakedQuad> getGeneralQuads() {
		return inner.getGeneralQuads();
	}

	@Override
	public boolean isAmbientOcclusion() {
		return inner.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return inner.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return inner.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return inner.getParticleTexture();
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return inner.getItemCameraTransforms();
	}

	@Override
	public VertexFormat getFormat() {
		return asFlexible(inner).getFormat();
	}

	private static IFlexibleBakedModel asFlexible(IBakedModel model) {
		if (model instanceof IFlexibleBakedModel) return (IFlexibleBakedModel)model;
		return new FlexibleWrapper(model);
	}

	/** Renders nothing (no quads); used for the deployed glider in hand perspectives. */
	private static final class HiddenModel implements IFlexibleBakedModel {
		private final IBakedModel inner;

		HiddenModel(IBakedModel inner) {
			this.inner = inner;
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
			return inner.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return inner.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return inner.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return inner.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return inner.getItemCameraTransforms();
		}

		@Override
		public VertexFormat getFormat() {
			return asFlexible(inner).getFormat();
		}
	}

	/** Adapts a plain baked model to the flexible interface by pure delegation. */
	private static final class FlexibleWrapper implements IFlexibleBakedModel {
		private final IBakedModel inner;

		FlexibleWrapper(IBakedModel inner) {
			this.inner = inner;
		}

		@Override
		public List<BakedQuad> getFaceQuads(EnumFacing facing) {
			return inner.getFaceQuads(facing);
		}

		@Override
		public List<BakedQuad> getGeneralQuads() {
			return inner.getGeneralQuads();
		}

		@Override
		public boolean isAmbientOcclusion() {
			return inner.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return inner.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return inner.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return inner.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return inner.getItemCameraTransforms();
		}

		@Override
		public VertexFormat getFormat() {
			return DefaultVertexFormats.ITEM;
		}
	}

	/** Installs {@link GliderItemModel} over the freshly baked normal model on every bake. */
	public static final class BakeHandler {
		@SubscribeEvent
		public void onModelBake(ModelBakeEvent event) {
			IBakedModel current = event.modelRegistry.getObject(NORMAL);
			if (current == null) return;
			IBakedModel inner = (current instanceof GliderItemModel)
					? ((GliderItemModel)current).inner : current;
			event.modelRegistry.putObject(NORMAL, new GliderItemModel(inner));
		}
	}
}
