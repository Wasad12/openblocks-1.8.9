package openblocks.common;

import com.google.common.base.Preconditions;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openblocks.Config;
import openblocks.api.ElevatorCheckEvent;
import openblocks.api.IElevatorBlock;
import openblocks.api.IElevatorBlock.PlayerRotation;
import openblocks.common.network.OpenBlocksChannel;
import openmods.movement.PlayerMovementEvent;
import openmods.utils.EnchantmentUtils;

// 1.8.9 port of 1.12.2 ElevatorActionHandler: column scan, pass-through counting,
// XP gate (free at the default ratio 0 — the user's no-XP requirement), teleport
// and sound are verbatim. Two 1.8.9 adaptations: the lib C2S event is replaced by
// a native SimpleNetworkWrapper message (ID_ELEVATOR_ACTION on OpenBlocksChannel),
// and the lib's MovementInputUpdateEvent trigger (VERIFIED absent in 1.8.9 Forge)
// is replaced by rising-edge polling of movementInput in the client tick — same
// edge semantics, input never suppressed, exactly like 1.12.2. The cancelable
// PlayerMovementEvent bus seam is kept verbatim.
public class ElevatorActionHandler {

	private static class SearchResult extends BlockPos {
		public final PlayerRotation rotation;

		public SearchResult(Vec3i other, PlayerRotation rotation) {
			super(other);
			this.rotation = rotation;
		}
	}

	private static boolean canTeleportPlayer(World world, BlockPos pos) {
		if (world.isAirBlock(pos)) return true;

		if (!Config.irregularBlocksArePassable) return false;
		final IBlockState blockState = world.getBlockState(pos);
		final AxisAlignedBB aabb = blockState.getBlock().getCollisionBoundingBox(world, pos, blockState);
		return aabb == null || aabb.getAverageEdgeLength() < 0.7;
	}

	private static boolean canTeleportPlayer(EntityPlayer entity, World world, BlockPos pos) {
		final AxisAlignedBB aabb = entity.getEntityBoundingBox();
		double height = Math.abs(aabb.maxY - aabb.minY);
		int blockHeight = Math.max(1, MathHelper.ceiling_float_int((float)height));

		for (int dy = 0; dy < blockHeight; dy++)
			if (!canTeleportPlayer(world, pos.up(dy))) return false;

		return true;
	}

	private static ElevatorCheckEvent checkIsElevator(EntityPlayer player, World world, BlockPos pos, IBlockState state) {
		final ElevatorCheckEvent evt = new ElevatorCheckEvent(world, pos, state, player);

		final Block block = state.getBlock();
		if (block instanceof IElevatorBlock) {
			final IElevatorBlock elevatorBlock = (IElevatorBlock)block;
			evt.setColor(elevatorBlock.getColor(world, pos, state));
			evt.setRotation(elevatorBlock.getRotation(world, pos, state));
		}

		ElevatorBlockRules.instance.configureEvent(evt);

		MinecraftForge.EVENT_BUS.post(evt);

		return evt;
	}

	private static SearchResult findLevel(EntityPlayer player, World world, EnumDyeColor thisColor, BlockPos pos, EnumFacing searchDirection) {
		Preconditions.checkArgument(searchDirection == EnumFacing.UP
				|| searchDirection == EnumFacing.DOWN, "Must be either up or down... for now");

		int blocksInTheWay = 0;
		BlockPos searchPos = pos;
		for (int i = 0; i < Config.elevatorTravelDistance; i++) {
			searchPos = searchPos.offset(searchDirection);
			if (!world.isBlockLoaded(searchPos)) break;
			if (world.isAirBlock(searchPos)) continue;

			final IBlockState blockState = world.getBlockState(searchPos);
			final ElevatorCheckEvent elevatorCheckResult = checkIsElevator(player, world, searchPos, blockState);

			if (elevatorCheckResult.isElevator()) {
				final EnumDyeColor otherColor = elevatorCheckResult.getColor();
				if (otherColor == thisColor && canTeleportPlayer(player, world, searchPos.up())) {
					final PlayerRotation rotation = elevatorCheckResult.getRotation();
					return new SearchResult(searchPos, rotation);
				}
			}

			if (!Config.elevatorIgnoreBlocks) {
				ElevatorBlockRules.Action action = ElevatorBlockRules.instance.getActionForBlock(blockState);
				switch (action) {
					case ABORT:
						return null;
					case IGNORE:
						continue;
					case INCREMENT:
					default:
						break;
				}

				if (++blocksInTheWay > Config.elevatorMaxBlockPassCount) break;
			}
		}

		return null;
	}

