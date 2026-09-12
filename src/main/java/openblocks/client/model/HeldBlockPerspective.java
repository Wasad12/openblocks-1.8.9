package openblocks.client.model;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Applies 1.12.2's {@code forge:default-block} third-person look to 1.8.9 block items.
 *
 * <p>1.12.2 renders these items with the wrapped transform
 * {@code convert(0, 2.5, 0, 75, 45, 0, 0.375)} (see 1.12.x
 * {@code ForgeBlockStateV1.Transforms}: raw TRS folded with
 * {@code blockCenterToCorner}). 1.8.9 JSON {@code display} blocks cannot express the
 * fold (nor its XYZ rotation order), so the equivalent matrix is composed here in
 * vecmath, replicating Forge's exact operation order, and installed via
 * {@link IPerspectiveAwareModel} — the same mechanism as {@link GliderItemModel}.</p>
 *
 * <p>Residual difference vs 1.12.2: the hardcoded arm poses differ between versions
 * (1.9 hand rework) and cannot be compensated from an item model. The glider's
 * redstone-verbatim convergence proved the residual is small.</p>
 *
 * <p>Only THIRD_PERSON is remapped (the reported broken context); every other context
 * keeps today's behavior (identity, or the inner model's own perspective).</p>
 */
public final class HeldBlockPerspective {

	private HeldBlockPerspective() {}

	// 1.12.2 default-block THIRD_PERSON_RIGHT_HAND, folded exactly as Forge does:
	// M = T(0.5) . T(t) . Rx(75) . Ry(45) . S(0.375) . T(-0.5), t = (0, 2.5/16, 0).
	private static final Matrix4f THIRD_PERSON = buildThirdPerson();

	private static Matrix4f buildThirdPerson() {
		final float s = 0.375f;

		// 1.12 quatFromXYZ order: q = qx * qy (* qz = identity here).
		final Quat4f qx = new Quat4f();
		qx.set(new AxisAngle4d(1, 0, 0, Math.toRadians(75)));
		final Quat4f qy = new Quat4f();
		qy.set(new AxisAngle4d(0, 1, 0, Math.toRadians(45)));
		final Quat4f q = new Quat4f(0, 0, 0, 1);
		q.mul(qx);
		q.mul(qy);
		final Matrix4f r = new Matrix4f();
		r.set(q);

		final Matrix4f m = new Matrix4f();
		m.setIdentity();
		m.setTranslation(new Vector3f(0.5f, 0.5f + 2.5f / 16f, 0.5f));
		m.mul(r);

		final Matrix4f scale = new Matrix4f();
		scale.setIdentity();
		scale.m00 = s;
		scale.m11 = s;
		scale.m22 = s;
		m.mul(scale);

		final Matrix4f corner = new Matrix4f();
		corner.setIdentity();
		corner.setTranslation(new Vector3f(-0.5f, -0.5f, -0.5f));
		m.mul(corner);

		return m;
	}

	public static Pair<? extends IFlexibleBakedModel, Matrix4f> handlePerspective(
			IFlexibleBakedModel self, IBakedModel inner,
			ItemCameraTransforms.TransformType type) {
		if (type == ItemCameraTransforms.TransformType.THIRD_PERSON)
			return Pair.<IFlexibleBakedModel, Matrix4f>of(self, THIRD_PERSON);
		if (inner instanceof IPerspectiveAwareModel)
			return ((IPerspectiveAwareModel)inner).handlePerspective(type);
		return Pair.<IFlexibleBakedModel, Matrix4f>of(asFlexible(inner), new Matrix4f());
	}

	public static IFlexibleBakedModel asFlexible(IBakedModel model) {
		if (model instanceof IFlexibleBakedModel) return (IFlexibleBakedModel)model;
		return new FlexibleWrapper(model);
	}

	/** Adapts a plain baked model to the flexible interface by pure delegation. */
	private static final class FlexibleWrapper implements IFlexibleBakedModel {
		private final IBakedModel inner;

		FlexibleWrapper(IBakedModel inner) {
			this.inner = inner;
		}

		@Override
		public java.util.List<BakedQuad> getFaceQuads(EnumFacing facing) {
			return inner.getFaceQuads(facing);
		}

		@Override
		public java.util.List<BakedQuad> getGeneralQuads() {
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
		public net.minecraft.client.renderer.block.model.ItemCameraTransforms getItemCameraTransforms() {
			return inner.getItemCameraTransforms();
		}

		@Override
		public VertexFormat getFormat() {
			return DefaultVertexFormats.ITEM;
		}
	}
}
