package openblocks.client.bindings;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import openblocks.Config;
import openblocks.common.Vario;
import org.lwjgl.input.Keyboard;

public class KeyInputHandler {

	private KeyBinding varioSwitchBinding;
	private KeyBinding varioVolUpBinding;
	private KeyBinding varioVolDownBinding;

	private boolean varioSwitchKeyPressed;
	private boolean varioVolUpKeyPressed;
	private boolean varioVolDownKeyPressed;

	public void setup() {
		if (Config.hanggliderEnableThermal) {
			varioSwitchBinding = new KeyBinding("openblocks.keybind.vario_switch", Keyboard.KEY_V, "openblocks.keybind.category");
			varioVolUpBinding = new KeyBinding("openblocks.keybind.vario_vol_up", Keyboard.KEY_NONE, "openblocks.keybind.category");
			varioVolDownBinding = new KeyBinding("openblocks.keybind.vario_vol_down", Keyboard.KEY_NONE, "openblocks.keybind.category");
			ClientRegistry.registerKeyBinding(varioSwitchBinding);
			ClientRegistry.registerKeyBinding(varioVolUpBinding);
			ClientRegistry.registerKeyBinding(varioVolDownBinding);
		}

		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent evt) {
		if (varioSwitchBinding != null && varioSwitchBinding.isPressed()) {
			if (!varioSwitchKeyPressed) {
				Vario.instance.toggle();
				varioSwitchKeyPressed = true;
			}
		} else varioSwitchKeyPressed = false;
		if (varioVolUpBinding != null && varioVolUpBinding.isPressed()) {
			if (!varioVolUpKeyPressed) {
				Vario.instance.incVolume();
				varioVolUpKeyPressed = true;
			}
		} else varioVolUpKeyPressed = false;
		if (varioVolDownBinding != null && varioVolDownBinding.isPressed()) {
			if (!varioVolDownKeyPressed) {
				Vario.instance.decVolume();
				varioVolDownKeyPressed = true;
			}
		} else varioVolDownKeyPressed = false;
	}

}
