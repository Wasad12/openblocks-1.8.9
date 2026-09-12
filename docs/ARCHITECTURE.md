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
- Survival drops (fix loop 6): `harvestBlock` override stashes the live TE's tank NBT
  and `getDrops` prefers the stash over a world TE lookup (the 1.8.9 break path provably
  calls `getDrops`, yet the lookup observed an empty tank — stash removes the lookup
  from the equation entirely).

### XP Drain + XP Shower (2026-09-12, Phase B plan — behavior preserved, lib adapted)

Source: `BlockXPDrain`/`TileEntityXPDrain`, `BlockXPShower`/`TileEntityXPShower`,
`EntityXPOrbNoFly`, `FXLiquidSpray`, `EnchantmentUtils` (XP math), recipes
(iron bars; iron + obsidian), `xp_drain`/`xp_shower` models + textures, lang keys
(already in our `en_US.lang`).

- No lib port: `OpenTileEntity`/`SyncedTileEntity`/`SyncMap`/`SyncableBoolean`/
  `GenericTank`/`CompatibilityUtils`/`BlockUtils` replaced with plain `TileEntity` +
  NBT + `S35` sync (spray flag only) + old `IFluidHandler` + inline AABBs.
  `FourDirections` orientation dropped for the drain (flat symmetric plate — 1.12.2's
  orientation variants render identically); shower keeps a vanilla horizontal `FACING`
  (tank side) + `POWERED`, meta-mapped like 1.12.2 (`0x8` bit).
- Shower AABB + model rotations: authored-north geometry rotated per facing
  (E:y90/S:y180/W:y270, vanilla stairs pattern — INFERRED, user eyes verify the arm
  touches the tank). `canPlaceBlockOnSide` N/S/E/W verbatim; redstone via
  `isBlockIndirectlyGettingPowered` + `onNeighborBlockChange`/`onBlockAdded`.
