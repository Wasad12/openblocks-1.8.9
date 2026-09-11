package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLLog;import net.minecraft.client.resources.model.ModelBakery;
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
import openblocks.client.renderer.entity.EntityHangGliderRenderer;
import openblocks.common.entity.EntityHangGlider;
import openblocks.common.item.ItemOBGeneric;

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
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
		probeResources("init");
	}

	@Override
	public void postInit() {
		probeResources("postInit");
		dumpVariantMaps();
	}

	// TEMPORARY DEBUG (fix loop 2, variant registration investigation) — reverted after diagnosis
	private static void dumpVariantMaps() {
		try {
			for (java.lang.reflect.Field f : ModelBakery.class.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
						&& java.util.Map.class.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					java.util.Map<?, ?> map = (java.util.Map<?, ?>)f.get(null);
					for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
						Object key = e.getKey();
						String itemName = String.valueOf(key);
						try {
							Object item = key.getClass().getMethod("get").invoke(key);
							if (item instanceof net.minecraft.item.Item)
								itemName = String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject((net.minecraft.item.Item)item));
						} catch (Exception ignored) {}
						if (itemName.contains("openblocks"))
							FMLLog.info("[VARIANTDGB] static map %s: item=%s values=%s", f.getName(), itemName, e.getValue());
					}
				}
			}
		} catch (Exception e) {
			FMLLog.info("[VARIANTDGB] dump failed: %s", e);
		}
	}
	private static void probeResources(String phase) {
		final IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
		for (String path : new String[] {
				"models/item/glider_wing.json",
				"models/item/hang_glider.json",
				"item/glider_wing.json",
				"textures/models/hang_glider.png",
				"textures/items/glider_wing.png",
				"lang/en_US.lang" }) {
			try {
				rm.getResource(new ResourceLocation("openblocks", path));
				FMLLog.info("[MODELDGB] %s: FOUND openblocks:%s", phase, path);
			} catch (Exception e) {
				FMLLog.info("[MODELDGB] %s: MISSING openblocks:%s (%s)", phase, path, e);
			}
		}
	}

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
			final ModelResourceLocation hiddenLocation = new ModelResourceLocation("openblocks:hang_glider_hidden", "inventory");
			ModelBakery.registerItemVariants(OpenBlocks.Items.hangGlider, normalLocation);
			// TEMPORARY DEBUG (fix loop 2): separate single-arg call — testing whether 2318
			// drops non-first varargs. Reverted/merged after diagnosis.
			ModelBakery.registerItemVariants(OpenBlocks.Items.hangGlider, hiddenLocation);
			ModelLoader.setCustomMeshDefinition(OpenBlocks.Items.hangGlider, new ItemMeshDefinition() {
				@Override
				public ModelResourceLocation getModelLocation(ItemStack stack) {
					return EntityHangGlider.isStackDeployedGlider(stack)? hiddenLocation : normalLocation;
				}
			});
		}

		if (OpenBlocks.Items.generic != null) {
			ModelLoader.setCustomModelResourceLocation(OpenBlocks.Items.generic, ItemOBGeneric.META_GLIDER_WING,
					new ModelResourceLocation("openblocks:glider_wing", "inventory"));
		}
	}

	@Override
	public boolean isClientPlayer(EntityPlayer player) {
		return player == Minecraft.getMinecraft().thePlayer;
	}
}
