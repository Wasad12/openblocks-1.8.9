package openblocks;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public class Config {

	public static boolean hanggliderEnableThermal = true;

	public static int bucketsPerTank = 16;
	public static boolean shouldTanksUpdate = true;
	public static boolean displayAllFilledTanks = true;
	public static int tankFluidUpdateThreshold = 0;
	public static boolean tanksEmitLight = true;

	public static int xpToLiquidRatio = 20;

	public static double fanForce = 0.05;
	public static double fanRange = 10;
	public static boolean redstoneActivatedFan = true;

	public static void init(File configFile) {
		Configuration config = new Configuration(configFile);
		config.load();

		hanggliderEnableThermal = config.get("hangglider", "enableThermal", true,
				"Enable a whole new level of hanggliding experience through thermal lift. See keybindings for acoustic vario controls").getBoolean(true);

		bucketsPerTank = config.get("tanks", "bucketsPerTank", 16,
				"The amount of buckets each tank can hold").getInt(16);
		shouldTanksUpdate = config.get("tanks", "tankTicks", true,
				"Should tanks try to balance liquid amounts with neighbours").getBoolean(true);
		displayAllFilledTanks = config.get("tanks", "displayAllFluids", true,
				"Should filled tanks be searchable with creative menu").getBoolean(true);
		tankFluidUpdateThreshold = config.get("tanks", "fluidDifferenceUpdateThreshold", 0,
				"Minimal difference in fluid level between neigbors required for tank update (can be used for performance finetuning").getInt(0);
		tanksEmitLight = config.get("tanks", "emitLight", true,
				"Tanks will emit light when they contain a liquid that glows (eg. lava)").getBoolean(true);

		xpToLiquidRatio = config.get("features", "xpToLiquidRatio", 20,
				"Storage in mB needed to store single XP point").getInt(20);

		fanForce = config.get("fan", "fanForce", 0.05,
				"Maximum force applied every tick to entities nearby (linear decay)").getDouble(0.05);
		fanRange = config.get("fan", "fanRange", 10,
				"Range of fan in blocks").getDouble(10);
		redstoneActivatedFan = config.get("fan", "isRedstoneActivated", true,
				"Is fan force controlled by redstone current").getBoolean(true);

		if (config.hasChanged()) config.save();
	}
}
