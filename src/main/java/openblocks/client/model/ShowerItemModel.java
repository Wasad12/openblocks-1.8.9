package openblocks.client.model;

import java.util.List;
import javax.vecmath.Matrix4f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Gives the XP Shower item 1.12.2's {@code forge:default-block} third-person look
 * (see {@link HeldBlockPerspective} for the derivation); every other context renders
 * the plain baked model unchanged. The shower has no item states, so one instance
 * wraps the baked model directly (reload-safe via the unwrap guard, glider pattern).
 */
public class ShowerItemModel implements ISmartItemModel, IPerspectiveAwareModel {

	public static final ModelResourceLocation LOCATION = new ModelResourceLocation("openblocks:xp_shower", "inventory");

	private final IBakedModel base;

	public ShowerItemModel(IBakedModel base) {
		this.base = base;
	}

	@Override
	public IBakedModel handleItemState(ItemStack stack) {
		return this;
	}

	@Override
	public Pair<? extends IFlexibleBakedModel, Matrix4f> handlePerspective(
			ItemCameraTransforms.TransformType cameraTransformType) {
		return HeldBlockPerspective.handlePerspective(this, base, cameraTransformType);
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

	@Override
	public VertexFormat getFormat() {
		return DefaultVertexFormats.ITEM;
	}

	/** Installs {@link ShowerItemModel} over the freshly baked model on every bake. */
	public static final class BakeHandler {
		@SubscribeEvent
		public void onModelBake(ModelBakeEvent event) {
			IBakedModel current = event.modelRegistry.getObject(LOCATION);
			if (current == null) return;
			IBakedModel inner = (current instanceof ShowerItemModel)
					? ((ShowerItemModel)current).base : current;
			event.modelRegistry.putObject(LOCATION, new ShowerItemModel(inner));
		}
	}
}
