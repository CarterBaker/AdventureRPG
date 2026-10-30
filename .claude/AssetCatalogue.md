# AI Asset Catalogue

Every asset in this project that Claude generated. The owner replaces these with their own work over time, so this
file is the record of what is still AI-made. The rules for keeping it are in `.claude/CLAUDE.md` under
"AI Asset Catalogue". An entry leaves this file only when the owner says their own asset has replaced it.

Not listed: the owner's assets and libraries in `.claude/assets/` and `.claude/catalogue/` and anything copied
from them, maps generated from the owner's art (the owner's by rule), and the third-party fonts
`assets/fonts/IMFellEnglish.ttf` and `assets/fonts/MontserratAlternates.ttf`.

---

## Log

One line per session that changed this file: the date, what was added or removed, and why.

- 2026-10-29: Catalogue created. Every file under `assets/` recorded as AI-generated (2187 images, 172 shaders,
  1436 data files), the two fonts excepted.
- 2026-10-29: Rule change. Maps generated from the owner's art are the owner's and are not catalogued;
  `.claude/assets/` added as the owner's asset folder.
- 2026-10-29: `.claude/catalogue/` added as the owner's personal library, listed in `.claude/assets/Catalogue.txt`.
- 2026-10-30: Sound pipeline. Added the transport icons `LoopIcon`, `PauseIcon`, `PlayIcon`, `RecordIcon`,
  `RewindIcon`, `SkipEndIcon` and `StopIcon`, the interface sounds `Click.wav` and `Confirm.wav` with their
  companions, the `SoundLibrary.arpg` menu and the `Sounds.arpg` schema; regenerated `Recorder.arpg` (icon transport,
  sound clip bars) and `EditorWindow.arpg` (Sound Library entry).
- 2026-10-30: Full game audio. Added 159 generated placeholder sounds (footsteps per surface, movement, water,
  combat, blocks, items, trees, weather, ambience, ships, cannons, chests, progression, vitality, interface and three
  music pieces), a companion `.arpg` for every sound the game plays (generated or imported from the library), 106 sound
  cues under `assets/soundcues/` and the `SoundCues.arpg` schema; rewrote `TerraArcanaBlocks.arpg` and every covering
  (surface sounds), `Cannons.arpg` and `Furniture.arpg` (action cues), the game menus (open, close and click cues),
  `Launcher.arpg` (manifest) and the `Blocks`, `Coverings`, `Items` and `Sounds` schemas.
- 2026-10-30: Sounds are Ogg Vorbis only. The 134 generated `.wav` sounds under `assets/sounds/` renamed to `.ogg`,
  converted on the owner's PC.
- 2026-10-30: Removed `sounds/interface/Confirm.ogg` and its companion `Confirm.arpg`: no cue or code plays it.

---

## Textures and Sprites (2194 files)

One line per image set: the name before the last underscore, then the suffixes present (`AbyssalOoze: a, ao, h, n, spec`
is `AbyssalOoze_a.png` through `AbyssalOoze_spec.png`). `(no suffix)` is the bare name, such as `TerraArcana.png`.

### `assets/sprites/menus/`

- `Button`
- `ButtonOutline`
- `CloseIcon`
- `CursorStretchIconHorizontal`
- `CursorStretchIconVertical`
- `DialFace`
- `DialNeedle`
- `DialRing`
- `EditorTab`
- `FrameOutline`
- `LoopIcon`
- `MaximizeIcon`
- `MinimizeIcon`
- `Panel`
- `PanelHeader`
- `PanelOutline`
- `PauseIcon`
- `PlayIcon`
- `RecordIcon`
- `RewindIcon`
- `Shadow`
- `SkipEndIcon`
- `StatusBar`
- `StopIcon`
- `Sword`
- `Title`
- `ToolBar`

### `assets/sprites/menus/boards/`

- `BoardButtonA`
- `BoardButtonB`
- `BoardButtonC`
- `BoardButtonD`
- `BoardButtonWide`

### `assets/sprites/menus/creator/`

- `SliderTrack`
- `Swatch`
- `SwatchRing`

### `assets/sprites/menus/hud/`

- `Crosshair`
- `KeyCap`

### `assets/sprites/menus/inventory/`

- `Eye`
- `EyeClosed`
- `Inset`
- `Slot`
- `ViewFrame`

### `assets/sprites/menus/paper/`

- `PaperScrap`
- `PaperStrip`

### `assets/sprites/menus/title/`

- `TitleBoard`
- `TitleGlint`
- `TitleSlit`
- `TitleSword`

### `assets/sprites/menus/wood/`

- `WoodPanel`
- `WoodPeg`
- `WoodSlat`

### `assets/sprites/sky/`

- `Moon`
- `Star`
- `Sun`
- `SunFlames`

### `assets/sprites/util/`

- `Default`

### `assets/textures/characters/standard/`

- `AlmondEyes`: A
- `AngryBrows`: A
- `ArchedBrows`: A
- `BrightEyes`: A
- `BushyBrows`: A
- `ClosedEyes`: A
- `DeterminedEyes`: A
- `Face`: A
- `FlatMouth`: A
- `FrownMouth`: A
- `GrinMouth`: A
- `Hair`: A
- `NarrowEyes`: A
- `OpenMouth`: A
- `PoutMouth`: A
- `RaisedBrows`: A
- `RoundEyes`: A
- `Skin`: A
- `SleepyEyes`: A
- `SmileMouth`: A
- `SmirkMouth`: A
- `ThickBrows`: A
- `ThinBrows`: A
- `Unibrow`: A
- `WideEyes`: A
- `WideSmileMouth`: A

### `assets/textures/items/standard/`

- `AbyssalOoze`: A
- `AcaciaBark`: A
- `AcaciaHeartwood`: A
- `AccentAcorn`: A
- `AccentBaobab`: A
- `AccentBlossom`: A
- `AccentCoconut`: A
- `AccentDates`: A
- `AccentEmber`: A
- `AccentGlowBlue`: A
- `AccentGlowPink`: A
- `AccentGold`: A
- `AccentHazelnut`: A
- `AccentJuniper`: A
- `AccentMangrove`: A
- `AccentMoon`: A
- `AccentSamara`: A
- `AgateOre`: A
- `AlexandriteOre`: A
- `AlkaliFlat`: A
- `Alluvium`: A
- `AmazoniteOre`: A
- `AmberOre`: A
- `AmethystCrystal`: A
- `AmethystOre`: A
- `Amphibolite`: A
- `Andesite`: A
- `Anorthosite`: A
- `Apple`: A
- `AquamarineOre`: A
- `Aragonite`: A
- `Arkose`: A
- `ArsenopyriteOre`: A
- `AshSoil`: A
- `AzurewoodBark`: A
- `AzurewoodHeartwood`: A
- `AzuriteOre`: A
- `BandedIron`: A
- `BandedSandstone`: A
- `BaobabBark`: A
- `BaobabHeartwood`: A
- `BaryteOre`: A
- `Basalt`: A
- `BasaltScree`: A
- `BastnasiteOre`: A
- `BauxiteOre`: A
- `Beachrock`: A
- `BeechBark`: A
- `BeechHeartwood`: A
- `BenitoiteOre`: A
- `Bentonite`: A
- `BerylOre`: A
- `BirchBark`: A
- `BirchHeartwood`: A
- `BismuthiniteOre`: A
- `BlackEarth`: A
- `BlackGranite`: A
- `BlackGravel`: A
- `BlackMarble`: A
- `BlackMud`: A
- `BlackSand`: A
- `BlueClay`: A
- `BlueIce`: A
- `Bluestone`: A
- `BlushwoodBark`: A
- `BlushwoodHeartwood`: A
- `BoraxOre`: A
- `BorniteOre`: A
- `BraggiteOre`: A
- `Bread`: A
- `Breccia`: A
- `BromargyriteOre`: A
- `BrownSand`: A
- `CalaveriteOre`: A
- `CalcareousOoze`: A
- `Calcite`: A
- `Caliche`: A
- `Canvas`: A
- `CarnalliteOre`: A
- `CarnelianOre`: A
- `CarnotiteOre`: A
- `CelestineOre`: A
- `ChalcopyriteOre`: A
- `Chalk`: A
- `CherryBark`: A
- `CherryHeartwood`: A
- `Chert`: A
- `ChromiteOre`: A
- `ChrysoberylOre`: A
- `ChrysocollaOre`: A
- `Cinders`: A
- `CinnabarOre`: A
- `CitrineOre`: A
- `ClausthaliteOre`: A
- `Clay`: A
- `Claystone`: A
- `CoalOre`: A
- `CoarseDirt`: A
- `CobaltOre`: A
- `Cobblestone`: A
- `ColemaniteOre`: A
- `ColumbiteOre`: A
- `Conglomerate`: A
- `CopperOre`: A
- `Coquina`: A
- `CoralRubble`: A
- `CoralSand`: A
- `CrackedClay`: A
- `CrackedEarth`: A
- `Crimson`: A
- `CryoliteOre`: A
- `CypressBark`: A
- `CypressHeartwood`: A
- `Dacite`: A
- `DarkLimestone`: A
- `DarkTimber`: A
- `Diabase`: A
- `DiamondOre`: A
- `Diatomite`: A
- `DioptaseOre`: A
- `Diorite`: A
- `Dirt`: A
- `Dolomite`: A
- `Dripstone`: A
- `Eclogite`: A
- `ElvenheartBark`: A
- `ElvenheartHeartwood`: A
- `EmberbarkBark`: A
- `EmberbarkHeartwood`: A
- `EmeraldOre`: A
- `Fieldstone`: A
- `Flagstone`: A
- `Flint`: A
- `FlintGravel`: A
- `Flowstone`: A
- `FluoriteOre`: A
- `FossilLimestone`: A
- `Gabbro`: A
- `GalenaOre`: A
- `GalliteOre`: A
- `GarnetOre`: A
- `GarnetSand`: A
- `GermaniteOre`: A
- `Geyserite`: A
- `GlacialTill`: A
- `Glass`: A
- `Gneiss`: A
- `Gold`: A
- `GoldenSand`: A
- `GoldenSandstone`: A
- `GoldOre`: A
- `Granite`: A
- `GraniteScree`: A
- `GraphiteOre`: A
- `Gravel`: A
- `GreenMarble`: A
- `GreenockiteOre`: A
- `GreenSlate`: A
- `Greenstone`: A
- `GreyClay`: A
- `GreyMud`: A
- `GreySand`: A
- `Greywacke`: A
- `Guano`: A
- `GypsumRock`: A
- `Halite`: A
- `Hardpan`: A
- `HazelBark`: A
- `HazelHeartwood`: A
- `HematiteOre`: A
- `Hornfels`: A
- `Humus`: A
- `Ice`: A
- `IlmeniteOre`: A
- `IoliteOre`: A
- `Iron`: A
- `IronOre`: A
- `Ironstone`: A
- `JadeOre`: A
- `Jasper`: A
- `Jet`: A
- `JuniperBark`: A
- `JuniperHeartwood`: A
- `Kimberlite`: A
- `Komatiite`: A
- `KunziteOre`: A
- `KyaniteOre`: A
- `LabradoriteOre`: A
- `LapisOre`: A
- `LarimarOre`: A
- `Laterite`: A
- `LauriteOre`: A
- `LautariteOre`: A
- `LeadedWindow`: A
- `LeadOre`: A
- `LeafLitter`: A
- `Leather`: A
- `LeavesAcacia`: A
- `LeavesAzure`: A
- `LeavesBaobab`: A
- `LeavesBeech`: A
- `LeavesBirch`: A
- `LeavesBlush`: A
- `LeavesCherry`: A
- `LeavesCypress`: A
- `LeavesDate`: A
- `LeavesElven`: A
- `LeavesEmber`: A
- `LeavesHazel`: A
- `LeavesJuniper`: A
- `LeavesMangrove`: A
- `LeavesMaple`: A
- `LeavesMoon`: A
- `LeavesOak`: A
- `LeavesPalm`: A
- `LeavesPine`: A
- `LeavesPoplar`: A
- `LeavesRedwood`: A
- `LeavesSpruce`: A
- `LeavesWillow`: A
- `LepidoliteOre`: A
- `Limestone`: A
- `LimestoneScree`: A
- `LimoniteOre`: A
- `Linen`: A
- `Loam`: A
- `Loess`: A
- `LoranditeOre`: A
- `MagnesiteOre`: A
- `MagnetiteOre`: A
- `MalachiteOre`: A
- `ManganeseOre`: A
- `MangroveBark`: A
- `MangroveHeartwood`: A
- `MangroveMud`: A
- `MapleBark`: A
- `MapleHeartwood`: A
- `Marble`: A
- `Marl`: A
- `MicaOre`: A
- `Migmatite`: A
- `MolybdeniteOre`: A
- `MonaziteOre`: A
- `Moonmilk`: A
- `MoonsilverBark`: A
- `MoonsilverHeartwood`: A
- `MoonstoneOre`: A
- `MorganiteOre`: A
- `Mud`: A
- `MuddySand`: A
- `Mudstone`: A
- `MycelialEarth`: A
- `NickelOre`: A
- `NitratineOre`: A
- `NoduleBed`: A
- `Oak`: A
- `OakBark`: A
- `OakHeartwood`: A
- `OakPlanks`: A
- `Obsidian`: A
- `OchreEarth`: A
- `OchreSand`: A
- `OilShale`: A
- `OlivineSand`: A
- `OnyxOre`: A
- `Oolite`: A
- `OpalOre`: A
- `OrpimentOre`: A
- `OsmiridiumOre`: A
- `PackedDirt`: A
- `PalmBark`: A
- `PalmHeartwood`: A
- `PeaGravel`: A
- `Peat`: A
- `Pebbles`: A
- `Pegmatite`: A
- `PelagicClay`: A
- `Peridotite`: A
- `PeridotOre`: A
- `PetrifiedWood`: A
- `PhosphateOre`: A
- `Phyllite`: A
- `PillowBasalt`: A
- `PineBark`: A
- `PineHeartwood`: A
- `PineNeedles`: A
- `PinkGranite`: A
- `PinkMarble`: A
- `PinkSand`: A
- `PlatinumOre`: A
- `PlayaCrust`: A
- `Podzol`: A
- `PolluciteOre`: A
- `PoplarBark`: A
- `PoplarHeartwood`: A
- `Porphyry`: A
- `Potion`: A
- `Pumice`: A
- `PurpleSlate`: A
- `PyriteOre`: A
- `PyrochloreOre`: A
- `QuartzCrystal`: A
- `Quartzite`: A
- `QuartzOre`: A
- `QuartzSand`: A
- `RealgarOre`: A
- `RedClay`: A
- `RedEarth`: A
- `RedGranite`: A
- `RedGravel`: A
- `RedMud`: A
- `RedRoofTile`: A
- `RedSand`: A
- `RedSandstone`: A
- `RedwoodBark`: A
- `RedwoodHeartwood`: A
- `ReefRock`: A
- `RhodochrositeOre`: A
- `RhodoniteOre`: A
- `Rhyolite`: A
- `RockSalt`: A
- `RockyDirt`: A
- `RootedEarth`: A
- `RoquesiteOre`: A
- `RoseQuartzOre`: A
- `Ruby`: A
- `RubyOre`: A
- `RutileOre`: A
- `SaltCrust`: A
- `SaltpeterOre`: A
- `Sand`: A
- `Sandstone`: A
- `SandstoneScree`: A
- `SandyLoam`: A
- `Sapphire`: A
- `SapphireOre`: A
- `Schist`: A
- `Scoria`: A
- `Scree`: A
- `SeedAzure`: A
- `SeedBerry`: A
- `SeedCone`: A
- `SeedEmber`: A
- `SeedGold`: A
- `SeedHusk`: A
- `SeedNut`: A
- `SeedPink`: A
- `SeedPit`: A
- `SeedPod`: A
- `SeedSilver`: A
- `SeedWing`: A
- `Selenite`: A
- `Serpentinite`: A
- `Shale`: A
- `ShellSand`: A
- `Shingle`: A
- `SideriteOre`: A
- `Silt`: A
- `Siltstone`: A
- `Silver`: A
- `SilverOre`: A
- `Slate`: A
- `SlateScree`: A
- `SmokyQuartzOre`: A
- `Soapstone`: A
- `SodaliteOre`: A
- `SperryliteOre`: A
- `SphaleriteOre`: A
- `SpinelOre`: A
- `SpodumeneOre`: A
- `SpruceBark`: A
- `SpruceHeartwood`: A
- `Stem`: A
- `StibniteOre`: A
- `Stone`: A
- `StoneBrick`: A
- `SulfideRock`: A
- `SulfurCrust`: A
- `SulfurOre`: A
- `SunstoneOre`: A
- `SwampMuck`: A
- `Syenite`: A
- `SylviteOre`: A
- `Table`: A
- `TantaliteOre`: A
- `TanzaniteOre`: A
- `Thatch`: A
- `ThorianiteOre`: A
- `ThortveititeOre`: A
- `TigerEyeOre`: A
- `TinOre`: A
- `TitaniumOre`: A
- `TopazOre`: A
- `TourmalineOre`: A
- `Travertine`: A
- `TronaOre`: A
- `Tuff`: A
- `TundraSoil`: A
- `TungstenOre`: A
- `TurquoiseOre`: A
- `UraniniteOre`: A
- `VanadiniteOre`: A
- `VolcanicAsh`: A
- `WhiteClay`: A
- `WhitePlaster`: A
- `WhiteSand`: A
- `WhiteSandstone`: A
- `WillowBark`: A
- `WillowHeartwood`: A
- `WollastoniteOre`: A
- `XenotimeOre`: A
- `ZincOre`: A
- `ZirconOre`: A