- Drain fills the tank BELOW via old `IFluidHandler.fill(UP, ...)` (our Tank speaks it);
  orb/player logic + `random.orb` pickup verbatim. Shower pulls 100 mB/cycle from the
  tank BEHIND (`FACING`) with an xpJuice-only guard, spawns `EntityXPOrbNoFly`
  (id 709 like 1.12.2; `moveEntity` for 1.8.9, lava plays `random.fizz` like vanilla
  1.8.9 orbs — 1.12.2's burn sound has no 1.8.9 asset).
- Orb renderer: 1.12.2 registers NONE (relies on 1.12 superclass fallback); 1.8.9 gets
  an explicit vanilla `RenderXPOrb` binding (no reliance on fallback).
- Spray FX: `EntityFX` port (`setParticleIcon` EXISTS in 1.8.9 — VERIFIED via `javap`;
  `canCollide` does not — dropped, `moveEntity` collides by default); layer 1 =
  block-atlas sprite like vanilla dig particles. Proxy gains `getParticleSettings` +
  `spawnLiquidSpray` (server: 2 / no-op).
- Harvest rules: vanilla defaults both versions (`OpenBlock` sets none) — glass drain
  and rock shower behave like vanilla glass/stone. No custom sounds beyond `random.orb`.
- Shower blockstate (fix loop 1): must enumerate facing x powered (8 combos) — 1.8.9
  falls back to the missing model for any valid state without a variant entry.
- Drain item display (fix loop 1, SUPERSEDED by fix loop 3): flat plate first got the
  verbatim vanilla flat-item `display` — wrong family (1.12.2 renders ALL THREE via
  `forge:default-block`, i.e. 3D block look, not flat).
- TPP-held block look (fix loop 2, SUPERSEDED by fix loop 3): custom
  `HeldBlockPerspective` vecmath fold via `IPerspectiveAwareModel` rendered tank/shower
  invisible in hand (non-TPP zero-matrix `new Matrix4f()` collapses to a point — PROVED
  by vecmath run; TPP fold degenerate in-game). DELETED. Fix loop 3 (faithful,
  glider-lesson): all three item JSONs carry the verbatim vanilla 1.8.9 block-item
  `display` (thirdperson [10,-45,170]/[0,1.5,-2.75]/0.375 — stone/glass, read from the
  client jar); FPP has no override (vanilla block default, like stone). `TankItemModel`
  stays as a plain `ISmartItemModel` (fluid box only, no perspective).
- Shower hitbox (fix loop 3): `unionForFacing` returned full-length axis boxes
  (N+S both z0-16, E+W both x0-16). Fixed to directional half-boxes matching the
  per-facing arm (N:z0-9, S:z7-16, E:x7-16, W:x0-9; y7-9).
- Compiler lessons (fix loop 1, all 1.9-isms caught at build): `Vec3d`→`Vec3`;
  `BlockStateContainer`→`BlockState`; `getStateForPlacement`→`onBlockPlaced` (same args);
  no `resetPositionToBB` (inlined from boundingBox); `slipperiness` is a public field.
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
- Fill sound (fix loop 2): silent for every fluid — DELIBERATE user request
  (2026-09-12; lava fizz removed too).
- Faithful details kept: 16-bucket capacity, neighbour balancing + bottom-fill column logic,
  pick-block NBT (incl. the 1.12.2 quirk of normalizing `Amount` to capacity), harvest drops
  with fluid NBT, comparator output, light emission, creative-search filled-tank listing
  (gated on `tabAllSearch` like the glider's `isInCreativeTab` equivalent), recipe
  (obsidian + `paneGlass` → 2), all five `tanks` config keys + `xpToLiquidRatio`.

### Auto Enchantment Table (2026-09-12, Phase B plan — behavior preserved, OpenMods GUI stack replaced)

Source: `BlockAutoEnchantmentTable`, `TileEntityAutoEnchantmentTable` (484 lines),
`ContainerAutoEnchantmentTable`, `GuiAutoEnchantmentTable` (screenshots = target GUI),
`TileEntityAutoEnchantmentTableRenderer` (book), `rpc/ILevelChanger`, recipe
(iii/iei/rrr iron + enchanting table + redstone), block model (12px base, 3 textures),
lang keys (already in our `en_US.lang`, incl. autoextract/autoeject/autodrink/limit/
available_power).

- NO OpenModsLib port (§9: per-feature local equivalents). The whole `openmods.gui`
  component stack + RPC + SyncMap is ported as real local equivalents under the SAME
  `openmods.*` packages (faithful port, not a vanilla lookalike): `ContainerBase`,
  `GuiConfigurableSlots` + components (Panel/Slider/Label/TankLevel/ToggleButton/Tab/
  SideSelector/Checkbox), `SyncMap` + all `Syncable*`, `GenericInventory`/
  `TileEntityInventory`/`ItemMover`, `SidedInventoryAdapter`/`SidedItemHandlerAdapter`
  (1.8.9 HAS item-handler caps — VERIFIED `CapabilityItemHandler` in the 1722
  `forgeBin` jar), `SyncedTileEntity`/`OpenTileEntity`, `CommonGuiHandler`, RPC
  interfaces. Dropped only: fixers (no 1.8.9 framework), fluid caps (don't exist —
  `IFluidHandler` + `FluidTank` like the Tank). The single invisible adaptation is the
  byte transport behind the SAME seams (`RpcCallDispatcher.senders`, `SyncRpcTarget`):
  1.8.9 `SimpleNetworkWrapper` instead of the 1.12.2 FML channel wiring.
- 1.8.9 API facts (all VERIFIED via `javap` on the 1722 `forgeBin` jar unless noted):
  `calcItemStackEnchantability` same signature; `buildEnchantmentList` is 3-arg
  (no treasure flag — 1.8.9 has no treasure enchants, faithful by construction);
  `EnchantmentData.enchantmentobj` (not `enchantment`); `ItemEnchantedBook.
  addEnchantment` is an INSTANCE method; `ItemStack.isItemEnchantable`/
  `addEnchantment` exist; `ForgeHooks.getEnchantPower` exists; `ModelBook` exists
  (TESR port is mechanical, `WorldRenderer` signature); level icons at
  `enchanting_table.png` (0/16/32,223) + lapis slot (34,46) VERIFIED present with
  content in the 1.8.9 texture (pixel check).
- TE: `SyncedTileEntity` + `GenericInventory`/`TileEntityInventory` (3 slots,
  tool=enchantable / lapis=`gemLapis` ore dict / output) + `SyncableTank` xpJuice
  (capacity = liquid-for-30-levels via restored `LiquidXpUtils.getLiquidForLevel` +
  `FLUID_TO_LEVELS`) + `SyncableSides` x4 + `SyncableFlags` + `SyncableInt` x2 +
  `SyncableEnum<Level>`, NBT (inventory + seed), `S35` + `SyncMap` updates.
  Update + `tryEnchantItem` verbatim (power check every 20 ticks, auto in/out movers,
  lapis/XP gating, seed reseed). Item caps via `SidedItemHandlerAdapter` (verbatim);
  fluids via old `IFluidHandler` (no 1.8.9 fluid caps).
- Container: ported `ContainerInventoryProvider` (3 slots at 1.12.2 coords + player
  inventory at y=93, 176x175) with its shift-click `transferStackInSlot`.
- GUI: the REAL ported `GuiConfigurableSlots` + components replicating the screenshots —
  power-limit slider (1-30), available-power label, XP tank gauge (levels display),
  L1/L2/L3 toggle button, lapis slot icon, 4 side tabs (tool/lapis/output/xp with
  pickaxe/dye/enchanted-pickaxe/bucket icons + autoextract/autoeject/autodrink labels),
  each tab with side selector + auto checkbox. Client→server edits go through the
  ported RPC (`ILevelChanger`, side/auto receivers) over the 1.8.9 transport.
  `CommonGuiHandler` dispatches `IHasGui` like 1.12.2.
- Block: plain `Block` (rock, hardness 1.0F), 0.75-height AABB with the shower-lesson
  bounds pattern (`setBlockBoundsBasedOnState` + offset collision, raytraceable),
  `getRenderType` MODEL (tank lesson), `randomDisplayTick` particles verbatim,
  `onBlockActivated` opens GUI.
- Book rotation math (`BookState`) ported verbatim (`MathHelper` + `getClosestPlayer`
  exist in 1.8.9).

### Auto Anvil (2026-09-12, Phase B plan — behavior preserved, anvil logic adapted to 1.8.9 IDs)

Source: `BlockAutoAnvil` (TwoDirections anvil, non-opaque/solid sides), `TileEntityAutoAnvil`
(3 slots tool/modifier/output, xpJuice tank for 45 levels, 4 side-maps + auto flags,
40-tick cooldown repair), `ContainerAutoAnvil` (3 slots at (14/56/110,40) + player inv),
`GuiAutoAnvil` (hammer + plus sprites, XP tank gauge in levels, 4 tabs), recipe
(iii/iai/rrr iron + anvil + redstone), block model (4 elements, 6 textures), lang keys
(already in our `en_US.lang`).

- All GUI/sync/inventory/RPC infra REUSED from the Auto Enchantment Table port (same
  `openmods.*` packages, no new framework): `SyncedTileEntity`, `SyncableSides` x4 +
  `SyncableTank` + `SyncableFlags`, `TileEntityInventory`, `SidedInventoryAdapter`/
  `SidedItemHandlerAdapter`, `ItemMover`, `ContainerInventoryProvider`/`RestrictedSlot`,
  `GuiConfigurableSlots` + components, existing `IRpcDirectionBitMap`/`IRpcIntBitMap`
  (no new RPC interfaces — the anvil has no level/power setting). One new component:
  `GuiComponentSprite` (hammer/plus/result icons) ported verbatim — `Icon.
  createSheetIcon(WIDGETS,...)`, `BaseComponent.drawSprite` and `TextureAtlasSprite.
  getIconWidth/getIconHeight` all exist in 1.8.9.
- `VanillaAnvilLogic` ported to 1.8.9 enchantment IDs (all VERIFIED via `javap` on the
  1722 `forgeBin` jar): 1.8.9 `EnchantmentHelper.getEnchantments/setEnchantments` use
  `Map<Integer,Integer>` (effect IDs, not `Enchantment` objects — the object-map form is
  1.9+), so the logic resolves `Enchantment.getEnchantmentById` per key; `isCompatibleWith`
  → `canApplyTogether` (VERIFIED exists); `getRarity()` does NOT exist (Rarity enum is
  1.9+) — cost uses `getWeight()` (VERIFIED exists) with the threshold mapping
  >=10→1 / >=5→2 / >=2→4 / else 8, which agrees EXACTLY with COMMON/UNCOMMON/RARE/
  VERY_RARE→1/2/4/8 for every vanilla enchant (weights are 10/5/2/1; INFERRED for modded
  weights, same numbers Mojang used); `AnvilUpdateEvent` EXISTS with PUBLIC fields
  (`output`/`cost`/`materialCost`, null-able output — no getters yet); `ItemEnchantedBook.
  getEnchantments` is an INSTANCE method returning `NBTTagList` (`tagCount() > 0`
  replaces `hasNoTags()`); `getCount()`→`stackSize`, `shrink`→manual decrement,
  `isEmpty`/`EMPTY`→null. Null-tolerant (null input = empty path, null modifier = empty
  path — 1.12.2 relied on EMPTY singletons). `Optional<String>` itemName kept (guava
  ships with MC); TE always passes `absent()` (no rename feature, like 1.12.2).
- TE: same shape as the enchant table (movers: output-push / tool-pull-if-empty /
  modifier-pull, auto-drink 100 mB via `fillFromSides` on xp sides, `needsTankUpdate`
  flag, `S35` + `SyncMap`, NBT inventory, `dropContents` on break, old `IFluidHandler`
  fill-only on xp sides + item-handler cap). `repairItem` verbatim (level→XP→liquid
  cost, atomic drain check, `removeModifiers`, clear tool, set output) except the sound:
  no `SoundEvents`/`playSoundAtBlock` helper in 1.8.9 — inline
  `worldObj.playSoundEffect(..., "random.anvil_use", 0.3f, 1f)` (vanilla 1.8.9 anvil
  sound string). `MAX_STORED_LEVELS` 45 (vs 30 for the enchant table).
- Block: plain `Block` (`Material.anvil`, `setStepSound(Block.soundTypeAnvil)` — VERIFIED
  inner-class field via `javap`; hardness 1.0F like every `OpenBlock`), full-cube bounds
  (1.12.2 has no custom AABB), `isOpaqueCube` false + `isSideSolid` false,
  `getRenderType` MODEL (tank lesson), GUI open + neighbour dispatch + break drops
  (enchant-table pattern). `TwoDirections` (ZN_YP/XP_YP, long axis perpendicular to
  placer) → vanilla-style horizontal `FACING` with `rotateY` placement (same
  perpendicular result, the 1.8.9-native anvil mechanism); 4-variant blockstate in the
  shower convention (authored long-axis-Z = north).
- Models: `models/block/auto_anvil.json` keeps the 4 elements/UVs verbatim but drops
  `parent: block/block` (DOES NOT EXIST in 1.8.9 — VERIFIED absent from the client jar;
  our shower block model is parentless too) and the `display.fixed` key (no `fixed`
  transform in 1.8.9); `models/item/auto_anvil.json` = parent + verbatim block-item
  `thirdperson` display (fix-loop-3 lesson, shower precedent). 6 textures copied;
  bare `auto_anvil.png` is unreferenced anywhere in 1.12.2 (VERIFIED by grep) — skipped.

### Vacuum Hopper (2026-09-12, Phase B plan — behavior preserved, nozzle model + caps adapted)

Source: `BlockVacuumHopper` (plain OpenBlock, 0.25-0.75 bounds / ~full collision /
0.3-0.7 selection, ExtendedBlockState + VariantModelState nozzle indicators,
entity-collision intake), `TileEntityVacuumHopper` (344 lines: 10-slot inventory,
xpJuice tank for 5 levels, item/xp output side-maps, vacuumDisabled flag, suction
physics + portal particles, neighbour output every 10 ticks, sneak-toggle),
`ContainerVacuumHopper` (5-wide grid + player inv at 69, 176x151 GUI),
`GuiVacuumHopper` (tank gauge in raw mB, item/xp side tabs), shapeless recipe
(hopper + obsidian + ender eye), body + 6 nozzle models + 4 textures, lang keys
(already in our `en_US.lang`).

- New ports under the same packages: `SyncableBoolean` (verbatim — `SyncableObjectBase`
  + `ISyncableValueProvider` exist; registered in preInit like the other sync types),
  `InventoryUtils.canInsertStack` (added to our existing `InventoryUtils` — 1.8.9 has
  `insertItem` but NOT `insertItemStacked` (VERIFIED absent via `javap`), so the check
  is `insertItem(handler, copy, true)` with null-tolerant leftover comparison instead
  of `insertItemStacked`; `ItemHandlerHelper.insertItem` itself exists).
  `ItemUtils.setEntityItemStack` stays a private TE helper (full `ItemUtils` pulls
  hashing deps per §19; `EntityItem.setEntityItemStack` VERIFIED to exist in 1.8.9).
- Dropped: `EntityItemProjectile` branch of the entity selector (cannon entity — NOT
  STARTED, no such entities can exist; restored with the Cannon feature), `EnumHand`
  (single-hand sneak check via `getHeldItem()` null), `IActivateAwareTile` (block
  handles sneak-toggle inline, same outcome), fluid/item capability wrappers
  (old `IFluidHandler` drain-only on xp sides + item-handler cap, enchant-table
  pattern), fixers/IncludeInterface (precedent).
- 1.8.9 API facts (all VERIFIED via `javap` unless noted): `EntityItem.
  getEntityItem()` (not `getItem()`); `EntityXPOrb.getXpValue()` (XPDrain precedent);
  `AxisAlignedBB` has NO `grow` (use `expand(3,3,3)` — identical); `getEntitiesWithinAABB`
  3-arg guava-Predicate overload exists; `spawnParticle` needs 7 doubles (block-center
  + jitter position, zero motion — the 1.12.2 4-arg call has no 1.8.9 overload);
  `OpenMods.proxy.getTicks` == `getTotalWorldTime()` (read from the lib source);
  `ExtendedBlockState` exists (unused by Tank, used here); `BlockPartFace` has NO UV
  rotation in 1.8.9 — nozzle `"rotation"` keys are silently ignored (cosmetic only,
  outer nozzle faces carry none); `addInventoryGrid` row math fits 10 slots in 2x5.
- Nozzle model (TankFrameModel precedent): new `VacuumHopperModel` ISmartBlockModel
  installed at ModelBakeEvent over the static body — body ALWAYS renders, plus one
  nozzle piece per side present in the output state. New `HopperOutputState` unlisted
  property (`Map<String,String>`, TankNeighbourState pattern) fed from TE.
  `getOutputState()` via `ExtendedBlockState`; 18 nozzle JSONs script-generated from
  the 6 side geometries × items/fluids/both textures (1.8.9 variants can't override
  textures). Item model = body parent + verbatim block-item display (shower pattern).
  Bare `vacuum_hopper.png` unreferenced in 1.12.2 (VERIFIED by grep) — skipped.
