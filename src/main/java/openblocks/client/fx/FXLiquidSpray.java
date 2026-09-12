package openblocks.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

// 1.8.9 port of 1.12.2 FXLiquidSpray. setParticleIcon EXISTS in 1.8.9 (VERIFIED via
// javap) and layer 1 renders from the block atlas like vanilla dig particles, so the
// port is direct; canCollide has no 1.8.9 field and is dropped (moveEntity collides
// by default). Sprite lookup mirrors the tank TESR.
public class FXLiquidSpray extends EntityFX {

	public FXLiquidSpray(World world, FluidStack fluid, double x, double y, double z, float scale, float gravity, Vec3 velocity) {
		this(world, Minecraft.getMinecraft().getTextureMapBlocks()
				.getAtlasSprite(fluid.getFluid().getStill(fluid).toString()),
				x, y, z, scale, gravity, velocity);
	}

	public FXLiquidSpray(World world, TextureAtlasSprite icon, double x, double y, double z, float scale, float gravity, Vec3 velocity) {
		super(world, x, y, z, velocity.xCoord, velocity.yCoord, velocity.zCoord);

		particleGravity = gravity;
		this.particleMaxAge = 50;
		setSize(0.2f, 0.2f);
		this.particleScale = scale;
		motionX = velocity.xCoord;
		motionY = velocity.yCoord;
		motionZ = velocity.zCoord;

		setParticleIcon(icon);
	}

	@Override
	public int getFXLayer() {
		return 1;
	}
}
