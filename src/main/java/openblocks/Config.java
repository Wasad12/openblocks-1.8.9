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

	public static boolean lastStandEnchantmentEnabled = true;
	public static String lastStandEnchantmentFormula = "max(1, 50*(1-(hp-dmg))/ench)";

	public static int elevatorTravelDistance = 20;
	public static boolean elevatorIgnoreBlocks = false;
	public static boolean elevatorIgnoreHalfBlocks = false;
	public static int elevatorMaxBlockPassCount = 4;
	public static boolean elevatorCenter = false;
	public static String[] elevatorRules = new String[0];
	public static float elevatorXpDrainRatio = 0;
	public static boolean irregularBlocksArePassable = true;

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

		lastStandEnchantmentEnabled = config.get("features", "lastStandEnchantment", true,
				"Is 'Last Stand' enchantment enabled").getBoolean(true);
		lastStandEnchantmentFormula = config.get("features", "lastStandFormula",
				"max(1, 50*(1-(hp-dmg))/ench)",
				"Formula for XP cost (variables: hp,dmg,ench,xp). Note: calculation only triggers when hp - dmg < 1.").getString();

		elevatorTravelDistance = config.get("dropblock", "searchDistance", 20,
				"The range of the drop block").getInt(20);
		elevatorIgnoreBlocks = config.get("dropblock", "ignoreAllBlocks", false,
				"Disable limit of blocks between elevators (equivalent to maxPassThrough = infinity)").getBoolean(false);
		elevatorIgnoreHalfBlocks = config.get("dropblock", "ignoreHalfBlocks", false,
				"The elevator will ignore half blocks when counting the blocks it can pass through").getBoolean(false);
		elevatorMaxBlockPassCount = config.get("dropblock", "maxPassThrough", 4,
				"The maximum amount of blocks the elevator can pass through before the teleport fails").getInt(4);
		elevatorCenter = config.get("dropblock", "centerOnBlock", false,
				"Should elevator move player to center of block after teleporting").getBoolean(false);
		elevatorRules = config.get("dropblock", "specialBlockRules", new String[0],
				"Defines blocks that are handled specially by elevators. Entries are in form <modId>:<blockName>:<action> or id:<blockId>:<action>. Possible actions: abort (elevator can't pass block), increment (counts for elevatorMaxBlockPassCount limit) and ignore").getStringList();
		elevatorXpDrainRatio = (float)config.get("dropblock", "elevatorXpDrainRatio", 0,
				"XP consumed by elevator (total amount = ratio * distance)").getDouble(0);
		irregularBlocksArePassable = config.get("dropblock", "irregularBlocksArePassable", true,
				"The elevator will try to pass through blocks that have custom collision boxes").getBoolean(true);

		if (config.hasChanged()) config.save();
	}
}
