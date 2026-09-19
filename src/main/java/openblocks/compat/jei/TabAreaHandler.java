package openblocks.compat.jei;

import java.util.List;
import java.awt.Rectangle;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import openmods.gui.ComponentGui;

// JEI-side equivalent of the tab overlap handling: every visible tab (open or
// folded handle) is reported as an exclusion area, so JEI moves its item panel
// aside instead of rendering under open tabs (ThermalExpansion 1.8.9
// MachineTabAreaHandler + Forestry 1.8.9 ledger areas precedent). One
// registration on ComponentGui covers every tabbed GUI: JEI matches handlers
// by isAssignableFrom (VERIFIED in the 2.28.18 ItemListOverlay bytecode).
// Client-only: loaded solely under JEI discovery, never referenced from mod
// code, so the game runs fine with JEI absent.
public class TabAreaHandler implements IAdvancedGuiHandler<ComponentGui> {

	@Override
	public Class<ComponentGui> getGuiContainerClass() {
		return ComponentGui.class;
	}

	@Override
	public List<Rectangle> getGuiExtraAreas(ComponentGui gui) {
		return gui.getTabAreas();
	}
}
