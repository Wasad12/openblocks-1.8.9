package openblocks.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import openblocks.Config;
import openblocks.api.ElevatorCheckEvent;
import openmods.Log;

// 1.8.9 port of 1.12.2 ElevatorBlockRules, minus the state-override system:
// overrides need CommandBase.convertArgToBlockState (VERIFIED absent in 1.8.9),
// so the override map, its parser and the live-reload listener are dropped
// (documented). The plain block rules (modId:blockName:action) port verbatim;
// everything else (half-block passability, action resolution, event hook) is
// verbatim — Log replaces the lib logger with the same call shape.
public class ElevatorBlockRules {

	public final static ElevatorBlockRules instance = new ElevatorBlockRules();

	private ElevatorBlockRules() {}

	public enum Action {
		IGNORE,
		ABORT,
		INCREMENT
	}

	private static final Map<String, Action> ACTIONS;

	static {
		ImmutableMap.Builder<String, Action> builder = ImmutableMap.builder();
		for (Action a : Action.values())
			builder.put(a.name().toLowerCase(Locale.ENGLISH), a);
		ACTIONS = builder.build();
	}

	private Map<Block, Action> rules;

	private Map<Block, Action> getRules() {
		if (rules == null) {
			rules = Maps.newIdentityHashMap();
			for (String entry : Config.elevatorRules) {
				try {
					tryAddRule(rules, entry);
				} catch (Throwable t) {
					Log.warn(t, "Invalid entry in elevator actions: %s", entry);
				}
			}
		}

		return rules;
	}

	private static void tryAddRule(Map<Block, Action> rules, String entry) {
		String[] parts = entry.split(":");
		if (parts.length == 0) return;
		Preconditions.checkState(parts.length == 3, "Each entry must have exactly 3 colon-separated fields");
		final String modId = parts[0];
		final String blockName = parts[1];
		final String actionName = parts[2].toLowerCase(Locale.ENGLISH);
		final Action action = ACTIONS.get(actionName);

		Preconditions.checkNotNull(action, "Unknown action: %s", actionName);

		Block block = Block.blockRegistry.getObject(new ResourceLocation(modId, blockName));

		if (block != Blocks.air) rules.put(block, action);
		else Log.warn("Can't find block %s", entry);
	}

	private static boolean isPassable(IBlockState state) {
		return Config.elevatorIgnoreHalfBlocks && !state.getBlock().isNormalCube();
	}

	public Action getActionForBlock(IBlockState state) {
		Action action = getRules().get(state.getBlock());
		if (action != null) return action;

		return isPassable(state)? Action.IGNORE : Action.INCREMENT;
	}

	public void configureEvent(ElevatorCheckEvent evt) {
		// No overrides on 1.8.9 (see above) — kept as the extension point so the
		// handler flow stays verbatim.
	}
}