### `assets/textures/surface/standard/`

- `AbyssalOoze`: a, ao, h, n, spec
- `AgateOreOverlay`: a, ao, n, spec
- `AlexandriteOreOverlay`: a, ao, n, spec
- `AlkaliFlat`: a, ao, h, n, spec
- `Alluvium`: a, ao, h, n, spec
- `AlpineGrassSide`: a, ao, g, h, n, spec
- `AlpineGrassTop`: a, ao, g, h, n, spec
- `AmazoniteOreOverlay`: a, ao, n, spec
- `AmberOreOverlay`: a, ao, n, spec
- `AmethystCrystal`: a, ao, h, n, spec
- `AmethystOreOverlay`: a, ao, n, spec
- `Amphibolite`: a, ao, h, n, spec
- `Andesite`: a, ao, h, n, spec
- `Anorthosite`: a, ao, h, n, spec
- `AquamarineOreOverlay`: a, ao, n, spec
- `Aragonite`: a, ao, h, n, spec
- `Arkose`: a, ao, h, n, spec
- `ArsenopyriteOreOverlay`: a, ao, n, spec
- `AshenGrassSide`: a, ao, g, h, n, spec
- `AshenGrassTop`: a, ao, g, h, n, spec
- `AshSoil`: a, ao, h, n, spec
- `AzuriteOreOverlay`: a, ao, n, spec
- `BandedIron`: a, ao, h, n, spec
- `BandedSandstone`: a, ao, h, n, spec
- `BaryteOreOverlay`: a, ao, n, spec
- `Basalt`: a, ao, h, n, spec
- `BasaltScree`: a, ao, h, n, spec
- `BastnasiteOreOverlay`: a, ao, n, spec
- `BauxiteOreOverlay`: a, ao, n, spec
- `Beachrock`: a, ao, h, n, spec
- `BenitoiteOreOverlay`: a, ao, n, spec
- `Bentonite`: a, ao, h, n, spec
- `BentoniteLensesOverlay`: a, ao, n, spec
- `BerylOreOverlay`: a, ao, n, spec
- `BismuthiniteOreOverlay`: a, ao, n, spec
- `BlackEarth`: a, ao, h, n, spec
- `BlackGranite`: a, ao, h, n, spec
- `BlackGravel`: a, ao, h, n, spec
- `BlackMarble`: a, ao, h, n, spec
- `BlackMud`: a, ao, h, n, spec
- `BlackSand`: a, ao, h, n, spec
- `BlueClay`: a, ao, h, n, spec
- `BlueIce`: a, ao, h, n, spec
- `Bluestone`: a, ao, h, n, spec
- `BlushwoodPetalsTop`: a, ao, g, h, n, spec
- `BoraxOreOverlay`: a, ao, n, spec
- `BorniteOreOverlay`: a, ao, n, spec
- `BraggiteOreOverlay`: a, ao, n, spec
- `BrambleSide`: a, ao, g, h, n, spec
- `BrambleTop`: a, ao, g, h, n, spec
- `Breccia`: a, ao, h, n, spec
- `BromargyriteOreOverlay`: a, ao, n, spec
- `BrownSand`: a, ao, h, n, spec
- `CalaveriteOreOverlay`: a, ao, n, spec
- `CalcareousOoze`: a, ao, h, n, spec
- `Calcite`: a, ao, h, n, spec
- `Caliche`: a, ao, h, n, spec
- `CarnalliteOreOverlay`: a, ao, n, spec
- `CarnelianOreOverlay`: a, ao, n, spec
- `CarnotiteOreOverlay`: a, ao, n, spec
- `CelestineOreOverlay`: a, ao, n, spec
- `ChalcopyriteOreOverlay`: a, ao, n, spec
- `Chalk`: a, ao, h, n, spec
- `Chert`: a, ao, h, n, spec
- `ChromiteOreOverlay`: a, ao, n, spec
- `ChrysoberylOreOverlay`: a, ao, n, spec
- `ChrysocollaOreOverlay`: a, ao, n, spec
- `Cinders`: a, ao, h, n, spec
- `CinnabarOreOverlay`: a, ao, n, spec
- `CitrineOreOverlay`: a, ao, n, spec
- `ClausthaliteOreOverlay`: a, ao, n, spec
- `Clay`: a, ao, h, n, spec
- `Claystone`: a, ao, h, n, spec
- `CloverGrassSide`: a, ao, g, h, n, spec
- `CloverGrassTop`: a, ao, g, h, n, spec
- `CoalOreOverlay`: a, ao, n, spec
- `CoarseDirt`: a, ao, h, n, spec
- `CobaltOreOverlay`: a, ao, n, spec
- `Cobblestone`: a
- `ColemaniteOreOverlay`: a, ao, n, spec
- `ColumbiteOreOverlay`: a, ao, n, spec
- `Conglomerate`: a, ao, h, n, spec
- `CopperOreOverlay`: a, ao, n, spec
- `Coquina`: a, ao, h, n, spec
- `CoralRubble`: a, ao, h, n, spec
- `CoralSand`: a, ao, h, n, spec
- `CrackedClay`: a, ao, h, n, spec
- `CrackedEarth`: a, ao, h, n, spec
- `CryoliteOreOverlay`: a, ao, n, spec
- `CushionMossSide`: a, ao, g, h, n, spec
- `CushionMossTop`: a, ao, g, h, n, spec
- `Dacite`: a, ao, h, n, spec
- `DarkLimestone`: a, ao, h, n, spec
- `DarkTimber`: a
- `Diabase`: a, ao, h, n, spec
- `DiamondOreOverlay`: a, ao, n, spec
- `Diatomite`: a, ao, h, n, spec
- `DioptaseOreOverlay`: a, ao, n, spec
- `Diorite`: a, ao, h, n, spec
- `Dirt`: a
- `Dolomite`: a, ao, h, n, spec
- `Dripstone`: a, ao, h, n, spec
- `DryGrassSide`: a, ao, g, h, n, spec
- `DryGrassTop`: a, ao, g, h, n, spec
- `DuneGrassSide`: a, ao, g, h, n, spec
- `DuneGrassTop`: a, ao, g, h, n, spec
- `Eclogite`: a, ao, h, n, spec
- `EmeraldOreOverlay`: a, ao, n, spec
- `FernSide`: a, ao, g, h, n, spec
- `FernTop`: a, ao, g, h, n, spec
- `Fieldstone`: a, ao, h, n, spec
- `Flagstone`: a
- `Flint`: a, ao, h, n, spec
- `FlintGravel`: a, ao, h, n, spec
- `FlintNodulesOverlay`: a, ao, n, spec
- `Flowstone`: a, ao, h, n, spec
- `FluoriteOreOverlay`: a, ao, n, spec
- `ForestLitterTop`: a, ao, g, h, n, spec
- `FossilLimestone`: a, ao, h, n, spec
- `Gabbro`: a, ao, h, n, spec
- `GalenaOreOverlay`: a, ao, n, spec
- `GalliteOreOverlay`: a, ao, n, spec
- `GarnetOreOverlay`: a, ao, n, spec
- `GarnetSand`: a, ao, h, n, spec
- `GermaniteOreOverlay`: a, ao, n, spec
- `Geyserite`: a, ao, h, n, spec
- `GlacialTill`: a, ao, h, n, spec
- `Gneiss`: a, ao, h, n, spec
- `GoldenGrassSide`: a, ao, g, h, n, spec
- `GoldenGrassTop`: a, ao, g, h, n, spec
- `GoldenSand`: a, ao, h, n, spec
- `GoldenSandstone`: a, ao, h, n, spec
- `GoldOreOverlay`: a, ao, n, spec
- `Granite`: a, ao, h, n, spec
- `GraniteScree`: a, ao, h, n, spec
- `GraphiteOreOverlay`: a, ao, n, spec
- `Gravel`: a, ao, h, n, spec
- `GreenMarble`: a, ao, h, n, spec
- `GreenMossSide`: a, ao, g, h, n, spec
- `GreenMossTop`: a, ao, g, h, n, spec
- `GreenockiteOreOverlay`: a, ao, n, spec
- `GreenSlate`: a, ao, h, n, spec
- `Greenstone`: a, ao, h, n, spec
- `GreyClay`: a, ao, h, n, spec
- `GreyMud`: a, ao, h, n, spec
- `GreySand`: a, ao, h, n, spec
- `Greywacke`: a, ao, h, n, spec
- `Guano`: a, ao, h, n, spec
- `GypsumRock`: a, ao, h, n, spec
- `GypsumRosesOverlay`: a, ao, n, spec
- `Halite`: a, ao, h, n, spec
- `Hardpan`: a, ao, h, n, spec
- `HeatherSide`: a, ao, g, h, n, spec
- `HeatherTop`: a, ao, g, h, n, spec
- `HematiteOreOverlay`: a, ao, n, spec
- `Hornfels`: a, ao, h, n, spec
- `Humus`: a, ao, h, n, spec
- `Ice`: a, ao, h, n, spec
- `IlmeniteOreOverlay`: a, ao, n, spec
- `IoliteOreOverlay`: a, ao, n, spec
- `IronOreOverlay`: a, ao, n, spec
- `Ironstone`: a, ao, h, n, spec
- `JadeOreOverlay`: a, ao, n, spec
- `Jasper`: a, ao, h, n, spec
- `Jet`: a, ao, h, n, spec
- `JetOverlay`: a, ao, n, spec
- `JungleGrassSide`: a, ao, g, h, n, spec
- `JungleGrassTop`: a, ao, g, h, n, spec
- `Kimberlite`: a, ao, h, n, spec
- `Komatiite`: a, ao, h, n, spec
- `KunziteOreOverlay`: a, ao, n, spec
- `KyaniteOreOverlay`: a, ao, n, spec
- `LabradoriteOreOverlay`: a, ao, n, spec
- `LapisOreOverlay`: a, ao, n, spec
- `LarimarOreOverlay`: a, ao, n, spec
- `Laterite`: a, ao, h, n, spec
- `LauriteOreOverlay`: a, ao, n, spec
- `LautariteOreOverlay`: a, ao, n, spec
- `LeadedWindow`: a
- `LeadOreOverlay`: a, ao, n, spec
- `LeafLitter`: a, ao, h, n, spec
- `LepidoliteOreOverlay`: a, ao, n, spec
- `LichenSide`: a, ao, g, h, n, spec
- `LichenTop`: a, ao, g, h, n, spec
- `Limestone`: a, ao, h, n, spec
- `LimestoneScree`: a, ao, h, n, spec
- `LimoniteOreOverlay`: a, ao, n, spec
- `Loam`: a, ao, h, n, spec
- `Loess`: a, ao, h, n, spec
- `LoranditeOreOverlay`: a, ao, n, spec
- `MagnesiteOreOverlay`: a, ao, n, spec
- `MagnetiteOreOverlay`: a, ao, n, spec
- `MalachiteOreOverlay`: a, ao, n, spec
- `ManganeseOreOverlay`: a, ao, n, spec
- `MangroveLitterTop`: a, ao, g, h, n, spec
- `MangroveMud`: a, ao, h, n, spec
- `Marble`: a, ao, h, n, spec
- `Marl`: a, ao, h, n, spec
- `MarshGrassSide`: a, ao, g, h, n, spec
- `MarshGrassTop`: a, ao, g, h, n, spec
- `MeadowGrassSide`: a, ao, g, h, n, spec
- `MeadowGrassTop`: a, ao, g, h, n, spec
- `MicaOreOverlay`: a, ao, n, spec
- `Migmatite`: a, ao, h, n, spec
- `MolybdeniteOreOverlay`: a, ao, n, spec
- `MonaziteOreOverlay`: a, ao, n, spec
- `MoonlitGrassSide`: a, ao, g, h, n, spec
- `MoonlitGrassTop`: a, ao, g, h, n, spec
- `Moonmilk`: a, ao, h, n, spec
- `MoonstoneOreOverlay`: a, ao, n, spec
- `MorganiteOreOverlay`: a, ao, n, spec
- `Mud`: a, ao, h, n, spec
- `MuddySand`: a, ao, h, n, spec
- `Mudstone`: a, ao, h, n, spec
- `MycelialEarth`: a, ao, h, n, spec
- `NickelOreOverlay`: a, ao, n, spec
- `NitratineOreOverlay`: a, ao, n, spec
- `NoduleBed`: a, ao, h, n, spec
- `OakLogNorth`: a
- `OakPlanks`: a
- `Obsidian`: a, ao, h, n, spec
- `ObsidianShardsOverlay`: a, ao, n, spec
- `OchreEarth`: a, ao, h, n, spec
- `OchreSand`: a, ao, h, n, spec
- `OilShale`: a, ao, h, n, spec
- `OilShaleSeamsOverlay`: a, ao, n, spec
- `OlivineSand`: a, ao, h, n, spec
- `OnyxOreOverlay`: a, ao, n, spec
- `Oolite`: a, ao, h, n, spec
- `OpalOreOverlay`: a, ao, n, spec
- `OrpimentOreOverlay`: a, ao, n, spec
- `OsmiridiumOreOverlay`: a, ao, n, spec
- `PackedDirt`: a
- `PeaGravel`: a, ao, h, n, spec
- `Peat`: a, ao, h, n, spec
- `Pebbles`: a, ao, h, n, spec
- `PebblesOverlay`: a, ao, n, spec
- `Pegmatite`: a, ao, h, n, spec
- `PelagicClay`: a, ao, h, n, spec
- `Peridotite`: a, ao, h, n, spec
- `PeridotOreOverlay`: a, ao, n, spec
- `PetrifiedWood`: a, ao, h, n, spec
- `PhosphateOreOverlay`: a, ao, n, spec
- `Phyllite`: a, ao, h, n, spec
- `PillowBasalt`: a, ao, h, n, spec
- `PineLitterTop`: a, ao, g, h, n, spec
- `PineNeedles`: a, ao, h, n, spec
- `PinkGranite`: a, ao, h, n, spec
- `PinkMarble`: a, ao, h, n, spec
- `PinkSand`: a, ao, h, n, spec
- `PlatinumOreOverlay`: a, ao, n, spec
- `PlayaCrust`: a, ao, h, n, spec
- `Podzol`: a, ao, h, n, spec
- `PolluciteOreOverlay`: a, ao, n, spec
- `Porphyry`: a, ao, h, n, spec
- `Pumice`: a, ao, h, n, spec
- `PurpleSlate`: a, ao, h, n, spec
- `PyriteOreOverlay`: a, ao, n, spec
- `PyrochloreOreOverlay`: a, ao, n, spec
- `QuartzCrystal`: a, ao, h, n, spec
- `Quartzite`: a, ao, h, n, spec
- `QuartzOreOverlay`: a, ao, n, spec
- `QuartzSand`: a, ao, h, n, spec
- `RealgarOreOverlay`: a, ao, n, spec
- `RedClay`: a, ao, h, n, spec
- `RedEarth`: a, ao, h, n, spec
- `RedGranite`: a, ao, h, n, spec
- `RedGravel`: a, ao, h, n, spec
- `RedMud`: a, ao, h, n, spec
- `RedRoofTile`: a
- `RedSand`: a, ao, h, n, spec
- `RedSandstone`: a, ao, h, n, spec
- `ReedBedSide`: a, ao, g, h, n, spec
- `ReedBedTop`: a, ao, g, h, n, spec
- `ReefRock`: a, ao, h, n, spec
- `RhodochrositeOreOverlay`: a, ao, n, spec
- `RhodoniteOreOverlay`: a, ao, n, spec
- `Rhyolite`: a, ao, h, n, spec
- `RockSalt`: a, ao, h, n, spec
- `RockyDirt`: a, ao, h, n, spec
- `RootedEarth`: a, ao, h, n, spec
- `RootletsOverlay`: a, ao, n, spec
- `RootsOverlay`: a, ao, n, spec
- `RoquesiteOreOverlay`: a, ao, n, spec
- `RoseQuartzOreOverlay`: a, ao, n, spec
- `RubyOreOverlay`: a, ao, n, spec
- `RutileOreOverlay`: a, ao, n, spec
- `SaltCrust`: a, ao, h, n, spec
- `SaltmarshGrassSide`: a, ao, g, h, n, spec
- `SaltmarshGrassTop`: a, ao, g, h, n, spec
- `SaltpeterOreOverlay`: a, ao, n, spec
- `Sand`: a
- `Sandstone`: a, ao, h, n, spec
- `SandstoneScree`: a, ao, h, n, spec
- `SandyLoam`: a, ao, h, n, spec
- `SapphireOreOverlay`: a, ao, n, spec
- `SavannaGrassSide`: a, ao, g, h, n, spec
- `SavannaGrassTop`: a, ao, g, h, n, spec
- `Schist`: a, ao, h, n, spec
- `Scoria`: a, ao, h, n, spec
- `Scree`: a, ao, h, n, spec
- `SeagrassSide`: a, ao, g, h, n, spec
- `SeagrassTop`: a, ao, g, h, n, spec
- `Selenite`: a, ao, h, n, spec
- `Serpentinite`: a, ao, h, n, spec
- `ShadowMossSide`: a, ao, g, h, n, spec
- `ShadowMossTop`: a, ao, g, h, n, spec
- `Shale`: a, ao, h, n, spec
- `ShellSand`: a, ao, h, n, spec
- `Shingle`: a, ao, h, n, spec
- `SideriteOreOverlay`: a, ao, n, spec
- `Silt`: a, ao, h, n, spec
- `Siltstone`: a, ao, h, n, spec
- `SilverOreOverlay`: a, ao, n, spec
- `Slate`: a, ao, h, n, spec
- `SlateScree`: a, ao, h, n, spec
- `SmokyQuartzOreOverlay`: a, ao, n, spec
- `Soapstone`: a, ao, h, n, spec
- `SodaliteOreOverlay`: a, ao, n, spec
- `SperryliteOreOverlay`: a, ao, n, spec
- `SphaleriteOreOverlay`: a, ao, n, spec
- `SpinelOreOverlay`: a, ao, n, spec
- `SpodumeneOreOverlay`: a, ao, n, spec
- `SteppeGrassSide`: a, ao, g, h, n, spec
- `SteppeGrassTop`: a, ao, g, h, n, spec
- `StibniteOreOverlay`: a, ao, n, spec
- `Stone`: a
- `StoneBrick`: a
- `StonesOverlay`: a, ao, n, spec
- `SulfideRock`: a, ao, h, n, spec
- `SulfurCrust`: a, ao, h, n, spec
- `SulfurOreOverlay`: a, ao, n, spec
- `SunstoneOreOverlay`: a, ao, n, spec
- `SwampMuck`: a, ao, h, n, spec
- `Syenite`: a, ao, h, n, spec
- `SylviteOreOverlay`: a, ao, n, spec
- `TantaliteOreOverlay`: a, ao, n, spec
- `TanzaniteOreOverlay`: a, ao, n, spec
- `Thatch`: a
- `ThorianiteOreOverlay`: a, ao, n, spec
- `ThortveititeOreOverlay`: a, ao, n, spec
- `TigerEyeOreOverlay`: a, ao, n, spec
- `TinOreOverlay`: a, ao, n, spec
- `TitaniumOreOverlay`: a, ao, n, spec
- `TopazOreOverlay`: a, ao, n, spec
- `TourmalineOreOverlay`: a, ao, n, spec
- `Travertine`: a, ao, h, n, spec
- `TronaOreOverlay`: a, ao, n, spec
- `Tuff`: a, ao, h, n, spec
- `TundraGrassSide`: a, ao, g, h, n, spec
- `TundraGrassTop`: a, ao, g, h, n, spec
- `TundraSoil`: a, ao, h, n, spec
- `TungstenOreOverlay`: a, ao, n, spec
- `TurquoiseOreOverlay`: a, ao, n, spec
- `UraniniteOreOverlay`: a, ao, n, spec
- `VanadiniteOreOverlay`: a, ao, n, spec
- `VolcanicAsh`: a, ao, h, n, spec
- `WhiteClay`: a, ao, h, n, spec
- `WhitePlaster`: a
- `WhiteSand`: a, ao, h, n, spec
- `WhiteSandstone`: a, ao, h, n, spec
- `WildflowerMeadowSide`: a, ao, g, h, n, spec
- `WildflowerMeadowTop`: a, ao, g, h, n, spec
- `WollastoniteOreOverlay`: a, ao, n, spec
- `XenotimeOreOverlay`: a, ao, n, spec
- `ZincOreOverlay`: a, ao, n, spec
- `ZirconOreOverlay`: a, ao, n, spec

