package openmods.movement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

// Verbatim port of OpenModsLib 1.12.2 PlayerMovementEvent: cancelable jump/sneak
// edge seam. On 1.12.2 the lib posts it from MovementInputUpdateEvent (absent in
// 1.8.9 Forge); here ElevatorActionHandler polls the rising edges itself.
@Cancelable
public class PlayerMovementEvent extends PlayerEvent {

	public enum Type {
		JUMP,
		SNEAK;
	}

	public Type type;

	public PlayerMovementEvent(EntityPlayer player, Type type) {
		super(player);
		this.type = type;
	}

}
