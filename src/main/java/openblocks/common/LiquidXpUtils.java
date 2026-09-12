package openblocks.common;

import net.minecraftforge.fluids.FluidStack;
import openblocks.Config;
import openmods.utils.EnchantmentUtils;

// 1.8.9 equivalent of 1.12.2 LiquidXpUtils: ratio math (as before) plus the
// level helpers the Auto Enchantment Table needs (getLiquidForLevel for the tank
// capacity; levelsForDisplay instead of the guava-Function FLUID_TO_LEVELS — same
// math, no guava Function needed).
public class LiquidXpUtils {

	public static int liquidToXpRatio(int liquid) {
		return liquid / Config.xpToLiquidRatio;
	}

	public static int xpToLiquidRatio(int xp) {
		return xp * Config.xpToLiquidRatio;
	}

	public static int getLiquidForLevel(int level) {
		final int xp = EnchantmentUtils.getExperienceForLevel(level);
		return xpToLiquidRatio(xp);
	}

	public static FluidStack levelsForDisplay(FluidStack input) {
		if (input == null) return null;
		// display levels instead of actual xp fluid level
		final FluidStack result = input.copy();
		result.amount = EnchantmentUtils.getLevelForExperience(liquidToXpRatio(input.amount));
		return result;
	}
}
