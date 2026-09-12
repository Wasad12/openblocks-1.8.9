package openblocks.client.gui;

import com.google.common.collect.ImmutableList;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import openblocks.common.LiquidXpUtils;
import openblocks.common.container.ContainerAutoAnvil;
import openblocks.common.tileentity.TileEntityAutoAnvil;
import openblocks.common.tileentity.TileEntityAutoAnvil.AutoSlots;
import openmods.gui.GuiConfigurableSlots;
import openmods.gui.component.BaseComposite;
import openmods.gui.component.GuiComponentLabel;
import openmods.gui.component.GuiComponentSprite;
import openmods.gui.component.GuiComponentTab;
import openmods.gui.component.GuiComponentTankLevel;
import openmods.gui.logic.ValueCopyAction;
import openmods.utils.MiscUtils;
import openmods.utils.TranslationUtils;

// 1.8.9 port of 1.12.2 GuiAutoAnvil: same hammer/plus sprites, XP tank gauge in
// levels, same 4 tabs + labels. 1.8.9 adaptations: nullable stacks (icons are
// constructor args, no change needed), levelsForDisplay instead of the guava
// FLUID_TO_LEVELS function (same math), stable_22 lower_snake item/enchant names.
public class GuiAutoAnvil extends GuiConfigurableSlots<TileEntityAutoAnvil, ContainerAutoAnvil, TileEntityAutoAnvil.AutoSlots> {

	public GuiAutoAnvil(ContainerAutoAnvil container) {
		super(container, 176, 175, "openblocks.gui.autoanvil");
	}

	@Override
	protected void addCustomizations(BaseComposite main) {
		TileEntityAutoAnvil te = getContainer().getOwner();
		main.addComponent(new GuiComponentSprite(80, 34, GuiComponentSprite.Sprites.hammer));
		main.addComponent(new GuiComponentSprite(36, 41, GuiComponentSprite.Sprites.plus));

		final GuiComponentTankLevel tankLevel = new GuiComponentTankLevel(140, 30, 17, 37, TileEntityAutoAnvil.MAX_STORED_LEVELS);
		tankLevel.setDisplayFluidNameInTooltip(false);
		addSyncUpdateListener(ValueCopyAction.create(te.getFluidProvider(), tankLevel.fluidReceiver(), value -> LiquidXpUtils.levelsForDisplay(value)));

		main.addComponent(tankLevel);
	}

	@Override
	protected Iterable<AutoSlots> getSlots() {
		return ImmutableList.of(AutoSlots.tool, AutoSlots.modifier, AutoSlots.xp, AutoSlots.output);
	}

	@Override
	protected GuiComponentTab createTab(AutoSlots slot) {
		switch (slot) {
			case modifier:
				return new GuiComponentTab(StandardPalette.lightblue.getColor(), new ItemStack(Items.enchanted_book, 1), 100, 100);
			case output: {
				ItemStack enchantedAxe = new ItemStack(Items.diamond_pickaxe, 1);
				enchantedAxe.addEnchantment(Enchantment.fortune, 1);
				return new GuiComponentTab(StandardPalette.green.getColor(), enchantedAxe, 100, 100);
			}
			case tool:
				return new GuiComponentTab(StandardPalette.blue.getColor(), new ItemStack(Items.diamond_pickaxe, 1), 100, 100);
			case xp:
				return new GuiComponentTab(StandardPalette.yellow.getColor(), new ItemStack(Items.bucket, 1), 100, 100);
			default:
				throw MiscUtils.unhandledEnum(slot);
		}
	}

	@Override
	protected GuiComponentLabel createLabel(AutoSlots slot) {
		switch (slot) {
			case modifier:
			case tool:
				return new GuiComponentLabel(22, 82, TranslationUtils.translateToLocal("openblocks.gui.autoextract"));
			case output:
				return new GuiComponentLabel(22, 82, TranslationUtils.translateToLocal("openblocks.gui.autoeject"));
			case xp:
				return new GuiComponentLabel(22, 82, TranslationUtils.translateToLocal("openblocks.gui.autodrink"));
			default:
				throw MiscUtils.unhandledEnum(slot);

		}
	}

}
