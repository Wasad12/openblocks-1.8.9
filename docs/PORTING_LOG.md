# Porting Log

Chronological history. Newest entries go at the END. Never erase previous history (§12).

---

## 2026-09-11 — First session initialization (§15)

- Inspected workspace `C:\Users\wassi\Desktop\minecraft mods\openblocks\`:
  - `OpenBlocks-1.12.X/` — OpenBlocks 1.7.4 source for MC 1.12.2 (372 .java files). No `.git`.
    Submodule dir `OpenModsLib/` is EMPTY.
  - `OpenModsLib-1.12.X/` — OpenModsLib 0.11.4 source for MC 1.12.2 (540 .java files). No `.git`.
  - No 1.8.9 project, no `docs/`, no prior port work exists anywhere in workspace. (VERIFIED)
- Environment: git 2.54.0 available; Java 8 (2x Adoptium) + Java 11/21 present;
  test instance `1.8.9(6)` exists with unrelated mods (left untouched).
- Created `docs/` (PORT_STATUS, PORTING_LOG, FEATURES, ARCHITECTURE, KNOWN_ISSUES) + feature inventory.
- Baseline build: NOT run — there is no 1.8.9 code to build, and building the 1.12.2 tree would
  only validate the 1.12.2 toolchain (ForgeGradle 2.3, dead repos likely), not the target.
  Toolchain check (Java 8, git, instance dir) recorded in PORT_STATUS.md instead. (INFERRED decision, documented)
- Baseline git checkpoint: see `git log` (to be recorded in PORT_STATUS.md).
- Status: waiting for user's first feature instruction.

---

## 2026-09-11 — Feature: Hang Glider (Phase A — investigate, 1.12.2 source only)

Source classes (all under `OpenBlocks-1.12.X/src/main/java/openblocks/`, VERIFIED by reading):
- `common/item/ItemHangGlider.java` — item; right-click toggles `EntityHangGlider` spawn (server-side only);
  per-player `spawnedGlidersMap` (weak keys/values); `IStateItem` (deployed/inHand states).
- `common/entity/EntityHangGlider.java` — non-persistent (`writeToNBTOptional` false, empty NBT) entity
  bound 1:1 to a player+hand; `gliderMap`; synced `PROPERTY_DEPLOYED`; `onUpdate` applies glide physics
  (VSPEED_NORMAL -0.052 / VSPEED_FAST -0.176 sneak / VSPEED_MIN -0.32 / VSPEED_MAX 0.4, horizontal 0.03/0.1),
  thermal noise lift (`getNoise`, Perlin, height band 70–136, daytime, overworld, rain suppression),
  vario updates every 4 ticks; `fixPositions` (+1.2 Y offset); `updateGliders` client fixup.
- `client/renderer/entity/EntityHangGliderRenderer.java` — textured quad (2.4 half-size,
  `textures/models/hang_glider.png`), FPP/TPP + deployed/folded offsets, hidden in FPP when folded.
- `client/GliderPlayerRenderHandler.java` — on `PlayerBodyRenderEvent`: zero limb swing + 75° X-rotation
  when deployed.
- `client/ClientTickHandler.java` — render tick calls `EntityHangGlider.updateGliders(world)`.
- `client/bindings/KeyInputHandler.java` — V toggles vario, vol up/down (unbound); gated by config.
- `common/Vario.java`, `common/IVarioController.java`, `common/BeepGenerator.java` — acoustic variometer
  (javax.sound, watchdog thread; master-volume scaling).
- `common/item/ItemOBGeneric.java` + `MetasGeneric.gliderWing` (meta 0 = `glider_wing`, crafting ingredient).
- Registration: `@RegisterItem(id="hang_glider")`, `@RegisterItem(id="generic")`,
  entity id 701 (`ENTITY_HANGGLIDER_ID`), tracking 64/1/true; `Config.hanggliderEnableThermal` (default true).
- Resources: `textures/items/{hang_glider,glider_wing}.png`, `textures/models/hang_glider.png`,
  `models/item/glider_wing.json`, `models/item/hang_glider_hidden.json` (empty `{}`),
  `blockstates/hang_glider.json` (deployed=true hides item in hand perspectives via `openmods:stateitem` /
  `perspective-aware` loaders, stays visible in GUI), recipes `glider_wing_{0,1}.json` (sticks+leather),
  `hang_glider_0.json` (2 wings + stick), lang keys `item.openblocks.{hang_glider,glider_wing}.name`,
  hang_glider description, `openblocks.keybind.*` keys.

OpenModsLib deps used: `openmods.renderer.DisplayListWrapper` (pure LWJGL, copy verbatim),
`openmods.renderer.PlayerBodyRenderEvent` + ASM `PlayerRendererHookVisitor` (injects at END of
`RenderLivingBase.applyRotations`; replaced — see ARCHITECTURE.md), `openmods.model.itemstate.*`
(replaced by 1.8.9 `ItemMeshDefinition`), `openmods.state.*` (replaced), `openmods.infobook`
(dropped, infobook is an unrelated system), `openmods.Log` (replaced by log4j),
`OpenMods.proxy.isClientPlayer` (replaced by own sided proxy), config annotations (replaced by
Forge `Configuration`), `@RegisterItem`/startup helper (replaced by direct `GameRegistry` calls).

Unavoidable 1.8.9 API adaptations (no elytra, no off-hand, no DataManager, no JSON recipes,
no per-perspective item models): see ARCHITECTURE.md "Hang Glider" section.
Uncertain 1.8.9 API names to be resolved by compiler: `NoiseGeneratorPerlin.getValue`,
`SoundCategory` package for `BeepGenerator`, `World.getCurrentDate` — see KNOWN_ISSUES.md.

---

## 2026-09-11 — Feature: Hang Glider (Phases C–E — implement, build, deploy)

- New project `OpenBlocks-1.8.9/` (ForgeGradle 2.1.3, Forge 11.15.1.1722, MCP snapshot_20160209,
  Gradle 2.14 wrapper copied from reference tree, Java 8 Adoptium 8.0.492.9, own gradle home
  `C:\Users\wassi\.gradle-189`). 16 .java files + 9 resources, per Phase B plan (see ARCHITECTURE.md).
- Compile fixes (all caught by compiler, 4 build iterations):
  1. `NoiseGeneratorPerlin.getValue` → `func_151601_a` (VERIFIED unrenamed in snapshot_20160209 via
     `srgs/mcp-srg.srg`; methods.csv only lists renamed methods).
  2. `net.minecraft.client.renderer.block.model.{ModelBakery,ModelResourceLocation}` →
     `net.minecraft.client.resources.model.*` (1.8.9 package layout).
  3. Own typo: `EntityHangGlider.java` had `package openblocks.common;` → fixed to
     `openblocks.common.entity` (this masked all other errors as "package does not exist").
  4. Added dropped imports `openblocks.common.{IVarioController,Vario}` to `EntityHangGlider`.
- Compiler-VERIFIED 1.8.9 facts (close KNOWN_ISSUES #3): `SoundCategory` =
  `net.minecraft.client.audio.SoundCategory` + `getSoundLevel` exists; `World.getCurrentDate()` exists;
  `IRenderFactory` + factory-based `registerEntityRenderingHandler` exists;
  `ModelLoader.setCustomMeshDefinition` + `ModelBakery.registerItemVariants` exist;
  `RenderLivingEvent.Pre`, `ShapedOreRecipe`, `Configuration.get(cat,key,default,comment)` all exist.
- Result: BUILD SUCCESSFUL incl. `:reobfJar` → `OpenBlocks-1.8.9-1.0.0.jar` (53,687 bytes; classes,
  all glider assets, expanded mcmod.info VERIFIED inside).
- Deployed to `1.8.9(6)/minecraft/mods` (no prior OpenBlocks build; unrelated mods untouched).
- Status: awaiting user test. Known deviation for test: deployed item hidden in ALL contexts incl.
  inventory GUI (1.12.2 keeps GUI visible); `RenderLivingEvent.Pre` body-rotation is INFERRED equivalent.

---

## 2026-09-11 — Feature: Hang Glider (verification pass + stable_22 — user-requested)

- Fidelity verification: every ported file diffed against its 1.12.2 original
  (`git diff --no-index`). Results:
  - `EntityHangGlider`: all physics/thermal/vario/NBT/fixPositions logic identical; only API
    adaptations (`worldObj`, `getDimensionId`, `getBiomeGenForCoords`, DataWatcher 17,
    no `EnumHand`, no elytra check) + `isClientPlayer` via own proxy. VERIFIED faithful.
  - `ItemHangGlider`: toggle logic identical; hand branch removed (impossible with one hand);
    `IStateItem`/property-override dropped (replaced by mesh def — the 1.12.2 `hidden` override
    value is read by nothing, VERIFIED by grep); `BookDocumentation` dropped (unrelated system).
  - `EntityHangGliderRenderer`: 1-line change (`owner == thePlayer`); all GL math identical.
  - `GliderPlayerRenderHandler`: same body, `RenderLivingEvent.Pre` instead of ASM event (INFERRED).
  - `Vario`, `IVarioController`, `DisplayListWrapper`: byte-identical (empty diff). `BeepGenerator`:
    only logger + `SoundCategory` import changed.
  - `ClientTickHandler`/`KeyInputHandler`: glider/vario parts identical; glasses/brick parts dropped
    (unrelated systems). `Config`: same category/name/default/comment for `enableThermal`.
  - Resources: 3 PNGs + 2 JSON models byte-identical (SHA256); `en_US.lang` content identical to
    `en_us.lang`; new `hang_glider.json` mirrors 1.12.2 blockstate `defaults`; recipes match JSON
    patterns/ingredients; entity id 701 + tracking 64/1/true preserved.
  - ONE unfaithful bit found and fixed: `ItemOBGeneric.getSubItems` lacked the lib's
    `isInCreativeTab` gate (wing would list in every tab). 1.8.9 has no `isInCreativeTab`
    (added 1.9 — compiler + stable_22 methods.csv VERIFIED); implemented equivalent
    `tab == getCreativeTab() || tab == CreativeTabs.tabAllSearch` (`tabAllSearch` VERIFIED in
    stable_22 fields.csv), matching 1.12.2 `isInCreativeTab` semantics (own tab + search).
- Mappings switched snapshot_20160209 → **stable_22** (user instruction; recorded in
  `gradle.properties`). stable_22 artifact `mcp_stable-22-1.8.9` resolves fine.
  `func_151601_a` is STILL searge-named in stable_22 (VERIFIED absent from methods.csv) — no code
  change needed; the only rebuild error was the `isInCreativeTab` gate above.
- Rebuilt (`:reobfJar`, BUILD SUCCESSFUL) → `OpenBlocks-1.8.9-1.0.0.jar` (53,762 bytes), redeployed to
  `1.8.9(6)/minecraft/mods` (overwrote snapshot-mapped build; unrelated mods untouched).
- Status: awaiting user test (unchanged).

---

## 2026-09-11 — Feature: Hang Glider (fix loop 1 — user test: missing item models, TPP body broken)

User runtime evidence (screenshots): logic fine (deploy/fold/sneak-speed), FPP fine; item icons missing
(purple-black, giant in hand); third-person player body positioned/oriented wrong. Investigation:

1. Item models missing — `fml-client-latest.log` PROVED three independent causes:
   a. TIMING: first model bake runs in `Minecraft.startGame`, before mod `init()`. 1.12.2 registers
      item models during preInit (lib startup helper); ours ran in `init()` → variants unknown at
      bake (`openblocks:generic#inventory` fallback in log). Fix: registration moved to
      `ClientProxy.preInit()`.
   b. VARIANT NAME FORM: 1.8.9 resolves item model FILES from the registered variant-name strings,
      and `ModelResourceLocation.toString()` form (`name#inventory`) breaks file lookup. Working
      1.8.9 mods register PLAIN strings (no `#inventory`). Fix: `ModelBakery.addVariantName` with
      plain `openblocks:{hang_glider,hang_glider_hidden,glider_wing}` (mesh defs return the MRLs).
      Note: the same failure shape hits ActuallyAdditions r26's crystal hoe in this instance —
      corroborates it's a 1.8.9 mechanism trap, not our files (which are byte-identical to 1.12.2).
   c. EMPTY MODEL: `hang_glider_hidden.json` (`{}`) is accepted by 1.12.2 but 1.8.9 throws
      `JsonParseException: BlockModel requires either elements or parent, found neither`, and a
      parentless `builtin/generated` model bakes to null (missing). Fix: hidden model is now
      `{"parent": "builtin/entity"}` (BuiltInModel with no TESR bound renders nothing).
