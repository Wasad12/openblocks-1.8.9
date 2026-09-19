package openblocks;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;
import openblocks.common.ElevatorActionHandler;
import openblocks.common.block.BlockAutoAnvil;
import openblocks.common.block.BlockAutoEnchantmentTable;
import openblocks.common.block.BlockElevator;
import openblocks.common.block.BlockFan;
import openblocks.common.block.BlockTank;
import openblocks.common.block.BlockVacuumHopper;
import openblocks.common.block.BlockXPDrain;
import openblocks.common.block.BlockXPShower;
import openblocks.common.entity.EntityHangGlider;
import openblocks.common.entity.EntityXPOrbNoFly;
import openblocks.common.item.ItemHangGlider;
import openblocks.common.item.ItemOBGeneric;
import openblocks.common.item.ItemTankBlock;
import openblocks.common.tileentity.TileEntityAutoAnvil;
import openblocks.common.tileentity.TileEntityFan;
import openblocks.common.tileentity.TileEntityVacuumHopper;
import openblocks.common.tileentity.TileEntityAutoEnchantmentTable;
import openblocks.common.tileentity.TileEntityTank;
import openblocks.common.tileentity.TileEntityXPDrain;
import openblocks.common.tileentity.TileEntityXPShower;
import openblocks.enchantments.EnchantmentLastStand;
import openblocks.enchantments.LastStandEnchantmentsHandler;

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
	private static final int ENTITY_XP_ID = 709;

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

		@Override
		@SideOnly(Side.CLIENT)
		public void displayAllReleventItems(List<ItemStack> result) {
			super.displayAllReleventItems(result);
			// 1.12.2 lists every level book on its own tab (vanilla Combat only
			// ever lists the max level — PROVED via javap, same for all 1.8.9
			// enchantments, not a port gap). NOTE: vanilla 1.8.9 misspells this
			// method displayAllReleventItems (fixed in 1.9+).
			if (Enchantments.lastStand != null)
				openmods.utils.EnchantmentUtils.addAllBooks(Enchantments.lastStand, result);
		}
	};

	public static class Items {
		public static ItemHangGlider hangGlider;
		public static ItemOBGeneric generic;
		public static openblocks.common.item.ItemSlimalyzer slimalyzer;
	}

	public static class Blocks {
		public static BlockTank tank;
		public static BlockXPDrain xpDrain;
		public static BlockXPShower xpShower;
		public static BlockAutoEnchantmentTable autoEnchantmentTable;
		public static BlockAutoAnvil autoAnvil;
		public static BlockVacuumHopper vacuumHopper;
		public static BlockFan fan;
		public static BlockElevator elevator;
	}

	public static class Enchantments {
		public static Enchantment lastStand;
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

		Items.slimalyzer = new openblocks.common.item.ItemSlimalyzer();
		GameRegistry.registerItem(Items.slimalyzer, "slimalyzer");

		FluidRegistry.registerFluid(Fluids.xpJuice);

		Blocks.tank = new BlockTank();
		GameRegistry.registerBlock(Blocks.tank, ItemTankBlock.class, "tank");
		GameRegistry.registerTileEntity(TileEntityTank.class, "openblocks_tank");

		Blocks.xpDrain = new BlockXPDrain();
		GameRegistry.registerBlock(Blocks.xpDrain, net.minecraft.item.ItemBlock.class, "xp_drain");
		GameRegistry.registerTileEntity(TileEntityXPDrain.class, "openblocks_xp_drain");

		Blocks.xpShower = new BlockXPShower();
		GameRegistry.registerBlock(Blocks.xpShower, net.minecraft.item.ItemBlock.class, "xp_shower");
		GameRegistry.registerTileEntity(TileEntityXPShower.class, "openblocks_xp_shower");

		Blocks.autoEnchantmentTable = new BlockAutoEnchantmentTable();
		GameRegistry.registerBlock(Blocks.autoEnchantmentTable, net.minecraft.item.ItemBlock.class, "auto_enchantment_table");
		GameRegistry.registerTileEntity(TileEntityAutoEnchantmentTable.class, "openblocks_auto_enchantment_table");

		Blocks.autoAnvil = new BlockAutoAnvil();
		GameRegistry.registerBlock(Blocks.autoAnvil, net.minecraft.item.ItemBlock.class, "auto_anvil");
		GameRegistry.registerTileEntity(TileEntityAutoAnvil.class, "openblocks_auto_anvil");

		Blocks.vacuumHopper = new BlockVacuumHopper();
		GameRegistry.registerBlock(Blocks.vacuumHopper, net.minecraft.item.ItemBlock.class, "vacuum_hopper");
		GameRegistry.registerTileEntity(TileEntityVacuumHopper.class, "openblocks_vacuum_hopper");

		Blocks.fan = new BlockFan();
		GameRegistry.registerBlock(Blocks.fan, net.minecraft.item.ItemBlock.class, "fan");
		GameRegistry.registerTileEntity(TileEntityFan.class, "openblocks_fan");

		Blocks.elevator = new BlockElevator();
		GameRegistry.registerBlock(Blocks.elevator, net.minecraft.item.ItemBlock.class, "elevator");
		MinecraftForge.EVENT_BUS.register(new ElevatorActionHandler());

		// last stand enchantment (mirrors 1.12.2 Config registration: handler + enchantment)
		if (Config.lastStandEnchantmentEnabled) {
			MinecraftForge.EVENT_BUS.register(new LastStandEnchantmentsHandler());
			Enchantments.lastStand = new EnchantmentLastStand();
			Enchantment.addToBookList(Enchantments.lastStand);
		}

		// syncable field types (local table — 1.8.9 has no data registries)
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableBoolean.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableFloat.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableByte.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableInt.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableTank.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableSides.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableFlags.ByteFlags.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableFlags.ShortFlags.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableFlags.IntFlags.class);
		openmods.sync.SyncableObjectTypeRegistry.register(openmods.sync.SyncableEnum.class, openmods.sync.SyncableEnum.DUMMY_SUPPLIER);

		// RPC methods (local table — same call sites as 1.12.2)
		openmods.network.rpc.RpcMethodRegistry.registerInterface(openblocks.rpc.ILevelChanger.class);
		openmods.network.rpc.RpcMethodRegistry.registerInterface(openmods.utils.bitmap.IRpcDirectionBitMap.class);
		openmods.network.rpc.RpcMethodRegistry.registerInterface(openmods.utils.bitmap.IRpcIntBitMap.class);
		openmods.network.rpc.RpcCallDispatcher.init();
		openblocks.common.network.OpenBlocksChannel.init();

		EntityRegistry.registerModEntity(EntityHangGlider.class, "hang_glider", ENTITY_HANGGLIDER_ID, instance, 64, 1, true);
		EntityRegistry.registerModEntity(EntityXPOrbNoFly.class, "xp_orb_no_fly", ENTITY_XP_ID, instance, 64, 1, true);

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

		// xp drain (mirrors 1.12.2 xp_drain_0.json: 9x iron bars)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.xpDrain),
				"iii", "iii", "iii",
				'i', net.minecraft.init.Blocks.iron_bars));

		// xp shower (mirrors 1.12.2 xp_shower_0.json: 3x iron + obsidian)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.xpShower),
				"iii", "  o",
				'i', "ingotIron", 'o', net.minecraft.init.Blocks.obsidian));

		// auto enchantment table (mirrors 1.12.2 auto_enchantment_table_0.json)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.autoEnchantmentTable),
				"iii", "iei", "rrr",
				'i', "ingotIron", 'e', net.minecraft.init.Blocks.enchanting_table, 'r', "dustRedstone"));

		// auto anvil (mirrors 1.12.2 auto_anvil_0.json: iron + anvil + redstone)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.autoAnvil),
				"iii", "iai", "rrr",
				'i', "ingotIron", 'a', net.minecraft.init.Blocks.anvil, 'r', "dustRedstone"));

		// vacuum hopper (mirrors 1.12.2 vacuum_hopper_0.json: shapeless hopper + obsidian + ender eye)
		GameRegistry.addShapelessRecipe(new ItemStack(Blocks.vacuumHopper),
				net.minecraft.init.Blocks.hopper, net.minecraft.init.Blocks.obsidian, net.minecraft.init.Items.ender_eye);

		// fan (mirrors 1.12.2 fan_0.json: iron bars + iron + stone slab, vertical)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.fan),
				"f", "i", "s",
				'f', net.minecraft.init.Blocks.iron_bars, 'i', "ingotIron",
				's', new ItemStack(net.minecraft.init.Blocks.stone_slab, 1, 0)));

		// elevator (mirrors 1.12.2 elevator_0.json: white wool ring + ender pearl)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.elevator),
				"www", "wew", "www",
				'w', new ItemStack(net.minecraft.init.Blocks.wool, 1, 0),
				'e', net.minecraft.init.Items.ender_pearl));

		// slimalyzer (mirrors 1.12.2 slimalyzer_0.json: iron + glass + slimeball + redstone)
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.slimalyzer),
				"igi", "isi", "iri",
				'i', "ingotIron", 'g', "paneGlass",
				's', "slimeball", 'r', "dustRedstone"));

		proxy.init();
		proxy.registerRenderInformation();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		proxy.postInit();
	}
}
