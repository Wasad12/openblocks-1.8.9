package openblocks.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import openblocks.common.ElevatorActionHandler;
import openmods.Log;

// 1.8.9 replacement for the lib ElevatorActionEvent C2S packet: same payload (jump
// vs sneak), native SimpleNetworkWrapper envelope (established OpenBlocksChannel
// pattern). Server runs the ported handler logic on the server thread.
public class MessageElevatorAction implements IMessage {

	private boolean jump;

	@SuppressWarnings("unused")
	public MessageElevatorAction() {}

	public MessageElevatorAction(boolean jump) {
		this.jump = jump;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		jump = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(jump);
	}

	public static class Handler implements IMessageHandler<MessageElevatorAction, IMessage> {
		@Override
		public IMessage onMessage(final MessageElevatorAction message, final MessageContext ctx) {
			final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			final WorldServer world = player.getServerForPlayer();
			world.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					try {
						ElevatorActionHandler.handleAction(player, message.jump);
					} catch (Exception e) {
						Log.warn(e, "Failed to handle elevator action");
					}
				}
			});
			return null;
		}
	}
}
