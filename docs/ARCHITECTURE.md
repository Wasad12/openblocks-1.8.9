# Architecture Decisions

Records 1.8.9-specific adaptations and reasons for deviations from 1.12.2 (§12).
The 1.12.2 source stays the primary authority (§2); everything here is adaptation, not redesign (§3).

## Standing constraints

- (2026-09-11, user instruction, PERMANENT) The historical OpenBlocks/OpenModsLib 1.8 branches
  must NOT be consulted or used. They are considered broken/failed. This project is a faithful
  port FROM the 1.12.2 source. 1.8.9 API questions are resolved via 1.12.2 source comparison,
  vanilla/MCP knowledge, and compiler errors — never via the old 1.8 branches.

## Open

- (2026-09-11) How the 1.8.9 project consumes OpenModsLib is UNDECIDED (see KNOWN_ISSUES.md).
  `OpenBlocks-1.12.X/OpenModsLib/` submodule is empty; sibling `OpenModsLib-1.12.X/` holds the
  1.12.2 lib source. Options: (a) port OpenModsLib to 1.8.9 as separate Gradle project,
  (b) compatibility shim, (c) small local equivalents per feature. Decide with first feature (§9).
- (2026-09-11) 1.8.9 project scaffold (ForgeGradle version, mappings, package layout) does not
  exist yet — created with the first feature port.

## Decided

### Hang Glider (2026-09-11, Phase B plan — minimal adaptation, 1.12.2 behavior preserved)

- New Gradle project `OpenBlocks-1.8.9/` (ForgeGradle 2.1, MC 1.8.9, Java 8). No OpenModsLib: only the
  lib pieces the glider needs are carried over (see below). Decision recorded per §9: full lib port is
  out of scope for this feature; revisit if a later feature needs more.
- `openmods.renderer.DisplayListWrapper`: copied VERBATIM from OpenModsLib 1.12.2 (pure LWJGL, no MC API).
- `Vario` / `IVarioController`: copied VERBATIM (pure Java). `BeepGenerator`: same, except `Log` → log4j
  and `SoundCategory` import adjusted to 1.8.9 location (compiler verifies).
- `PlayerBodyRenderEvent` + ASM hook NOT ported (no ASM for this feature, §10). Replacement:
  `RenderLivingEvent.Pre` handler doing the same limb-zero + 75° X-rotation. Rationale (INFERRED):
  the ASM hook fires at the end of `applyRotations`, i.e. after vanilla corpse rotations with the
  matrix positioned at the entity — same point `RenderLivingEvent.Pre` fires. Visual check by user test.
- `EnumHand` removed (1.8.9 has no off-hand — unavoidable). Single-hand logic; the "glider in other
  hand, ignore" case cannot occur.