2. TPP body broken — Forge 1.8.9 patch PROVED `RenderLivingEvent.Pre` fires at the START of
   `doRender` (pre-push, world-origin matrix), while the 1.12.2 ASM hook fires at the END of
   `applyRotations`. Old code rotated around the world origin. Fix: conjugate the 1.12.2 rotation
   into the earlier point — push, translate(x,y,z), yaw-flip, R75, yaw-flip-inverse,
   translate(-x,-y,-z) in Pre; pop in Post. Net composite is bit-identical to 1.12.2
   (translate*corpse*R75*scale*shift). Yaw uses the frame's render partial ticks (captured from
   `RenderTickEvent`, since 1.8.9 exposes no partial ticks in the event and `Minecraft.timer` is
   private). Death/Dinnerbone branches omitted: deployed implies alive, and omitting them from the
   conjugated pair is exactly equivalent anyway.
- Rebuilt (`:reobfJar`, BUILD SUCCESSFUL) → `OpenBlocks-1.8.9-1.0.0.jar` (54,991 bytes), redeployed.
  Two compile iterations: `Minecraft.timer` is private → render partial ticks now captured in
  `ClientTickHandler`; `MinecraftForgeClient.getRenderPartialTicks()` doesn't exist in 1.8.9.
- Status: awaiting user retest of items in inventory/hand + TPP body while gliding.

---

## 2026-09-11 — Feature: Hang Glider (fix loop 2 — TPP confirmed fixed, models still missing)

- User confirmed TPP/second-person body fixed (conjugation works). Item models STILL missing in new
  build (fresh log: same bake failures with preInit+plain-name registration).
- Root-cause hunt: byte-compared JEI (no model APIs referenced — adds no items in 1.8.9, proves
  nothing); TE uses `ModelLoader.setCustomModelResourceLocation` (MRL form) in its client proxy and
  renders with zero log errors. Vanilla universal bucket = mesh def + MRL variants, also proven.
  Conclusion: match the proven winners exactly — REVERTED to MRL-form registration
  (`registerItemVariants` with MRLs + mesh def for the glider, `setCustomModelResourceLocation`
  for the wing meta), still in preInit. The `addVariantName`-plain-strings detour did not help;
  recorded here so it isn't retried.
- Hidden model (`builtin/entity` parent) and conjugation rotation kept (both verified/fixed).
- Rebuilt (`:reobfJar`, BUILD SUCCESSFUL) → `OpenBlocks-1.8.9-1.0.0.jar` (54,311 bytes), redeployed.
- OPEN: glide distance shorter than 1.12.2 (user: ground jump goes ~6 blocks in 1.12.2, ~0.5 here).
  Physics code is diff-verified identical; server log shows no "moved wrongly/quickly" corrections.
  Cause not yet found — needs discriminating observations from user (wing visually deployed during
  hop? slow fall felt? standing or moving jump? survival or creative?). See KNOWN_ISSUES.

---

## 2026-09-11 — Feature: Hang Glider (fix loop 3 — item models, root cause found)

- Reflection dump PROVED registration contents are correct
  (`glider_wing#inventory`, `hang_glider#inventory` + `hang_glider_hidden#inventory` in the static
  map), yet the bake never attempts the hidden variant and file lookup fails for existing files.
  Analysis: on this Forge line (1.8.9-11.15.1.2318 runtime) the MRL-style `name#inventory` strings
  break item-model FILE lookup (no `#` stripping), and the deprecated `addVariantName` helper's
  strings never reach the bake. Fix, matching vanilla/1.8.9 convention:
  - `ModelBakery.registerItemVariants` with PLAIN `ResourceLocation`s (no MRL, no `#inventory`),
    exactly like vanilla's own variant names (`bow`, `coal`, ...).
  - Restored 1.8.9-format `assets/openblocks/blockstates/{hang_glider,hang_glider_hidden,
    glider_wing}.json` (`variants.inventory.model`), the 1.8.9-native item-variant mechanism and
    the counterpart of 1.12.2's `blockstates/hang_glider.json`.
  - Mesh definitions + `setCustomModelResourceLocation` + `builtin/entity` hidden model unchanged.
  - Removed answered debug probes (resource dump, variant dump); server-side physics trace kept.
- Rebuilt (`:reobfJar`, BUILD SUCCESSFUL) → `OpenBlocks-1.8.9-1.0.0.jar` (55,832 bytes), redeployed.
- Status: awaiting user test of item textures + deploy+jump for physics trace.

---

## 2026-09-11 — Feature: Hang Glider (fix loop 3 — bake forensics, plain-only experiment)

- `openblocks:block/glider_wing` in bake log PROVED blockstate `model` values resolve under
  `models/block/` (wrong dir for items) → 1.8.9 blockstates are useless for item variants; DELETED
  the three blockstate JSONs again. (User correctly noticed our JSONs were thinner than 1.12.2's —
  1.12.2 relies on `forge_marker` custom loaders that don't exist in 1.8.9.)
- Reflection dump (`VARIANTDGB`, since removed) PROVED static registration map contents are exactly
  right (`glider_wing#inventory`, `hang_glider#inventory` + `hang_glider_hidden#inventory`), yet the
  bake only ever attempts normal+wing, never hidden — mechanism of the drop still unexplained.
- TE ships item models ONLY (`models/item/*.json`, no item blockstates) and renders with zero log
  errors; its only model call is `ModelLoader.setCustomModelResourceLocation`. AA r26's crystal hoe
  fails the same way ours does (shared 1.8.9 mechanism trap, not our files).
- Decisive experiment (`bc7cb6d`, DEPLOYED, UNTESTED): all `#` sources eliminated — plain
  `ResourceLocation`s in `registerItemVariants`, mesh defs (no `setCustomModelResourceLocation`),
  no blockstates. Next session: read the bake log — silence means file-first works and the bug moves
  to render-time; persistent `isn't found` means file-first lookup itself is broken on 2318 and the
  next option is a custom `ICustomModelLoader` with explicit `models/` paths.
- TEMPORARY `GLIDERDBG` server trace still active in `EntityHangGlider.onUpdate` (short-glide
  diagnosis). Needs one deploy+jump+`latest.log`, then MUST be reverted.
- Repo hygiene: reference trees flagged `assume-unchanged` (see PORT_STATUS.md) —phantom 1369-file
  `git status` noise from environmental mtime churn; content proven identical.

---

## 2026-09-11 — SESSION HANDOFF (next session starts here)

- State: Hang Glider IN PROGRESS. TPP rotation FIXED (user-confirmed). Item models BROKEN
  (experiment `bc7cb6d` deployed, bake log UNREAD). Glide distance OPEN (trace armed, jump untested).
- Read FIRST: `docs/PORT_STATUS.md` (has ordered next actions), then this log tail, then
  `git log --oneline -5`. Tree is clean (`git status` = 0 only because of assume-unchanged flags).
- Do NOT trust memory of this session's theories — several were disproven (MRL-`#` poisoning via
  `addVariantName` detour, split-call varargs loss, init-vs-preInit as sole cause). What is VERIFIED
  is marked VERIFIED; the rest is open. Next actions are in PORT_STATUS.md.

---

## 2026-09-11 — Feature: Hang Glider (bake mechanism VERIFIED + GLIDERDBG trace analyzed)

Bake mechanism (all VERIFIED against 1.8.9-11.15.1.1722 `forgeBin` bytecode via `javap` + Forge
1.8.9-branch `ModelLoader.java` / `ModelLoaderRegistry.java` / `ModelBakery.java.patch` sources):

1. Blockstates can NEVER serve item models: `ModelBlockDefinition$Variant$Deserializer`
   bytecode PROVES `makeModelLocation` = `new ResourceLocation(domain, "block/" + path)`.
   Any blockstate `model` value resolves under `models/block/`. Deleting our item blockstates
   (again) in `bc7cb6d` was correct; do NOT restore them.
2. The old "MRL `#inventory` strings break file lookup (no `#` stripping)" theory is WRONG and
   must not be used: Forge's `ModelBakery.java.patch` PROVES `getItemLocation` strips `#.*`
   before mapping to `domain:item/path`. Plain `openblocks:hang_glider` and MRL
   `openblocks:hang_glider#inventory` hit the SAME file. Form is irrelevant at lookup level.
