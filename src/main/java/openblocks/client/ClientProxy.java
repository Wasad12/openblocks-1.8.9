package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openblocks.IOpenBlocksProxy;
import openblocks.OpenBlocks;
import openblocks.client.bindings.KeyInputHandler;
import openblocks.client.fx.FXLiquidSpray;
import openblocks.client.model.GliderItemModel;
import openblocks.client.model.TankFrameModel;
import openblocks.client.model.TankItemModel;
import openblocks.client.model.VacuumHopperModel;
import openblocks.client.renderer.entity.EntityHangGliderRenderer;
import openblocks.client.renderer.tileentity.TileEntityAutoEnchantmentTableRenderer;
import openblocks.client.renderer.tileentity.TileEntityFanRenderer;
import openblocks.client.renderer.tileentity.TileEntityTankRenderer;
import openblocks.common.entity.EntityHangGlider;
import openblocks.common.entity.EntityXPOrbNoFly;
import openblocks.common.tileentity.TileEntityAutoEnchantmentTable;
import openblocks.common.tileentity.TileEntityFan;
import openblocks.common.tileentity.TileEntityTank;

public class ClientProxy implements IOpenBlocksProxy {

	public ClientProxy() {}

	@Override
	public void preInit() {
		new KeyInputHandler().setup();

		RenderingRegistry.registerEntityRenderingHandler(EntityHangGlider.class, new IRenderFactory<EntityHangGlider>() {
			@Override
			public Render<? super EntityHangGlider> createRenderFor(RenderManager manager) {
				return new EntityHangGliderRenderer(manager);
			}
		});

		RenderingRegistry.registerEntityRenderingHandler(EntityXPOrbNoFly.class, new IRenderFactory<EntityXPOrbNoFly>() {
			@Override
			public Render<? super EntityXPOrbNoFly> createRenderFor(RenderManager manager) {
				return new RenderXPOrb(manager);
			}
		});

		registerItemModels();

		// Glider visibility switching (deployed: hidden in hands, shown in GUI) lives in
		// GliderItemModel, installed over the baked model at ModelBakeEvent. Must register
		// before the first bake (preInit — the first ModelManager load predates mod init()).
		MinecraftForge.EVENT_BUS.register(new GliderItemModel.BakeHandler());

		if (OpenBlocks.Blocks.tank != null) {
			ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTank.class, new TileEntityTankRenderer());
			// same as 1.12.2 ClientProxy: stitch every registered fluid's still icon
			// (mod fluids like xpJuice are NOT stitched automatically on 1.8.9).
			MinecraftForge.EVENT_BUS.register(new FluidTextureRegisterListener());
			// connected frame edges (fix 1) + fluid in item (fix 2); both reload-safe.
			MinecraftForge.EVENT_BUS.register(new TankFrameModel.BakeHandler());
			MinecraftForge.EVENT_BUS.register(new TankItemModel.BakeHandler());
		}

