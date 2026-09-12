package openblocks.common.entity;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

// 1.8.9 port of 1.12.2 EntityXPOrbNoFly: vanilla orb motion with the player magnet
// stripped out (copied from vanilla 1.8.9 EntityXPOrb.onUpdate minus attraction).
// move() -> moveEntity(); burn sound is vanilla 1.8.9's orb lava sound (random.fizz).
public class EntityXPOrbNoFly extends EntityXPOrb {

	public EntityXPOrbNoFly(World world) {
		super(world);
		setSize(0.25f, 0.25f);
		this.motionX = this.motionZ = 0;
		this.motionY = -0.1f * world.rand.nextFloat();
	}

	public EntityXPOrbNoFly(World world, double x, double y, double z, int xp) {
		super(world, x, y, z, xp);
		setSize(0.25f, 0.25f);
		setPosition(x, y, z);
		this.motionX = this.motionZ = 0;
		this.motionY = -0.1f * world.rand.nextFloat();
	}

	@Override
	public void onUpdate() {
		final AxisAlignedBB aabb = getEntityBoundingBox();

		final double vx = motionX;
		final double vy = motionY;
		final double vz = motionZ;

		// let original update run
		super.onUpdate();
		if (isDead) return;

		// and then re-do motion calculations without player tracking

		setEntityBoundingBox(aabb);
		final AxisAlignedBB bb = getEntityBoundingBox();
		this.posX = (bb.minX + bb.maxX) / 2.0D;
		this.posY = bb.minY;
		this.posZ = (bb.minZ + bb.maxZ) / 2.0D;

		this.motionX = vx;
		this.motionY = vy;
		this.motionZ = vz;

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.motionY -= 0.029999999329447746D;

		final BlockPos pos = new BlockPos(this);
		if (this.worldObj.getBlockState(pos).getBlock().getMaterial() == Material.lava) {
			this.motionY = 0.20000000298023224D;
			this.motionX = (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F;
			this.motionZ = (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F;
			playSound("random.fizz", 0.4F, 2.0F + this.rand.nextFloat() * 0.4F);
		}

		pushOutOfBlocks(this.posX, (getEntityBoundingBox().minY + getEntityBoundingBox().maxY) / 2.0D, this.posZ);

		moveEntity(this.motionX, this.motionY, this.motionZ);
		final float f;

		if (this.onGround) {
			BlockPos underPos = new BlockPos(MathHelper.floor_double(this.posX), MathHelper.floor_double(getEntityBoundingBox().minY) - 1, MathHelper.floor_double(this.posZ));
			IBlockState underState = this.worldObj.getBlockState(underPos);
			// 1.8.9 slipperiness is a public field (the methods are 1.9+).
			f = underState.getBlock().slipperiness * 0.98F;
		} else {
			f = 0.98F;
		}

		this.motionX *= f;
		this.motionY *= 0.98;
		this.motionZ *= f;

		if (this.onGround) {
			this.motionY *= -0.8999999761581421D;
		}
	}
}
