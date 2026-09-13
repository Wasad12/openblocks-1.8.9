package openblocks.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.util.ResourceLocation;

// 1.8.9 port of 1.12.2 EnchantmentLastStand: same armor coverage, max level 2,
// same enchantability curve. 1.8.9 adaptations: numeric-ID constructor (no
// Rarity enum pre-1.9 — UNCOMMON maps to weight 5, anvil precedent; no equipment
// slots pre-1.9 — the ARMOR type governs applicability), hardcoded ID 180 (above
// vanilla's max 62; documented free choice).
public class EnchantmentLastStand extends Enchantment {

	public static final int ENCHANTMENT_ID = 180;

	public EnchantmentLastStand() {
		super(ENCHANTMENT_ID, new ResourceLocation("openblocks:last_stand"), 5, EnumEnchantmentType.ARMOR);
		setName("openblocks.laststand");
	}

	@Override
	public int getMaxLevel() {
		return 2;
	}

	@Override
	public int getMinEnchantability(int level) {
		switch (level) {
			case 1:
				return 15;
			default:
				return 25;
		}
	}

	@Override
	public int getMaxEnchantability(int level) {
		return getMinEnchantability(level) + 10;
	}
}