### `assets/worlds/`

- `TerraArcana`: (no suffix), Height

## Shaders (172 files)

### `assets/shaders/characters/includes/`

`CharacterFace.glsl`, `CharacterHidden.glsl`, `CharacterNormal.glsl`, `CharactersStandard.glsl`

### `assets/shaders/characters/standard/`

`StandardcharacterShader.fsh`, `StandardcharacterShader.vsh`

### `assets/shaders/debug/`

`DebugCameraOrientationBaseShader.fsh`, `DebugCameraOrientationHUDShader.fsh`, `DebugSurfaceShader.fsh`,
`DebugSurfaceShader.vsh`, `DebugSurfaceUVShader.fsh`, `DebugSurfaceUVShader.vsh`, `DebugUVShader.fsh`,
`DebugUVShader.vsh`

### `assets/shaders/editor/charactereditor/`

`CharacterMeshOverlayShader.fsh`, `CharacterMeshOverlayShader.vsh`

### `assets/shaders/editor/characterview/`

`CharacterViewCharacterShader.fsh`, `CharacterViewMarkerShader.fsh`, `CharacterViewMarkerShader.vsh`

### `assets/shaders/editor/itemeditor/`

`ItemEditorCursorShader.fsh`, `ItemEditorCursorShader.vsh`, `ItemEditorGridShader.fsh`, `ItemEditorGridShader.vsh`,
`ItemEditorModelShader.fsh`, `ItemEditorModelShader.vsh`

### `assets/shaders/editor/placementicon/`

`PlacementIconShader.fsh`

### `assets/shaders/editor/textureviewer/`

`TextureViewerAtlasShader.fsh`, `TextureViewerAtlasShader.vsh`

### `assets/shaders/editor/worldeditor/`

`WorldEditorBrushShader.fsh`, `WorldEditorHeightShader.fsh`

### `assets/shaders/editor/worldmap/`

`WorldMapMarkerShader.fsh`, `WorldMapMarkerShader.vsh`, `WorldMapTileShader.fsh`, `WorldMapTileShader.vsh`

### `assets/shaders/fonts/standard/`

`StandardFontShader.fsh`, `StandardFontShader.vsh`

### `assets/shaders/includes/`

`BlockOrientationMapData.glsl`, `CameraData.glsl`, `CelestialData.glsl`, `ContentData.glsl`, `CoveringData.glsl`,
`GBufferData.glsl`, `GBufferNormal.glsl`, `GridCoordinateData.glsl`, `ItemRotationData.glsl`, `MacroCoverageData.glsl`,
`MacroCoverageUtility.glsl`, `MacroWaterData.glsl`, `MoonLightData.glsl`, `NaturalNoiseData.glsl`, `NoiseUtility.glsl`,
`OceanData.glsl`, `OrthoData.glsl`, `PlayerPositionData.glsl`, `PostProcessData.glsl`, `PrecipitationData.glsl`,
`QuadRecord.glsl`, `SettingsData.glsl`, `SkyColorData.glsl`, `SliceData.glsl`, `SunLightData.glsl`, `TimeData.glsl`,
`WeatherMapData.glsl`, `WeatherMapUtility.glsl`, `WindData.glsl`

### `assets/shaders/items/equipment/`

`EquipmentItemShader.fsh`, `EquipmentItemShader.vsh`

### `assets/shaders/items/includes/`

`BlockRepeat.glsl`, `ItemsStandard.glsl`

### `assets/shaders/items/inventory/`

`InventoryItemShader.fsh`, `InventoryItemShader.vsh`, `InventoryVehicleShader.fsh`, `InventoryVehicleShader.vsh`

### `assets/shaders/items/standard/`

`StandardItemShader.fsh`, `StandardItemShader.vsh`

### `assets/shaders/items/tierframe/`

`TierFrameShader.fsh`, `TierFrameShader.vsh`

### `assets/shaders/items/vehicle/`

`VehicleItemShader.fsh`, `VehicleItemShader.vsh`

### `assets/shaders/liquid/`

`MacroWaterShader.fsh`, `MacroWaterShader.vsh`, `UnderwaterShader.fsh`, `WaterShader.fsh`, `WaterShader.tcs`,
`WaterShader.tes`, `WaterShader.vsh`

### `assets/shaders/liquid/includes/`

`OceanSurface.glsl`, `OceanTessellation.glsl`, `WaterLight.glsl`, `WaterReflection.glsl`, `WaterSceneData.glsl`,
`WaterShading.glsl`

### `assets/shaders/postprocessing/effects/`

`PostBlurShader.fsh`, `PostCompositeShader.fsh`, `PostEffectShader.vsh`, `PostPrefilterShader.fsh`

### `assets/shaders/postprocessing/effects/includes/`

`PostAntiAliasing.glsl`, `PostBlurRadius.glsl`, `PostFocus.glsl`

### `assets/shaders/postprocessing/includes/`

`InverseDistance.glsl`, `ViewPosReconstruct.glsl`

### `assets/shaders/postprocessing/lighting/`

`LightingShader.fsh`, `LightingShader.vsh`

### `assets/shaders/postprocessing/lighting/includes/`

`AtmosphericFog.glsl`, `Outline.glsl`

### `assets/shaders/postprocessing/ssao/`

`SSAOData.glsl`, `SSAOOcclusion.glsl`, `SSAOShader.fsh`, `SSAOShader.vsh`

### `assets/shaders/shipwright/`

`ShipwrightPaperShader.fsh`, `ShipwrightPaperShader.vsh`

### `assets/shaders/sky/squint/`

`SquintShader.fsh`

### `assets/shaders/sky/standard/`

`StandardSkyShader.fsh`, `StandardSkyShader.vsh`

### `assets/shaders/sky/util/`

`SkyBodies.glsl`, `SkyColor.glsl`, `SkyMoon.glsl`, `SkyNoise.glsl`, `SkyStars.glsl`, `SkySun.glsl`,
`SkySunVisibility.glsl`

### `assets/shaders/sprites/standard/`

`StandardSpriteShader.fsh`, `StandardSpriteShader.vsh`

### `assets/shaders/surface/includes/`

`Albedo.glsl`, `AO.glsl`, `Bevel.glsl`, `CloudShadow.glsl`, `Content.glsl`, `Coverage.glsl`, `Emissive.glsl`,
`Height.glsl`, `HeightDisplacement.glsl`, `Metal.glsl`, `NearTerrainNoise.glsl`, `Normal.glsl`, `PatchCulling.glsl`,
`Specular.glsl`, `SurfaceStandard.glsl`, `SurfaceTessellationTier.glsl`, `SurfaceTierFlat.glsl`, `SurfaceTierFull.glsl`,
`TiledSampling.glsl`

### `assets/shaders/surface/macro/`

`MacroTerrainShader.fsh`, `MacroTerrainShader.vsh`

### `assets/shaders/surface/standard/`

`StandardSurfaceShader.fsh`, `StandardSurfaceShader.tcs`, `StandardSurfaceShader.tes`, `StandardSurfaceShader.vsh`

### `assets/shaders/trees/bark/`

`TreeBarkFallingShader.fsh`, `TreeBarkFallingShader.vsh`, `TreeBarkShader.fsh`, `TreeBarkShader.vsh`

### `assets/shaders/trees/includes/`

`TreeBark.glsl`, `TreeLeaf.glsl`, `TreeLeafSprig.glsl`, `TreeShading.glsl`

### `assets/shaders/trees/leaf/`

`TreeLeafFallingShader.fsh`, `TreeLeafFallingShader.vsh`, `TreeLeafShader.fsh`, `TreeLeafShader.vsh`

### `assets/shaders/util/`

`BlitShader.fsh`, `BlitShader.vsh`, `PlanarPassShader.vsh`, `SphericalPassShader.vsh`

### `assets/shaders/vitality/`

`VitalityShader.fsh`

### `assets/shaders/weather/includes/`

`CloudComposite.glsl`, `CloudDome.glsl`, `CloudMarch.glsl`, `CloudRain.glsl`, `CloudVisual.glsl`

### `assets/shaders/weather/precipitation/`

`PrecipitationShader.fsh`

### `assets/shaders/weather/standard/`

`WeatherShader.fsh`

## Sounds (160 files)

Audio the sound pipeline loads from `assets/sounds/`, each beside its companion `.arpg` (listed under Data Files).
Every sound is Ogg Vorbis. Sounds imported from `.claude/catalogue/` are the owner's library and are not listed.

### `assets/sounds/ambience/`

`Birds.ogg`, `Crickets.ogg`, `Ocean.ogg`

### `assets/sounds/blocks/dirt/`

`Break_A.ogg`, `Break_B.ogg`, `Break_C.ogg`, `Dig_A.ogg`, `Dig_B.ogg`, `Dig_C.ogg`, `Place_A.ogg`, `Place_B.ogg`,
`Place_C.ogg`

### `assets/sounds/blocks/glass/`

`Break_A.ogg`, `Break_B.ogg`, `Break_C.ogg`, `Dig_A.ogg`, `Dig_B.ogg`, `Dig_C.ogg`, `Place_A.ogg`, `Place_B.ogg`,
`Place_C.ogg`

### `assets/sounds/blocks/leaves/`

`Break_A.ogg`, `Break_B.ogg`, `Break_C.ogg`, `Dig_A.ogg`, `Dig_B.ogg`, `Dig_C.ogg`, `Place_A.ogg`, `Place_B.ogg`,
`Place_C.ogg`