- `EntityDataManager` → `DataWatcher` id 17 (byte), same default/mechanics. No elytra check
  (`isElytraFlying` doesn't exist in 1.8.9 — unavoidable; documented, no behavior to preserve).
- `ItemStack.isEmpty` → null checks (1.8.9 stacks are nullable — unavoidable).
- `IStateItem`/`State`/`openmods:stateitem` model + `forge_marker` blockstate NOT ported. Replacement:
  `ModelLoader.setCustomMeshDefinition` + `ModelBakery.registerItemVariants` (both exist in 1.8.9);
  deployed variant → `hang_glider_hidden` (empty `{}`), otherwise normal generated model. Deployed state
  resolved by scanning the client `gliderMap` for identity match (same rule as 1.12.2 `getState`).
  KNOWN DEVIATION: while deployed the item is hidden in ALL contexts including inventory GUI; 1.12.2
  keeps it visible in GUI via the perspective-aware model. No per-perspective mechanism exists in 1.8.9.
- `world` → `worldObj`, `getDimension()` → `getDimensionId()`, `getBiomeForCoordsBody` →
  `getBiomeGenForCoords`, `spawnEntity` → `spawnEntityInWorld`, `owner.isUser()` →
  `owner == Minecraft.getMinecraft().thePlayer`, `mc.world` → `mc.theWorld`.
- Registration: direct `GameRegistry.registerItem` (`hang_glider`, `generic`), `EntityRegistry.registerModEntity`
  (same numeric id 701, tracking 64/1/true), `RenderingRegistry.registerEntityRenderingHandler`.
- Recipes converted from JSON to `ShapedOreRecipe` (`stickWood` + leather → `generic:0`; wings + stick →
  `hang_glider`). Same patterns/ingredients as 1.12.2 JSON.
- Config: local Forge `Configuration`, only `hangglider`/`enableThermal` (default true, 1.12.2 comment kept).
  Brick/soSerious keys dropped (unrelated systems, §19).
- `BookDocumentation`, info-book descriptions, legacy id remaps, mod GUI factory, OpenMods dependency:
  dropped (unrelated systems / no lib). Item ids kept as 1.12.2 (`hang_glider`, `glider_wing` meta 0).
- Lang: full 1.12.2 `en_us.lang` content shipped as 1.8.9-style `en_US.lang` (keys already compatible).
- `javax.annotation` dropped (no jsr305 guarantee in 1.8.9 dev env); `MapMaker` kept (guava via MC).
- Mappings: **stable_22** (user instruction 2026-09-11). `func_151601_a` remains searge-named in stable_22.
- Creative-tab listing: 1.8.9 has no `Item.isInCreativeTab`; equivalent used is
  `tab == getCreativeTab() || tab == CreativeTabs.tabAllSearch` (matches 1.12.2 semantics).
- Item models (fix loop 1, 2026-09-11 — 1.8.9 mechanism traps, all PROVED via runtime log + sources):
  1. Registration MUST run in preInit: the first model bake happens in `Minecraft.startGame`,
     before mod `init()`. (Matches 1.12.2, where the lib registers item models in preInit.)
  2. Register PLAIN variant-name strings via `ModelBakery.addVariantName` (no `#inventory` suffix):
     1.8.9 resolves item model files from these strings and MRL-style strings break lookup.
  3. Hidden/invisible item model = `{"parent": "builtin/generated"}` is WRONG in 1.8.9: `{}` throws
     `JsonParseException` (1.12.2 accepts it) and parentless-generated bakes to null (missing).
     Use `{"parent": "builtin/entity"}` (renders nothing, no TESR bound).
- Player-body tilt (fix loop 1): no ASM. `RenderLivingEvent.Pre` fires at the START of 1.8.9
  `doRender` (pre-push, world origin), NOT at end of `applyRotations` like the 1.12.2 ASM hook.
  The handler therefore conjugates the 1.12.2 rotation into the earlier point
  (push/T/yaw/R75/yaw^-1/T^-1 in Pre, pop in Post) for a bit-identical final composite.
  Partial ticks come from `RenderTickEvent` (`Minecraft.timer` is private in 1.8.9,
  `MinecraftForgeClient.getRenderPartialTicks()` doesn't exist yet).
  VERIFIED by user test 2026-09-11 (TPP/second-person correct).
- Item models (fix loops 2–3, still open as of `bc7cb6d` — findings for the next session):
  1. Registration MUST run in preInit (first model bake is in `Minecraft.startGame`, before mod init).
  2. 1.8.9 blockstates resolve `model` values under `models/block/` (VERIFIED twice: runtime
     `Unable to load block model: 'openblocks:block/glider_wing'` AND `Variant$Deserializer`
     bytecode `new ResourceLocation(domain, "block/" + path)`) — blockstates are USELESS for
     item variants here; 1.12.2's `forge_marker` custom loaders don't exist in 1.8.9. Deleted again.
  3. Reflection dump VERIFIED static `customVariantNames` holds exactly
     `glider_wing#inventory`, `hang_glider#inventory`, `hang_glider_hidden#inventory`, yet bakes only
     ever attempt normal+wing. Mechanism of the drop unexplained — prime suspect is now the
     file-first lookup path on Forge 1.8.9-11.15.1.2318 (note: we BUILD against 1722).
  4. CORRECTION (2026-09-11, supersedes the old `#`-poisoning claim in fix loop 1): Forge's
     `ModelBakery.java.patch` PROVES `getItemLocation` strips `#.*` before mapping to
     `domain:item/path`, and `registerVariantNames` merges ALL `customVariantNames` strings into
     the bake. Plain and MRL-form strings hit the SAME file — variant-string FORM is irrelevant
     at lookup level. Do NOT retry form-swapping without new evidence.
  5. File-first chain VERIFIED on 1.8.9-branch sources: `loadItems` → `getItemLocation` →
     `ModelLoaderRegistry.getActualLocation` (prepends `models/`) → `VanillaLoader` →
     `models/item/*.json`. Our files exist at those paths (VERIFIED in deployed JAR), so the
     bake SHOULD succeed — the `FileNotFoundException` contradicts the static picture, which is
     exactly what the `bc7cb6d` bake log discriminates.
  6. Current experiment (`bc7cb6d`): plain `ResourceLocation`s everywhere, mesh defs only, no
     blockstates. Render-time is consistent too (memory MRLs = mesh-def MRLs). If the bake log
     goes silent, file-first works and any remainder is render-time (mesher). If `isn't found`
     persists, file-first lookup itself is broken on 2318 → next option:
     custom `ICustomModelLoader` serving explicit `models/` paths (resource-manager resolution of
     explicit `models/item/*.json` was PROVEN working by probe).
- Item models ROOT CAUSE (fix loop 4, 2026-09-12 — the actual bug, all above superseded): our
  JSONs used `"parent": "item/generated"` copied from 1.12.2, but `models/item/generated.json`
  does not exist in 1.8.9 vanilla (VERIFIED: absent from the client jar; vanilla 1.8.9 items
  use `"parent": "builtin/generated"`, handled in-code). Our files loaded fine; the parent
  load inside `resolveDependencies` threw the FileNotFoundException, and the bake log only
  ever prints the variant — hence four probe cycles chasing the wrong file. `builtin/entity`
  (hidden model) short-circuits dependency loading, which is why it always baked. Fix: both
  JSONs rewritten with `"parent": "builtin/generated"` (same sprites, same textures —
  the 1.8.9-native spelling of exactly what 1.12.2's `item/generated` means).
- Held-item `display` transforms (2026-09-12): 1.12.2 inherits them from the `item/generated`
  parent; 1.8.9's `builtin/generated` base has none, so both JSONs declare the standard vanilla
  1.8.9 `display` block verbatim (thirdperson rot [0,90,-35] / trans [0,1.25,-3.5] / scale 0.85;
  firstperson rot [0,-135,25] / trans [0,4,2] / scale 1.7 — from 1.8.9 `diamond_sword.json`).
  Without it, held items render at wrong scale in FPP/TPP (user-verified symptom).
  REFINEMENT (2026-09-12): `thirdperson` corrected to 1.12.2 `item/generated.json`'s real
  `thirdperson_righthand` values (rot [0,0,0] / trans [0,3,1] / scale 0.55, key respelled to
  1.8.9); `firstperson` kept as sword values (user-confirmed identical FPP). Ground/head/fixed
  keys omitted (no 1.8.9 support; vanilla 1.8.9 items omit them).
  REFINEMENT 2 (2026-09-12): generated-TPP values user-REJECTED (tiny/flat/floating — 1.12.2
  Forge wraps display transforms in `blockCenterToCorner`, a frame shift 1.8.9 lacks, so the
  numbers can't transfer literally). `thirdperson` REMOVED: 1.12.2-default-item is equivalent
  to 1.8.9-no-display (both = this version's plain-flat-item handling, cf. vanilla apple).
  `firstperson` kept (confirmed). If 1.8.9-default TPP still mismatches, tune from that baseline.
  REFINEMENT 3 (2026-09-12, user-directed "just like redstone"): 1.8.9 vanilla flat items
  (redstone/paper, read from the client jar) share thirdperson rot [-90,0,0] / trans [0,1,-3] /
  scale 0.55 with firstperson equal to our sword values. Both JSONs carry that thirdperson
  verbatim — the vanilla flat-item-in-fist look, no more guessing across version pipelines.
- Deployed-visibility (2026-09-12): the old "hidden in ALL contexts incl. GUI" deviation is
  GONE. New `openblocks.client.model.GliderItemModel` (smart + perspective-aware baked-model
  wrapper, installed at `ModelBakeEvent`) is the 1.8.9-native equivalent of 1.12.2's
  `openmods:perspective-aware` model: deployed → invisible in FIRST/THIRD person, visible in
  GUI/GROUND/HEAD/everything else; folded → plain model. Mesh def always resolves normal now.
  (1.8.9 has no off-hand; single-hand logic throughout.)
- Unstackable glider (2026-09-12, DELIBERATE user-requested deviation): 1.12.2 stacks the
  glider to 64; ours is `setMaxStackSize(1)`. Wings still stack (crafting ingredient).

### Tank (2026-09-12, Phase B plan — behavior preserved, lib/sync/render adapted to 1.8.9)

Source: `BlockTank`, `TileEntityTank`, `ItemTankBlock`, `TileEntityTankRenderer`,
`client/renderer/tileentity/tank/*` (10 files), `LiquidXpUtils` (part), `Config` tanks keys +
`xpToLiquidRatio`, recipe `tank_0.json`, `tank.png`, `xp_juice_{still,flowing}.png`,
lang keys (already in our `en_US.lang`: `tile.openblocks.tank.*`, `fluid.openblocks.xp_juice`).

- NO OpenModsLib port (§9: per-feature local equivalents). Dropped lib systems: `OpenBlock` base
  (plain `BlockContainer` + `setHardness(1.0F)` like `OpenBlock`'s default), `SyncedTileEntity`/
  `SyncMap`/`SyncableTank`/`GenericTank` (own NBT + `S35PacketUpdateTileEntity` sync, throttled
  with the same SYNC/UPDATE thresholds), `VariantModelState`/`NeighbourMap` model state,
  `ItemOpenBlock`, `ItemTextureCapability`, `IItemPropertyGetter` level override (1.9+ API).
- NO fluid capabilities in 1.8.9 Forge 1722 (VERIFIED: no `fluids/capability` package in
  `forgeBin` jar). `TileEntityTank` implements the old facing-agnostic `IFluidHandler`
  (column fill/drain logic verbatim); containers handled via `FluidContainerRegistry`
  (container→tank only, exactly like 1.12.2's `tryEmptyItem` which never fills buckets FROM
  the tank). `ItemTankBlock` keeps NBT/tooltip/display-name/`fillTankItem` but no item caps.
- `FastTESR` EXISTS in 1.8.9 (VERIFIED via javap) with `(…, WorldRenderer)` signature —
  renderer port is mechanical (`BufferBuilder` → `WorldRenderer`; API exists: pos/color/tex/
  lightmap/endVertex/setTranslation all VERIFIED). `TankRenderLogic` + all connection classes +
  `TankRenderUtils` + `openmods.utils.Diagonal` (verbatim copy, same convention as
  `DisplayListWrapper`) port unchanged; `GenericTank` refs replaced with `FluidTank`
  (`getSpace()` = capacity − amount locally). Fluid sprite via `TextureMapBlocks`
  `getAtlasSprite(fluid.getStill(stack))`; still textures stitched by ported
  `FluidTextureRegisterListener` (same as 1.12.2 `ClientProxy` listener).
- Block model: static full-frame `models/block/tank.json` (verbatim `tank_frame.json`
  elements + particle) as base/fallback and inventory parent. The 1.12.2
  `openmods:variantmodel` edge-hiding is implemented natively — see "Frame connectivity"
  below (the old KNOWN DEVIATION v1 is GONE).
- Sounds: 1.8.9 `Fluid` has no sound hooks — see "Fill sound" below. XP drain:
  `player.addExperience` directly (`EnchantmentUtils.addPlayerXP` is a thin
  wrapper; only ratio math from `LiquidXpUtils` ported, `FLUID_TO_LEVELS` dropped per §19).
- `xpJuice` fluid registered as part of this feature (required by tank XP drain; §19):
  same name/props (`luminosity 10, density 800, viscosity 1500`), sounds adapted to 1.8.9
  string form. XP Bucket item itself stays a separate future feature (no `BucketFillHandler`).
- Invisible-block fix (2026-09-12): 1.8.9 `BlockContainer.getRenderType()` returns -1
  (INVISIBLE — VERIFIED via `javap`: `iconst_m1`), which is why the placed tank rendered
  nothing while the TESR fluid showed. 1.12.2 `OpenBlock` extends plain `Block` (MODEL),
  so 1.12.2 never hits this. Fix: `BlockTank.getRenderType()` returns 3 (MODEL).
- Frame connectivity (fix loop 2, 2026-09-12): 1.8.9 HAS the native counterpart —
  `ISmartBlockModel` (VERIFIED via `javap`), the era mechanism for connected textures.
  New `openblocks.client.model.TankFrameModel` (installed over the static model at
  `ModelBakeEvent`, glider-`BakeHandler` pattern): bakes the 12 `tank_edge_*`.json pieces
  once (exact 1.12.2 geometry/UVs, split from `tank_frame.json`) and combines the visible
  ones per-block from a new unlisted `TankNeighbourState` property (same 18 flags + same
  `accepts()` rule as 1.12.2 `NeighbourMap`). The 12 edge expressions are ported verbatim
  from the `expansions` in 1.12.2's blockstate.
- Fluid in inventory (fix loop 2): new `TankItemModel` (smart item model over the frame
  item model): filled tanks show a procedural fluid box (full footprint, sixteenth height
  like 1.12.2's 17 `tank_fluid_N` models; winding/UVs mirror the TESR), cached per
  fluid + level, cache cleared on every bake (sprite reload-safe). Compiler lessons: 1.8.9
  `BakedQuad` carries no sprite (dropped `setTexture`); `ISmartItemModel` extends plain
  `IBakedModel` (no `getFormat` — that comes from `IPerspectiveAwareModel`'s branch).
  Crash lesson (fix loop 3): 1.8.9 ITEM vertex format has a 5th PADDING_1B element
  (PROVED via `javap -c` on `DefaultVertexFormats`); `UnpackedBakedQuad.Builder` counts
  puts per vertex against the format, so every vertex needs the padding `put` too —
  done generically via `getElementCount()`.
- Fill sound (fix loop 2): 1.8.9 `ItemBucket` bytecode VERIFIED to reference only
  `random.fizz` (lava; water placement is silent) — the 1.12.2 fluid empty-sound has no
  1.8.9 asset. Faithful behavior: lava → `random.fizz`, everything else silent
  (replaces the wrong `random.splash`, which is the entity-splash noise).
- Faithful details kept: 16-bucket capacity, neighbour balancing + bottom-fill column logic,
  pick-block NBT (incl. the 1.12.2 quirk of normalizing `Amount` to capacity), harvest drops
  with fluid NBT, comparator output, light emission, creative-search filled-tank listing
  (gated on `tabAllSearch` like the glider's `isInCreativeTab` equivalent), recipe
  (obsidian + `paneGlass` → 2), all five `tanks` config keys + `xpToLiquidRatio`.
