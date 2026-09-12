package openmods.utils;

import com.google.common.base.Optional;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import org.apache.commons.lang3.StringUtils;

// 1.8.9 port of 1.12.2 VanillaAnvilLogic (itself adapted from ContainerRepair).
// 1.8.9 adaptations (all VERIFIED via javap on the 1722 forgeBin jar): enchantment
// maps are ID-based (Map<Integer,Integer> — the Enchantment-keyed form is 1.9+), so
// every map key goes through Enchantment.getEnchantmentById; isCompatibleWith does
// not exist — canApplyTogether is its 1.8.9 name; getRarity does not exist (the
// Rarity enum is 1.9+) — cost uses getWeight() with thresholds >=10→1 / >=5→2 /
// >=2→4 / else 8, which agrees exactly with COMMON/UNCOMMON/RARE/VERY_RARE→1/2/4/8
// for every vanilla enchant (weights are 10/5/2/1); AnvilUpdateEvent has PUBLIC
// fields (output/cost/materialCost, null-able output — no getters yet);
// ItemEnchantedBook.getEnchantments is an instance method returning NBTTagList;
// getCount/shrink/isEmpty/EMPTY become stackSize/manual/null. Null-tolerant: null
// input takes the empty path, null modifier takes the no-modifier path (1.12.2
// relied on the EMPTY singleton for both).
public class VanillaAnvilLogic {

	private int materialCost;
	private int maximumCost;

	private ItemStack outputStack = null;

	public VanillaAnvilLogic(ItemStack inputStack, ItemStack modifierStack, boolean isCreativeMode, Optional<String> itemName) {
		this.materialCost = -1;
		this.outputStack = null;

		final String repairedItemName = itemName.orNull();

		// adapted/copied from ContainerRepair.updateRepairOutput
		this.maximumCost = 1;
		int i = 0;
		int j = 0;
		int k = 0;

		if (inputStack == null) {
			outputStack = null;
			this.maximumCost = 0;
		} else {
			ItemStack itemstack1 = inputStack.copy();
			ItemStack itemstack2 = modifierStack;
			Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
			j = j + inputStack.getRepairCost() + (itemstack2 == null? 0 : itemstack2.getRepairCost());
			this.materialCost = 0;
			boolean flag = false;

			if (itemstack2 != null) {
				if (!onAnvilChange(inputStack, itemstack2, repairedItemName, j)) return;
				flag = itemstack2.getItem() == Items.enchanted_book && ((ItemEnchantedBook)itemstack2.getItem()).getEnchantments(itemstack2).tagCount() > 0;

				if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(inputStack, itemstack2)) {
					int l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

					if (l2 <= 0) {
						outputStack = null;
						this.maximumCost = 0;
						return;
					}

					int i3;

					for (i3 = 0; l2 > 0 && i3 < itemstack2.stackSize; ++i3) {
						int j3 = itemstack1.getItemDamage() - l2;
						itemstack1.setItemDamage(j3);
						++i;
						l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
					}

					this.materialCost = i3;
				} else {
					if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
						outputStack = null;
						this.maximumCost = 0;
						return;
					}

					if (itemstack1.isItemStackDamageable() && !flag) {
						int l = inputStack.getMaxDamage() - inputStack.getItemDamage();
						int i1 = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
						int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
						int k1 = l + j1;
						int l1 = itemstack1.getMaxDamage() - k1;

						if (l1 < 0) {
							l1 = 0;
						}

						if (l1 < itemstack1.getMetadata()) {
							itemstack1.setItemDamage(l1);
							i += 2;
						}
					}

					Map<Integer, Integer> map1 = EnchantmentHelper.getEnchantments(itemstack2);
					boolean flag2 = false;
					boolean flag3 = false;

					for (Integer enchId : map1.keySet()) {
						Enchantment enchantment1 = Enchantment.getEnchantmentById(enchId.intValue());
						if (enchantment1 != null) {
							int i2 = map.containsKey(enchId)? map.get(enchId).intValue() : 0;
							int j2 = map1.get(enchId).intValue();
							j2 = i2 == j2? j2 + 1 : Math.max(j2, i2);
							boolean flag1 = enchantment1.canApply(inputStack);

							if (isCreativeMode || inputStack.getItem() == Items.enchanted_book) {
								flag1 = true;
							}

							for (Integer otherId : map.keySet()) {
								if (!otherId.equals(enchId)) {
									Enchantment other = Enchantment.getEnchantmentById(otherId.intValue());
									if (other != null && !enchantment1.canApplyTogether(other)) {
										flag1 = false;
										++i;
									}
								}
							}

							if (!flag1) {
								flag3 = true;
							} else {
								flag2 = true;

								if (j2 > enchantment1.getMaxLevel()) {
									j2 = enchantment1.getMaxLevel();
								}

								map.put(enchId, Integer.valueOf(j2));
								int k3 = 0;

								final int weight = enchantment1.getWeight();
								if (weight >= 10) k3 = 1;
								else if (weight >= 5) k3 = 2;
								else if (weight >= 2) k3 = 4;
								else k3 = 8;

								if (flag) {
									k3 = Math.max(1, k3 / 2);
								}

								i += k3 * j2;

								if (inputStack.stackSize > 1) {
									i = 40;
								}
							}
						}
					}

					if (flag3 && !flag2) {
						outputStack = null;
						this.maximumCost = 0;
						return;
					}
				}
			}

			if (StringUtils.isBlank(repairedItemName)) {
				if (inputStack.hasDisplayName()) {
					k = 1;
					i += k;
					itemstack1.clearCustomName();
				}
			} else if (!repairedItemName.equals(inputStack.getDisplayName())) {
				k = 1;
				i += k;
				itemstack1.setStackDisplayName(repairedItemName);
			}
			if (flag && !itemstack1.getItem().isBookEnchantable(itemstack1, itemstack2)) itemstack1 = null;

			this.maximumCost = j + i;

			if (i <= 0) {
				itemstack1 = null;
			}

			if (k == i && k > 0 && this.maximumCost >= 40) {
				this.maximumCost = 39;
			}

			if (this.maximumCost >= 40 && !isCreativeMode) {
				itemstack1 = null;
			}

			if (itemstack1 != null) {
				int k2 = itemstack1.getRepairCost();

				if (itemstack2 != null && k2 < itemstack2.getRepairCost()) {
					k2 = itemstack2.getRepairCost();
				}

				if (k != i || k == 0) {
					k2 = k2 * 2 + 1;
				}

				itemstack1.setRepairCost(k2);
				EnchantmentHelper.setEnchantments(map, itemstack1);
			}

			outputStack = itemstack1;
		}
	}

	private boolean onAnvilChange(ItemStack inputItem, ItemStack modifierItem, String itemName, int baseCost) {
		AnvilUpdateEvent e = new AnvilUpdateEvent(inputItem, modifierItem, itemName, baseCost);
		if (MinecraftForge.EVENT_BUS.post(e)) return false;
		if (e.output != null) {
			this.outputStack = e.output;
			this.maximumCost = e.cost;
			this.materialCost = e.materialCost;
			return false;
		}
		return true;
	}

	public int getModifierCost() {
		return materialCost;
	}

	public int getLevelCost() {
		return maximumCost;
	}

	public ItemStack getOutputStack() {
		return outputStack;
	}
}
