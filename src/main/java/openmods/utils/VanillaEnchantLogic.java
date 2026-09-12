package openmods.utils;

import java.util.List;
import java.util.Random;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;

// 1.8.9 port of OpenModsLib VanillaEnchantLogic: same seed math and enchant flow.
// 1.8.9 adaptations: nullable stacks (no isEmpty/EMPTY), 3-arg buildEnchantmentList
// (1.8.9 has no treasure flag — no treasure enchants exist there), enchantmentobj
// field name, instance ItemEnchantedBook.addEnchantment. javax.annotation dropped
// (project convention).
public class VanillaEnchantLogic {

	public enum Level {
		L1, L2, L3
	}

	private final long seed;

	private final Random rand = new Random();

	public VanillaEnchantLogic(long seed) {
		this.seed = seed;
	}

	private ItemStack toEnchant = null;

	private Level level;

	private int xpLevels;

	public boolean setup(ItemStack stack, Level level, int power) {
		if (stack == null || !stack.isItemEnchantable()) return false;

		rand.setSeed(seed);
		this.toEnchant = stack.copy();
		this.level = level;
		this.xpLevels = EnchantmentHelper.calcItemStackEnchantability(rand, level.ordinal(), power, toEnchant);

		if (this.xpLevels <= level.ordinal() + 1) this.xpLevels = 0;
		return true;
	}

	public int getLevelCost() {
		return level.ordinal() + 1;
	}

	public int getLevelRequirement() {
		return xpLevels;
	}

	public int getLapisCost() {
		return level.ordinal() + 1;
	}

	public ItemStack enchant() {
		if (toEnchant == null)
			return null;

		ItemStack enchantedItem = toEnchant.copy();
		final boolean isBook = enchantedItem.getItem() == Items.book;

		final List<EnchantmentData> enchantmentsToApply = getEnchantmentList(toEnchant, level, xpLevels);
		if (!enchantmentsToApply.isEmpty()) {
			if (isBook) {
				enchantedItem = new ItemStack(Items.enchanted_book);
			}

			for (EnchantmentData enchantment : enchantmentsToApply) {
				if (isBook) {
					((ItemEnchantedBook)enchantedItem.getItem()).addEnchantment(enchantedItem, enchantment);
				} else {
					enchantedItem.addEnchantment(enchantment.enchantmentobj, enchantment.enchantmentLevel);
				}
			}
		}

		return enchantedItem;
	}

	private List<EnchantmentData> getEnchantmentList(ItemStack stack, Level level, int xpLevels) {
		rand.setSeed(seed + level.ordinal());
		List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(rand, stack, xpLevels);

		if (stack.getItem() == Items.book && list.size() > 1) {
			list.remove(this.rand.nextInt(list.size()));
		}

		return list;
	}

}