### `assets/sounds/blocks/stone/`

`Break_A.ogg`, `Break_B.ogg`, `Break_C.ogg`, `Dig_A.ogg`, `Dig_B.ogg`, `Dig_C.ogg`, `Place_A.ogg`, `Place_B.ogg`,
`Place_C.ogg`

### `assets/sounds/blocks/wood/`

`Break_A.ogg`, `Break_B.ogg`, `Break_C.ogg`, `Dig_A.ogg`, `Dig_B.ogg`, `Dig_C.ogg`, `Place_A.ogg`, `Place_B.ogg`,
`Place_C.ogg`

### `assets/sounds/combat/`

`Death.ogg`, `HitFlesh_A.ogg`, `HitFlesh_B.ogg`, `HitFlesh_C.ogg`, `HitWood_A.ogg`, `HitWood_B.ogg`, `HitWood_C.ogg`,
`Swing_A.ogg`, `Swing_B.ogg`, `Swing_C.ogg`, `SwingHeavy_A.ogg`, `SwingHeavy_B.ogg`, `SwingHeavy_C.ogg`, `Throw_A.ogg`,
`Throw_B.ogg`, `Throw_C.ogg`

### `assets/sounds/footsteps/dirt/`

`Dirt_A.ogg`, `Dirt_B.ogg`, `Dirt_C.ogg`, `Dirt_D.ogg`

### `assets/sounds/footsteps/grass/`

`Grass_A.ogg`, `Grass_B.ogg`, `Grass_C.ogg`, `Grass_D.ogg`

### `assets/sounds/footsteps/gravel/`

`Gravel_A.ogg`, `Gravel_B.ogg`, `Gravel_C.ogg`, `Gravel_D.ogg`

### `assets/sounds/footsteps/leaves/`

`Leaves_A.ogg`, `Leaves_B.ogg`, `Leaves_C.ogg`, `Leaves_D.ogg`

### `assets/sounds/footsteps/sand/`

`Sand_A.ogg`, `Sand_B.ogg`, `Sand_C.ogg`, `Sand_D.ogg`

### `assets/sounds/footsteps/stone/`

`Stone_A.ogg`, `Stone_B.ogg`, `Stone_C.ogg`, `Stone_D.ogg`

### `assets/sounds/footsteps/water/`

`Water_A.ogg`, `Water_B.ogg`, `Water_C.ogg`, `Water_D.ogg`

### `assets/sounds/footsteps/wood/`

`Wood_A.ogg`, `Wood_B.ogg`, `Wood_C.ogg`, `Wood_D.ogg`

### `assets/sounds/furnishings/`

`ChestClose.ogg`, `ChestOpen.ogg`

### `assets/sounds/interface/`

`Click.ogg`, `Close.ogg`, `Error.ogg`

### `assets/sounds/items/`

`BagClose.ogg`, `BagOpen.ogg`, `Drop_A.ogg`, `Drop_B.ogg`, `Equip_A.ogg`, `Equip_B.ogg`, `EquipMetal_A.ogg`,
`EquipMetal_B.ogg`, `Pickup_A.ogg`, `Pickup_B.ogg`

### `assets/sounds/movement/`

`Jump_A.ogg`, `Jump_B.ogg`, `Land_A.ogg`, `Land_B.ogg`, `LandHard_A.ogg`, `LandHard_B.ogg`

### `assets/sounds/music/`

`Day.ogg`, `MainMenu.ogg`, `Night.ogg`

### `assets/sounds/progression/`

`LevelUp.ogg`, `Profession.ogg`, `Skill.ogg`

### `assets/sounds/trees/`

`Chop_A.ogg`, `Chop_B.ogg`, `Chop_C.ogg`, `Fall.ogg`, `Land.ogg`

### `assets/sounds/vehicles/ship/`

`Anchor.ogg`, `HullCreak.ogg`, `HullImpact_A.ogg`, `HullImpact_B.ogg`, `Rudder_A.ogg`, `Rudder_B.ogg`, `SailLuff.ogg`

### `assets/sounds/vitality/`

`Defeat.ogg`, `Heartbeat.ogg`

### `assets/sounds/water/`

`Splash_A.ogg`, `Splash_B.ogg`, `Splash_C.ogg`, `SplashBig_A.ogg`, `SplashBig_B.ogg`, `SplashBig_C.ogg`,
`SwimStroke_A.ogg`, `SwimStroke_B.ogg`, `SwimStroke_C.ogg`, `Underwater.ogg`

### `assets/sounds/weapons/`

`Fizzle.ogg`, `Load.ogg`, `Prime.ogg`

### `assets/sounds/weather/`

`Rain.ogg`, `RainHeavy.ogg`, `Thunder_A.ogg`, `Thunder_B.ogg`, `Thunder_C.ogg`, `ThunderFar_A.ogg`, `ThunderFar_B.ogg`,
`ThunderFar_C.ogg`, `Wind.ogg`, `WindStrong.ogg`

## Data Files (1721 files)

Sealed `.arpg` content: definitions, meshes, animations, menus, schemas and the companions beside PNGs.

### `assets/animations/humanoid/`

`HumanoidAim.arpg`, `HumanoidAttackOverhead.arpg`, `HumanoidAttackRising.arpg`, `HumanoidAttackSlashLeft.arpg`,
`HumanoidAttackSlashRight.arpg`, `HumanoidAttackThrust.arpg`, `HumanoidBlock.arpg`, `HumanoidChop.arpg`,
`HumanoidDive.arpg`, `HumanoidFall.arpg`, `HumanoidGuardHigh.arpg`, `HumanoidGuardLeft.arpg`, `HumanoidGuardLow.arpg`,
`HumanoidGuardMiddle.arpg`, `HumanoidGuardRight.arpg`, `HumanoidIdle.arpg`, `HumanoidJog.arpg`, `HumanoidJump.arpg`,
`HumanoidLeanLeft.arpg`, `HumanoidLeanRight.arpg`, `HumanoidLookDown.arpg`, `HumanoidLookForward.arpg`,
`HumanoidLookLeft.arpg`, `HumanoidLookRight.arpg`, `HumanoidLookUp.arpg`, `HumanoidPickUp.arpg`, `HumanoidPlace.arpg`,
`HumanoidRecoil.arpg`, `HumanoidRun.arpg`, `HumanoidShallowWadeIdle.arpg`, `HumanoidShallowWadeRun.arpg`,
`HumanoidStagger.arpg`, `HumanoidSurfacing.arpg`, `HumanoidSwim.arpg`, `HumanoidSwing.arpg`, `HumanoidThrow.arpg`,
`HumanoidTread.arpg`, `HumanoidUnderwaterSwim.arpg`, `HumanoidUnderwaterTread.arpg`, `HumanoidWade.arpg`,
`HumanoidWadeIdle.arpg`, `HumanoidWadeRun.arpg`, `HumanoidWadeShallow.arpg`, `HumanoidWalk.arpg`,
`HumanoidWaterJump.arpg`, `HumanoidWaterLeap.arpg`

### `assets/animationtrees/humanoid/`

`HumanoidAnimationTree.arpg`

### `assets/application/fbos/`

`FBOs.arpg`

### `assets/application/manifests/`

`Launcher.arpg`

### `assets/application/threads/`

`ThreadPools.arpg`

### `assets/architectures/`

`Fachwerk.arpg`, `Tudor.arpg`

### `assets/attributes/`

`Attributes.arpg`

### `assets/behaviors/`

`Bipedal.arpg`

### `assets/biomes/aldermere/`

`AshbourneValeBiome.arpg`, `BlushwoodBiome.arpg`, `CairnhollowMoorsBiome.arpg`, `DuskwaterValeBiome.arpg`,
`EllowenTerracesBiome.arpg`, `GildenSavannaBiome.arpg`, `GreyspineMountainsBiome.arpg`, `HoarfrostReachBiome.arpg`,
`NorthmarkPinewildsBiome.arpg`, `SunmereVineyardsBiome.arpg`, `ValdrenHeartmeadowsBiome.arpg`, `WardenPeaksBiome.arpg`

### `assets/biomes/aldermere/ashbourne/`

`AshbourneKnollsBiome.arpg`, `AshbourneMireBiome.arpg`, `AshbourneOxbowsBiome.arpg`

### `assets/biomes/aldermere/blush/`

`BlushClearingsBiome.arpg`, `BlushKnollsBiome.arpg`, `BlushMeresBiome.arpg`, `BlushTorsBiome.arpg`

### `assets/biomes/aldermere/cairnhollow/`

`CairnhollowGorgeBiome.arpg`, `CairnhollowLochsBiome.arpg`, `CairnhollowMireBiome.arpg`,
`CairnhollowMonadnockBiome.arpg`, `CairnhollowTorsBiome.arpg`

### `assets/biomes/aldermere/duskwater/`

`DuskwaterDownsBiome.arpg`, `DuskwaterMireBiome.arpg`, `DuskwaterOxbowsBiome.arpg`

### `assets/biomes/aldermere/ellowen/`

`EllowenGrovesBiome.arpg`, `EllowenKarstTowersBiome.arpg`, `EllowenMeresBiome.arpg`, `EllowenScarpsBiome.arpg`

### `assets/biomes/aldermere/gilden/`

`GildenGrovesBiome.arpg`, `GildenKopjeHillsBiome.arpg`, `GildenLionsmountBiome.arpg`, `GildenMesasBiome.arpg`,
`GildenWaterholesBiome.arpg`

### `assets/biomes/aldermere/greyspine/`

`GreyspineAlpineMeadowsBiome.arpg`, `GreyspineCirquesBiome.arpg`, `GreyspineSaddlesBiome.arpg`,
`GreyspineScreeFieldsBiome.arpg`, `GreyspineSpiresBiome.arpg`

### `assets/biomes/aldermere/hoarfrost/`

`HoarfrostCrevassesBiome.arpg`, `HoarfrostFrostfangPeaksBiome.arpg`, `HoarfrostIceDomesBiome.arpg`,
`HoarfrostMorainesBiome.arpg`

### `assets/biomes/aldermere/northmark/`

`NorthmarkDrumlinsBiome.arpg`, `NorthmarkKettleLakesBiome.arpg`, `NorthmarkMireBiome.arpg`,
`NorthmarkMorainesBiome.arpg`, `NorthmarkTorsBiome.arpg`

### `assets/biomes/aldermere/sunmere/`

`SunmereButtesBiome.arpg`, `SunmereGrovesBiome.arpg`, `SunmereHillsBiome.arpg`, `SunmereScarpsBiome.arpg`

### `assets/biomes/aldermere/valdren/`

`ValdrenBarrowDownsBiome.arpg`, `ValdrenCopseBiome.arpg`, `ValdrenKnollsBiome.arpg`, `ValdrenMeresBiome.arpg`,
`ValdrenTorsBiome.arpg`

### `assets/biomes/aldermere/warden/`

`WardenAlpineMeadowsBiome.arpg`, `WardenCirquesBiome.arpg`, `WardenScreeFieldsBiome.arpg`, `WardenSpiresBiome.arpg`

### `assets/biomes/isles/`

`CinnabarKeysBiome.arpg`, `CorwenBiome.arpg`, `DrownbellIslesBiome.arpg`, `EmberlightArchipelagoBiome.arpg`,
`FrostholmBiome.arpg`, `HrimgardBiome.arpg`, `JotunTeethBiome.arpg`, `LanternIslesBiome.arpg`, `SerrensRestBiome.arpg`,
`ShiverstonesBiome.arpg`, `SkaldhornIsleBiome.arpg`, `VeylBiome.arpg`, `WhisperstonesBiome.arpg`,
`WinterholtGlacierBiome.arpg`, `WinterholtPinesBiome.arpg`, `WinterholtSkerriesBiome.arpg`,
`WyrmtoothSkerriesBiome.arpg`

### `assets/biomes/isles/cinnabar/`

`CinnabarDunesBiome.arpg`, `CinnabarLagoonsBiome.arpg`, `CinnabarMangrovesBiome.arpg`

### `assets/biomes/isles/corwen/`

`CorwenDownsBiome.arpg`, `CorwenLochsBiome.arpg`, `CorwenMireBiome.arpg`, `CorwenTorsBiome.arpg`

### `assets/biomes/isles/drownbell/`

`DrownbellCirquesBiome.arpg`, `DrownbellMireBiome.arpg`, `DrownbellTorsBiome.arpg`

### `assets/biomes/isles/emberlight/`

`EmberlightGrovesBiome.arpg`, `EmberlightHillsBiome.arpg`, `EmberlightLagoonsBiome.arpg`,
`EmberlightTwinPitonsBiome.arpg`

### `assets/biomes/isles/frostholm/`

`FrostholmIceDomesBiome.arpg`, `FrostholmKettleLakesBiome.arpg`, `FrostholmMorainesBiome.arpg`,
`FrostholmTorsBiome.arpg`

### `assets/biomes/isles/hrimgard/`

`HrimgardMorainesBiome.arpg`, `HrimgardPingosBiome.arpg`, `HrimgardTorsBiome.arpg`

### `assets/biomes/isles/jotun/`

`JotunCirquesBiome.arpg`, `JotunSpiresBiome.arpg`

### `assets/biomes/isles/lanternisle/`

`LanternisleCinderConesBiome.arpg`, `LanternisleGorgeBiome.arpg`, `LanternisleHillsBiome.arpg`

### `assets/biomes/isles/serren/`

`SerrenCinderConesBiome.arpg`, `SerrenDownsBiome.arpg`, `SerrenWatchersQuarryBiome.arpg`

### `assets/biomes/isles/shiver/`

`ShiverMireBiome.arpg`, `ShiverRavinesBiome.arpg`, `ShiverTorsBiome.arpg`

### `assets/biomes/isles/skaldhorn/`

`SkaldhornCinderConesBiome.arpg`, `SkaldhornLavaFlowsBiome.arpg`

### `assets/biomes/isles/veyl/`

`VeylHillsBiome.arpg`, `VeylMangrovesBiome.arpg`, `VeylTsingarStoneForestBiome.arpg`

### `assets/biomes/isles/whisperkey/`

`WhisperkeyLagoonsBiome.arpg`, `WhisperkeyMangrovesBiome.arpg`

### `assets/biomes/isles/winterglacier/`

`WinterglacierCrevassesBiome.arpg`, `WinterglacierIceDomesBiome.arpg`, `WinterglacierNunataksBiome.arpg`

### `assets/biomes/isles/winterpine/`

`WinterpineMireBiome.arpg`, `WinterpineMorainesBiome.arpg`, `WinterpineTorsBiome.arpg`

### `assets/biomes/isles/winterskerry/`

`WinterskerryIceDomesBiome.arpg`, `WinterskerryTorsBiome.arpg`

### `assets/biomes/isles/wyrmtooth/`

`WyrmtoothIceDomesBiome.arpg`, `WyrmtoothSpiresBiome.arpg`

### `assets/biomes/lakes/`

`LakeSolenneBiome.arpg`, `MirrormereBiome.arpg`, `SkyfallTarnBiome.arpg`

### `assets/biomes/lakes/mirror/`

`MirrorDeepPoolsBiome.arpg`, `MirrorReedIslesBiome.arpg`, `MirrorShallowsBiome.arpg`

### `assets/biomes/lakes/solenne/`

`SolenneDeepPoolsBiome.arpg`, `SolenneReedIslesBiome.arpg`

### `assets/biomes/lakes/tarn/`

`TarnDeepPoolsBiome.arpg`, `TarnReedfolkIslesBiome.arpg`

### `assets/biomes/seas/`

