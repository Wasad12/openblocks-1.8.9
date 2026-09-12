package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import openblocks.IOpenBlocksProxy;
import openblocks.OpenBlocks;
import openblocks.client.bindings.KeyInputHandler;
import openblocks.client.model.GliderItemModel;
import openblocks.client.renderer.entity.EntityHangGliderRenderer;
import openblocks.common.entity.EntityHangGlider;

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

		registerItemModels();

		// Glider visibility switching (deployed: hidden in hands, shown in GUI) lives in
		// GliderItemModel, installed over the baked model at ModelBakeEvent. Must register
		// before the first bake (preInit — the first ModelManager load predates mod init()).
		MinecraftForge.EVENT_BUS.register(new GliderItemModel.BakeHandler());
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
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
	}

	@Override
	public boolean isClientPlayer(EntityPlayer player) {
		return player == Minecraft.getMinecraft().thePlayer;
	}
}
