package openblocks.enchantments;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.Config;
import openblocks.OpenBlocks;
import openmods.utils.EnchantmentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// 1.8.9 port of 1.12.2 LastStandEnchantmentsHandler: same near-death XP-for-health
// trade (lethal hit with levels worn + enough XP -> health set to 1, XP drained,
// hit canceled), same variables and default formula. 1.8.9 adaptations:
// LivingHurtEvent is field-based (public ammount field — note the vanilla typo —
// no accessors, PROVED via javap); getEnchantmentLevel takes the numeric effectId
// (VERIFIED int-based); armor iterated via getEquipmentInSlot (no
// getArmorInventoryList pre-1.9, INFERRED). The info.openmods.calc expression
// engine is an external shaded lib, unavailable to the offline 1.8.9 build, so the
// configured formula evaluates on JDK8 Nashorn (max/min/sqrt/abs/pow/floor/ceil/
// round prebound; the default formula is valid JS under those bindings with a
// bit-identical result), compiled once per distinct formula string; ANY failure
// falls back to the inline default math — the same evaluate-with-fallback
// structure as 1.12.2. No live-reload listener (our Config has no config-change
// events — restart to apply formula edits).
public class LastStandEnchantmentsHandler {

	private static final Logger LOG = LogManager.getLogger();

	private static final String VAR_ENCH_LEVEL = "ench";
	private static final String VAR_PLAYER_XP = "xp";
	private static final String VAR_PLAYER_HP = "hp";
	private static final String VAR_DAMAGE = "dmg";

	private static final String MATH_PRELUDE =
			"var max=Math.max;var min=Math.min;var sqrt=Math.sqrt;var abs=Math.abs;"
			+ "var pow=Math.pow;var floor=Math.floor;var ceil=Math.ceil;var round=Math.round;";

	private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
	private String compiledFormula;
	private CompiledScript compiled;

	public LastStandEnchantmentsHandler() {}

	@SubscribeEvent
	public void onHurt(final LivingHurtEvent e) {
		if (!(e.entityLiving instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer)e.entityLiving;

		final int enchantmentLevels = countLastStandEnchantmentLevels(player);

		if (enchantmentLevels > 0) {
			final float playerHealth = player.getHealth();
			final float healthAvailable = playerHealth - e.ammount;

			if (healthAvailable < 1f) {
				final int xpAvailable = EnchantmentUtils.getPlayerXP(player);
				final float xpRequired = evaluateFormula(enchantmentLevels, xpAvailable, playerHealth, e.ammount);

				if (xpAvailable >= xpRequired) {
					player.setHealth(1f);
					EnchantmentUtils.addPlayerXP(player, -(int)xpRequired);
					e.ammount = 0;
					e.setCanceled(true);
				}
			}
		}
	}

	private float evaluateFormula(int enchantmentLevels, int xpAvailable, float playerHealth, float damage) {
		final String formula = Config.lastStandEnchantmentFormula;
		if (!formula.equals(compiledFormula)) {
			compiledFormula = formula;
			compiled = tryCompile(formula);
		}

		if (compiled != null) {
			try {
				final Bindings bindings = new SimpleBindings();
				bindings.put(VAR_ENCH_LEVEL, (double)enchantmentLevels);
				bindings.put(VAR_PLAYER_XP, (double)xpAvailable);
				bindings.put(VAR_PLAYER_HP, (double)playerHealth);
				bindings.put(VAR_DAMAGE, (double)damage);
				final Object result = compiled.eval(bindings);
				if (result instanceof Number) return ((Number)result).floatValue();
			} catch (Exception e) {
				LOG.warn("LastStand formula failed, using default: " + e.getMessage());
			}
		}

		return defaultCost(enchantmentLevels, playerHealth, damage);
	}

	private CompiledScript tryCompile(String formula) {
		try {
			if (engine instanceof Compilable)
				return ((Compilable)engine).compile(MATH_PRELUDE + formula);
		} catch (Exception e) {
			LOG.warn("Invalid lastStandFormula, using default: " + e.getMessage());
		}
		return null;
	}

	private static float defaultCost(int enchantmentLevels, float playerHealth, float damage) {
		float xp = 1f - (playerHealth - damage);
		xp *= 50;
		xp /= enchantmentLevels;
		xp = Math.max(1, xp);
		return xp;
	}

	public static int countLastStandEnchantmentLevels(EntityLivingBase living) {
		if (living != null) {
			int count = 0;
			for (int slot = 1; slot <= 4; slot++) {
				final ItemStack stack = living.getEquipmentInSlot(slot);
				if (stack != null)
					count += EnchantmentHelper.getEnchantmentLevel(OpenBlocks.Enchantments.lastStand.effectId, stack);
			}
			return count;
		}
		return 0;
	}
}
