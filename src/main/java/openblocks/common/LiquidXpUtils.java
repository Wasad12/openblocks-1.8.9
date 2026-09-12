package openblocks.common;

import openblocks.Config;

// Minimal 1.8.9 equivalent of 1.12.2 LiquidXpUtils: only the xp<->liquid ratio math
// the Tank needs. FLUID_TO_LEVELS (GUI display) dropped per §19 (no Tank GUI).
public class LiquidXpUtils {

	public static int liquidToXpRatio(int liquid) {
		return liquid / Config.xpToLiquidRatio;
	}

	public static int xpToLiquidRatio(int xp) {
		return xp * Config.xpToLiquidRatio;
	}
}
