package openblocks;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import openblocks.common.block.BlockTank;
import openblocks.common.entity.EntityHangGlider;
import openblocks.common.item.ItemHangGlider;
import openblocks.common.item.ItemOBGeneric;
import openblocks.common.item.ItemTankBlock;
import openblocks.common.tileentity.TileEntityTank;

@Mod(modid = OpenBlocks.MODID, name = OpenBlocks.NAME, version = OpenBlocks.VERSION, updateJSON = OpenBlocks.UPDATE_JSON)
public class OpenBlocks {

	public static final String MODID = "openblocks";
	public static final String NAME = "OpenBlocks";
	// NOTE: hardcoded, keep in sync with gradle.properties mod_version (no FG replaceIn on 1.8.9)
	public static final String VERSION = "1.8.9-1.0.0";
	public static final String PROXY_SERVER = "openblocks.common.ServerProxy";
	public static final String PROXY_CLIENT = "openblocks.client.ClientProxy";
	public static final String UPDATE_JSON = "http://openmods.info/versions/openblocks.json"; // HTTP, for wider support

	private static final int ENTITY_HANGGLIDER_ID = 701;

	@Instance(MODID)
	public static OpenBlocks instance;

	@SidedProxy(clientSide = OpenBlocks.PROXY_CLIENT, serverSide = OpenBlocks.PROXY_SERVER)
	public static IOpenBlocksProxy proxy;

	public static CreativeTabs tabOpenBlocks = new CreativeTabs("tabOpenBlocks") {
		@Override
		public Item getTabIconItem() {
			// NOTE: fully qualified — our own OpenBlocks.Blocks inner class shadows the import.
			return Item.getItemFromBlock(net.minecraft.init.Blocks.sponge);
		}
	};

	public static class Items {
		public static ItemHangGlider hangGlider;
		public static ItemOBGeneric generic;
	}

	public static class Blocks {
		public static BlockTank tank;
	}

	public static class Fluids {
		// 1.8.9 Fluid has no sound hooks (strings only, no SoundEvent version of
		// setEmptySound/setFillSound), so the 1.12.2 levelup/orb sounds are dropped.
		public static final Fluid xpJuice = new Fluid("xpjuice", location("blocks/xp_juice_still"), location("blocks/xp_juice_flowing"))
				.setLuminosity(10)
				.setDensity(800)
				.setViscosity(1500)
				.setUnlocalizedName("openblocks.xp_juice");
	}

	public static ResourceLocation location(String path) {
		return new ResourceLocation("openblocks", path);
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		Config.init(evt.getSuggestedConfigurationFile());

		Items.hangGlider = new ItemHangGlider();
		GameRegistry.registerItem(Items.hangGlider, "hang_glider");

		Items.generic = new ItemOBGeneric();
		GameRegistry.registerItem(Items.generic, "generic");

		FluidRegistry.registerFluid(Fluids.xpJuice);

		Blocks.tank = new BlockTank();
		GameRegistry.registerBlock(Blocks.tank, ItemTankBlock.class, "tank");
		GameRegistry.registerTileEntity(TileEntityTank.class, "openblocks_tank");

		EntityRegistry.registerModEntity(EntityHangGlider.class, "hang_glider", ENTITY_HANGGLIDER_ID, instance, 64, 1, true);

		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		// glider wings (mirrors 1.12.2 glider_wing_0.json / glider_wing_1.json)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.generic, 1, ItemOBGeneric.META_GLIDER_WING),
				" sl", "sll", "lll",
				's', "stickWood", 'l', net.minecraft.init.Items.leather));
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.generic, 1, ItemOBGeneric.META_GLIDER_WING),
				"ls ", "lls", "lll",
				's', "stickWood", 'l', net.minecraft.init.Items.leather));

		// hang glider (mirrors 1.12.2 hang_glider_0.json)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.hangGlider),
				"wsw",
				'w', new ItemStack(Items.generic, 1, ItemOBGeneric.META_GLIDER_WING), 's', "stickWood"));

		// tank (mirrors 1.12.2 tank_0.json: obsidian + glass -> 2)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.tank, 2),
				"ogo", "ggg", "ogo",
				'o', net.minecraft.init.Blocks.obsidian, 'g', "paneGlass"));

		proxy.init();
		proxy.registerRenderInformation();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		proxy.postInit();
	}
}