	private static void activate(EntityPlayer player, World world, EnumDyeColor color, BlockPos pos, EnumFacing dir) {
		SearchResult result = findLevel(player, world, color, pos, dir);
		if (result != null) {
			boolean doTeleport = checkXpCost(player, result);

			if (doTeleport) {
				if (result.rotation != PlayerRotation.NONE) player.rotationYaw = getYaw(result.rotation);
				if (Config.elevatorCenter) player.setPositionAndUpdate(result.getX() + 0.5, result.getY() + 1.1, result.getZ() + 0.5);
				else player.setPositionAndUpdate(player.posX, result.getY() + 1.1, player.posZ);
				world.playSoundEffect(result.getX() + 0.5, result.getY() + 1, result.getZ() + 0.5, "openblocks:elevator.activate", 1, 1);
			}
		}
	}

	private static float getYaw(PlayerRotation rotation) {
		switch (rotation) {
			case EAST:
				return 90;
			case NORTH:
				return 0;
			case SOUTH:
				return 180;
			case WEST:
				return -90;
			default:
				return 0;
		}
	}

	protected static boolean checkXpCost(EntityPlayer player, SearchResult result) {
		int distance = (int)Math.abs(player.posY - result.getY());
		if (Config.elevatorXpDrainRatio == 0 || player.capabilities.isCreativeMode) return true;

		int playerXP = EnchantmentUtils.getPlayerXP(player);
		int neededXP = MathHelper.ceiling_float_int(Config.elevatorXpDrainRatio * distance);
		if (playerXP >= neededXP) {
			EnchantmentUtils.addPlayerXP(player, -neededXP);
			return true;
		}

		return false;
	}

	/** Server entry: runs the 1.12.2 onElevatorEvent logic for a C2S direction packet. */
	public static void handleAction(EntityPlayerMP player, boolean jump) {
		if (player == null) return;

		final World world = player.worldObj;
		if (world == null) return;

		final int x = MathHelper.floor_double(player.posX);
		final int y = MathHelper.floor_double(player.getEntityBoundingBox().minY) - 1;
		final int z = MathHelper.floor_double(player.posZ);
		final BlockPos blockPos = new BlockPos(x, y, z);

		if (player.isRiding()) return;

		final IBlockState blockState = world.getBlockState(blockPos);
		final ElevatorCheckEvent elevatorCheckResult = checkIsElevator(player, world, blockPos, blockState);

		if (elevatorCheckResult.isElevator()) {
			if (jump) activate(player, world, elevatorCheckResult.getColor(), blockPos, EnumFacing.UP);
			else activate(player, world, elevatorCheckResult.getColor(), blockPos, EnumFacing.DOWN);
		}
	}

	private boolean wasJumping;
	private boolean wasSneaking;

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientTick(TickEvent.ClientTickEvent evt) {
		if (evt.phase != Phase.END) return;
		// 1.8.9 has no MovementInputUpdateEvent: same rising-edge semantics by poll.
		final net.minecraft.client.entity.EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if (player == null || player.movementInput == null) {
			wasJumping = false;
			wasSneaking = false;
			return;
		}

		final boolean jump = player.movementInput.jump;
		final boolean sneak = player.movementInput.sneak;
		if (jump && !wasJumping) postMovement(player, PlayerMovementEvent.Type.JUMP);
		if (sneak && !wasSneaking) postMovement(player, PlayerMovementEvent.Type.SNEAK);
		wasJumping = jump;
		wasSneaking = sneak;
	}

	@SideOnly(Side.CLIENT)
	private static void postMovement(EntityPlayer player, PlayerMovementEvent.Type type) {
		if (!MinecraftForge.EVENT_BUS.post(new PlayerMovementEvent(player, type)))
			OpenBlocksChannel.sendElevatorAction(type == PlayerMovementEvent.Type.JUMP);
	}
}
