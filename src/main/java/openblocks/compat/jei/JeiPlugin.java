package openblocks.compat.jei;

import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;

// JEI plugin (client-only: loaded solely under JEI discovery, safe when JEI
// is absent — nothing in the mod references this class). Scope is narrow on
// purpose: it only tells JEI where our GUI tabs are so its item panel reflows
// around open tabs instead of overlapping them. No recipes, no blacklist.
@JEIPlugin
public class JeiPlugin extends BlankModPlugin {

	@Override
	public void register(IModRegistry registry) {
		registry.addAdvancedGuiHandlers(new TabAreaHandler());
	}
}