`BayOfBrineBiome.arpg`, `BlackwaterShallowsBiome.arpg`, `BonewhiteBanksBiome.arpg`, `CinderBayBiome.arpg`,
`DrownedThroneBiome.arpg`, `EmberlightReefBiome.arpg`, `FrostholmSoundBiome.arpg`, `GlasswaterSeaBiome.arpg`,
`GreyfrostShallowsBiome.arpg`, `GreyhallowFirthBiome.arpg`, `GulfOfWhispersBiome.arpg`, `HollowExpanseBiome.arpg`,
`KelpwrackShallowsBiome.arpg`, `KrakensGateBiome.arpg`, `KrakenShoalsBiome.arpg`, `LanternReefsBiome.arpg`,
`MonsoonShoalsBiome.arpg`, `MournwaterFlatsBiome.arpg`, `NethervaultTrenchBiome.arpg`, `PaleFloesBiome.arpg`,
`PearlShallowsBiome.arpg`, `RimeveilBanksBiome.arpg`, `RimeveilSeaBiome.arpg`, `SaltwindSeaBiome.arpg`,
`SargassineCalmBiome.arpg`, `SeaOfLanternsBiome.arpg`, `ShatteredFloesBiome.arpg`, `ShiverstoneShoalsBiome.arpg`,
`StormwrackPassageBiome.arpg`, `StraitOfVeylBiome.arpg`, `SunderingDeepBiome.arpg`, `SunkenSpineBiome.arpg`,
`TidemournShoalsBiome.arpg`, `VeylShoalsBiome.arpg`, `WeedmireShallowsBiome.arpg`, `WeepingBightBiome.arpg`,
`WhisperstoneShoalsBiome.arpg`, `WinterholtChannelBiome.arpg`, `WreckersShoalsBiome.arpg`

### `assets/biomes/seas/bight/`

`BightSandBanksBiome.arpg`, `BightSeamountsBiome.arpg`, `BightSeaStacksBiome.arpg`

### `assets/biomes/seas/blackwater/`

`BlackwaterBasaltIslesBiome.arpg`, `BlackwaterHotSpringsBiome.arpg`, `BlackwaterSeaStacksBiome.arpg`

### `assets/biomes/seas/bonewhite/`

`BonewhiteIcebergsBiome.arpg`, `BonewhiteSandBanksBiome.arpg`, `BonewhiteSkerriesBiome.arpg`

### `assets/biomes/seas/brine/`

`BrineMangroveCaysBiome.arpg`, `BrineMudBanksBiome.arpg`, `BrineSandBanksBiome.arpg`

### `assets/biomes/seas/channel/`

`ChannelIcebergsBiome.arpg`, `ChannelSeamountsBiome.arpg`, `ChannelSkerriesBiome.arpg`

### `assets/biomes/seas/cinder/`

`CinderSeamountsBiome.arpg`, `CinderSurtlingIslesBiome.arpg`, `CinderVentFieldsBiome.arpg`

### `assets/biomes/seas/glasswater/`

`GlasswaterAbyssalPlainBiome.arpg`, `GlasswaterBirdRocksBiome.arpg`, `GlasswaterHighIslesBiome.arpg`,
`GlasswaterSeamountsBiome.arpg`, `GlasswaterSirensRocksBiome.arpg`

### `assets/biomes/seas/greyfrost/`

`GreyfrostIcebergsBiome.arpg`, `GreyfrostKelpForestBiome.arpg`, `GreyfrostSkerriesBiome.arpg`

### `assets/biomes/seas/greyhallow/`

`GreyhallowSeamountsBiome.arpg`, `GreyhallowSeaStacksBiome.arpg`, `GreyhallowSkerriesBiome.arpg`

### `assets/biomes/seas/hollow/`

`HollowAbyssalPlainBiome.arpg`, `HollowIcebergsBiome.arpg`, `HollowSeamountsBiome.arpg`, `HollowTabularBergsBiome.arpg`,
`HollowVolcanicIslesBiome.arpg`

### `assets/biomes/seas/kelpwrack/`

`KelpwrackKelpForestBiome.arpg`, `KelpwrackSeaStacksBiome.arpg`, `KelpwrackSkerriesBiome.arpg`

### `assets/biomes/seas/kraken/`

`KrakenIcebergsBiome.arpg`, `KrakenKrakensTeethBiome.arpg`, `KrakenSeamountsBiome.arpg`, `KrakenVentFieldsBiome.arpg`

### `assets/biomes/seas/lantern/`

`LanternCoralCaysBiome.arpg`, `LanternHighIslesBiome.arpg`, `LanternSeamountsBiome.arpg`, `LanternVentFieldsBiome.arpg`,
`LanternVolcanicIslesBiome.arpg`

### `assets/biomes/seas/lanternreef/`

`LanternreefBlueHoleBiome.arpg`, `LanternreefCoralCaysBiome.arpg`, `LanternreefPalmIslesBiome.arpg`,
`LanternreefReefsBiome.arpg`

### `assets/biomes/seas/monsoon/`

`MonsoonMangroveCaysBiome.arpg`, `MonsoonMudBanksBiome.arpg`, `MonsoonSandBanksBiome.arpg`

### `assets/biomes/seas/mournwater/`

`MournwaterReedIslesBiome.arpg`, `MournwaterSandBanksBiome.arpg`, `MournwaterSkerriesBiome.arpg`

### `assets/biomes/seas/nethervault/`

`NethervaultHadalPitsBiome.arpg`, `NethervaultTrenchWallsBiome.arpg`, `NethervaultVentFieldsBiome.arpg`

### `assets/biomes/seas/pale/`

`PaleIcebergsBiome.arpg`, `PalePressureRidgesBiome.arpg`, `PaleTabularBergsBiome.arpg`

### `assets/biomes/seas/pearl/`

`PearlCoralCaysBiome.arpg`, `PearlReefsBiome.arpg`, `PearlSandBanksBiome.arpg`

### `assets/biomes/seas/reef/`

`ReefBlueEyeBiome.arpg`, `ReefCoralCaysBiome.arpg`, `ReefPalmIslesBiome.arpg`, `ReefReefsBiome.arpg`

### `assets/biomes/seas/rimebank/`

`RimebankIcebergsBiome.arpg`, `RimebankSandBanksBiome.arpg`, `RimebankSkerriesBiome.arpg`

### `assets/biomes/seas/rimeveil/`

`RimeveilAbyssalPlainBiome.arpg`, `RimeveilIcebergsBiome.arpg`, `RimeveilSeamountsBiome.arpg`,
`RimeveilTabularBergsBiome.arpg`

### `assets/biomes/seas/saltwind/`

`SaltwindCoralCaysBiome.arpg`, `SaltwindDragonbloodIsleBiome.arpg`, `SaltwindSandBanksBiome.arpg`,
`SaltwindSeamountsBiome.arpg`

### `assets/biomes/seas/sargassine/`

`SargassineAbyssalPlainBiome.arpg`, `SargassineDriftingIslesBiome.arpg`, `SargassineSeamountsBiome.arpg`,
`SargassineVentFieldsBiome.arpg`

### `assets/biomes/seas/shattered/`

`ShatteredIcebergsBiome.arpg`, `ShatteredPressureRidgesBiome.arpg`

### `assets/biomes/seas/shiverstone/`

`ShiverstoneKelpForestBiome.arpg`, `ShiverstoneSeaStacksBiome.arpg`, `ShiverstoneSkerriesBiome.arpg`

### `assets/biomes/seas/shoals/`

`ShoalsIcebergsBiome.arpg`, `ShoalsKelpForestBiome.arpg`, `ShoalsSkerriesBiome.arpg`

### `assets/biomes/seas/sound/`

`SoundIcebergsBiome.arpg`, `SoundSeamountsBiome.arpg`, `SoundSkerriesBiome.arpg`

### `assets/biomes/seas/spine/`

`SpineRiftValleysBiome.arpg`, `SpineSeamountsBiome.arpg`, `SpineVentFieldsBiome.arpg`, `SpineVolcanicIslesBiome.arpg`

### `assets/biomes/seas/stormwrack/`

`StormwrackAbyssalPlainBiome.arpg`, `StormwrackIcebergsBiome.arpg`, `StormwrackSeaStacksBiome.arpg`

### `assets/biomes/seas/strait/`

`StraitCoralCaysBiome.arpg`, `StraitHighIslesBiome.arpg`, `StraitSeamountsBiome.arpg`

### `assets/biomes/seas/sundering/`

`SunderingAbyssalPlainBiome.arpg`, `SunderingGuyotsBiome.arpg`, `SunderingSeamountsBiome.arpg`,
`SunderingSmokingIslesBiome.arpg`, `SunderingVentFieldsBiome.arpg`

### `assets/biomes/seas/throne/`

`ThroneBrineLakesBiome.arpg`, `ThroneHadalPitsBiome.arpg`

### `assets/biomes/seas/tidemourn/`

`TidemournKelpForestBiome.arpg`, `TidemournSandBanksBiome.arpg`, `TidemournSeaStacksBiome.arpg`,
`TidemournSkerriesBiome.arpg`

### `assets/biomes/seas/veylshoal/`

`VeylshoalCoralCaysBiome.arpg`, `VeylshoalPalmIslesBiome.arpg`, `VeylshoalReefsBiome.arpg`

### `assets/biomes/seas/weedmire/`

`WeedmireReefsBiome.arpg`, `WeedmireSandBanksBiome.arpg`, `WeedmireSandCaysBiome.arpg`

### `assets/biomes/seas/whisper/`

`WhisperBlueHoleBiome.arpg`, `WhisperMangroveCaysBiome.arpg`, `WhisperSandBanksBiome.arpg`, `WhisperSandCaysBiome.arpg`

### `assets/biomes/seas/whisperstone/`

`WhisperstoneMangroveCaysBiome.arpg`, `WhisperstoneSandBanksBiome.arpg`, `WhisperstoneSandCaysBiome.arpg`

### `assets/biomes/seas/wreckers/`

`WreckersKelpForestBiome.arpg`, `WreckersShipbreakersBiome.arpg`, `WreckersSkerriesBiome.arpg`

### `assets/biomes/shores/`

`AmberCoastBiome.arpg`, `BlacksandShoreBiome.arpg`, `BrinewoodMangrovesBiome.arpg`, `DragonwallCliffsBiome.arpg`,
`FjordsOfCairnhollowBiome.arpg`, `FrostShingleBiome.arpg`, `GildenshoreBiome.arpg`, `GreyspinePalisadesBiome.arpg`,
`IcecliffsBiome.arpg`, `MossbackBluffsBiome.arpg`, `MudflatsOfThornwyldBiome.arpg`, `PearlbeachBiome.arpg`,
`RedwallBluffsBiome.arpg`, `SaffronStrandBiome.arpg`, `SunlitStrandBiome.arpg`, `SunmereStrandBiome.arpg`

### `assets/biomes/solmara/`

`AmberleaPlainsBiome.arpg`, `AshenCragsBiome.arpg`, `AurelmarkSteppeBiome.arpg`, `BrinewoodDeltaBiome.arpg`,
`DragonspineBiome.arpg`, `EmberfallCinderfieldsBiome.arpg`, `GlimmerfrostExpanseBiome.arpg`,
`HighthronePlateauBiome.arpg`, `MountAurethysBiome.arpg`, `MountCindermawBiome.arpg`, `NineKingsValeBiome.arpg`,
`RedrockBadlandsBiome.arpg`, `SaffronWastesBiome.arpg`, `SaltglassFlatsBiome.arpg`, `SkyfallValeBiome.arpg`,
`SunscarRidgeBiome.arpg`, `TelAmarinOasesBiome.arpg`, `VarskTaigaBiome.arpg`

### `assets/biomes/solmara/amberlea/`

`AmberleaButtesBiome.arpg`, `AmberleaHillsBiome.arpg`, `AmberleaHollowboughCopseBiome.arpg`,
`AmberleaMonadnockBiome.arpg`, `AmberleaPotholesBiome.arpg`, `AmberleaRavinesBiome.arpg`

### `assets/biomes/solmara/ashen/`

`AshenCinderConesBiome.arpg`, `AshenFumarolesBiome.arpg`, `AshenScreeFieldsBiome.arpg`, `AshenSpiresBiome.arpg`

### `assets/biomes/solmara/aurelmark/`

`AurelmarkDunesBiome.arpg`, `AurelmarkHillsBiome.arpg`, `AurelmarkInselbergBiome.arpg`, `AurelmarkKurgansBiome.arpg`,
`AurelmarkSaltLakesBiome.arpg`

### `assets/biomes/solmara/aurethys/`

`AurethysCirquesBiome.arpg`, `AurethysSpiresBiome.arpg`

### `assets/biomes/solmara/cindermaw/`

`CindermawCraterBiome.arpg`, `CindermawFumarolesBiome.arpg`, `CindermawLavaFlowsBiome.arpg`

### `assets/biomes/solmara/delta/`

`DeltaLagoonsBiome.arpg`, `DeltaLeveesBiome.arpg`, `DeltaMangrovesBiome.arpg`, `DeltaTerracesBiome.arpg`

### `assets/biomes/solmara/dragonspine/`

`DragonspineAlpineMeadowsBiome.arpg`, `DragonspineSaddlesBiome.arpg`, `DragonspineScreeFieldsBiome.arpg`,
`DragonspineSpiresBiome.arpg`, `DragonspineWeepingIcefallBiome.arpg`

### `assets/biomes/solmara/emberfall/`

`EmberfallCinderConesBiome.arpg`, `EmberfallFumarolesBiome.arpg`, `EmberfallGeyserFlatsBiome.arpg`,
`EmberfallLavaFlowsBiome.arpg`

### `assets/biomes/solmara/glimmerfrost/`

`GlimmerfrostCrevassesBiome.arpg`, `GlimmerfrostIceDomesBiome.arpg`, `GlimmerfrostNunataksBiome.arpg`,
`GlimmerfrostPingosBiome.arpg`

### `assets/biomes/solmara/highthrone/`

`HighthroneMonadnockBiome.arpg`, `HighthroneMorainesBiome.arpg`, `HighthronePlayaBiome.arpg`,
`HighthroneRavinesBiome.arpg`

### `assets/biomes/solmara/ninekings/`

`NinekingsKnollsBiome.arpg`, `NinekingsMeresBiome.arpg`, `NinekingsTerracesBiome.arpg`

### `assets/biomes/solmara/redrock/`

`RedrockArchesBiome.arpg`, `RedrockButtesBiome.arpg`, `RedrockGorgeBiome.arpg`, `RedrockHoodooChoirBiome.arpg`,
`RedrockMesasBiome.arpg`

### `assets/biomes/solmara/saffron/`

`SaffronInselbergBiome.arpg`, `SaffronMesasBiome.arpg`, `SaffronOasisPoolsBiome.arpg`, `SaffronPlayaBiome.arpg`,
`SaffronSingingDunesBiome.arpg`, `SaffronStarDunesBiome.arpg`

### `assets/biomes/solmara/saltglass/`

`SaltglassBrinePoolsBiome.arpg`, `SaltglassSaltDomesBiome.arpg`, `SaltglassThornkeepBiome.arpg`

### `assets/biomes/solmara/skyfall/`

`SkyfallKnollsBiome.arpg`, `SkyfallPlayaBiome.arpg`, `SkyfallTorsBiome.arpg`

### `assets/biomes/solmara/sunscar/`

`SunscarButtesBiome.arpg`, `SunscarHoodoosBiome.arpg`, `SunscarMesasBiome.arpg`, `SunscarRavinesBiome.arpg`

### `assets/biomes/solmara/telamarin/`

`TelamarinDunesBiome.arpg`, `TelamarinGrovesBiome.arpg`, `TelamarinOasisPoolsBiome.arpg`

### `assets/biomes/solmara/varsk/`

`VarskHillsBiome.arpg`, `VarskKettleLakesBiome.arpg`, `VarskMireBiome.arpg`, `VarskMorainesBiome.arpg`,
`VarskPingosBiome.arpg`

### `assets/biomes/thornwyld/`