3. File-first path for items: `ModelLoader.loadItems` → `getItemLocation(s)` =
   `domain:item/path` → `ModelLoaderRegistry.getModel` → `getActualLocation` prepends
   `models/` → `VanillaLoader` → `models/item/<name>.json`. Our files exist at exactly those
   paths (VERIFIED inside deployed JAR). Mechanically the bake SHOULD succeed — the observed
   `FileNotFoundException` is still unexplained, which is exactly what the `bc7cb6d` bake log
   will discriminate. (Reflection dumps already PROVED variant strings reach the bake.)
4. `registerVariantNames` (patched) merges static `customVariantNames` into the bake's
   `variantNames` — registration timing (preInit) is correct and sufficient.
5. Render-time lookup is consistent in `bc7cb6d`: plain-registered `s` → memory
   `getInventoryVariant(s)` = MRL(s, inventory) = exactly what the mesh defs return, and
   `ItemModelMesherForge` gets mesh defs via `ModelLoader.onRegisterItems`. If the bake log
   goes silent but items still render missing, the bug is in mesher/render-time, not files.

GLIDERDBG trace (fml-client-latest.log 21:46 run, e15197d build — a FULL standing jump WAS
captured, lines ~887–916; `latest.log` is the same session, no newer run exists):

- Ascent 0.42→0.003 over ~7 ticks with zero horizontal push (correct: `motionY < lastMotionY(0.0)`
  is false while rising — identical to 1.12.2 by design), then glider engages at -0.078,
  resets fall to **-0.212** and adds 0.03/tick horizontal for ~5 ticks before landing.
- -0.212 EXACTLY equals `VSPEED_NORMAL + (-0.5)*(-VSPEED_MIN)` = -0.052-0.16 → the RAIN
  suppression branch fired (INFERRED: it was raining, and/or night). Rain sink (-0.212) cuts
  airtime ~4x vs dry (-0.052).
- `motionZ=0.0` the entire flight → NO forward/strafe input. Total horizontal ≈ 0.25 blocks.
  A 6-block 1.12.2 hop REQUIRES holding forward (walk speed over a long slow fall) and dry
  day air. Current evidence points to test methodology (no W key + rain), NOT a port bug.
  Controlled retest protocol: day + clear weather, hold W from jump, compare distances.

No code changed this session (read-only analysis). Deployed JAR (55,643 bytes, `bc7cb6d`) VERIFIED
matching current source (`git diff bc7cb6d..HEAD` = docs only; JAR lists `models/item/*.json`,
no blockstate JSONs). `fml-client-latest.log`/`latest.log` (21:47) predate the deployed build
(21:55) — the decisive bake log is still UNREAD. Next: user test run (see PORT_STATUS.md).

---

## 2026-09-11 — Feature: Hang Glider (fix loop 4 — bc7cb6d FAILED, 2318 ground truth, probe)

User test of `bc7cb6d` (22:26 run): item models STILL missing — purple-black 3D cubes in
hotbar/hand (the missing-model fallback), `Glider Wing` label OK (lang works). Screenshots show
snowy biome, snowing. One more standing jump captured (no forward input again, -0.212 rain/snow
sink) — controlled glide retest (day/clear/hold W) STILL pending.

Decisive bake result: `Item json isn't found` PERSISTS for plain-registered names
(`openblocks:glider_wing#inventory`, `openblocks:hang_glider#inventory`), now with FULL stack
traces (2318 line numbers): `ModelLoader.loadItems:255` → `getModelBlockDefinition:211` throws
`Could not load model definition` ← vanilla `getModelBlockDefinition:165` wraps FNFE for
`openblocks:blockstates/{hang_glider,glider_wing}.json` (expected — blockstates deleted).
Notable: `hang_glider_hidden` is NEVER attempted in either bake, exactly as in fix loop 3 when
the reflection dump PROVED it was registered. The loss is between `customVariantNames` and the
bake loop, not in our call.

2318 ground truth (VERIFIED against the LOCAL runtime
`forge-1.8.9-11.15.1.2318-1.8.9-universal.jar` via `javap` — no 1722-vs-2318 drift):
`ModelLoaderRegistry.getModel` calls `getActualLocation` (prepends `models/`);
`VanillaLoader.loadModel` matches the 1.8.9-branch source; the `getModelLocation` override
(`path + ".json"`, SRG `d`) is present. Runtime WILL request
`assets/openblocks/models/item/*.json`, which EXIST in the deployed JAR (re-VERIFIED entry by
entry) under a working pack (`pack_format: 1`, translations resolve). The FNFE is paradoxical
at static level — the requested path string must be confirmed empirically.

Next step (DEPLOYED, UNTESTED): TEMPORARY passive `MODELPROBE` in `ClientProxy.preInit`
(an `ICustomModelLoader` that only LOGS every `openblocks`-domain location the bake requests
and always declines, + a direct `getResource("openblocks:models/item/hang_glider.json")`
visibility check). Zero behavior change. Build: `:reobfJar` BUILD SUCCESSFUL →
`OpenBlocks-1.8.9-1.0.0.jar` (57,119 bytes, new `ClientProxy$4` VERIFIED inside), deployed to
`1.8.9(6)/minecraft/mods` 22:33 (unrelated mods untouched). User only needs launch-to-menu +
quit (both bakes run during startup); then read `[MODELPROBE]` lines. `GLIDERDBG` trace kept.
Probe MUST be removed before any release build.

---

## 2026-09-12 — Feature: Hang Glider (glide distance CLOSED, probe v1 results)

- Glide distance: user confirmed the short hop happens in rain/snow and matches the original
  1.12.2 behavior (the rain-suppression branch). NOT a port bug — CLOSED. (`GLIDERDBG` trace
  still in the deployed build; revert together with the probe removal to save a build cycle.)
- MODELPROBE v1 results (01:20 run, passive logging build):
  1. Direct `getResource("openblocks:models/item/hang_glider.json")` in preInit → OK. File visible.
  2. Bake requests exactly `openblocks:models/item/{glider_wing,hang_glider,hang_glider_hidden}`
     (all three incl. hidden — the earlier "hidden never attempted" reading was wrong; the
     reflection-dump era predates `bc7cb6d`).
  3. Interleaved order PROVES `hang_glider_hidden` (`{"parent": "builtin/entity"}`) bakes
     SILENTLY (no `isn't found`, no missing-definition error), while `glider_wing` +
     `hang_glider` (`{"parent": "item/generated", ...}`) throw FileNotFoundException. Same dir,
     same manager, same second — content is the only discriminator.
  4. Cross-check: ActuallyAdditions' failing hoe is a CASE mismatch in AA's own jar
     (`itemHoeCrystalblue.json` on disk vs `itemHoeCrystalBlue` variant) — explains AA, not us
     (our names match exactly; deployed-JAR entries re-VERIFIED byte-for-byte with real sizes).
- Next step (DEPLOYED 01:29, 57,507 bytes): MODELPROBE v2 — `accepts()` now also does a
  re-entrant `getResource(path + ".json")` INSIDE the bake loop and logs OK/FAIL per file.
  Binary answer: if wing FAILS but hidden OKs at bake time, the manager can't see our files
  during the bake (pack/timing mystery); if both OK, the FNFE comes from downstream
  (armature/parent chain) and the probe moves there. User needs launch-to-menu + quit only.

---

## 2026-09-12 — Feature: Hang Glider (probe v2 result + v3 manager-capture)

Probe v2 (01:31 run) is luminosity-grade paradoxical: INSIDE the bake loop, same thread,
microseconds before each failure, game-manager `getResource(models/item/<name>.json)` logs
OK for ALL THREE files — then VanillaLoader throws FileNotFoundException for wing+glider
(and hidden bakes silently). Same strings, same thread. Eliminated this session: every other
`ICustomModelLoader` in 2318 (B3D/OBJ/Fluid/ItemLayer/MultiLayer/DynBucket — all reject our
paths; VERIFIED via `javap` on the runtime jar), duplicate jars/packs (mods/1.8.9 empty,
resourcepacks empty, exactly one OpenBlocks jar), jar-entry corruption (all entries present
with real sizes). Remaining variable: the bake's resource manager may NOT be the game manager
(listener wiring), or the FNFE comes from a downstream step (armature/parent).
DEPLOYED 01:35 (58,002 bytes): MODELPROBE v3 captures the reload-listener manager into
`ClientProxy.probeManager` and, per requested file, logs identity hashes of game vs bake
managers plus `getResource` OK/FAIL through EACH. That discriminates same-manager
(downstream bug) from divergent-managers (pack-list bug) in one run. Launch-to-menu + quit.

---

## 2026-09-12 — Feature: Hang Glider (probe v3 result + v4 instrumented call)

Probe v3 (01:37 run): game and bake managers are THE SAME object
(`SimpleReloadableResourceManager@204d101`), and `getResource` through it logs OK for ALL
THREE files — yet the bake's `getModel` still throws FileNotFoundException for wing+glider.
Eliminated: all other 2318 loaders (none accept our paths), duplicate jars/packs, jar-entry
corruption. Conclusion: the FNFE must come from a DOWNSTREAM step inside
`VanillaLoader.loadModel` (armature/parent chain), NOT the main file lookup — the exact
path string was never visible because Forge logs only the variant, never the exception.
DEPLOYED 01:39 (58,745 bytes): MODELPROBE v4 runs the EXACT failing call itself
(`ModelLoaderRegistry.getModel` on the file-form location, re-entrancy-guarded) inside
`accepts()` and logs the FULL throwable cause chain incl. messages. The FNFE message names
the real path. Side note: if the instrumented call succeeds, its result lands in the
registry cache and the bake may heal itself that run — also diagnostic either way.

---

## 2026-09-12 — Feature: Hang Glider (probe v4 result + v5 reflection)