		if (OpenBlocks.Blocks.autoEnchantmentTable != null) {
			ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAutoEnchantmentTable.class, new TileEntityAutoEnchantmentTableRenderer());
		}

		if (OpenBlocks.Blocks.vacuumHopper != null) {
			// per-side output nozzles over the static body (TankFrameModel pattern)
			MinecraftForge.EVENT_BUS.register(new VacuumHopperModel.BakeHandler());
		}

		if (OpenBlocks.Blocks.fan != null) {
			ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFan.class, new TileEntityFanRenderer());
		}
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
		net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(OpenBlocks.instance, new openmods.gui.ClientGuiHandler());
	}

	@Override
	public void postInit() {}

	@Override
	public void registerRenderInformation() {
		if (OpenBlocks.Items.hangGlider != null) {
			MinecraftForge.EVENT_BUS.register(new GliderPlayerRenderHandler());
		}
	}

	// NOTE: must run in preInit (1.12.2 registers item models during preInit as well).
	// Model bake happens before mod init(), so init-time registration is silently ignored.
	private static void registerItemModels() {
		if (OpenBlocks.Items.hangGlider != null) {
			final ModelResourceLocation normalLocation = new ModelResourceLocation("openblocks:hang_glider", "inventory");
			// NOTE: plain ResourceLocations (NOT ModelResourceLocations): matches vanilla 1.8.9
			// convention (e.g. vanilla "bow", "coal"). Forge maps these to models/item/*.json.
			// Item JSONs must use "builtin/generated" as parent: 1.8.9 has no models/item/generated.json
			// (added in 1.9); builtin/generated is its 1.8.9 equivalent (vanilla items use it too).
			// The mesh definition always resolves to the normal model; deployed/folded visibility
			// switching happens per-stack AND per-perspective in GliderItemModel (see that class).
			ModelBakery.registerItemVariants(OpenBlocks.Items.hangGlider,
					new ResourceLocation("openblocks:hang_glider"));
			ModelLoader.setCustomMeshDefinition(OpenBlocks.Items.hangGlider, new ItemMeshDefinition() {
				@Override
				public ModelResourceLocation getModelLocation(ItemStack stack) {
					return normalLocation;
				}
			});
		}

		if (OpenBlocks.Items.generic != null) {
			final ModelResourceLocation wingLocation = new ModelResourceLocation("openblocks:glider_wing", "inventory");
			ModelBakery.registerItemVariants(OpenBlocks.Items.generic,
					new ResourceLocation("openblocks:glider_wing"));
			ModelLoader.setCustomMeshDefinition(OpenBlocks.Items.generic, new ItemMeshDefinition() {
				@Override
				public ModelResourceLocation getModelLocation(ItemStack stack) {
					return wingLocation;
				}
			});
		}

		if (OpenBlocks.Blocks.tank != null) {
			final Item tankItem = Item.getItemFromBlock(OpenBlocks.Blocks.tank);
			if (tankItem != null)
				ModelLoader.setCustomModelResourceLocation(tankItem, 0,
						new ModelResourceLocation("openblocks:tank", "inventory"));
		}

		if (OpenBlocks.Blocks.xpDrain != null) {
			final Item drainItem = Item.getItemFromBlock(OpenBlocks.Blocks.xpDrain);
			if (drainItem != null)
				ModelLoader.setCustomModelResourceLocation(drainItem, 0,
						new ModelResourceLocation("openblocks:xp_drain", "inventory"));
		}

		if (OpenBlocks.Blocks.xpShower != null) {
			final Item showerItem = Item.getItemFromBlock(OpenBlocks.Blocks.xpShower);
			if (showerItem != null)
				ModelLoader.setCustomModelResourceLocation(showerItem, 0,
						new ModelResourceLocation("openblocks:xp_shower", "inventory"));
		}

		if (OpenBlocks.Blocks.autoEnchantmentTable != null) {
			final Item tableItem = Item.getItemFromBlock(OpenBlocks.Blocks.autoEnchantmentTable);
			if (tableItem != null)
				ModelLoader.setCustomModelResourceLocation(tableItem, 0,
						new ModelResourceLocation("openblocks:auto_enchantment_table", "inventory"));
		}

		if (OpenBlocks.Blocks.autoAnvil != null) {
			final Item anvilItem = Item.getItemFromBlock(OpenBlocks.Blocks.autoAnvil);
			if (anvilItem != null)
				ModelLoader.setCustomModelResourceLocation(anvilItem, 0,
						new ModelResourceLocation("openblocks:auto_anvil", "inventory"));
		}

		if (OpenBlocks.Blocks.vacuumHopper != null) {
			final Item hopperItem = Item.getItemFromBlock(OpenBlocks.Blocks.vacuumHopper);
			if (hopperItem != null)
				ModelLoader.setCustomModelResourceLocation(hopperItem, 0,
						new ModelResourceLocation("openblocks:vacuum_hopper", "inventory"));
		}

		if (OpenBlocks.Blocks.fan != null) {
			final Item fanItem = Item.getItemFromBlock(OpenBlocks.Blocks.fan);
			if (fanItem != null)
				ModelLoader.setCustomModelResourceLocation(fanItem, 0,
						new ModelResourceLocation("openblocks:fan", "inventory"));
		}

		if (OpenBlocks.Blocks.elevator != null) {
			final Item elevatorItem = Item.getItemFromBlock(OpenBlocks.Blocks.elevator);
			if (elevatorItem != null)
				ModelLoader.setCustomModelResourceLocation(elevatorItem, 0,
						new ModelResourceLocation("openblocks:elevator", "inventory"));
		}

		if (OpenBlocks.Items.slimalyzer != null) {
			// No property overrides in 1.8.9 (no IItemPropertyGetter): the mesh
			// definition switches on the Active NBT tag instead (glider pattern).
			final ModelResourceLocation offLocation = new ModelResourceLocation("openblocks:slimalyzer", "inventory");
			final ModelResourceLocation onLocation = new ModelResourceLocation("openblocks:slimalyzer_active", "inventory");
			ModelBakery.registerItemVariants(OpenBlocks.Items.slimalyzer,
					new ResourceLocation("openblocks:slimalyzer"),
					new ResourceLocation("openblocks:slimalyzer_active"));
			ModelLoader.setCustomMeshDefinition(OpenBlocks.Items.slimalyzer, new ItemMeshDefinition() {
				@Override
				public ModelResourceLocation getModelLocation(ItemStack stack) {
					return openblocks.common.item.ItemSlimalyzer.isActive(stack) ? onLocation : offLocation;
				}
			});
		}
	}

	@Override
	public boolean isClientPlayer(EntityPlayer player) {
		return player == Minecraft.getMinecraft().thePlayer;
	}

	@Override
	public World getServerWorld(int dimensionId) {
		// NOTE: on an integrated server OpenBlocks.proxy IS the client proxy (SidedProxy
		// picks by physical side), so this must resolve the server world via FML just
		// like ServerProxy does — returning null here broke ALL client→server RPC in
		// singleplayer (settings never reached the server).
		return net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
	}

	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().theWorld;
	}

	@Override
	public int getParticleSettings() {
		return Minecraft.getMinecraft().gameSettings.particleSetting;
	}

	@Override
	public void spawnLiquidSpray(World world, FluidStack fluid, double x, double y, double z, float scale, float gravity, Vec3 velocity) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new FXLiquidSpray(world, fluid, x, y, z, scale, gravity, velocity));
	}

	// same listener as 1.12.2 ClientProxy.FluidTextureRegisterListener (verbatim),
	// plus the vacuum hopper nozzle sprites (fix loop 1): VacuumHopperModel bakes
	// its 18 nozzle models lazily at runtime via ModelLoaderRegistry (never through
	// the normal blockstate bake), so their textures are never stitched — the body
	// renders (normal bake) while the nozzles came out purple-black. Explicit
	// stitch here fixes it, same reason mod fluid stills need stitching above.
	private static class FluidTextureRegisterListener {
		@SubscribeEvent
		public void onTextureStitch(TextureStitchEvent.Pre evt) {
			for (Fluid f : FluidRegistry.getRegisteredFluids().values()) {
				final ResourceLocation fluidTexture = f.getStill();
				if (fluidTexture != null)
					evt.map.registerSprite(fluidTexture);
			}

			if (OpenBlocks.Blocks.vacuumHopper != null) {
				evt.map.registerSprite(new ResourceLocation("openblocks", "blocks/vacuum_hopper_items"));
				evt.map.registerSprite(new ResourceLocation("openblocks", "blocks/vacuum_hopper_fluids"));
				evt.map.registerSprite(new ResourceLocation("openblocks", "blocks/vacuum_hopper_both"));
			}
		}
	}
}