`BleakmarchBiome.arpg`, `CapeMournBiome.arpg`, `FernmarchBiome.arpg`, `GrievingPinesBiome.arpg`,
`HollowEchoesValeBiome.arpg`, `MirrormereFensBiome.arpg`, `MossbackFellsBiome.arpg`, `ThornwyldDeepwoodBiome.arpg`,
`UmbralCanopyBiome.arpg`

### `assets/biomes/thornwyld/bleakmarch/`

`BleakmarchKettleLakesBiome.arpg`, `BleakmarchMesasBiome.arpg`, `BleakmarchMorainesBiome.arpg`,
`BleakmarchPingosBiome.arpg`

### `assets/biomes/thornwyld/deepwyld/`

`DeepwyldCenotesBiome.arpg`, `DeepwyldHillsBiome.arpg`, `DeepwyldKarstTowersBiome.arpg`,
`DeepwyldMoonlitGladesBiome.arpg`, `DeepwyldTepuiOlvashtiBiome.arpg`

### `assets/biomes/thornwyld/echoes/`

`EchoesClearingsBiome.arpg`, `EchoesKnollsBiome.arpg`, `EchoesOxbowsBiome.arpg`

### `assets/biomes/thornwyld/fells/`

`FellsBaldsBiome.arpg`, `FellsCirquesBiome.arpg`, `FellsGorgeBiome.arpg`, `FellsKnobsBiome.arpg`

### `assets/biomes/thornwyld/fens/`

`FensLagoonsBiome.arpg`, `FensLeveesBiome.arpg`, `FensMireBiome.arpg`

### `assets/biomes/thornwyld/fernmarch/`

`FernmarchClearingsBiome.arpg`, `FernmarchKnollsBiome.arpg`, `FernmarchMeresBiome.arpg`, `FernmarchRavinesBiome.arpg`

### `assets/biomes/thornwyld/grieving/`

`GrievingKettleLakesBiome.arpg`, `GrievingMireBiome.arpg`, `GrievingMorainesBiome.arpg`, `GrievingTorsBiome.arpg`

### `assets/biomes/thornwyld/mourn/`

`MournGorgeBiome.arpg`, `MournMireBiome.arpg`, `MournMonadnockBiome.arpg`, `MournTorsBiome.arpg`

### `assets/biomes/thornwyld/umbral/`

`UmbralCloudPeaksBiome.arpg`, `UmbralGorgeBiome.arpg`, `UmbralScarpsBiome.arpg`, `UmbralTepuiBiome.arpg`

### `assets/blocks/`

`TerraArcanaBlocks.arpg`

### `assets/brushes/`

`Basic.arpg`, `Diamond.arpg`, `Soft.arpg`, `SoftSquare.arpg`, `Speckle.arpg`, `Spray.arpg`, `Square.arpg`

### `assets/calendars/`

`TerraArcanaCalendar.arpg`

### `assets/cavebiomes/`

`AmethystGeodes.arpg`, `AridEvaporites.arpg`, `CarbonatiteVaults.arpg`, `ClayBanks.arpg`, `CopperPorphyry.arpg`,
`DolomiteCaverns.arpg`, `DripstoneHalls.arpg`, `EpithermalVeins.arpg`, `GabbroDeeps.arpg`, `GemPockets.arpg`,
`GneissBasement.arpg`, `GraniteFissures.arpg`, `GuanoCaves.arpg`, `GypsumCrystalCaves.arpg`, `IceCaves.arpg`,
`KimberlitePipes.arpg`, `LavaTubes.arpg`, `LimestoneGrottos.arpg`, `MarbleCaves.arpg`, `MossyHollows.arpg`,
`PegmatitePockets.arpg`, `RareEarthPegmatites.arpg`, `SaltCaverns.arpg`, `SandstoneCaves.arpg`, `SchistDeeps.arpg`,
`SeaGrottos.arpg`, `SerpentiniteDeeps.arpg`, `ShaleSeams.arpg`, `SulfideVaults.arpg`, `SulfurGrottos.arpg`

### `assets/clouds/standard/`

`Altocumulus.arpg`, `Altostratus.arpg`, `Cumulonimbus.arpg`, `Cumulus.arpg`, `Nimbus.arpg`, `Sirrus.arpg`,
`Stratocumulus.arpg`, `Stratus.arpg`

### `assets/commands/`

`Camera.arpg`, `Items.arpg`, `Recorder.arpg`, `Teleport.arpg`, `Time.arpg`, `Vehicles.arpg`

### `assets/contents/ores/`

`AgateOre.arpg`, `AlexandriteOre.arpg`, `AmazoniteOre.arpg`, `AmberOre.arpg`, `AmethystOre.arpg`, `AquamarineOre.arpg`,
`ArsenopyriteOre.arpg`, `AzuriteOre.arpg`, `BaryteOre.arpg`, `BastnasiteOre.arpg`, `BauxiteOre.arpg`,
`BenitoiteOre.arpg`, `BerylOre.arpg`, `BismuthiniteOre.arpg`, `BoraxOre.arpg`, `BorniteOre.arpg`, `BraggiteOre.arpg`,
`BromargyriteOre.arpg`, `CalaveriteOre.arpg`, `CarnalliteOre.arpg`, `CarnelianOre.arpg`, `CarnotiteOre.arpg`,
`CelestineOre.arpg`, `ChalcopyriteOre.arpg`, `ChromiteOre.arpg`, `ChrysoberylOre.arpg`, `ChrysocollaOre.arpg`,
`CinnabarOre.arpg`, `CitrineOre.arpg`, `ClausthaliteOre.arpg`, `CoalOre.arpg`, `CobaltOre.arpg`, `ColemaniteOre.arpg`,
`ColumbiteOre.arpg`, `CopperOre.arpg`, `CryoliteOre.arpg`, `DiamondOre.arpg`, `DioptaseOre.arpg`, `EmeraldOre.arpg`,
`FluoriteOre.arpg`, `GalenaOre.arpg`, `GalliteOre.arpg`, `GarnetOre.arpg`, `GermaniteOre.arpg`, `GoldOre.arpg`,
`GraphiteOre.arpg`, `GreenockiteOre.arpg`, `HematiteOre.arpg`, `IlmeniteOre.arpg`, `IoliteOre.arpg`, `IronOre.arpg`,
`JadeOre.arpg`, `KunziteOre.arpg`, `KyaniteOre.arpg`, `LabradoriteOre.arpg`, `LapisOre.arpg`, `LarimarOre.arpg`,
`LauriteOre.arpg`, `LautariteOre.arpg`, `LeadOre.arpg`, `LepidoliteOre.arpg`, `LimoniteOre.arpg`, `LoranditeOre.arpg`,
`MagnesiteOre.arpg`, `MagnetiteOre.arpg`, `MalachiteOre.arpg`, `ManganeseOre.arpg`, `MicaOre.arpg`,
`MolybdeniteOre.arpg`, `MonaziteOre.arpg`, `MoonstoneOre.arpg`, `MorganiteOre.arpg`, `NickelOre.arpg`,
`NitratineOre.arpg`, `OnyxOre.arpg`, `OpalOre.arpg`, `OrpimentOre.arpg`, `OsmiridiumOre.arpg`, `PeridotOre.arpg`,
`PhosphateOre.arpg`, `PlatinumOre.arpg`, `PolluciteOre.arpg`, `PyriteOre.arpg`, `PyrochloreOre.arpg`, `QuartzOre.arpg`,
`RealgarOre.arpg`, `RhodochrositeOre.arpg`, `RhodoniteOre.arpg`, `RoquesiteOre.arpg`, `RoseQuartzOre.arpg`,
`RubyOre.arpg`, `RutileOre.arpg`, `SaltpeterOre.arpg`, `SapphireOre.arpg`, `SideriteOre.arpg`, `SilverOre.arpg`,
`SmokyQuartzOre.arpg`, `SodaliteOre.arpg`, `SperryliteOre.arpg`, `SphaleriteOre.arpg`, `SpinelOre.arpg`,
`SpodumeneOre.arpg`, `StibniteOre.arpg`, `SulfurOre.arpg`, `SunstoneOre.arpg`, `SylviteOre.arpg`, `TantaliteOre.arpg`,
`TanzaniteOre.arpg`, `ThorianiteOre.arpg`, `ThortveititeOre.arpg`, `TigerEyeOre.arpg`, `TinOre.arpg`,
`TitaniumOre.arpg`, `TopazOre.arpg`, `TourmalineOre.arpg`, `TronaOre.arpg`, `TungstenOre.arpg`, `TurquoiseOre.arpg`,
`UraniniteOre.arpg`, `VanadiniteOre.arpg`, `WollastoniteOre.arpg`, `XenotimeOre.arpg`, `ZincOre.arpg`, `ZirconOre.arpg`

### `assets/contents/rocks/`

`BentoniteLenses.arpg`, `FlintNodules.arpg`, `GypsumRoses.arpg`, `Jet.arpg`, `ObsidianShards.arpg`,
`OilShaleSeams.arpg`, `Pebbles.arpg`, `Stones.arpg`

### `assets/contents/roots/`

`Rootlets.arpg`, `Roots.arpg`

### `assets/coverings/grass/`

`AlpineGrass.arpg`, `AshenGrass.arpg`, `Bramble.arpg`, `CloverGrass.arpg`, `DryGrass.arpg`, `DuneGrass.arpg`,
`Fern.arpg`, `GoldenGrass.arpg`, `Heather.arpg`, `JungleGrass.arpg`, `MarshGrass.arpg`, `MeadowGrass.arpg`,
`MoonlitGrass.arpg`, `ReedBed.arpg`, `SaltmarshGrass.arpg`, `SavannaGrass.arpg`, `Seagrass.arpg`, `SteppeGrass.arpg`,
`TundraGrass.arpg`, `WildflowerMeadow.arpg`

### `assets/coverings/litter/`

`BlushwoodPetals.arpg`, `ForestLitter.arpg`, `MangroveLitter.arpg`, `PineLitter.arpg`

### `assets/coverings/moss/`

`CushionMoss.arpg`, `GreenMoss.arpg`, `Lichen.arpg`, `ShadowMoss.arpg`

### `assets/damagetypes/`

`DamageTypes.arpg`

### `assets/deeds/`

`Deeds.arpg`

### `assets/entities/`

`HumanoidEntity.arpg`

### `assets/features/humanoid/brows/`

`AngryBrows.arpg`, `ArchedBrows.arpg`, `BushyBrows.arpg`, `RaisedBrows.arpg`, `ThickBrows.arpg`, `ThinBrows.arpg`,
`Unibrow.arpg`

### `assets/features/humanoid/eyes/`

`AlmondEyes.arpg`, `BrightEyes.arpg`, `ClosedEyes.arpg`, `DeterminedEyes.arpg`, `NarrowEyes.arpg`, `RoundEyes.arpg`,
`SleepyEyes.arpg`, `WideEyes.arpg`

### `assets/features/humanoid/hair/`

`BobHair.arpg`, `BunHair.arpg`, `BuzzHair.arpg`, `LongHair.arpg`, `MohawkHair.arpg`, `PonytailHair.arpg`,
`ShortHair.arpg`, `SpikyHair.arpg`, `SweptHair.arpg`

### `assets/features/humanoid/heads/`

`BroadHead.arpg`, `HeartHead.arpg`, `OvalHead.arpg`, `RoundHead.arpg`, `SquareHead.arpg`, `TaperedHead.arpg`

### `assets/features/humanoid/mouths/`

`FlatMouth.arpg`, `FrownMouth.arpg`, `GrinMouth.arpg`, `OpenMouth.arpg`, `PoutMouth.arpg`, `SmileMouth.arpg`,
`SmirkMouth.arpg`, `WideSmileMouth.arpg`

### `assets/features/humanoid/noses/`

`BroadNose.arpg`, `ButtonNose.arpg`, `HookedNose.arpg`, `PointedNose.arpg`, `SnubNose.arpg`, `StraightNose.arpg`

### `assets/furnishings/farm/`

`Stores.arpg`

### `assets/furnishings/house/`

`Bed.arpg`, `Bench.arpg`, `Chair.arpg`, `Door.arpg`, `Hearth.arpg`, `Light.arpg`, `Rug.arpg`, `Shelf.arpg`,
`SmallTable.arpg`, `Storage.arpg`, `Table.arpg`

### `assets/furnishings/keep/`

`Bed.arpg`, `Seat.arpg`, `Table.arpg`

### `assets/furnishings/ship/`

`Bedding.arpg`, `Bench.arpg`, `Berth.arpg`, `Bookshelf.arpg`, `CabinChair.arpg`, `Cannon.arpg`, `CaptainsBed.arpg`,
`CaptainsChair.arpg`, `Chest.arpg`, `Door.arpg`, `GreatTable.arpg`, `Lamp.arpg`, `MessTable.arpg`, `Rug.arpg`,
`Sideboard.arpg`, `Stores.arpg`, `Stove.arpg`, `TableLight.arpg`, `WritingDesk.arpg`

### `assets/furnishings/tavern/`

`Bed.arpg`, `Bench.arpg`, `Casks.arpg`, `LongTable.arpg`

### `assets/genders/`

`Genders.arpg`

### `assets/items/armor/`

`Armor.arpg`

### `assets/items/clothing/`

`Clothing.arpg`

### `assets/items/consumables/`

`Consumables.arpg`

### `assets/items/containers/`

`Containers.arpg`

### `assets/items/flora/`

`Coverings.arpg`, `Seeds.arpg`

### `assets/items/furniture/`

`Furniture.arpg`

### `assets/items/jewelry/`

`Jewelry.arpg`

### `assets/items/materials/`

`Materials.arpg`

### `assets/items/misc/`

`Misc.arpg`

### `assets/items/timber/`

`Logs.arpg`

### `assets/items/tools/`

`Tools.arpg`

### `assets/items/weapons/`

`Cannons.arpg`, `Weapons.arpg`

### `assets/loottables/containers/`

`HouseChest.arpg`, `KeepChest.arpg`, `MinersCache.arpg`, `ShipStores.arpg`, `SmugglersStash.arpg`, `TavernChest.arpg`

### `assets/loottables/gear/`

`GuardArms.arpg`, `GuardOutfit.arpg`, `GuardShield.arpg`, `MinerOutfit.arpg`, `SmugglerOutfit.arpg`,
`VillagerOutfit.arpg`

### `assets/loottables/npcs/`

`Guard.arpg`, `Miner.arpg`, `Smuggler.arpg`, `Villager.arpg`

### `assets/materials/characters/`

`StandardCharacterMaterial.arpg`

### `assets/materials/debug/`

`DebugCameraOrientationBaseMaterial.arpg`, `DebugCameraOrientationHUDMaterial.arpg`, `DebugSurfaceMaterial.arpg`,
`DebugSurfaceUVMaterial.arpg`, `DebugUVMaterial.arpg`

### `assets/materials/editor/`

`CharacterMeshOverlayMaterial.arpg`, `CharacterViewCharacterMaterial.arpg`, `CharacterViewGridMaterial.arpg`,
`CharacterViewMarkerMaterial.arpg`, `CharacterViewMarkerSelectedMaterial.arpg`, `ItemEditorCursorEraseMaterial.arpg`,
`ItemEditorCursorPaintMaterial.arpg`, `ItemEditorCursorPlaceMaterial.arpg`, `ItemEditorGridMaterial.arpg`,
`ItemEditorModelMaterial.arpg`, `PlacementIconMaterial.arpg`, `TextureViewerAtlasMaterial.arpg`,
`WorldEditorBrushMaterial.arpg`, `WorldEditorHeightMaterial.arpg`, `WorldEditorImageMaterial.arpg`,
`WorldMapMarkerMaterial.arpg`, `WorldMapTileMaterial.arpg`

### `assets/materials/fonts/`

`StandardFontMaterial.arpg`

### `assets/materials/items/`

`EquipmentItemMaterial.arpg`, `InventoryItemMaterial.arpg`, `InventoryVehicleMaterial.arpg`,
`StandardItemMaterial.arpg`, `TierFrameMaterial.arpg`, `VehicleItemMaterial.arpg`

