package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import openblocks.IOpenBlocksProxy;
import openblocks.OpenBlocks;
import openblocks.client.bindings.KeyInputHandler;
import openblocks.client.renderer.entity.EntityHangGliderRenderer;
import openblocks.common.entity.EntityHangGlider;

public class ClientProxy implements IOpenBlocksProxy {

	public ClientProxy() {}

	// TEMPORARY DEBUG (fix loop 4) — holds the resource manager handed to reload listeners.
	static volatile IResourceManager probeManager = null;
	// TEMPORARY DEBUG (fix loop 4) — re-entrancy guard for the instrumented probe call below.
	static final java.util.Set<String> probeActive =
			java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

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

		// TEMPORARY DEBUG (fix loop 4, bake forensics) — remove once file-first failure is explained.
		// Passive listener: logs every model location the bake requests for our domain, changes nothing.
		ModelLoaderRegistry.registerLoader(new ICustomModelLoader() {
			@Override
			public void onResourceManagerReload(IResourceManager resourceManager) {
				ClientProxy.probeManager = resourceManager;
				org.apache.logging.log4j.LogManager.getLogger().info(
						"[MODELPROBE] reload manager: {}@{}",
						resourceManager.getClass().getName(),
						Integer.toHexString(System.identityHashCode(resourceManager)));
			}

			@Override
			public boolean accepts(ResourceLocation modelLocation) {
				if ("openblocks".equals(modelLocation.getResourceDomain())) {
					org.apache.logging.log4j.Logger probeLog = org.apache.logging.log4j.LogManager.getLogger();
					probeLog.info("[MODELPROBE] loader asked for '{}' (class {})",
							modelLocation, modelLocation.getClass().getSimpleName());
					// Same-thread, bake-time visibility check for the exact file VanillaLoader will request.
					ResourceLocation exact = new ResourceLocation(modelLocation.getResourceDomain(),
							modelLocation.getResourcePath() + ".json");
					IResourceManager game = Minecraft.getMinecraft().getResourceManager();
					IResourceManager bake = ClientProxy.probeManager;
					probeLog.info("[MODELPROBE] managers: game={}@{} bake={}@{}",
							game == null ? "null" : game.getClass().getName(),
							game == null ? "?" : Integer.toHexString(System.identityHashCode(game)),
							bake == null ? "null" : bake.getClass().getName(),
							bake == null ? "?" : Integer.toHexString(System.identityHashCode(bake)));
					if (game != null) {
						try {
							game.getResource(exact);
							probeLog.info("[MODELPROBE] game-manager getResource OK: {}", exact);
						} catch (Exception e) {
							probeLog.info("[MODELPROBE] game-manager getResource FAIL: {} : {}", exact, e.toString());
						}
					}
					if (bake != null) {
						try {
							bake.getResource(exact);
							probeLog.info("[MODELPROBE] bake-manager getResource OK: {}", exact);
						} catch (Exception e) {
							probeLog.info("[MODELPROBE] bake-manager getResource FAIL: {} : {}", exact, e.toString());
						}
					}
					// TEMPORARY DEBUG v4: run the EXACT failing call ourselves and log the full
					// exception chain (Forge swallows the FNFE path). Guarded against re-entrancy.
					if (modelLocation.getResourcePath().startsWith("models/")
							&& ClientProxy.probeActive.add(modelLocation.toString())) {
						try {
							ResourceLocation file = new ResourceLocation(
									modelLocation.getResourceDomain(),
									modelLocation.getResourcePath().substring("models/".length()));
							try {
								IModel m = ModelLoaderRegistry.getModel(file);
								probeLog.info("[MODELPROBE] instrumented getModel OK: {} -> {}",
										file, m == null ? "null" : m.getClass().getName());
							} catch (Throwable t) {
								StringBuilder chain = new StringBuilder(t.toString());
								for (Throwable c = t.getCause(); c != null; c = c.getCause()) {
									chain.append(" <= ").append(c.toString());
								}
								probeLog.info("[MODELPROBE] instrumented getModel THROW: {} : {}",
										file, chain.toString());
							}
						} finally {
							ClientProxy.probeActive.remove(modelLocation.toString());
						}
					}
				}
				return false;
			}

			@Override
			public IModel loadModel(ResourceLocation modelLocation) {
				return null; // never reached (accepts always false)
			}
		});

		// TEMPORARY DEBUG (fix loop 4) — direct resource-manager visibility check for the exact
		// file the bake should resolve. Runs in preInit; result approximates bake-time visibility.
		try {
			Minecraft.getMinecraft().getResourceManager()
					.getResource(new ResourceLocation("openblocks", "models/item/hang_glider.json"));
			org.apache.logging.log4j.LogManager.getLogger().info("[MODELPROBE] direct getResource models/item/hang_glider.json OK");
		} catch (Exception e) {
			org.apache.logging.log4j.LogManager.getLogger().info("[MODELPROBE] direct getResource models/item/hang_glider.json FAIL: {}", e.toString());
		}
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
			final ModelResourceLocation hiddenLocation = new ModelResourceLocation("openblocks:hang_glider_hidden", "inventory");
			// NOTE: plain ResourceLocations (NOT ModelResourceLocations): 1.8.9 resolves item model
			// files from these strings, and MRL-style "name#inventory" strings break file lookup
			// (no "#" stripping on this Forge line). See docs/ARCHITECTURE.md.
			ModelBakery.registerItemVariants(OpenBlocks.Items.hangGlider,
					new ResourceLocation("openblocks:hang_glider"),
					new ResourceLocation("openblocks:hang_glider_hidden"));
			ModelLoader.setCustomMeshDefinition(OpenBlocks.Items.hangGlider, new ItemMeshDefinition() {
				@Override
				public ModelResourceLocation getModelLocation(ItemStack stack) {
					return EntityHangGlider.isStackDeployedGlider(stack)? hiddenLocation : normalLocation;
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