Probe v4 (01:40 run) was supposed to name the path — instead it deepened the paradox:
our instrumented `ModelLoaderRegistry.getModel(openblocks:item/glider_wing)` SUCCEEDS
(`VanillaModelWrapper`) from inside the bake loop, and the bake's own identical call then
throws FileNotFoundException. Same thread, same manager (`@204d101` for game AND bake),
milliseconds apart. Eliminated: every other 2318 loader (none accept our paths),
duplicate jars/packs, jar-entry corruption. Remaining suspects: (a) the FNFE comes from
`loadAnyModel`'s `resolveDependencies` step (parent chain), not the file lookup;
(b) `ModelLoader.resourceManager` (the field vanilla `loadModel` actually uses) is a THIRD,
stale manager object, not the reload-listener one.
DEPLOYED 01:45 (59,199 bytes): MODELPROBE v5 reflects `VanillaLoader.instance.getLoader()`
→ `ModelBakery.resourceManager`, logs its identity/class/domains, and tests OUR file + a TE
file + a vanilla file through it. That corners (b) definitively; if (b) is clean, v6 will
replicate `resolveDependencies` step by step.

---

## 2026-09-12 — Feature: Hang Glider (ROOT CAUSE FOUND + fixed, probes removed)

User asked to stop probing and rewrite the JSONs faithfully from 1.12.2 — that broke the case:
1.12.2 `glider_wing.json` uses `"parent": "item/generated"`, which exists in 1.12 but
**`models/item/generated.json` DOES NOT EXIST in 1.8.9 vanilla** (VERIFIED: 515 item JSONs in
the 1.8.9 client jar, no generated/handheld files). 1.8.9 vanilla items
(e.g. `diamond_sword.json`, read from the client jar) use `"parent": "builtin/generated"` —
handled in-code via `ModelBakery.MODEL_GENERATED`, no file needed.
Full causal chain (fits every observation): our JSONs loaded fine; then
`loadAnyModel` → `resolveDependencies` tried the parent `minecraft:item/generated` →
FileNotFoundException → `loadItems` logs `Item json isn't found` (it only logs the variant,
never the path — that misdirected four probe cycles). `hang_glider_hidden` always worked
because `builtin/entity` short-circuits dependency loading. AA's crystal hoe fails the same
way (its JSONs use the 1.9+ `item/handheld` parent). TE works (uses builtin-compatible parents).
Why the probes confused us: the instrumented `getModel` used the STATIC registry path which
skips `resolveDependencies` — so it succeeded while the bake's instance path (with deps) failed.
FIX (faithful 1.8.9 adaptation, same flat-sprite result, same textures): both JSONs rewritten
with `"parent": "builtin/generated"`, textures untouched. All TEMPORARY debug removed in the
same build (`MODELPROBE` loader + helpers from `ClientProxy`, `GLIDERDBG` from
`EntityHangGlider`; glide issue already CLOSED as 1.12.2-consistent weather behavior).
Build: `:reobfJar` BUILD SUCCESSFUL → `OpenBlocks-1.8.9-1.0.0.jar` (55,189 bytes; fixed JSONs
VERIFIED inside), deployed 01:50 (unrelated mods untouched). Awaiting user visual confirmation.

---

## 2026-09-12 — Feature: Hang Glider (inventory fixed, held-item scale wrong)