### `assets/materials/liquid/`

`MacroWaterMaterial.arpg`, `UnderwaterMaterial.arpg`, `WaterMaterial.arpg`

### `assets/materials/postprocessing/`

`LightingMaterial.arpg`, `PostBlurMaterial.arpg`, `PostCompositeMaterial.arpg`, `PostPrefilterMaterial.arpg`,
`SSAOMaterial.arpg`

### `assets/materials/shipwright/`

`ShipwrightPaperMaterial.arpg`

### `assets/materials/sky/`

`SquintMaterial.arpg`, `StandardSkyMaterial.arpg`

### `assets/materials/sprites/`

`StandardSpriteMaterial.arpg`

### `assets/materials/surface/`

`MacroTerrainMaterial.arpg`, `StandardSurfaceMaterial.arpg`

### `assets/materials/trees/`

`TreeBarkFallingMaterial.arpg`, `TreeBarkMaterial.arpg`, `TreeLeafFallingMaterial.arpg`, `TreeLeafMaterial.arpg`

### `assets/materials/util/`

`BlitMaterial.arpg`

### `assets/materials/vitality/`

`VitalityMaterial.arpg`

### `assets/materials/weather/`

`PrecipitationMaterial.arpg`, `WeatherMaterial.arpg`

### `assets/menus/`

`Character.arpg`, `CharacterCreator.arpg`, `HUD.arpg`, `Inventory.arpg`, `Launcher.arpg`, `MainMenu.arpg`,
`PauseMenu.arpg`, `Settings.arpg`, `Shipwright.arpg`

### `assets/menus/editor/`

`AnimationEditor.arpg`, `AnimationTreeEditor.arpg`, `ArpgViewer.arpg`, `BiomePlanner.arpg`, `BiomeViewer.arpg`,
`Brushes.arpg`, `CameraPlanner.arpg`, `CharacterEditor.arpg`, `CommandConsole.arpg`, `Console.arpg`,
`EditorWindow.arpg`, `InfoPanel.arpg`, `ItemEditor.arpg`, `Palette.arpg`, `Profiler.arpg`, `Recorder.arpg`,
`SettlementPlanner.arpg`, `SoundLibrary.arpg`, `TabFrame.arpg`, `TextureViewer.arpg`, `Tools.arpg`, `WorldEditor.arpg`,
`WorldMap.arpg`

### `assets/menus/util/`

`Containers.arpg`, `GenericButtons.arpg`, `Hierarchy.arpg`, `MenuToolBar.arpg`

### `assets/mesh/characters/humanoid/`

`HumanoidBodyMesh.arpg`

### `assets/mesh/characters/humanoid/hair/`

`BobHairMesh.arpg`, `BunHairMesh.arpg`, `BuzzHairMesh.arpg`, `LongHairMesh.arpg`, `MohawkHairMesh.arpg`,
`PonytailHairMesh.arpg`, `ShortHairMesh.arpg`, `SpikyHairMesh.arpg`, `SweptHairMesh.arpg`

### `assets/mesh/characters/humanoid/heads/`

`BroadHeadMesh.arpg`, `HeartHeadMesh.arpg`, `OvalHeadMesh.arpg`, `RoundHeadMesh.arpg`, `SquareHeadMesh.arpg`,
`TaperedHeadMesh.arpg`

### `assets/mesh/characters/humanoid/noses/`

`BroadNoseMesh.arpg`, `ButtonNoseMesh.arpg`, `HookedNoseMesh.arpg`, `PointedNoseMesh.arpg`, `SnubNoseMesh.arpg`,
`StraightNoseMesh.arpg`

### `assets/mesh/editor/`

`CharacterMeshOverlayVAO.arpg`, `CharacterViewGrid.arpg`, `CharacterViewMarker.arpg`, `EditorViewportQuad.arpg`,
`ItemEditorCursor.arpg`, `ItemEditorGrid.arpg`

### `assets/mesh/fonts/`

`FontMesh.arpg`

### `assets/mesh/items/armor/`

`IronBreastplate.arpg`, `IronGauntlet.arpg`, `IronGreave.arpg`, `IronHelm.arpg`, `IronPauldron.arpg`,
`LeatherBelt.arpg`, `LeatherBoot.arpg`, `LeatherBracer.arpg`

### `assets/mesh/items/clothing/`

`CrimsonCloak.arpg`, `LinenPants.arpg`, `LinenShirt.arpg`

### `assets/mesh/items/consumables/`

`Apple.arpg`, `BreadLoaf.arpg`, `HealthPotion.arpg`, `WaterFlask.arpg`

### `assets/mesh/items/containers/`

`CanvasRucksack.arpg`, `CoinPouch.arpg`, `ExpeditionPack.arpg`, `LeatherSatchel.arpg`, `OakChest.arpg`,
`TravelersPack.arpg`

### `assets/mesh/items/flora/`

`AcaciaPod.arpg`, `Acorn.arpg`, `AzureSeed.arpg`, `BaobabSeed.arpg`, `Beechnut.arpg`, `BirchSeed.arpg`,
`BlushSeed.arpg`, `CherryPit.arpg`, `Coconut.arpg`, `CypressCone.arpg`, `DateStone.arpg`, `ElvenAcorn.arpg`,
`EmberSeed.arpg`, `Hazelnut.arpg`, `JuniperBerry.arpg`, `MangrovePropagule.arpg`, `MapleSamara.arpg`,
`MoisturePouch.arpg`, `MoonSeed.arpg`, `PineCone.arpg`, `PoplarSeed.arpg`, `RedwoodCone.arpg`, `SeedPouch.arpg`,
`SporePouch.arpg`, `SpruceCone.arpg`, `WillowCatkin.arpg`

### `assets/mesh/items/furniture/`

`Barrel.arpg`, `Bedroll.arpg`, `Bench.arpg`, `Berth.arpg`, `Bookshelf.arpg`, `Candlestick.arpg`, `CaptainsBed.arpg`,
`CaptainsChair.arpg`, `CargoCrate.arpg`, `DraftingTable.arpg`, `GalleyStove.arpg`, `GreatTable.arpg`, `Lantern.arpg`,
`LongTable.arpg`, `OakChair.arpg`, `OakDoor.arpg`, `OakDoorOpen.arpg`, `Rug.arpg`, `Sideboard.arpg`, `Stool.arpg`,
`WritingDesk.arpg`

### `assets/mesh/items/jewelry/`

`GoldRing.arpg`, `RubyRing.arpg`, `SapphireRing.arpg`, `SilverRing.arpg`

### `assets/mesh/items/materials/`

`GoldIngot.arpg`, `Gunpowder.arpg`, `IronIngot.arpg`, `LeatherHide.arpg`, `OakPlank.arpg`, `Ruby.arpg`, `Sapphire.arpg`,
`SilverIngot.arpg`

### `assets/mesh/items/misc/`

`GoldCoin.arpg`, `Lockpick.arpg`, `OldKey.arpg`, `RolledMap.arpg`, `SilverCoin.arpg`, `Torch.arpg`

### `assets/mesh/items/timber/`

`AcaciaGreatLog.arpg`, `AcaciaLargeLog.arpg`, `AcaciaLog.arpg`, `AcaciaSmallLog.arpg`, `AzurewoodGreatLog.arpg`,
`AzurewoodLargeLog.arpg`, `AzurewoodLog.arpg`, `AzurewoodSmallLog.arpg`, `BaobabGreatLog.arpg`, `BaobabLargeLog.arpg`,
`BaobabLog.arpg`, `BaobabSmallLog.arpg`, `BeechGreatLog.arpg`, `BeechLargeLog.arpg`, `BeechLog.arpg`,
`BeechSmallLog.arpg`, `BirchGreatLog.arpg`, `BirchLargeLog.arpg`, `BirchLog.arpg`, `BirchSmallLog.arpg`,
`BlushwoodGreatLog.arpg`, `BlushwoodLargeLog.arpg`, `BlushwoodLog.arpg`, `BlushwoodSmallLog.arpg`,
`CherryGreatLog.arpg`, `CherryLargeLog.arpg`, `CherryLog.arpg`, `CherrySmallLog.arpg`, `CypressGreatLog.arpg`,
`CypressLargeLog.arpg`, `CypressLog.arpg`, `CypressSmallLog.arpg`, `ElvenheartGreatLog.arpg`, `ElvenheartLargeLog.arpg`,
`ElvenheartLog.arpg`, `ElvenheartSmallLog.arpg`, `EmberbarkGreatLog.arpg`, `EmberbarkLargeLog.arpg`,
`EmberbarkLog.arpg`, `EmberbarkSmallLog.arpg`, `HazelLog.arpg`, `HazelSmallLog.arpg`, `JuniperLog.arpg`,
`JuniperSmallLog.arpg`, `MangroveGreatLog.arpg`, `MangroveLargeLog.arpg`, `MangroveLog.arpg`, `MangroveSmallLog.arpg`,
`MapleGreatLog.arpg`, `MapleLargeLog.arpg`, `MapleLog.arpg`, `MapleSmallLog.arpg`, `MoonsilverGreatLog.arpg`,
`MoonsilverLargeLog.arpg`, `MoonsilverLog.arpg`, `MoonsilverSmallLog.arpg`, `OakGreatLog.arpg`, `OakLargeLog.arpg`,
`OakLog.arpg`, `OakSmallLog.arpg`, `PalmGreatLog.arpg`, `PalmLargeLog.arpg`, `PalmLog.arpg`, `PalmSmallLog.arpg`,
`PineGreatLog.arpg`, `PineLargeLog.arpg`, `PineLog.arpg`, `PineSmallLog.arpg`, `PoplarGreatLog.arpg`,
`PoplarLargeLog.arpg`, `PoplarLog.arpg`, `PoplarSmallLog.arpg`, `RedwoodGreatLog.arpg`, `RedwoodLargeLog.arpg`,
`RedwoodLog.arpg`, `RedwoodSmallLog.arpg`, `SpruceGreatLog.arpg`, `SpruceLargeLog.arpg`, `SpruceLog.arpg`,
`SpruceSmallLog.arpg`, `WillowGreatLog.arpg`, `WillowLargeLog.arpg`, `WillowLog.arpg`, `WillowSmallLog.arpg`

### `assets/mesh/items/tools/`

`Axe.arpg`, `Pickaxe.arpg`, `Shovel.arpg`

### `assets/mesh/items/weapons/`

`BattleAxe.arpg`, `Cannonball.arpg`, `IronCannon.arpg`, `IronDagger.arpg`, `IronSword.arpg`, `OakShield.arpg`,
`OakStaff.arpg`

### `assets/mesh/shipwright/`

`ShipwrightQuad.arpg`

### `assets/mesh/sprites/`

`SpriteMesh.arpg`

### `assets/mesh/util/`

`BlitMesh.arpg`, `DockChromeQuad.arpg`, `LensPass.arpg`, `PlanarPass.arpg`

### `assets/mesh/util/vao/`

`ChunkVAO.arpg`, `DockChromeVAO.arpg`, `ItemVAO.arpg`, `LabelVAO.arpg`, `MacroVAO.arpg`, `MacroWaterVAO.arpg`,
`QuadVAO.arpg`

### `assets/npcs/`

`Guard.arpg`, `Miner.arpg`, `Smuggler.arpg`, `Villager.arpg`

### `assets/processingpasses/`

`DebugCameraOrientationBase.arpg`, `DebugCameraOrientationHUD.arpg`, `DebugUV.arpg`, `Lighting.arpg`, `PostBlur.arpg`,
`PostComposite.arpg`, `PostPrefilter.arpg`, `Precipitation.arpg`, `Sky.arpg`, `Squint.arpg`, `SSAO.arpg`,
`Underwater.arpg`, `Vitality.arpg`, `Weather.arpg`

### `assets/professions/`

`Professions.arpg`

### `assets/rigs/`

`HumanoidRig.arpg`

### `assets/roads/`

`CobbleRoad.arpg`, `CobbleStreet.arpg`, `DirtPath.arpg`, `FlagstoneStreet.arpg`

### `assets/schemas/`

`Animations.arpg`, `AnimationTrees.arpg`, `Architectures.arpg`, `Attributes.arpg`, `Behaviors.arpg`, `Biomes.arpg`,
`Blocks.arpg`, `Brushes.arpg`, `Calendars.arpg`, `CaveBiomes.arpg`, `Clouds.arpg`, `Contents.arpg`, `Coverings.arpg`,
`DamageTypes.arpg`, `Deeds.arpg`, `Entities.arpg`, `FBOs.arpg`, `Features.arpg`, `Furnishings.arpg`, `Genders.arpg`,
`Items.arpg`, `LootTables.arpg`, `Materials.arpg`, `Npcs.arpg`, `Passes.arpg`, `Professions.arpg`, `Rigs.arpg`,
`Roads.arpg`, `Seasons.arpg`, `Settlements.arpg`, `Shaders.arpg`, `Ships.arpg`, `Shortcuts.arpg`, `Skills.arpg`,
`SoundCues.arpg`, `Sounds.arpg`, `Sprites.arpg`, `Stats.arpg`, `StructureLists.arpg`, `Structures.arpg`,
`TextureAliases.arpg`, `Threads.arpg`, `Tiers.arpg`, `Tools.arpg`, `Trades.arpg`, `Trees.arpg`, `UBOs.arpg`,
`Vehicles.arpg`, `Weapons.arpg`, `Weathers.arpg`, `Worlds.arpg`

### `assets/seasons/`

`Autumn.arpg`, `Spring.arpg`, `Summer.arpg`, `Winter.arpg`

### `assets/settlements/`

`Hamlet.arpg`, `Town.arpg`, `Village.arpg`

### `assets/shaders/characters/standard/`

`StandardcharacterShader.arpg`

### `assets/shaders/debug/`

`DebugCameraOrientationBaseShader.arpg`, `DebugCameraOrientationHUDShader.arpg`, `DebugSurfaceShader.arpg`,
`DebugSurfaceUVShader.arpg`, `DebugUVShader.arpg`

### `assets/shaders/editor/charactereditor/`

`CharacterMeshOverlayShader.arpg`

### `assets/shaders/editor/characterview/`

`CharacterViewCharacterShader.arpg`, `CharacterViewMarkerShader.arpg`

### `assets/shaders/editor/itemeditor/`

`ItemEditorCursorShader.arpg`, `ItemEditorGridShader.arpg`, `ItemEditorModelShader.arpg`

### `assets/shaders/editor/placementicon/`

`PlacementIconShader.arpg`

### `assets/shaders/editor/textureviewer/`

`TextureViewerAtlasShader.arpg`

### `assets/shaders/editor/worldeditor/`

`WorldEditorBrushShader.arpg`, `WorldEditorHeightShader.arpg`

### `assets/shaders/editor/worldmap/`

`WorldMapMarkerShader.arpg`, `WorldMapTileShader.arpg`

### `assets/shaders/fonts/standard/`

`StandardFontShader.arpg`

### `assets/shaders/items/equipment/`

`EquipmentItemShader.arpg`

### `assets/shaders/items/inventory/`

`InventoryItemShader.arpg`, `InventoryVehicleShader.arpg`

### `assets/shaders/items/standard/`

`StandardItemShader.arpg`

### `assets/shaders/items/tierframe/`

`TierFrameShader.arpg`

### `assets/shaders/items/vehicle/`

`VehicleItemShader.arpg`

### `assets/shaders/liquid/`

`MacroWaterShader.arpg`, `UnderwaterShader.arpg`, `WaterShader.arpg`

### `assets/shaders/postprocessing/effects/`

`PostBlurShader.arpg`, `PostCompositeShader.arpg`, `PostPrefilterShader.arpg`

### `assets/shaders/postprocessing/lighting/`

`LightingShader.arpg`

### `assets/shaders/postprocessing/ssao/`

`SSAOShader.arpg`

### `assets/shaders/shipwright/`

