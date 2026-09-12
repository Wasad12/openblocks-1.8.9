# Feature Inventory

Derived from `OpenBlocks-1.12.X/src/main/java/openblocks/common/{block,item,tileentity}`
class names. A feature moves to COMPLETED only on explicit user confirmation (§7).

## IN PROGRESS

_none — awaiting next feature instruction._

## COMPLETED

## COMPLETED

- Hang Glider (`ItemHangGlider` + `EntityHangGlider` + renderer + vario/thermal + recipes +
  perspective-aware item model + unstackable glider). User-confirmed COMPLETED 2026-09-12.
  Post-completion survival hand-hiding fix (player-scoped, no identity scan) user-confirmed FIXED 2026-09-12.
- Tank (`BlockTank` + `TileEntityTank` + `ItemTankBlock` + connected-frame smart model +
  TESR fluid + item fluid box + xpJuice fluid + recipe + silent fills + harvest-stash NBT
  drops). User-confirmed COMPLETED 2026-09-12 ("working and looking fine").
- XP Drain (`BlockXPDrain` + `TileEntityXPDrain`) + XP Shower (`BlockXPShower` +
  `TileEntityXPShower` + `EntityXPOrbNoFly` + spray FX). User-confirmed COMPLETED 2026-09-12
  ("all fixed now" — drain/shower behavior, held looks identical to 1.12.2, shower solid +
  hitbox fixed; fix loops 1-4 included). May revisit if issues surface later.
- Auto Enchantment Table (`BlockAutoEnchantmentTable` + `TileEntityAutoEnchantmentTable` +
  `ContainerAutoEnchantmentTable` + `GuiAutoEnchantmentTable` + book TESR +
  `VanillaEnchantLogic` + side-tab automation + recipe). User-confirmed COMPLETED 2026-09-12
  ("works exactly like 1.12.2" — visuals/tabs/block preview identical, settings persist,
  auto-drink works; fix loop 1 included).

## NOT STARTED

### Blocks (+ tile entities where present)

- Auto Anvil (`BlockAutoAnvil` + `TileEntityAutoAnvil`)
- Auto Enchantment Table (`BlockAutoEnchantmentTable` + `TileEntityAutoEnchantmentTable`)
- Bear Trap (`BlockBearTrap` + `TileEntityBearTrap`)
- Big Button (`BlockBigButton` + `TileEntityBigButton`)
- Block Breaker (`BlockBlockBreaker` + `TileEntityBlockBreaker`)
- Block Manipulator base (`BlockBlockManpulatorBase` + `TileEntityBlockManipulator`)
- Block Placer (`BlockBlockPlacer` + `TileEntityBlockPlacer`)
- Builder Guide (`BlockBuilderGuide` + `TileEntityBuilderGuide`)
- Cannon (`BlockCannon` + `TileEntityCannon`)
- Canvas (`BlockCanvas` + `TileEntityCanvas`) / Canvas Glass (`BlockCanvasGlass` + `TileEntityCanvasGlass`)
- Donation Station (`BlockDonationStation` + `TileEntityDonationStation`)
- Drawing Table (`BlockDrawingTable` + `TileEntityDrawingTable`)
- Elevator (`BlockElevator`) / Elevator Rotating (`BlockElevatorRotating` + `TileEntityElevatorRotating`)
- Fan (`BlockFan` + `TileEntityFan`)
- Flag (`BlockFlag` + `TileEntityFlag`)
- Golden Egg (`BlockGoldenEgg` + `TileEntityGoldenEgg`)
- Grave (`BlockGrave` + `TileEntityGrave`)
- Guide (`BlockGuide` + `TileEntityGuide`)
- Heal (`BlockHeal` + `TileEntityHealBlock`)
- Imaginary (`BlockImaginary` + `TileEntityImaginary`)
- Item Dropper (`BlockItemDropper` + `TileEntityItemDropper`)
- Ladder (`BlockLadder`) / Rope Ladder (`BlockRopeLadder`)
- Paint Can (`BlockPaintCan` + `TileEntityPaintCan`) / Paint Mixer (`BlockPaintMixer` + `TileEntityPaintMixer`)
- Path (`BlockPath`)
- Projector (`BlockProjector` + `TileEntityProjector`)
- Scaffolding (`BlockScaffolding`)
- Sky (`BlockSky` + `TileEntitySky`)
- Sponge (`BlockSponge`)
- Sprinkler (`BlockSprinkler` + `TileEntitySprinkler`)
- Tank (`BlockTank` + `TileEntityTank`)
- Target (`BlockTarget` + `TileEntityTarget`)
- Trophy (`BlockTrophy` + `TileEntityTrophy`)
- Vacuum Hopper (`BlockVacuumHopper` + `TileEntityVacuumHopper`)
- Village Highlighter (`BlockVillageHighlighter` + `TileEntityVillageHighlighter`)
- XP Bottler (`BlockXPBottler` + `TileEntityXPBottler`)
- XP Drain (`BlockXPDrain` + `TileEntityXPDrain`)
- XP Shower (`BlockXPShower` + `TileEntityXPShower`)

### Items

- Cartographer, Crane Backpack, Crane Control, Cursor, Dev Null, Elevator item,
  Empty Map, Epic Eraser, Flag Block item, Golden Eye, Guide item, Hang Glider,
  Height Map, Imaginary item, Imagination Glasses, Info Book, Luggage,
  Generic (OBGeneric/Unstackable/Meta variants), Paint Brush, Paint Can item,
  Pedometer, Sky Block item, Sleeping Bag, Slimalyzer, Sonic Glasses,
  Sponge On A Stick, Squeegee, Stencil, Tank Block item, Tasty Clay,
  Trophy Block item, Wrench, XP Bucket, Miracle Magnet, Pointer

### Systems (cross-cutting, port only as required by features per §19)

- Entities (`common/entity`), containers (`common/container`), sync (`common/sync`),
  recipes, advancements/loot, client renderers (`client/renderer`), GUIs (`client/gui`),
  models (`client/model`), FX, key bindings, sounds, enchantments, crane/radio/village systems

## BLOCKED

_none._
