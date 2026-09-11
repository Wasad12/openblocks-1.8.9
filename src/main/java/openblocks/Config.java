package openblocks;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public class Config {

	public static boolean hanggliderEnableThermal = true;

	public static void init(File configFile) {
		Configuration config = new Configuration(configFile);
		config.load();

		hanggliderEnableThermal = config.get("hangglider", "enableThermal", true,
				"Enable a whole new level of hanggliding experience through thermal lift. See keybindings for acoustic vario controls").getBoolean(true);

		if (config.hasChanged()) config.save();
	}
}