`ShipwrightPaperShader.arpg`

### `assets/shaders/sky/squint/`

`SquintShader.arpg`

### `assets/shaders/sky/standard/`

`StandardSkyShader.arpg`

### `assets/shaders/sprites/standard/`

`StandardSpriteShader.arpg`

### `assets/shaders/surface/macro/`

`MacroTerrainShader.arpg`

### `assets/shaders/surface/standard/`

`StandardSurfaceShader.arpg`

### `assets/shaders/trees/bark/`

`TreeBarkFallingShader.arpg`, `TreeBarkShader.arpg`

### `assets/shaders/trees/leaf/`

`TreeLeafFallingShader.arpg`, `TreeLeafShader.arpg`

### `assets/shaders/util/`

`BlitShader.arpg`

### `assets/shaders/vitality/`

`VitalityShader.arpg`

### `assets/shaders/weather/precipitation/`

`PrecipitationShader.arpg`

### `assets/shaders/weather/standard/`

`WeatherShader.arpg`

### `assets/ships/`

`Sloop.arpg`

### `assets/shortcuts/`

`AnimationEditor.arpg`, `AnimationTreeEditor.arpg`, `CharacterEditor.arpg`, `Hierarchy.arpg`, `InfoPanel.arpg`,
`ItemEditor.arpg`, `Profiler.arpg`, `Recorder.arpg`, `SettlementPlanner.arpg`, `Tools.arpg`, `WorldEditor.arpg`

### `assets/skills/`

`Skills.arpg`

### `assets/soundcues/ambience/`

`Birds.arpg`, `Crickets.arpg`, `Gust.arpg`, `Ocean.arpg`, `Rain.arpg`, `RainHeavy.arpg`, `Shore.arpg`, `Thunder.arpg`,
`TreesCalm.arpg`, `TreesStormy.arpg`, `TreesWindy.arpg`, `Underwater.arpg`, `Wind.arpg`, `WindStrong.arpg`

### `assets/soundcues/combat/`

`Defeat.arpg`, `Guard.arpg`, `Hit.arpg`, `ProjectileBounce.arpg`, `Swing.arpg`, `Throw.arpg`, `Turned.arpg`

### `assets/soundcues/furnishings/`

`ChestClose.arpg`, `ChestOpen.arpg`, `DoorClose.arpg`, `DoorOpen.arpg`

### `assets/soundcues/interface/`

`Click.arpg`, `Close.arpg`, `Open.arpg`

### `assets/soundcues/items/`

`BagClose.arpg`, `BagOpen.arpg`, `Equip.arpg`, `PickUp.arpg`, `SetDown.arpg`, `Stow.arpg`

### `assets/soundcues/movement/`

`Climb.arpg`, `Jump.arpg`, `Land.arpg`, `LandHard.arpg`, `Splash.arpg`, `SplashBig.arpg`, `Swim.arpg`, `Wade.arpg`

### `assets/soundcues/music/`

`Day.arpg`, `MainMenu.arpg`, `Night.arpg`

### `assets/soundcues/progression/`

`LevelUp.arpg`, `Profession.arpg`, `Skill.arpg`

### `assets/soundcues/ships/`

`Anchor.arpg`, `Door.arpg`, `Ground.arpg`, `Helm.arpg`, `Hull.arpg`, `Launch.arpg`, `Rigging.arpg`, `SailHoist.arpg`,
`Sails.arpg`, `SailSet.arpg`, `Wake.arpg`

### `assets/soundcues/shipwright/`

`Place.arpg`, `Refused.arpg`

### `assets/soundcues/surfaces/dirt/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/glass/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/grass/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/gravel/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/ice/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/leaves/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/sand/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/stone/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/surfaces/wood/`

`Break.arpg`, `Dig.arpg`, `Place.arpg`, `Step.arpg`

### `assets/soundcues/trees/`

`Chop.arpg`, `Fall.arpg`, `Land.arpg`

### `assets/soundcues/vitality/`

`Defeat.arpg`, `Heartbeat.arpg`

### `assets/soundcues/weapons/`

`CannonFire.arpg`, `CannonFizzle.arpg`, `CannonLoad.arpg`, `CannonPrime.arpg`

### `assets/sounds/ambience/`

`Birds.arpg`, `Crickets.arpg`, `Gust.arpg`, `Ocean.arpg`

### `assets/sounds/ambience/trees/`

`AspenGusts.arpg`, `CherryStrong.arpg`, `OakCalm.arpg`

### `assets/sounds/ambience/water/`

`Shore.arpg`

### `assets/sounds/blocks/dirt/`

`Break_A.arpg`, `Break_B.arpg`, `Break_C.arpg`, `Dig_A.arpg`, `Dig_B.arpg`, `Dig_C.arpg`, `Place_A.arpg`,
`Place_B.arpg`, `Place_C.arpg`

### `assets/sounds/blocks/glass/`

`Break_A.arpg`, `Break_B.arpg`, `Break_C.arpg`, `Dig_A.arpg`, `Dig_B.arpg`, `Dig_C.arpg`, `Place_A.arpg`,
`Place_B.arpg`, `Place_C.arpg`

### `assets/sounds/blocks/leaves/`

`Break_A.arpg`, `Break_B.arpg`, `Break_C.arpg`, `Dig_A.arpg`, `Dig_B.arpg`, `Dig_C.arpg`, `Place_A.arpg`,
`Place_B.arpg`, `Place_C.arpg`

### `assets/sounds/blocks/stone/`

`Break_A.arpg`, `Break_B.arpg`, `Break_C.arpg`, `Dig_A.arpg`, `Dig_B.arpg`, `Dig_C.arpg`, `Place_A.arpg`,
`Place_B.arpg`, `Place_C.arpg`

### `assets/sounds/blocks/wood/`

`Break_A.arpg`, `Break_B.arpg`, `Break_C.arpg`, `Dig_A.arpg`, `Dig_B.arpg`, `Dig_C.arpg`, `Place_A.arpg`,
`Place_B.arpg`, `Place_C.arpg`

### `assets/sounds/combat/`

`Death.arpg`, `HitFlesh_A.arpg`, `HitFlesh_B.arpg`, `HitFlesh_C.arpg`, `HitWood_A.arpg`, `HitWood_B.arpg`,
`HitWood_C.arpg`, `Swing_A.arpg`, `Swing_B.arpg`, `Swing_C.arpg`, `SwingHeavy_A.arpg`, `SwingHeavy_B.arpg`,
`SwingHeavy_C.arpg`, `Throw_A.arpg`, `Throw_B.arpg`, `Throw_C.arpg`

### `assets/sounds/combat/metal/`

`BarrelHit.arpg`, `IronHit.arpg`, `SpadeHit.arpg`

### `assets/sounds/footsteps/dirt/`

`Dirt_A.arpg`, `Dirt_B.arpg`, `Dirt_C.arpg`, `Dirt_D.arpg`

### `assets/sounds/footsteps/grass/`

`Grass_A.arpg`, `Grass_B.arpg`, `Grass_C.arpg`, `Grass_D.arpg`

### `assets/sounds/footsteps/gravel/`

`Gravel_A.arpg`, `Gravel_B.arpg`, `Gravel_C.arpg`, `Gravel_D.arpg`

### `assets/sounds/footsteps/leaves/`

`Leaves_A.arpg`, `Leaves_B.arpg`, `Leaves_C.arpg`, `Leaves_D.arpg`

### `assets/sounds/footsteps/sand/`

`Sand_A.arpg`, `Sand_B.arpg`, `Sand_C.arpg`, `Sand_D.arpg`

### `assets/sounds/footsteps/stone/`

`Stone_A.arpg`, `Stone_B.arpg`, `Stone_C.arpg`, `Stone_D.arpg`

### `assets/sounds/footsteps/water/`

`Water_A.arpg`, `Water_B.arpg`, `Water_C.arpg`, `Water_D.arpg`

### `assets/sounds/footsteps/wood/`

`Wood_A.arpg`, `Wood_B.arpg`, `Wood_C.arpg`, `Wood_D.arpg`

### `assets/sounds/furnishings/`

`ChestClose.arpg`, `ChestOpen.arpg`, `DoorCreak.arpg`, `DoorSlam.arpg`

### `assets/sounds/interface/`

`Click.arpg`, `Close.arpg`, `Error.arpg`, `WindowOpen.arpg`

### `assets/sounds/items/`

`BagClose.arpg`, `BagOpen.arpg`, `Drop_A.arpg`, `Drop_B.arpg`, `Equip_A.arpg`, `Equip_B.arpg`, `EquipMetal_A.arpg`,
`EquipMetal_B.arpg`, `Pickup_A.arpg`, `Pickup_B.arpg`

### `assets/sounds/movement/`

`Jump_A.arpg`, `Jump_B.arpg`, `Land_A.arpg`, `Land_B.arpg`, `LandHard_A.arpg`, `LandHard_B.arpg`

### `assets/sounds/music/`

`Day.arpg`, `MainMenu.arpg`, `Night.arpg`

### `assets/sounds/progression/`

`LevelUp.arpg`, `Profession.arpg`, `Skill.arpg`

### `assets/sounds/trees/`

`Chop_A.arpg`, `Chop_B.arpg`, `Chop_C.arpg`, `Fall.arpg`, `Land.arpg`

### `assets/sounds/vehicles/ship/`

`Anchor.arpg`, `Hoist.arpg`, `HullCreak.arpg`, `HullImpact_A.arpg`, `HullImpact_B.arpg`, `RopeCreak.arpg`,
`Rudder_A.arpg`, `Rudder_B.arpg`, `SailFill.arpg`, `SailLuff.arpg`, `WaveImpacts.arpg`

### `assets/sounds/vitality/`

`Defeat.arpg`, `Heartbeat.arpg`

### `assets/sounds/water/`

`Splash_A.arpg`, `Splash_B.arpg`, `Splash_C.arpg`, `SplashBig_A.arpg`, `SplashBig_B.arpg`, `SplashBig_C.arpg`,
`SwimStroke_A.arpg`, `SwimStroke_B.arpg`, `SwimStroke_C.arpg`, `Underwater.arpg`

### `assets/sounds/weapons/`

`CannonFire.arpg`, `Fizzle.arpg`, `Load.arpg`, `Prime.arpg`

### `assets/sounds/weather/`

`Rain.arpg`, `RainHeavy.arpg`, `Thunder_A.arpg`, `Thunder_B.arpg`, `Thunder_C.arpg`, `ThunderFar_A.arpg`,
`ThunderFar_B.arpg`, `ThunderFar_C.arpg`, `Wind.arpg`, `WindStrong.arpg`

### `assets/sprites/menus/`

`Button.arpg`, `ButtonOutline.arpg`, `DialFace.arpg`, `DialNeedle.arpg`, `DialRing.arpg`, `EditorTab.arpg`,
`FrameOutline.arpg`, `Panel.arpg`, `PanelHeader.arpg`, `PanelOutline.arpg`, `Shadow.arpg`, `StatusBar.arpg`,
`ToolBar.arpg`

### `assets/sprites/menus/creator/`

`SliderTrack.arpg`

### `assets/sprites/menus/hud/`

`KeyCap.arpg`

### `assets/sprites/menus/inventory/`

`Inset.arpg`, `Slot.arpg`, `ViewFrame.arpg`

### `assets/sprites/menus/paper/`

`PaperStrip.arpg`

### `assets/sprites/menus/wood/`

`WoodPanel.arpg`, `WoodPeg.arpg`, `WoodSlat.arpg`

### `assets/stats/`

`Stats.arpg`

### `assets/structurelists/fachwerk/`

`Houses.arpg`, `Outposts.arpg`, `Overpasses.arpg`, `Taverns.arpg`

### `assets/structurelists/shared/`

`Castles.arpg`, `Gatehouses.arpg`, `Keeps.arpg`, `WallTowers.arpg`

### `assets/structurelists/tudor/`

`Houses.arpg`, `Outposts.arpg`, `Overpasses.arpg`, `Taverns.arpg`, `Wells.arpg`

### `assets/structures/`

`LavaWatchtower.arpg`, `OreShrine.arpg`, `StoneCottage.arpg`

### `assets/structures/caves/`

`MinersCamp.arpg`, `SmugglersCove.arpg`

### `assets/structures/fachwerk/`

`Buergerhaus.arpg`, `Gasthaus.arpg`, `Haus.arpg`, `Overpass.arpg`

### `assets/structures/shared/`

`Castle.arpg`, `Gatehouse.arpg`, `Keep.arpg`, `WallTower.arpg`

### `assets/structures/tudor/`

`Cottage.arpg`, `Farm.arpg`, `Longhouse.arpg`, `Overpass.arpg`, `Tavern.arpg`, `Townhouse.arpg`, `Watchtower.arpg`,
`Well.arpg`, `Windmill.arpg`

### `assets/texturealiases/`

`Albedo.arpg`, `AO.arpg`, `Emissive.arpg`, `Growth.arpg`, `Height.arpg`, `Metal.arpg`, `Normal.arpg`, `Specular.arpg`

### `assets/tiers/`

`Tiers.arpg`

### `assets/tools/`

`Standard.arpg`

### `assets/trades/`

`Trades.arpg`

### `assets/trees/broadleaf/`

`Baobab.arpg`, `CopperBeech.arpg`, `EnglishOak.arpg`, `GoldenAcacia.arpg`, `SilverBirch.arpg`, `SugarMaple.arpg`

### `assets/trees/columnar/`

`ItalianCypress.arpg`, `LombardyPoplar.arpg`

### `assets/trees/conifer/`

`CoastRedwood.arpg`, `NorthmarkSpruce.arpg`, `ScotsPine.arpg`

### `assets/trees/fey/`

`BlueFairyTree.arpg`, `ElvenheartOak.arpg`, `EmberbarkTree.arpg`, `MoonsilverWillow.arpg`, `PinkFairyTree.arpg`,
`SakuraBlossom.arpg`

### `assets/trees/palm/`

`CoconutPalm.arpg`, `DatePalm.arpg`

### `assets/trees/shrub/`

`HazelThicket.arpg`, `JuniperScrub.arpg`, `RedMangrove.arpg`

### `assets/trees/weeping/`

`WeepingWillow.arpg`

### `assets/ubos/`

`BlockOrientationMapData.arpg`, `CameraData.arpg`, `CelestialData.arpg`, `CharactersStandard.arpg`, `ContentData.arpg`,
`CoveringData.arpg`, `GridCoordinateData.arpg`, `ItemRotationData.arpg`, `ItemsStandard.arpg`, `MacroCoverageData.arpg`,
`MoonLightData.arpg`, `NaturalNoiseData.arpg`, `OceanData.arpg`, `OrthoData.arpg`, `PlayerPositionData.arpg`,
`PostProcessData.arpg`, `PrecipitationData.arpg`, `SettingsData.arpg`, `SkyColorData.arpg`, `SliceData.arpg`,
`SSAOData.arpg`, `SunLightData.arpg`, `SurfaceStandard.arpg`, `TimeData.arpg`, `WeatherMapData.arpg`, `WindData.arpg`

### `assets/weapons/`

`Axe.arpg`, `Dagger.arpg`, `Improvised.arpg`, `Pickaxe.arpg`, `Shovel.arpg`, `Staff.arpg`, `Sword.arpg`, `Unarmed.arpg`

### `assets/weathers/standard/`

`BlizzardWeather.arpg`, `ClearWeather.arpg`, `CloudyWeather.arpg`, `DownpourWeather.arpg`, `DrizzleWeather.arpg`,
`FoggyWeather.arpg`, `HeatwaveWeather.arpg`, `HighCirrusWeather.arpg`, `OvercastWeather.arpg`,
`PartlyCloudyWeather.arpg`, `RainWeather.arpg`, `StormyWeather.arpg`, `SunnyWeather.arpg`, `WindyWeather.arpg`

### `assets/worlds/`

`TerraArcana.arpg`