User confirmed inventory/GUI textures now render (bake fixed ✓), but HELD items render
oversized/wrong vs 1.12.2 (FPP giant item, TPP wrong scale) — comparison screenshots provided.
Cause: our JSONs have no `display` block. In 1.12.2 the transforms come from the
`item/generated` PARENT; 1.8.9's `builtin/generated` base carries none, so vanilla 1.8.9 items
(e.g. `diamond_sword.json`, read verbatim from the client jar) declare their own `display`.
Fix (faithful adaptation): both JSONs given the standard vanilla 1.8.9 `display` block
(thirdperson rotation [0,90,-35] / translation [0,1.25,-3.5] / scale 0.85;
firstperson rotation [0,-135,25] / translation [0,4,2] / scale 1.7 — copied verbatim from
1.8.9 `diamond_sword.json`, the 1.8.9-native equivalent of 1.12.2's inherited transforms).
Build: `:reobfJar` BUILD SUCCESSFUL → 55,400 bytes, deployed 02:01. Awaiting visual check
of held item in FPP + TPP.

---

## 2026-09-12 — Feature: Hang Glider (FPP identical, TPP thirdperson corrected)

User: FPP now identical ✓, TPP better but still off (comparison screenshots). Cause: my
`thirdperson` values were sword values, but 1.12.2 inherits `thirdperson_righthand` from
`item/generated`. Fetched the REAL 1.12.2 `item/generated.json` display
(mcasset.cloud 1.12.2): thirdperson_righthand rot [0,0,0] / trans [0,3,1] / scale 0.55;
firstperson_righthand rot [0,-90,25] / trans [1.13,3.2,1.13] / scale 0.68. Fix: `thirdperson`
now carries the 1.12.2 values verbatim (key renamed to the 1.8.9 spelling);
`firstperson` DELIBERATELY untouched (sword values, user-confirmed identical in FPP —
1.12.2's held-glider FPP evidently doesn't use generated's firstperson as-is, don't break it).
Ground/head/fixed keys NOT ported (no 1.8.9 equivalents; vanilla 1.8.9 items omit them too).
Build: `:reobfJar` BUILD SUCCESSFUL → 55,388 bytes, deployed 02:12. Awaiting TPP check.

---

## 2026-09-12 — Feature: Hang Glider (generated-TPP rejected, default-TPP approach)

User: 1.12.2's real values ([0,0,0]/[0,3,1]/0.55) render "just wrong" in 1.8.9 TPP (tiny, flat,
floating at leg) — same numbers ≠ same pixels. Root mechanism found in Forge 1.12.x
`ForgeBlockStateV1.Transforms` source: 1.12.2 wraps every display transform in
`blockCenterToCorner` (half-block frame shift) which 1.8.9 lacks, so 1.12.2 numbers cannot
transfer literally; its `forge:default-item` === generated values, confirming 1.12.2 renders
the glider as a PLAIN flat item (no tool grip anywhere in its chain).
Faithful-port conclusion: 1.12.2-default-item ≡ 1.8.9-no-display ("whatever this version does
for normal flat items" — cf. vanilla apple/paper with no display block). Fix: `thirdperson`
REMOVED from both JSONs (1.8.9 vanilla defaults take over); `firstperson` KEPT at sword values
(user-confirmed identical FPP — perspectives are independent, don't touch what matches).
Textures are 16x16 (VERIFIED), so no size surprise either way.
Build: `:reobfJar` BUILD SUCCESSFUL → 55,337 bytes, deployed 02:21. Awaiting TPP check against
the sane vanilla-default baseline.

---

## 2026-09-12 — Feature: Hang Glider (TPP hybrid: sword grip + 1.12.2 scale)

User: vanilla-default TPP "still wrong" (big flat flag left of torso). Scoreboard:
sword-grip TPP "better", generated-values TPP "just wrong", default TPP "still wrong".
Reading: orientation family must be tool-grip (sword rotation/translation puts it AT the hand
in 1.8.9's pipeline); the salient delta vs 1.12.2 is SIZE (1.12.2 thirdperson scale 0.55).
Fix: `thirdperson` = sword rotation [0,90,-35] + sword translation [0,1.25,-3.5] + 1.12.2
scale 0.55; `firstperson` untouched (confirmed). Both JSONs.
Build: `:reobfJar` BUILD SUCCESSFUL → 55,401 bytes, deployed 02:27. Awaiting TPP check.

---

## 2026-09-12 — Feature: Hang Glider (1.10.X checked, TPP scale tuning)

User suggested 1.10.X JSONs — fetched from GitHub: **byte-identical to 1.12.X** for both files
(same blockstate with `forge:default-item`, same `item/generated` wing). No new signal; the
1.9 hand-rendering rework sits between 1.8.9 and 1.10+, so 1.8.9 needs its own numbers.
New approach — single-variable empirical convergence on the user-preferred sword grip:
in-shot item/player px ratios (same default TPP camera both versions) suggest ours renders
~2x too big; scale 0.55 → 0.4 (conservative step, rotation/translation untouched). Both JSONs.
Build: `:reobfJar` BUILD SUCCESSFUL → 55,399 bytes, deployed 02:34. User compares SIZE now;
tilt/position next only if size lands.

---

## 2026-09-12 — Feature: Hang Glider (TPP "just like redstone")

User: hybrid "still wrong but smaller", then gave the decisive direction — copy redstone.
Read 1.8.9 vanilla `redstone.json` (+paper/stick/bow) from the local client jar: vanilla FLAT
items (redstone, paper) share one display block — thirdperson rot [-90,0,0] / trans [0,1,-3] /
scale 0.55, firstperson IDENTICAL to our current sword values (so FPP cannot change — good).
That [-90,0,0] X-rotation is vanilla's flat-item-in-fist look; everything I tried before
(tool-grip yaw-90 family, generated flat values, bare defaults) was a different look family.
Applied redstone's thirdperson VERBATIM to both JSONs (firstperson untouched, already equal).
Build: `:reobfJar` BUILD SUCCESSFUL → 55,402 bytes, deployed 02:40. Awaiting TPP check.

---

## 2026-09-12 — Feature: Hang Glider (TPP CONFIRMED identical, redstone wins)

User: redstone-verbatim thirdperson renders EXACTLY like 1.12.2 ✓ (user's call — suggested it
after the hybrid). Final state of both item JSONs: parent `builtin/generated`, 1.12.2-identical
textures, `thirdperson` verbatim vanilla-redstone ([-90,0,0]/[0,1,-3]/0.55), `firstperson`
sword values (user-confirmed identical earlier; equal to redstone's firstperson anyway).
Scoreboard: bake ✓, inventory/GUI ✓, FPP ✓, TPP ✓, TPP body rotation ✓ (earlier),
glide distance CLOSED as 1.12.2-consistent weather behavior ✓. Remaining known deviation:
deployed glider hidden in ALL contexts incl. GUI (1.12.2 keeps GUI visible — impossible in
1.8.9, documented in ARCHITECTURE.md). No TEMPORARY code left in tree (probes + GLIDERDBG
all reverted). Feature awaits explicit user completion confirmation (§7).

---

## 2026-09-12 — Feature: Hang Glider (GUI deviation fix: perspective-aware model)

User wants the last deviation gone too: deployed glider must stay visible in inventory GUI
(hidden only in hands), exactly like 1.12.2's `openmods:perspective-aware` model. 1.8.9 Forge
has the NATIVE counterpart — verified present in the 2318 runtime via `javap`:
`ISmartItemModel.handleItemState` (called from Forge-patched vanilla `ItemModelMesher`,
patch fetched), `IPerspectiveAwareModel.handlePerspective` (called from
`ForgeHooksClient.handleCameraTransforms`, which patched `RenderItem` routes EVERY context
through — hand AND GUI, patch fetched + bytecode verified), `ModelBakeEvent.modelRegistry`.
New `openblocks.client.model.GliderItemModel` (smart + perspective-aware wrapper installed
over the normal baked model at every `ModelBakeEvent`): folded → plain model (unchanged);
deployed → self in hands → empty model, elsewhere → normal model. Mesh def now always
resolves normal (switching moved into the wrapper, the only place with stack+context).
Compiler schooled the interface shapes (1.8.9 `IBakedModel` = face/general quads +
`isBuiltInRenderer`, no state-based `getQuads`). One compile iteration.
Build: `:reobfJar` BUILD SUCCESSFUL → 60,199 bytes, deployed 03:00. Awaiting user check:
deploy glider → item hidden in hand, VISIBLE in inventory.

---

## 2026-09-12 — Feature: Hang Glider (GUI parity VERIFIED)

User: "it worked" — deployed glider hidden in hand perspectives, visible in inventory GUI,
exactly like 1.12.2. The `GliderItemModel` smart + perspective-aware wrapper works first try:
no bake errors introduced (mesh def always resolves normal; wrapper installed at every
`ModelBakeEvent`, reload-safe via unwrap guard). Hang Glider now at full 1.12.2 parity
within 1.8.9's means: bake ✓ inventory/GUI ✓ FPP ✓ TPP-held ✓ TPP body ✓ thermals/vario ✓
(user-verified behaviors throughout) + the one principled adaptation set in ARCHITECTURE.md.
No TEMPORARY code in tree. Awaiting explicit completion confirmation (§7).

---

## 2026-09-12 — Feature: Hang Glider (unstackable glider, user request)

User: the hang glider should NOT stack (1.12.2 stacks to 64 — VERIFIED: 1.12.2
`ItemHangGlider` sets no max stack size; ours neither until now). DELIBERATE deviation,
user-requested: `setMaxStackSize(1)` in our `ItemHangGlider` constructor. Wings (crafting
ingredient) intentionally still stack.
Build: `:reobfJar` BUILD SUCCESSFUL → 60,224 bytes, deployed 03:12. Trivial change, visual
check optional (stack size in inventory).

---

## 2026-09-12 — Feature: Hang Glider COMPLETED (user-confirmed)

User confirmed completion. Final state: faithful 1.8.9 port of the 1.12.2 Hang Glider with
user-verified parity on bake, inventory/GUI, FPP, TPP-held, TPP body rotation, thermals and
glide behavior, plus two deliberate user-requested deviations (unstackable glider,
perspective-aware GUI visibility implemented natively rather than merely accepted as a
limitation). Deployed build 60,224 bytes in `1.8.9(6)/minecraft/mods`. Tree clean, no
TEMPORARY code. Awaiting next feature instruction.

---

## 2026-09-12 — SESSION HANDOFF (next session starts here)

- State: Hang Glider COMPLETED (user-confirmed). No active feature, no open problems, no
  TEMPORARY code. Deployed build 60,224 bytes matches source.
- Debug audit PASSED this session: zero hits for TEMPORARY/GLIDERDBG/MODELPROBE/System.out/
  printStackTrace/TODO across `OpenBlocks-1.8.9/src`; no stray dirs; `git status` clean.
- GitHub: `Wasad12/openblocks-1.8.9` (public, empty before) received ONLY the
  `OpenBlocks-1.8.9/` subtree — `master` at `d204756`, root tree VERIFIED
  (`.gitignore, build.gradle, gradle.properties, gradle, gradlew, settings.gradle, src`).
  Future pushes: `git subtree push --prefix OpenBlocks-1.8.9 origin master`. Docs moved into
  the project so they push too; reference trees stay local-only. Remote `origin` configured locally.
- KNOWN_ISSUES #1 (lib wiring) and #2 (scaffold) both CLOSED: decided with Hang Glider —
  no lib port, per-feature local equivalents (ARCHITECTURE.md); scaffold builds/reobfs/deploys.
- Read order (§16): `OpenBlocks-1.8.9/docs/PORT_STATUS.md` → `FEATURES.md` → this log tail →
  `git log --oneline -5`. Then await user's next feature instruction. (Docs moved into the
  project 2026-09-12 so the GitHub subtree carries them; older entries above still say `docs/`.)
- Standing traps: reference trees are `assume-unchanged` (phantom mtime noise — verify via
  `git hash-object` vs `git rev-parse`, never mass-touch); Gradle needs `-g
  C:\Users\wassi\.gradle-189` + Java 8 (`jdk-8.0.492.9-hotspot`) + `--offline` is fine;
  PowerShell: no `head`, no piping paths into `git --stdin`; `jar xf` dumps into CWD.
  1.8 branches are BANNED sources (user instruction) — 1.10.X was consulted once at explicit
  user request (files identical to 1.12.X, no signal). Theories marked INFERRED are
  disproven until VERIFIED; trust only log/bytecode/probe evidence.

---

## 2026-09-12 — Feature: Tank (Phase A — investigate, 1.12.2 source only)

Source classes (all under `OpenBlocks-1.12.X/src/main/java/openblocks/`, VERIFIED by reading):
- `common/block/BlockTank.java` — `OpenBlock` + `ExtendedBlockState` (orientation +
  `VariantModelState` unlisted); pick-block NBT (with `Amount`=capacity quirk); comparator;
  light; creative-search filled listing.
- `common/tileentity/TileEntityTank.java` — `SyncedTileEntity` + column fill/drain, neighbour
  balancing, bottom-fill, bucket-empty + XP-drain activation, NBT-preserving drops,
  `FastTESR` + pass 1, fluid capability wrapper.
- `common/item/ItemTankBlock.java` — `ItemOpenBlock` + `level` property override (17
  `tank_fluid_N` submodels), item fluid-handler + texture capabilities, tooltip mB,
  `%s Tank` name, `fillTankItem` for creative listing.
- `client/renderer/tileentity/TileEntityTankRenderer.java` — `FastTESR`, neighbour-aware
  fluid walls + animated surface (`TankRenderLogic` data).
- `client/renderer/tileentity/tank/*` (10 files) — pure connection/wave logic (`Diagonal`
  is the only lib import).
- `common/LiquidXpUtils.java` (ratio math only), `Config` tanks keys + `xpToLiquidRatio`,
  `OpenBlocks.Fluids.xpJuice` (luminosity 10, density 800, viscosity 1500), recipe
  obsidian + `paneGlass` → 2, `tank.png`, `xp_juice_{still,flowing}.png`, lang keys
  (already in our `en_US.lang`).

OpenModsLib deps used: `OpenBlock` (hardness 1.0F), `SyncedTileEntity`/`SyncMap`/
`SyncableTank`/`GenericTank`, `VariantModelState`, `ItemOpenBlock`, item texture cap,
`openmods.utils.{Diagonal,ItemUtils,MiscUtils,TranslationUtils,TextureUtils,
EnchantmentUtils}`. Only `Diagonal` + fragments ported; rest replaced/dropped (see
ARCHITECTURE.md "Tank" section).

Unavoidable 1.8.9 API facts (all VERIFIED via `javap` on the `forgeBin` 1722 jar):
no `fluids/capability` package (→ old `IFluidHandler` + `FluidContainerRegistry`);
`FastTESR` exists with `WorldRenderer` signature (pos/color/tex/lightmap/endVertex/
setTranslation all present); `TileEntity` has `getDescriptionPacket`/`onDataPacket`/
`hasFastRenderer`; `ExtendedBlockState` exists (unused — static frame model instead).

---

## 2026-09-12 — Feature: Tank (Phase B — plan)

Full plan recorded in `ARCHITECTURE.md` ("Tank" section): behavior preserved (capacity,
balancing, columns, buckets-in, XP drain, NBT drops/pick, comparator, light, recipe,
config, search listing); lib/sync/render adapted (S35 sync, static frame model,
WorldRenderer TESR, `random.splash` fill sound, `xpJuice` without bucket handler).
KNOWN DEVIATIONS v1: internal frame strips between connected tanks (no variantmodel
loader); no per-level fluid in item model (no `ItemOverride`); bucket-out unsupported
(also unsupported in 1.12.2 — faithful, not a deviation).

---

## 2026-09-12 — Feature: Tank (Phase C — implemented, built, deployed, UNTESTED)

New files (all under `OpenBlocks-1.8.9/src/main/java/` + resources): `common/block/BlockTank`,
`common/tileentity/TileEntityTank`, `common/item/ItemTankBlock`, `common/LiquidXpUtils`
(ratios only), `openmods/utils/Diagonal` (verbatim, Vec3i import only),
`client/renderer/tileentity/tank/*` (10 files, `GenericTank`→`FluidTank`),
`client/renderer/tileentity/TileEntityTankRenderer` (WorldRenderer); `Config` tanks keys +
`xpToLiquidRatio`; `OpenBlocks` xpJuice + block/TE registration + recipe; `ClientProxy`
TESR binding + item model + stitch listener; `blockstates/tank.json`,
`models/{block,item}/tank.json` (frame elements verbatim + particle), `tank.png` +
`xp_juice_{still,flowing}.png{,.mcmeta}` (copied); lang already had all keys.

Build: 2 trivial compile iterations (missing `Block` import; own inner `Blocks` class
shadowed vanilla import in tab icon — fully qualified now). `:reobfJar` BUILD SUCCESSFUL →
132,610 bytes (JAR contents VERIFIED: all tank classes + models + textures), deployed
03:52 (unrelated mods untouched). Notably the whole batch of INFERRED 1.8.9 names
(Chunk CHECK-enum, 2-arg notify, S35 ctor, floor_double/sin, tabAllSearch, getSubBlocks
delegation, stitch `map`, CUTOUT layer) compiled clean on first pass.
Status: awaiting user test (checklist in PORT_STATUS.md).

---

## 2026-09-12 — Feature: Tank (fix loop 1 — invisible block, ROOT CAUSE FOUND)

User: placed tank invisible, liquid shows. Root cause PROVED via `javap` on the 1.8.9
`forgeBin` jar: `BlockContainer.getRenderType()` returns -1 (`iconst_m1`, INVISIBLE).
Our `BlockTank` inherited it, so the static frame model never rendered while the TESR
fluid did — exactly the reported symptom. 1.12.2 `OpenBlock` extends plain `Block`
(render type MODEL), so the original never hits this. Fix: `BlockTank.getRenderType()`
returns 3 (MODEL). Rebuilt (`:reobfJar` BUILD SUCCESSFUL → 132,622 bytes), redeployed,
committed (`e065b7d`). Awaiting user retest.

---

## 2026-09-12 — Feature: Tank (fix loop 2 — connectivity + item fluid + sound)

User reports after render-type fix: (1) adjacent-tank frames don't connect, (2) filled
tanks show the empty icon in inventory, (3) fill sound wrong. All three fixed, same build:
1. Connectivity: `TankFrameModel` smart block model (12 edge pieces + verbatim Karnaugh
   expressions + unlisted neighbour state; static model = fallback/inventory parent).
2. Item fluid: `TankItemModel` smart item model (procedural fluid box, 16 levels, per
   fluid+level cache cleared on bake). Compiler schooled two 1.8.9 shapes: no
   `Builder.setTexture` (quads carry no sprite pre-1.9) and no `getFormat` on
   `ISmartItemModel` (both VERIFIED via `javap` afterwards).
3. Sound: was `random.splash` (entity-splash noise — wrong); `ItemBucket` bytecode
   PROVES 1.8.9 water placement is silent, lava fizzes. Now: lava → `random.fizz`,
   else silent (1.12.2's pour asset doesn't exist on 1.8.9).
Build: `:reobfJar` BUILD SUCCESSFUL → 153,254 bytes (12 edge JSONs + 3 model classes
VERIFIED inside), deployed (unrelated mods untouched). Awaiting user retest of all three.

---

## 2026-09-12 — Feature: Tank (fix loop 3 — item-fluid hotbar crash, ROOT CAUSE FOUND)

User pasted `crash-2026-09-12_04.11.00-client.txt`: `IllegalStateException: not enough
data` in `UnpackedBakedQuad$Builder.build`, via `TankItemModel` while rendering a full
water tank in the hotbar. Root cause PROVED via `javap -c` on 1.8.9 `forgeBin`
`DefaultVertexFormats`: 1.8.9 ITEM = POSITION_3F + COLOR_4UB + TEX_2F + NORMAL_3B +
**PADDING_1B** = 5 elements per vertex. The builder counts `put()` calls per vertex
against the format (source fetched from the 1.8.9 Forge branch confirms the counting),
so my 4 puts/vertex completed only 3.2 vertices → `build()` threw. (The padding byte
was removed in later versions, which is why 1.12.2-era code omits it.) Fix: pad any
trailing elements per vertex (queries `getElementCount()`, no hardcoding).
Rebuilt (`:reobfJar` BUILD SUCCESSFUL → 153,331 bytes), redeployed, awaiting retest.

---

## 2026-09-12 — Feature: Tank (fix loop 4 — wrong edge pieces, ROOT CAUSE FOUND)

User screenshots: shared seam stays while outer edges vanish; stacked corners show black
squares; (item icon "wrong" still open). Root cause, PROVED by reading the 1.12.2
sources: fix loop 2 split the WRONG file. `tank_frame.json` (all 12 elements) is the
INVENTORY-ONLY model — in-world edges come from `tank_frame_{x,y,z}.json` (one element
each, with `cullface`, 1px-wide UV strips, and DIFFERENT corner naming: axis `nw` sits
at x≈0 while `tank_frame`'s `nw` sits at x≈16). Each blockstate variant = one axis file
+ Forge transform translation ([1,0,0] etc. = +1 block on normalized geometry; decoded
by hand, matches all 12 corners). My pieces had wrong geometry, wrong UVs, no cullface,
no translations — explaining every world symptom at once. Also read 1.8.9
`TRSRTransformation` source (implements `IModelState`, nulls = identity) but chose the
zero-risk path instead: the 12 `tank_edge_*`.json now carry the translation baked into
`from`/`to` (+16 px per block, script-generated from the axis files, cullface/UVs
verbatim), so runtime baking stays transform-free (proven path). `evaluate()` bit order
already matched `EDGES` — unchanged. Item icon: full-tank box already equals 1.12.2's
`tank_fluid_16` exactly (geometry + UVs), so asking user for a close-up classification.
Rebuilt (`:reobfJar` BUILD SUCCESSFUL → 154,014 bytes), redeployed.

---

## 2026-09-12 — Feature: Tank (fix loop 5 — item icon half-empty, ROOT CAUSE FOUND)

User classifies the icon: top + left show fluid, right side empty. Shared-edge audit of
my 6 item quads (closed solids must traverse every shared edge in opposite directions)
PROVES exactly one face backwards: NORTH ran its UP/WEST shared edges the same way as
those faces; the other five are pairwise consistent, and top/left visibly rendering
confirms the analysis orientation (a global flip would hide those too). The order was
copied from the TESR, which renders fine in-world — so the TESR path must run
unculled; item quads are culled, hence the missing side. Fix: NORTH reversed (vertex
to UV pairing kept). In-world TESR deliberately untouched (verified working). Rebuilt
(`:reobfJar` BUILD SUCCESSFUL, 154,017 bytes), redeployed. NOTE: user's screenshots
predate the loop-4 geometry build — world frames + icon both need a fresh retest.

---

## 2026-09-12 — Feature: Tank COMPLETED (user-confirmed) + glider fix verified

User: glider survival-hiding "fixed now"; Tank "working and looking fine" after own
testing - "i think we can move to the next feature". Per section 7 both count as explicit
confirmation: Tank moves to COMPLETED (fix loops 1-6 included: render type, connectivity
via true axis geometry, item fluid + padding crash + north winding, NBT drop stash,
silent fills), glider post-completion fix verified (player-scoped hiding). Final Tank
build 154,310 bytes deployed in 1.8.9(6)/minecraft/mods. Tree clean. Awaiting next
feature instruction.

---

## 2026-09-12 — Feature: Tank (fix loop 6 — survival drops + sound removal)

User: (1) breaking a tank in survival drops an empty tank (fluid lost); (2) remove the
fill sound entirely, lava fizz included. For (1), forensics PROVED the 1.8.9 break path
(harvestBlock -> dropBlockAsItem -> dropBlockAsItemWithChance -> getDrops, all VERIFIED
via javap -c, including the TE-alive ordering) DOES call our NBT-aware getDrops, so the
empty drop means the world TE lookup inside getDrops observed nothing, cause unknown
statically. Robust fix regardless of cause: harvestBlock override stashes the live TE's
tank NBT (handed to us directly, no lookup) and getDrops prefers the stash, falling back
to the live lookup; stash always cleared in finally (sequential server thread, cannot
leak). If drops are STILL empty, the fluid was never in the TE and the retest answers
(tooltip mB? pick-block full?) will say so. For (2): fill sound deleted outright per
user request (all fluids silent).

---

## 2026-09-12 — Feature: XP Drain + XP Shower (Phase A — investigate, 1.12.2 source only)

Source (all VERIFIED by reading): `BlockXPDrain` (glass plate, 1/16 AABB, CUTOUT),
`TileEntityXPDrain` (drains standing players ≤4 XP/tick + XP orbs into tank BELOW via
fluid capability, `random.orb` pickup), `BlockXPShower` (FourDirections wall mount,
POWERED bit, redstone-gated), `TileEntityXPShower` (pulls 100 mB/3 ticks from tank
BEHIND into 1-bucket xpJuice buffer, spawns `EntityXPOrbNoFly` below, client spray
particles), `EntityXPOrbNoFly` (no player magnet, lava bounce), `FXLiquidSpray`
(sprite-textured gravity particle, layer 1), `EnchantmentUtils` XP math, recipes
(9x iron bars; 3x iron + obsidian), flat models + textures, lang keys present.
Notable: 1.12.2 registers NO orb renderer (superclass fallback); entity id 709.
OpenModsLib deps: OpenTileEntity/Synced/SyncableBoolean/GenericTank/compat/block-utils
(all replaced, see ARCHITECTURE.md).

---

## 2026-09-12 — Feature: XP Drain + XP Shower (Phase B — plan)

Full plan in ARCHITECTURE.md (XP section). Heads-up items: shower arm rotation follows
the vanilla stairs pattern (INFERRED — needs eyes); orb gets explicit RenderXPOrb;
spray port drops canCollide (no 1.8.9 field); harvest rules stay vanilla (lib sets
none); proxy gains spray + particle-setting methods.

---

## 2026-09-12 — Feature: XP Drain + XP Shower (Phase C — implemented, built, deployed)

New: `BlockXPDrain`/`TileEntityXPDrain`, `BlockXPShower`/`TileEntityXPShower` (+FACING/
POWERED, wall mount, redstone), `EntityXPOrbNoFly` (id 709), `FXLiquidSpray`,
`EnchantmentUtils` (XP math subset), proxy spray/particle methods, registrations +
2 recipes, blockstates/models/textures, lang already present. Caught and fixed a real
self-introduced bug before building (shower drained-then-checked neighbour fluid —
would void wrong fluids; now simulate-first). One compile iteration for five 1.9-isms
(Vec3, BlockState, onBlockPlaced, resetPositionToBB, slipperiness field — all in
ARCHITECTURE.md). `:reobfJar` BUILD SUCCESSFUL, 176,240 bytes (contents VERIFIED),
deployed (unrelated mods untouched). Awaiting user test: drain players/orbs into tank
below; shower (redstone-powered, tank behind) pours orbs + spray; arm touches tank;
orb renderer visible; recipes craft.

---

## 2026-09-12 — Feature: XP Drain + XP Shower (fix loop 1 — shower states + held TPP)

User test: drain behavior (drain + take-back), shower behavior (orbs + redstone) all
CORRECT. Visual issues: (2) placed shower = missing-texture cube — ROOT CAUSE: my
blockstate listed only 4 facing variants while the block has facing x powered = 8
states; unmatched states fall back to missing. Fixed by enumerating all 8 (powered
changes nothing visually, like 1.12.2). (1) drain held-TPP wrong — drain is a flat
plate with no display block; gave the item the verbatim vanilla flat-item display
(redstone/hopper/torch/cauldron all share it — VERIFIED in the client jar), same
proven family as the glider fix. OPEN, needs eyes: (3) shower held-TPP, (6) tank
held-TPP (tank TPP was confirmed identical pre-TankItemModel; need current symptom +
screenshots before touching). Rebuilt (176,394 bytes), deployed.

---

## 2026-09-12 — Feature: XP Drain + XP Shower (fix loop 2 — TPP-held look)

User classifies: shower-held = wrong size/angle, tank-held = empty frame wrong too
(no screenshots). Both 1.12.2 items render TPP via forge:default-block, so the fix
replicates it exactly instead of guessing JSON numbers: read the real 1.12.x
ForgeBlockStateV1 source (defaults: TPP = convert(0, 2.5, 0, 75, 45, 0, 0.375)) and
1.8.9's application path (RenderItem routes EVERYTHING through
ForgeHooksClient.handleCameraTransforms; plain models take the vanilla unwrapped
branch, which is why 1.12.2 numbers cannot transfer literally — third independent
confirmation of the glider finding). New HeldBlockPerspective composes the identical
folded matrix in vecmath (T(0.5).T(t).Rx.Ry.S.T(-0.5)) and installs it via
IPerspectiveAwareModel on tank items (empty + filled) and a new shower item wrapper;
all other contexts keep today's behavior. Residual: hardcoded arm poses differ by
version (accepted, glider-proven small). Drain keeps its shipped flat display
(1.12.2 drain is a plain blockstate = vanilla path, different case). Rebuilt
(182,015 bytes), deployed. Awaiting TPP screenshots for all three held items.

---

## 2026-09-12 — SESSION HANDOFF (next session starts here)

- State: XP Drain + XP Shower IN PROGRESS. Comitted work: Phase C (`8a842f9`), fix loop 1
  (`b33eaf1`: shower 8-state blockstate, drain flat-item display), fix loop 2 (`e1f3596`:
  HeldBlockPerspective default-block TPP matrices for tank/shower). Deployed build
  182,015 bytes matches source (`e1f3596`). Tree clean, no TEMPORARY code.
- UNTESTED: everything from fix loops 1-2 (shower placed texture, drain/shower/tank
  held-TPP, FPP sanity). User was asked for TPP screenshots per item; none received yet.
- GitHub BEHIND: `Wasad12/openblocks-1.8.9` last received subtree push `c8fd7d5`
  (Hang Glider era). Local master is ~14 commits ahead (Tank + glider fix + XP).
  Push ONLY on explicit user request: `git subtree push --prefix OpenBlocks-1.8.9 origin master`.
- Completed and verified: Hang Glider (COMPLETED + survival-hiding fix verified),
  Tank (COMPLETED 2026-09-12).
- Read order (§16): `OpenBlocks-1.8.9/docs/PORT_STATUS.md` → `FEATURES.md` → this log
  tail → `git log --oneline -5`. Then await user retest / next instruction.
- Standing traps: reference trees are `assume-unchanged` (phantom mtime noise — verify
  via `git hash-object` vs `git rev-parse`, never mass-touch); Gradle needs `-g
  C:\Users\wassi\.gradle-189` + Java 8 (`jdk-8.0.492.9-hotspot`) + `--offline` is fine;
  PowerShell: no `head`, no piping paths into `git --stdin`; `jar xf` dumps into CWD.
  1.8 branches are BANNED sources. Theories marked INFERRED are disproven until
  VERIFIED; trust only log/bytecode/probe evidence.

---

## 2026-09-12 — Feature: Hang Glider (post-completion fix — survival hand-hiding)

User: deployed glider hides in hand in creative but NOT in survival (flight itself works
in both). Forensics: the hiding rule was my adaptation — scan the client glider map for
an identity match (`heldItem == stack`) — because 1.8.9's smart-item hook only receives
the stack, while 1.12.2's property getter also receives the rendering entity and does a
direct player lookup (`gliderMap.get(player)`, VERIFIED in 1.12.2 source). The code has
NO mode-dependent branch, so identical code seeing different results means the stack
objects differ by mode somewhere in vanilla's survival path (exact mechanism unproven;
not chased further). Fix removes identity from the equation: `handleItemState` now hides
whenever the stack is a glider and the LOCAL player has a live glider
(`getGliderFor(thePlayer)`, the same player-scoped `gliderMap.get` lookup the working
body-tilt already uses). Covers all modes/perspectives; SMP-correct (your glider, your
hands). The superseded scan (`isStackDeployedGlider`) is deleted; the faithful
`isHeldStackDeployedGlider` stays (unused, as before). Rebuilt (BUILD SUCCESSFUL,
154,310 bytes), redeployed. Awaiting retest in BOTH modes.

---

## 2026-09-12 — Feature: XP Drain + XP Shower (fix loop 3 — held invisible + shower hitbox)

User screenshots (1.8.9 vs 1.12.2 sets): (1) tank + shower invisible held-TPP,
(2) drain held-TPP wrong orientation, (3) drain + shower invisible held-FPP,
(4) shower selection box wrong. Root causes found by comparison + code read:

1-3. Fix loop 2's `HeldBlockPerspective` (custom vecmath default-block fold via
`IPerspectiveAwareModel`) rendered tank/shower invisible: for non-TPP it returned
`new Matrix4f()` (PROVED all-zero via vecmath run — collapses item to a point),
and the TPP fold itself proved degenerate in-game (empty hands). Drain looked
"wrong" for the opposite reason: it kept the flat-item display (redstone family,
[-90,0,0]) while 1.12.2 renders ALL THREE via `forge:default-block` (3D block
look). Fix follows the glider redstone-convergence lesson (vanilla JSON, no custom
matrices): DELETED `HeldBlockPerspective` + `ShowerItemModel`; `TankItemModel`
stripped to plain `ISmartItemModel` (fluid box kept, perspective removed);
all three item JSONs now carry the verbatim vanilla 1.8.9 block-item `display`
(thirdperson [10,-45,170]/[0,1.5,-2.75]/0.375 — stone/glass, read from client jar).
FPP intentionally has no override (vanilla block default, like stone).

4. `BlockXPShower.unionForFacing` returned full-length axis boxes (N+S both
z0-16, E+W both x0-16) — selection stretched through the tank side. Fixed to
directional half-boxes matching the per-facing rendered arm (N:z0-9, S:z7-16,
E:x7-16, W:x0-9; y7-9, cross 7-9), consistent with the y90/180/270 blockstate
rotations.

Build: `:reobfJar` BUILD SUCCESSFUL → 176,453 bytes (no Shower/Held classes,
fixed JSONs VERIFIED inside), deployed (unrelated mods untouched). Awaiting retest:
held-TPP/FPP tank(empty+filled)/shower/drain vs 1.12.2, shower box per facing,
inventory icons, placed-shower texture.

---

## 2026-09-12 — Feature: XP Drain + XP Shower (fix loop 4 — shower solid + selectable)

User: held looks now correct/identical ✓; remaining: shower pass-through + hitbox
miss (aiming at arm selects tank behind). Two 1.8.9 mechanism bugs, both PROVED via
`javap` on the 1722 `forgeBin` jar:

1. `addCollisionBoxesToList` adds `getCollisionBoundingBox` DIRECTLY with no offset
(the default impl builds a world box from min/max fields). Ours returned the LOCAL
0-1 box, so the solid sat at the world origin — entities at the block never touched
it. Fixed: both blocks return `.offset(pos)` (shower directional, drain plate).
2. `collisionRayTrace` uses min/max fields (via `setBlockBoundsBasedOnState`), NOT
either box method — which we never overrode, so the ray never matched the thin arm
and fell through to the tank. Fixed: `setBlockBoundsBasedOnState` sets the same
directional/thin bounds (shower per-facing, drain 1/16 plate).

Build: `:reobfJar` BUILD SUCCESSFUL → 176,716 bytes, deployed (unrelated mods
untouched). Awaiting retest: walk into shower (should collide), aim at arm (thin
outline, no tank fall-through), drain bounds unchanged.

---

## 2026-09-12 — Feature: XP Drain + XP Shower (fix loops 3-4 user-verified, pushed)

User: tank held correct, shower + drain held 100% identical to 1.12.2, shower solid +
hitbox fixed ("all fixed now, push"). Fix loops 3 (perspective-wrapper removal +
block-item display) and 4 (offset collision + block bounds) both VERIFIED. Committed
and subtree-pushed to `Wasad12/openblocks-1.8.9` on explicit user request. Feature
stays IN PROGRESS per §7 until explicit completion confirmation (next feature
instruction or "completed").

---

## 2026-09-12 — Feature: XP Drain + XP Shower COMPLETED (user-confirmed)

User: "xp drain + shower now complete we can move but if i found issue in them in the
future we can go back and fix them". Per §7 this is explicit completion confirmation:
feature moves to COMPLETED. Final state: faithful 1.8.9 port (drain players/orbs into
tank below, redstone-gated shower pours NoFly orbs + spray, arm touches tank, explicit
orb renderer, recipes) with user-verified parity on behavior, held looks (block-item
display), solidity and hitboxes — plus the standing agreement to revisit if issues
surface later. No active feature; awaiting next instruction.

---

## 2026-09-12 — Feature: Auto Enchantment Table (Phase A — investigate, 1.12.2 source only)

Source (all VERIFIED by reading): `BlockAutoEnchantmentTable` (0.75 AABB, book
particles, non-solid sides), `TileEntityAutoEnchantmentTable` (484 lines: 3 slots,
xpJuice tank for 30 levels, 4 side-maps + auto flags, power limit/level sync, seed,
BookState math, movers, enchant gating), `ContainerAutoEnchantmentTable` (3 slots +
player inv), `GuiAutoEnchantmentTable` (slider/labels/tank gauge/level button/4 tabs —
user screenshots are the target), `TileEntityAutoEnchantmentTableRenderer` (book),
`rpc/ILevelChanger`, recipe (iron + enchanting table + redstone), block model (12px
base), textures x3 (+top animation mcmeta), lang keys present.
OpenModsLib deps: full GUI component stack + SyncMap/RPC/network + inventory
(surveyed file-by-file via subagent — see ARCHITECTURE.md).

---

## 2026-09-12 — Feature: Auto Enchantment Table (Phase B — plan)

Full plan in ARCHITECTURE.md. User correction enforced: FAITHFUL port of the real
OpenMods systems under the same `openmods.*` packages (not a vanilla lookalike).
1.8.9 API facts VERIFIED via `javap`: calcItemStackEnchantability same;
buildEnchantmentList 3-arg (no treasure enchants in 1.8.9); EnchantmentData.
enchantmentobj; instance ItemEnchantedBook.addEnchantment; item-handler caps exist;
NO fluid caps; level icons present in 1.8.9 enchanting_table.png (pixel check).

---

## 2026-09-12 — Feature: Auto Enchantment Table (Phase C — implemented, built, deployed)

~60 new files: full `openmods` stack (sync/Map/Client/Server/Tile + all Syncable* +
local type registry, inventory + ItemMover + adapters, container, GUI framework +
all components + SideSelector/Trackball/SidePicker, RPC interfaces + local
method registry + 1.8.9 SimpleNetworkWrapper transport, TE bases, VanillaEnchantLogic,
EnchantmentUtils.getPower, GUI handlers) + feature files (block/TE/container/GUI/
book TESR/rpc) + registration (block/TE/GUI handlers/network/sync types/RPC
methods/recipe) + resources (blockstate, block/item models with block-item display,
3 textures + animation mcmeta, components.png).
Notable adaptations (all in ARCHITECTURE.md): nullable stacks, void writeToNBT,
isUseableByPlayer, crafters, xDisplayPosition, readNBTTagCompoundFromBuffer,
getDimensionId, RenderItem.entity package, no MoreObjects/fastFloor/NonNullList/
ItemStackHelper/ClickType/IContainerListener, NBT-safe pre-world sync read,
TE-side fluid gating inline (no fluid caps), single-pass block preview (no render
layers). One compile pass after 18 mechanical fixes, all 1.8.9 namings.
`:reobfJar` BUILD SUCCESSFUL → 436,189 bytes (contents VERIFIED), deployed
(unrelated mods untouched). Awaiting user test against the 1.12.2 screenshots.

---

## 2026-09-12 — Feature: Auto Enchantment Table (fix loop 1 — RPC world lookup)

User: visuals 100% identical ✓ (tabs, tab block preview, everything); two failures:
(1) GUI settings (auto flags, side selection) reset on GUI reopen, (2) adjacent tank
not drained even when its side is selected (user suspected related — correct).
Root cause PROVED via the runtime log (`fml-client-latest.log`: NPE "Invalid world
dimension 0" in `WorldUtils.getWorld` → `TileEntityRpcTarget.readFromStreamStream`,
every GUI edit): on an INTEGRATED server `OpenBlocks.proxy` IS the client proxy
(`@SidedProxy` picks by physical side), so the SERVER branch of `WorldUtils.getWorld`
called `ClientProxy.getServerWorld`, which returned null — every client→server RPC
died server-side, server state never changed, reopen re-read defaults, and xpSides
stayed empty so `fillFromSides` pulled nothing. Fix: `ClientProxy.getServerWorld`
resolves via FML exactly like `ServerProxy` (valid on the server thread in both
singleplayer and dedicated). Rebuilt (436,283 bytes), redeployed. Awaiting retest:
toggle auto/sides → reopen (must stick), then auto-drink from the adjacent tank.

---

## 2026-09-12 — Feature: Auto Enchantment Table COMPLETED (user-confirmed)

User: "now it works exactly like 1.12.2 ready to push". Per §7 this is explicit
completion confirmation: feature moves to COMPLETED. Final state: faithful 1.8.9
port — block (0.75 box, particles, GUI open), TE (slots/tank/sides/level/seed,
movers, enchant gating, book math), container, full tabbed GUI (slider, power
label, XP gauge, level button, 4 side tabs with 3D previews), book TESR, real
openmods sync/RPC/GUI stack over the 1.8.9 transport, recipe — with user-verified
parity on visuals, tabs, setting persistence and auto-drink. Committed and
subtree-pushed on explicit user request.

---

## 2026-09-12 — SESSION HANDOFF (next session starts here)

- State: Auto Enchantment Table COMPLETED (user-confirmed). No active feature, no
  open problems, no TEMPORARY code. Deployed build 436,283 bytes matches source
  (`ebcc4aa`). Completed tally: Hang Glider, Tank, XP Drain + XP Shower, Auto
  Enchantment Table (all user-confirmed; XP/auto-enchant may be revisited if issues
  surface later).
- Debug audit PASSED this session: zero hits for TEMPORARY/GLIDERDBG/MODELPROBE/
  System.out/printStackTrace/FIXME across `OpenBlocks-1.8.9/src` (the single TODO
  hit is `GenericInventory.java:231`, verbatim from the 1.12.2 original — kept
  faithfully, not our leftover); no stray dirs in the project (`assets/` absent,
  `jar xf` hygiene kept).
- GitHub: `Wasad12/openblocks-1.8.9` received subtree push `154070a` 2026-09-12
  (all work through `ebcc4aa` now visible). Local master is one record commit ahead
  (`4603401`, push bookkeeping — same pattern as before). Push ONLY on explicit
  user request: `git subtree push --prefix OpenBlocks-1.8.9 origin master`.
- Untracked at repo root: `open1.8.9.zip` (26MB, dated 2026-09-12, origin unknown —
  NOT created by this session). Left untouched; do not touch without asking.
- Docs fixed this session: FEATURES.md duplicate `## COMPLETED` header removed +
  Auto Enchantment Table removed from NOT STARTED; PORT_STATUS.md duplicate
  last-completed lines merged + stale lines refreshed.
- Read order (§16): `OpenBlocks-1.8.9/docs/PORT_STATUS.md` → `FEATURES.md` → this log
  tail → `git log --oneline -5`. Then await user's next feature instruction.
- Standing traps: reference trees are `assume-unchanged` (phantom mtime noise —
  verify via `git hash-object` vs `git rev-parse`, never mass-touch); Gradle needs
  `-g C:\Users\wassi\.gradle-189` + Java 8 (`jdk-8.0.492.9-hotspot`) + `--offline`
  is fine; PowerShell: no `head`, no piping paths into `git --stdin`, no
  `2>/dev/null` (Windows — use `-ErrorAction SilentlyContinue`); `jar xf` dumps
  into CWD. 1.8 branches are BANNED sources. Theories marked INFERRED are disproven
  until VERIFIED; trust only log/bytecode/probe evidence.

---

## 2026-09-12 — Feature: Auto Anvil (Phase A — investigate, 1.12.2 source only)

Source (all VERIFIED by reading): `BlockAutoAnvil` (TwoDirections anvil, non-opaque,
non-solid sides), `TileEntityAutoAnvil` (329 lines: Slots tool/modifier/output,
AutoSlots tool/modifier/output/xp, 4 SyncableSides + SyncableTank (45 levels) +
SyncableFlags, 40-tick cooldown, VanillaAnvilLogic repair gated on atomic liquid
drain, ItemMover auto in/out, `random` anvil sound), `ContainerAutoAnvil` (slots at
(14/56/110,40) + player inv at 93), `GuiAutoAnvil` (hammer + plus sprites, tank
gauge in levels, 4 tabs: blue pickaxe / lightblue book / green enchanted pickaxe /
yellow bucket), recipe iii/iai/rrr (iron + anvil + redstone), block model (4
elements, 6 textures, `block/block` parent + `fixed` display), blockstate
(orientation xp_yp/zn_yp), lang keys present in our `en_US.lang`.
OpenModsLib deps: `VanillaAnvilLogic` (232 lines, adapted from ContainerRepair),
`GuiComponentSprite`, full sync/inventory/RPC/GUI stack (ALL already ported for the
Auto Enchantment Table — reused, see ARCHITECTURE.md).

---

## 2026-09-12 — Feature: Auto Anvil (Phase B — plan)

Full plan in ARCHITECTURE.md ("Auto Anvil" section). Key 1.8.9 facts, all VERIFIED
via `javap` on the 1722 `forgeBin` jar (+ client jar for models): enchantment maps
are ID-based (`Map<Integer,Integer>`, object maps are 1.9+); `canApplyTogether`
replaces `isCompatibleWith`; no `Enchantment.getRarity` (weight thresholds instead,
exact for all vanilla enchants); `AnvilUpdateEvent` has public fields;
`ItemEnchantedBook.getEnchantments` is an instance method returning NBTTagList;
`Block$SoundType` is an inner class (`soundTypeAnvil` + `setStepSound`);
`block/block` parent does NOT exist in 1.8.9 (parentless block model like our
shower); bare `auto_anvil.png` unreferenced (skipped). TwoDirections → vanilla-style
`FACING` + `rotateY` placement (same perpendicular long axis).

---

## 2026-09-12 — Feature: Auto Anvil (Phase C — implemented, built, deployed)

New: `common/block/BlockAutoAnvil`, `common/tileentity/TileEntityAutoAnvil`,
`common/container/ContainerAutoAnvil`, `client/gui/GuiAutoAnvil`,
`openmods/utils/VanillaAnvilLogic` (ID-map port), `openmods/gui/component/
GuiComponentSprite` (verbatim) + registration (block/TE/recipe) + ClientProxy item
model + blockstate/block-item models + 6 textures (lang already had all keys).
Zero compile iterations — every INFERRED 1.8.9 name (`Material.anvil`,
`Block.soundTypeAnvil`/`setStepSound`, `Items.enchanted_book`,
`TextureMap.locationBlocksTexture`, `Blocks.anvil`, `NBTTagList.tagCount`)
compiled clean first try. `:reobfJar` BUILD SUCCESSFUL → 463,781 bytes (all anvil
classes + models + textures VERIFIED inside), deployed (unrelated mods untouched),
committed (`bfa3bb8`). Awaiting user test: recipe crafts; placed anvil renders +
orients; GUI (hammer/plus sprites, XP gauge, 4 tabs) matches 1.12.2; manual repair
consumes liquid XP + plays anvil sound; auto in/out/drink via side tabs.

---

## 2026-09-12 — Feature: Auto Anvil COMPLETED (user-confirmed)

User: "everything works, complited". Per §7 this is explicit completion confirmation:
feature moves to COMPLETED with zero fix loops — first-try build held up in-game
(recipe, placement/orientation, GUI, liquid-gated repair + sound, automation all
correct on first test). Final build 463,781 bytes deployed in
`1.8.9(6)/minecraft/mods`. Tree clean. Completed tally: Hang Glider, Tank,
XP Drain + XP Shower, Auto Enchantment Table, Auto Anvil (all user-confirmed).
Awaiting next feature instruction.
