package openblocks.common.tileentity;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ITickable;
import net.minecraft.util.Vec3;
import openblocks.Config;
import openmods.sync.SyncableByte;
import openmods.sync.SyncableFloat;
import openmods.tileentity.SyncedTileEntity;
import openmods.api.INeighbourAwareTile;

// 1.8.9 port of 1.12.2 TileEntityFan: same angle/power sync fields, cone suction
// physics, redstone power, sneak-click angle adjust, blade spin. 1.8.9 adaptations:
// plain SyncedTileEntity + INeighbourAwareTile (lib tile interfaces inlined — the
// block calls onBlockPlacedBy/onAdded/onNeighbourChanged/onBlockActivated directly,
// hopper pattern), worldObj (nullable-stack era), Vec3d -> Vec3 (all methods
// VERIFIED present in 1.8.9), AABB.grow -> expand (hopper precedent). Dropped: the
// EvalModelState render state (no eval system on 1.8.9 — the TESR reads angle and
// blade rotation directly) and with it the angle sync listener (nothing static
// depends on the angle anymore). One deliberate addition: onBlockPlacedBy syncs
// server-side, closing the 1.12.2 race where the onAdded-time sync fires before the
// angle is set (stale head direction until the next redstone change).
public class TileEntityFan extends SyncedTileEntity implements INeighbourAwareTile, ITickable {

	private static final int ANGLE_SPEED_PER_REDSTONE_POWER = 45;
	private static final double CONE_HALF_APERTURE = 1.2 / 2.0;

	private SyncableFloat angle;
	private SyncableByte power;
	private float bladeRotation;
	private float bladeSpeed;

	public TileEntityFan() {}

	@Override
	protected void createSyncedFields() {
		angle = new SyncableFloat();
		power = new SyncableByte();
	}

	@Override
	public void update() {
		float redstonePower = power.get() / 15.0f;

		bladeSpeed = ANGLE_SPEED_PER_REDSTONE_POWER * redstonePower;
		bladeRotation += bladeSpeed;

		final double maxForce = Config.fanForce * redstonePower;
		if (maxForce <= 0) return;

		List<Entity> entities = worldObj.getEntitiesWithinAABB(Entity.class, getEntitySearchBoundingBox());
		if (entities.isEmpty()) return;

		double angle = Math.toRadians(getAngle() - 90);
		final Vec3 blockPos = getConeApex(angle);
		final Vec3 basePos = getConeBaseCenter(angle);
		final Vec3 coneAxis = new Vec3(basePos.xCoord - blockPos.xCoord, basePos.yCoord - blockPos.yCoord, basePos.zCoord - blockPos.zCoord);

		for (Entity entity : entities) {
			if (entity instanceof EntityPlayer && ((EntityPlayer)entity).capabilities.isCreativeMode) continue;
			Vec3 directionVec = new Vec3(
					entity.posX - blockPos.xCoord,
					entity.posY - blockPos.yCoord,
					entity.posZ - blockPos.zCoord);

			if (isLyingInSphericalCone(coneAxis, directionVec, CONE_HALF_APERTURE)) {
				final double distToOrigin = directionVec.lengthVector();
				final double force = (1.0 - distToOrigin / Config.fanRange) * maxForce;
				if (force <= 0) continue;
				Vec3 normal = directionVec.normalize();
				entity.motionX += force * normal.xCoord;
				entity.motionZ += force * normal.zCoord;
			}
		}
	}

	private Vec3 getConeBaseCenter(double angle) {
		// TODO this may be semi-constant
		return new Vec3(pos.getX(), pos.getY(), pos.getZ())
				.addVector(
						(Math.cos(angle) * Config.fanRange),
						0.5,
						(Math.sin(angle) * Config.fanRange));
	}

	private Vec3 getConeApex(double angle) {
		return new Vec3(pos.getX(), pos.getY(), pos.getZ())
				.addVector(
						0.5 - Math.cos(angle) * 1.1,
						0.5,
						0.5 - Math.sin(angle) * 1.1);
	}

	private AxisAlignedBB getEntitySearchBoundingBox() {
		// 1.12.2 BlockUtils.aabbOffset(pos, 0, -2, 0, +1, +3, +1) inlined.
		AxisAlignedBB boundingBox = new AxisAlignedBB(
				pos.getX() + 0, pos.getY() - 2, pos.getZ() + 0,
				pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1);
		return boundingBox.expand(Config.fanRange, Config.fanRange, Config.fanRange);
	}

	private static boolean isLyingInSphericalCone(Vec3 coneAxis, Vec3 originToTarget, double halfAperture) {
		double angleToAxisCos = originToTarget.dotProduct(coneAxis) / originToTarget.lengthVector() / coneAxis.lengthVector();
		return angleToAxisCos > Math.cos(halfAperture);
	}

	public void onBlockPlacedBy(IBlockState state, EntityLivingBase placer, ItemStack stack) {
		final float placeAngle = placer.rotationYawHead;
		angle.set(placeAngle);
		// 1.8.9 Chunk.setBlockState fires onBlockAdded BEFORE creating the TE
		// (bytecode order PROVED via javap), so onAdded misses at placement and
		// power would never initialize — init it here instead (server-guarded).
		updateRedstone();
		if (!worldObj.isRemote) sync();
	}

	public float getAngle() {
		return angle.get();
	}

	public float getBladeRotation(float partialTickTime) {
		return (bladeRotation + bladeSpeed * partialTickTime) % 360;
	}

	@Override
	public void onNeighbourChanged(BlockPos neighbourPos, Block neighbourBlock) {
		updateRedstone();
	}

	public void onAdded() {
		updateRedstone();
	}

	private void updateRedstone() {
		if (!worldObj.isRemote) {
			int power = Config.redstoneActivatedFan? worldObj.isBlockIndirectlyGettingPowered(pos) : 15;
			this.power.set((byte)power);
			sync();
		}
	}

	public boolean onBlockActivated(EntityPlayer player) {
		if (!worldObj.isRemote) {
			angle.set(angle.get() + (player.isSneaking()? -10f : +10f));
			sync();
			return true;
		}

		return false;
	}
}
