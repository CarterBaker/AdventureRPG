# CharacterEditorPipeline

Where the character tools live, which method owns each operation, and the traps. Read it before working in the
Character Editor and its layers (the look modes, every region's modelling modes, the face), the Animation Editor, their
docker windows, races and their genders' models, or a character's appearance (morphs, face controls, zones, masks,
overlays, shaders). `.claude/CLAUDE.md` still applies in full.

---

## Windows

Every character tool is split into windows, the way Krita splits its dockers, and every part of making a character is
one window, the Character Editor, its tasks layers picked in the Layers tab (see `.claude/docs/Editor.md`). A viewport
tab holds the status bar and the viewport; saving, reverting, copying, deleting, undoing and redoing are the editor's
File and Edit menus' (`EditorSetting.FILE_MENU_CHARACTER`, `FILE_MENU_FACE` on the Face layer, `FILE_MENU_ANIMATION`),
shared by each tool's viewport and docker tabs; everything else is its own tab, opened from the editor's Characters menu
and restored by layouts like any other.

| Window | Context | Shows |
|---|---|---|
| Character Editor | `CharacterEditorContext` | the layer picked: the live preview in a look mode, or the region framed and shown alone in a mode that takes tools (a tool canvas) |
| Character Outliner | `CharacterOutlinerContext` | `CharacterEditorManager.buildListRows` |
| Face Outliner | `FaceOutlinerContext` | `FaceEditorManager.buildListRows` |
| Animation Editor | `AnimationEditorContext` | the posed character; the Animation Pose tool canvas while a clip plays on a character |
| Animation Outliner | `AnimationOutlinerContext` | `AnimationEditorManager.buildListRows` |
| Animation Timeline | `AnimationTimelineContext` | transport, summary track and a dope sheet lane per bone |
| Properties | `PropertiesContext` | the value rows of the character, face or animation tab worked in last |

- **Layers.** The Character Editor's layers are a region and a mode (`CharacterEditorModeSystem`, the window's own
  state, keyed `region/mode`): the Character group holds the look modes (Appearance, Build, Equipment) on the whole
  character, then the Body (`CharacterEditorRegion.WHOLE`) and every region nested under it by `getParent` — Head
  (with Hair, Ears, Eyes and Mouth), Hands, Feet — each a group holding every mode `CharacterEditorMode.isHeldBy` gives
  it: Mesh, Sculpt, Weights, Paint and Rig on every region, and Face (`CharacterEditorMode.FACE`, the face controls) on
  the Head alone. Every region has the same tools; a region is only the closer, isolated view. Picking a group keeps the
  mode where the region holds it, Mesh otherwise. Add a region or a mode as enum data, never as a window.
- **One window, one manager.** Each Character Editor window remembers its own layer and draws its preview in it
  (`CharacterEditorPreviewSystem` reads the window's mode, never the manager's); while it is the active window, and
  when a layer is picked for it, it hands its region and mode to `CharacterEditorManager.setRegion` / `setMode`, so the
  Outliner and the Properties tab follow the window worked in last. The context creates every viewport system; the
  mesh view, sculpt, rig, paint and overlay systems act only in the modes their flags name, and on the Face layer
  `FaceEditorPreviewSystem` and `FaceEditorGestureSystem` drive the viewport instead of the character preview (each
  binds afresh when it takes over). `CharacterEditorContext` answers its tool canvas, tool actions, Properties rows,
  templates and File and Edit table for the layer picked: the face editor's on Face, the character editor's on every
  other. Its keys are one file, `shortcuts/CharacterEditor.arpg`, kept to their layers by `canvases`; Undo, Redo and
  Save run the File menu's action (`FileMenuBranch.run`), so they act on the face on the Face layer.

- **The Appearance outliner is the player's creator.** In Appearance the Character Outliner lists what the game's
  creator offers the player: the gender, the features worn (Hair: the race default, none, or any style that fits),
  then every shape slider under its group in the order the race first lists each, each a ranged row
  (`CharacterViewRowStruct.ranged`) holding the value the game starts the previewed gender at, across the shape's
  own `min` to `max`: the gender preset's `shapes` value where it names the shape (`CharacterEditBranch.setGenderShape`),
  the race's `default` otherwise (`FaceEditorManager.setDefault`, the Face layer's default row path), the preview's
  override cleared so it shows the new start, with one pair row (`CharacterViewRowStruct.pair`, two typed value boxes side by side, no slider) for
  its Min and Max beneath it. Those ends are written through `FaceEditorManager.setRangeEnd`, the path the Face
  Editor's range rows take (a step apart, the default carried inside, one face history step), so the slider follows a
  new range the moment the preview rebuilds. Appearance lists no pose: a mode names the pose it stands in (`CharacterEditorMode.getStandingPose`,
  Appearance idles as the creator shows the player) or lets its outliner pick one (`picksPose`: Build, Equipment,
  Weights). Colors, tones, details and Blink stay in the Properties tab.
- **Values live in the shared Properties tab.** The character, face and animation viewport contexts implement
  `PropertiesProvider` (content `ROWS`) and `CharacterViewRowSource` (their manager's `buildPropertyRows`) and create a
  `PropertiesSourceSystem`; the Properties tab (`RowPropertiesSystem`) shows the rows of the one focused last and hands
  its sliders' steps back to it. There are no per-tool Properties windows.
- **Number rows have a slider.** `CharacterViewPanelSystem` refills a panel only when its rows change by kind, key,
  label or selection; otherwise it sets the moved values in place, so a dragged slider keeps its row. A slider spans
  `EditorSetting.CHARACTER_VIEW_SLIDER_STEPS` of the value's own steps centred where the value stood when the row was
  listed, and hands the tool the steps it moves through the context's `CharacterViewStepTarget.stepRow`, the path the
  step buttons take; the value box still opens for typing. A ranged row spans its declared range instead, its knob set
  where the value stands on every refill and refresh, a drag still handing over whole steps of the row's own step. A number row template carries the track and knob ids in
  its `CharacterViewTemplateStruct`.
- An outliner docker context implements `CharacterViewRowSource` and creates `CharacterViewPanelSystem`,
  `CharacterViewDockSystem` and `ToolCanvasSystem`; the dock system opens `menus/editor/CharacterDock.arpg` and
  refills it only when the source's revision moves. Row templates stay in the tool's own menu
  (`EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES`, `FACE_EDITOR_ROW_TEMPLATES`, `ANIMATION_EDITOR_ROW_TEMPLATES`) so
  their buttons route to the tool's branch. A tool without a text row template must never list a `TEXT` row.
- Tools are the Tools window's (`ToolCanvas.CHARACTER_MESH`, `CHARACTER_SCULPT`, `CHARACTER_WEIGHTS`,
  `CHARACTER_RIG`, `CHARACTER_FACE`, `ANIMATION_POSE`); brushes the Brushes window's. A tool strokes the brush where
  the canvas sizes one and the tool carries Size (`ToolCanvas.strokes`), so on the mesh and the weights Select sweeps
  the brush over vertices, each selected as much as the tip covers it (a hard brush whole, a soft one fading at its
  edge; Shift adds, Ctrl takes away, a press where the brush covers nothing still clears or boxes). Move, Rotate,
  Scale and the numeric transforms carry each vertex as far as it is selected, and Assign, Add, Subtract and Smooth
  weights reach it as far. Add a tool to a canvas in
  `ToolCanvas`, never as a panel button. The mesh's one-off operations (transform, geometry, weights, selection) are
  the canvas's action groups (`ToolActionGroupStruct`), shown in the Tools window under the tools and run by
  `CharacterEditorContext.runToolAction` through `CharacterEditorManager.act`, the same key their shortcuts carry; a
  face control's operations (add, duplicate, remove, drive the picked bone, mirror, reset the preview) are the face
  canvas's, run by `CharacterEditorContext.runToolAction` on the Face layer through `FaceEditorManager.act`.
  Properties keeps values only.
- Every viewport (Character, Face, Animation, Animation Tree Editor) looks through the editor's `ViewCameraSystem`,
  framing `CharacterViewPoseSystem` (the Character Editor frames its region through `CharacterRegionFrameSystem` on
  every layer, the Face layer's on the Head). A viewport
  system takes a click only when it acts on something (a vertex, a joint, the mesh under the brush) and claims it with
  `EditorInputSystem.claim`; any other left press orbits and a right press pans. Cancel clicks
  (`CHARACTER_VIEW_BUTTON_CANCEL`) are claimed too. Test `ViewCameraSystem.isDragging`, never a held button, to stay
  out of a camera drag.

## Regions

- **Regions.** `CharacterEditorRegion` is data: its label, the region it nests under, the feature it holds, the
  `RigPart` it holds on the body, whether it pins roots, takes the rig and the face, its bone prefix and the worn parts
  it shows beside its own (`showsFeature`). Whole, Head, Hair, Ears, Eyes, Mouth, Hands and Feet.
- **A region is data, its bones the rig's.** `CharacterEditorRegion` names the worn feature it holds (Hair: `HAIR`,
  Eyes: `EYES`, Mouth: `TEETH`) and the `RigPart` whose bones it holds on the body with every bone below them (Head,
  Ears, Hands, Feet, Eyes, Mouth). The bones come from the rig's own `parts` (`RigData.getPartBones`), never from names
  in code, so any creature's rig gets every part layer by naming its parts; a rig that names no such part leaves that
  layer empty. Hair also names its own bones by prefix (`hair_`, and the head they hang from). Bone membership is read
  only through `CharacterRegionBranch.holdsBone` / `holdsRigBone`. The active window hands its region to
  `CharacterEditorManager.setRegion`, which keeps the part picked while the region holds it, else picks its body, else
  its feature. A region holding both lists only those two in the outliner's Parts (`holdsPartKey`), so the Eyes layers
  sculpt the socket on the head and the eye itself, and the Mouth layers the lips and the teeth.
- **Sides.** A region whose every part bone has a twin (`_l`/`_r`) is sided (`CharacterRegionBranch.isSided`, cached
  per rig data): the outliner lists Side (`CharacterEditorSide`, kept per region in the manager), the window frames the
  side's first part bone (`getRegionFrameBone`), shows and picks only that side (`isOnRegionSide`, `isBoneShown`,
  `isRigBoneShown`), and `isMirror` is held on, so every edit is made to the hidden twin as well.
- **Isolation.** `CharacterRegionIsolationSystem` fills the viewport's `AppearanceIsolationStruct`
  (`CharacterViewRenderSystem.getIsolation`): the body only where the region holds a part, drawn through a `SHOW`
  bone mask of the bones shown, and of the worn features only those the region shows (the Head its hair, eyes and
  teeth), masked too where sided. The mesh view takes faces hidden this way out of occlusion (`isFaceShown`). The Body
  and the look modes isolate nothing.
- **One mask.** `CharacterRegionBranch` gives each vertex its share of the region (on the body the weight it puts on
  the part's bones; the region's own feature whole, or for pinned hair how far along its card it stands, root 0 to
  tip 1), cached per region until the mesh changes. The mesh view's pick, box and brush (`isInRegion`, the brush's
  strength times the share), the overlay, the proportional transform, sculpt mirroring, weight mirroring, the
  selection (`CharacterSelectionBranch.changed` drops anything outside) and rig joint picking all read it; never test
  a region anywhere else. A tab whose region's part is not worn edits nothing and says so.
- **Digits are found, not placed by hand.** `CharacterDigitBranch.detect` sweeps the vertices a hand or foot bone
  carries from the farthest in toward its joint, joining them face by face: every branch ending where it meets
  another, two long ones (`CHARACTER_RIG_DIGIT_MIN_LENGTH`) meeting are both digits. Five are thumb (meeting the palm
  nearest the joint), index, middle, ring and pinky by distance from it; a region with none apart takes one toe at
  `CHARACTER_RIG_BALL_SHARE` of its length. Rig mode's **Rig Digits** lays a chain of `CHARACTER_RIG_DIGIT_SHARES`
  joints on each (`CharacterRigBranch.addDigits`, named `<digit>_<n>_<side>`, a toe `toe_<side>`); Weights mode's
  **Weigh Onto Digits** (`CharacterWeightBranch.weighDigits`) then hands each vertex of the hand or foot bone to them,
  a digit's own vertices wholly, blended across each joint, the palm near each digit's root partly. Bones are
  structure, so save the rig and restart between the two.

## Modes

`CharacterEditorMode` carries its behaviour as data: its tool canvas, whether it edits a worn mesh, whether it holds
the bind pose, whether it draws vertices, and its status hint. Check those flags, never `mode == X`, except where a
mode's own branch is chosen (`SCULPT` strokes the shape, `RIG` undoes the rig).

- **Poses.** Every mode that picks a pose (Build, Equipment, Mesh, Sculpt, Weights) lists one Pose row, stepped by
  `CharacterEditorManager.stepPose` through Bind (the default: the mesh exactly as its file holds it in a mesh mode),
  each clip the rig names in `poses` (struck still at its start, `CharacterEditorPreviewSystem.strikeClip`, labelled
  by `CharacterViewUtility.toResourceLabel`), then Idle, Walk and Run played. Rig holds the bind pose. Vertices are
  moved only in Bind: a sculpt stroke or a transform while posed is refused with `CHARACTER_EDITOR_MESSAGE_POSED`
  (`isPosed`); selecting and painting weights work in any pose, the mesh view skinning every vertex by it. A pose is a
  plain clip on the rig (`animations/humanoid/poses/`); add one by naming it in the rig's `poses`.
- **Mirror pairs.** `CharacterSelectionBranch.resolveMirror` pairs each vertex with the one at its mirrored position
  on the decimal grid, then pairs every vertex left without a twin with the nearest one likewise left across x within
  `CHARACTER_MESH_MIRROR_REACH` (`pairNearest`, binned by reach-sized cells), so a mesh not modelled exactly
  symmetric still mirrors every stroke. `CharacterViewUtility.round` rounds half away from zero, so a point and its
  mirror always round alike.
- **Sculpt tools.** Brush, Smooth, Relax (even spacing along the surface without shrinking), Inflate, Pinch, Crease
  (a sharp line drawn in under the brush's centre, a ridge inverted: lid folds and lip lines), Flatten and Nudge, all
  in `CharacterShapeBranch.resolveMove`.

## Paint

Paint mode (`CharacterEditorMode.PAINT`, `ToolCanvas.CHARACTER_PAINT`: Brush, Erase, Smooth, Fill, Pick, the Brushes
tab's brush) paints the open part's texture maps on every region. It is a mesh mode that `paintsMaps`, so the
mesh view skins the part in any pose and the sculpt system stays out of it.

- **Projection painting**, as Blender and Substance paint: `CharacterEditorPaintSystem` lays a stamp every brush
  spacing and hands `CharacterPaintStampStruct` every face of the region under the brush whose middle the camera sees,
  its corners on the window; `CharacterPaintBranch.stamp` rasterises each face's two UV triangles over the map and
  paints each texel by the tip's coverage where the same barycentrics place it on the window, so strokes cross UV
  seams; texels within `CHARACTER_PAINT_BLEED_PIXELS` of an island edge are painted too. Fill covers the island of
  faces joined corner to corner around the face clicked (`pickFace`), Pick takes the flattened map's colour there.
- **Canvases** (`CharacterPaintCanvasStruct`): one per texture and map (albedo, normal, height, AO, metallic,
  roughness, tint; `CHARACTER_PAINT_MAP_*`), opened on first use from `<Name>_<suffix>.png` beside the texture or the
  alias's default colour. Its layers are image documents of `ImageManager` (`beginPaint` / `paintPixel` /
  `flushPaint`), the first the image itself, each above laid over by alpha and opacity; every stroke is one undo step
  of the layer it touched (the before colours of the pixels it changed), and each change is laid flat and uploaded
  into the atlas layer the map fills, only its rectangle, rows bottom first (`TextureManager.updateTextureArrayLayer`,
  `glTexSubImage3D`). The base keeps its own alpha; Erase wears the base to the default and a layer to nothing.
- **Values** (Map, Layer, Opacity, Red, Green, Blue) live in the Properties tab (`CharacterPaintPropertyBranch`,
  `CharacterPaintRowBranch`); Add Layer and Remove Layer are the canvas's tool actions. Save (File) writes every
  changed map flat into its image, Revert puts the atlas back as the images hold it, Undo and Redo (Edit) step the
  paint history in Paint mode. Painted maps are the owner's art: never catalogue them.
- **Not industry standard:** layers above the base last the editor session and are flattened into the image on save;
  a painting tool keeps them in a layered project file (Substance, Krita) beside the exported maps.

## Characters

Every entity template names its race (`race`, required), every race lists the genders it may be made as (`genders`, at
least one, the first the default), and every gender names the character model it requires: its `mesh`, the `rig` that
mesh is skinned to, its `material` and its `animation_tree`, with the `shapes` and `features` it starts with. The race
also holds the `equipment` anchors and the optional `appearance` (sliders, colors, features, build). The game reads
the race through `EntityData.getRaceData()`; `RaceManager` (`entitypipeline.racemanager`) loads races with
`RaceBuilder` and `AppearanceBuilder`. A character is a race's rigged body mesh per gender with its whole head
modelled in, plus worn feature meshes (`FeatureSlot`: hair). Nothing is a texture swap.

- **The character tools edit races.** A `CharacterDocumentInstance` is one race (`INFO_SCHEMA_RACES`), its preview a
  `RaceHandle` from `RaceManager.buildPreviewRace`. Picking a race opens it; picking an entity in the Hierarchy, or changing
  the race it names, opens its race through `CharacterEditorManager.openRace`, the one path that opens a race by name.
- **Entities and races are edited in the Properties tab.** Pick an entity or a race in the Hierarchy; its `race`,
  each gender's `mesh`, `rig`, `material`, `animation_tree` and `features`, and every other reference are choice rows
  stepping through what they may name. Make, duplicate, save, revert and delete them through the File menu; copy a
  gender to add one (its Copy button names the new gender). Every gender of a race must still name the same rig.
- **Mesh format** (`RiggedMeshArpgUtility`): every quad may name its `zone` (`MeshZone`: skin, eye, brow, lash, hair,
  cloth, mouth, teeth); `masks` (channels `EngineSetting.RIGGED_MESH_MASK_CHANNELS`) and `morphs` name vertices by the
  number the parse gives them, corners merged by position and influences in the order they first appear. Writing
  renumbers through `resolveFileVertices`, so never index masks or morphs by `RiggedMeshStruct` order in a file.
- **GPU path.** `RiggedMeshGeometryUtility` writes per corner its morph entry run, zone with raw UV and masks (the
  trailing attributes `EngineSetting.RIGGED_MESH_TRAILING_ATTRIBUTE_SIZES`); the morph deltas live in the mesh's morph
  texture (`MeshHandle.getMorphTexture`). Each instance's palette row (`SkinnedBufferData`) holds its bones, then the
  appearance texels (`SkinnedAppearanceStruct`: hidden bone, one texel per `SkinnedColor`, `SkinnedDetail` amounts),
  then its morph weights. The shaders read them through `characters/includes/CharacterAppearance.glsl` and shade by
  zone in `CharacterSurface.glsl`; keep the GLSL constants in step with the enums.
- **Eyes and teeth are worn parts.** `FeatureSlot` holds `HAIR`, `EYES` and `TEETH`; each feature may name its own
  `material` (`FeatureData.getMaterial`), drawn with it by `EntityRenderSystem.pushBody`, the body's otherwise. A
  removable slot (`FeatureSlot.isRemovable`, hair) is offered bare in the game's creator; eyes and teeth are not, and a
  save that names none for them keeps the gender's. The head's sockets, lids, lips and mouth cavity are the body's.
- **Bone masks.** A skinned draw's palette row carries a `SkinnedBoneMaskStruct` and its `SkinnedMaskMode`
  (`SkinnedAppearanceStruct.setBoneMask`): `HIDE` cuts every fragment the masked bones move at all (a character seen
  from its own eyes: `AppearanceData.getHeadMask`, the head and everything below it, eyes and teeth included), `SHOW`
  keeps only fragments they carry at least half of (part isolation). A rig may hold at most
  `SKINNED_BONE_MASK_CAPACITY` bones.
- **Overlays.** Brows, nipples and body hair (`SkinnedOverlay`: chest, stomach, pubic, underarm, arm, leg) are images
  painted over the skin in the body's own UV layout, never geometry: UV-space decal layers. Each gender's model places
  them (`genders.<g>.overlays`: the `region` of its UV layout the images cover, u0 v0 u1 v1, the `styles` it offers,
  each a `name`, a `texture` in the character atlas and an optional `relief` in blocks, and the style worn by
  `default`; `RaceGenderStruct.getOverlay`), written into each draw as three texels: the tile of the style worn, the
  region and the relief (`SkinnedOverlayStruct.write`, `AppearanceHandle.getOverlayStyle`, `INDEX_NOT_FOUND` for the
  model's default). The shader maps the corner UV into the region and samples the style's layers there
  (`shadeOverlays`), tinted by the overlay's `SkinnedColor` (`BROWS`, `NIPPLES`, `BODY_HAIR`) and shown as far as its
  `SkinnedDetail`. A style with relief (the nipples) rises from the skin through its height map by parallax occlusion
  (`marchOverlayRelief`: the ray enters the relief's top and marches down to the skin, so bare skin never shifts) and
  bends the normal through its normal map, both faded by the Character Detail distance. An overlay's images cover only
  its region, so a style is a small image; paint a new style in the body's layout, crop it to the region, and every
  map it carries (A, h, n, ...) goes beside it by suffix. Saves keep a chosen style by name (`overlay_styles`); the
  creator lists a style row and an amount per overlay, and the Character Appearance outliner's Marks rows set the
  previewed gender's `default`.
- **Eyes.** An eye is its variant's image set (`features/humanoid/eyes/HumanoidEyes`: Anime, Realistic, Cat, Ringed)
  on the eye zone's raw UVs, the iris centred at `CHARACTER_IRIS_CENTRE` (`shadeEye`): Iris Size scales the image about
  the iris, Pupil Size dilates the pupil by a radial remap inside it, and the iris, `CHARACTER_IRIS_DEPTH` behind the
  cornea, shifts against the view (refraction); the eye color reaches what the tint mask allows. The eye writes a
  specular falling from `CHARACTER_EYE_SPECULAR` toward `CHARACTER_EYE_SPECULAR_FAR` across the Character Detail
  distance, and the lighting pass (and the editor viewport) mirrors the sky on every glossy character pixel by that
  specular, weighted by Fresnel and occlusion (`SkyReflection.glsl`). **Not yet industry standard:** the environment
  is the analytic sky the sky shader paints, not a captured reflection probe of the surroundings.
- **Material variants.** A look offering several image sets is a `SkinnedVariantStruct` (names, tiles, reliefs,
  default), parsed for overlays' `styles` and features' `variants` alike by `SkinnedVariantUtility.parse`. A feature
  naming `variants` and a `default` is worn in the one its appearance picks (`AppearanceHandle.getFeatureVariant`,
  INDEX_NOT_FOUND for the default, cleared when the feature changes): `EntityRenderSystem.pushBody` writes the
  variant's tile into the draw's variant texel (`SkinnedAppearanceStruct.setVariant`, none for the body) and the shader
  lays the corner's raw UV into that tile for every layer (`resolveCharacterUV`), a texture swap on one mesh. Saves keep
  the variant by name (`feature_variants`); the creator lists a variant row under the feature row.
- **Material maps.** Every character texture may carry the material maps by file suffix like any atlas
  (`texturealiases/`): albedo, normal, height, AO, metallic, roughness and tint, each its own image; a map no file
  provides is its alias's default color (flat normal, AO 1, metallic 0, roughness 0.5, tint 1), and `TextureLoader`
  seeds every layer the atlas's UBO declares (`ubos/CharactersStandard.arpg`).
  `characters/includes/CharacterMaterial.glsl` samples them: the normal map through a tangent frame rebuilt from screen-space derivatives (meshes carry no tangents),
  AO into the lighting's ambient, roughness and metallic into the highlight, and the tint mask choosing where the
  player's colors reach (`resolveCharacterAlbedo`: 0 keeps the texture's own color, so gold or silver never takes the
  teeth color). Fine detail fades out toward the Character Detail distance (`Settings.characterDetailDistance`, the
  graphics menu's Character Detail row, pushed by `SettingsSystem.applyCharacterDetail` into `u_characterDetail` of
  `SettingsData`); every detail feature reads `resolveCharacterDetail`, never a distance of its own.
- **Body UVs.** Each body is unwrapped into its own layout (`characters/standard/MaleBody`, `FemaleBody`: albedo
  near white, tinted by the skin color, with AO and normal maps). Shells: head (denser), torso front and back (side and
  shoulder seams), each arm (seam under the arm), each leg with its foot (seam up the inner leg and along the sole),
  each hand's palm and back, each ear. The genders' hands differ in topology, so the genders do not share a layout.
  A topology change means a new unwrap and a transfer of the painted maps; sculpting, morphs and posing keep them.
- **Colors in the creator.** Every color section lists the race's palette swatches, then Hue, Saturation and
  Brightness sliders (`CreatorSlider.COLOR_*`, `Color.setHSB`) for any color between. Teeth take `SkinnedColor.TEETH`.
- **Appearance.** `AppearanceData` holds the race's body meshes (each gender's `mesh`, in the race's order),
  colors, palettes, details, shape sliders and the morphs each `AppearanceExpression` plays, the morphs resolved on
  every body (`getShapeMorph`, `getExpressionMorph`). A slider (`AppearanceShapeStruct`) is known by its `name` (its
  morph's when it gives none) and drives a `morph`, the `bones` it moves, turns and scales per unit of its value
  (`AppearanceBoneTargetStruct`), or both: that is a face control. `AppearanceHandle` keeps slider values as morph
  weights of the body worn and sums the bone targets into each bone's shaping (`AnimationStateHandle.setBoneShape`,
  laid on the bind pose under every pose) and proportion in `applyProportions`, plays the blink (`updateFace`, through
  the half shape) and hands everything to a draw in `writeAppearance`. The creator lists shapes by group (the body
  group on the Body tab); saves and gender presets store shapes by name.
- **Shape keys in the editor.** Sculpt mode's Shape Keys rows pick the morph being sculpted
  (`CharacterMeshBranch.editShape`): the morph is lifted into the positions so every tool works unchanged, and baked
  back into deltas when sculpting returns to the base, the mode or part changes, or the mesh is saved. Mesh modes
  preview with every slider at zero and the face still.
- **Genders.** Each gender's model is a `RaceGenderStruct` (`RaceData.getGender`). `EntityInstance.setGenderID` is the
  one way a being changes gender: it takes on the gender's model (`getRaceGender`), moves its animation state onto the
  gender's tree when that tree differs, and resets the appearance to the gender's preset. `EntityRenderSystem` draws the
  gender's mesh with its material. Every body is on the race's rig and carries every morph the sliders and
  expressions name. The Character Editor previews one gender at a time (the Gender row, or a gender picked in the Entity
  Editor): `CharacterViewPoseSystem.setGender` takes on its model and tree, its body and features are the parts the
  mesh modes edit, and untouched preview sliders sit where that gender starts them.
- **Assets.** The race `races/Human` (entity `HumanoidEntity`) is male, its default, or female. `HumanoidMaleMesh` and
  `HumanoidFemaleMesh` share the rig with its 15 face bones (jaw, chin, lips, mouth corners, nose, cheeks, eyes,
  brows, ears) set on the anime heads, three joints on every finger and the thumb of each hand and a toe at each
  foot's ball, and carry the same morphs (blinks, body sliders, `ear_point`). Each body is game topology about seven
  heads tall, a quad cage smoothed by two Catmull-Clark levels: edge loops at the joints, a radial loop (pole, areola
  ring, rings about it) on each side of the chest (flat on the male), hands with a palm, three-jointed fingers and a
  thumb following the rig's finger bones, and feet with heel, arch, ball and toe box. The head (a dense grid with eye
  sockets whose lids close over the eye, the upper lid edge in the lash zone, a mouth cavity behind the lips in the
  mouth zone, and ears as their own shells) joins the body through one quad reduction row (32 to 64) at the neck.
  Every vertex has an exact mirror twin, so mirroring pairs the whole body.
- **A-pose bind.** `HumanoidRig` binds the arms in an A-pose: each upper arm carries a bind `rotation` of 37 degrees
  about z (`upper_arm_r` -37), so the arms hang 45 degrees from vertical, and every mesh, anchor and arm or finger IK
  pole is laid out in that pose. A clip's rotation is added to the bind's and z composes outermost, so every clip
  outside an additive layer keys each upper arm's z 37 degrees below what it would key against arms at the sides,
  and lists an upper-arm track even when it holds the arm still (an override layer writes a missing track as zero,
  the A-pose). Additive clips (Lean, Look) never key the arms. Key a new clip the same way; the Animation Editor's
  pose and IK tools already work against the bind rotation. Both genders
  wear `features/humanoid/eyes/HumanoidEyes` (`HumanoidEyesMesh`, drawn from the eye zone's raw UVs with
  `CharacterEyeMaterial`) and `features/humanoid/teeth/HumanoidTeeth` (`HumanoidTeethMesh`: a UV-mapped strip per
  row of teeth, the upper on the head in front of the lower on the jaw, and the tongue; `CharacterTeethMaterial`; nine
  variants, from Standard to Braces, each an image set whose tint mask keeps metal from taking the teeth color).
  `HumanoidRig` names its `parts` (head, ears, eyes, mouth, hands, feet) and seven
  `poses` (`animations/humanoid/poses/`). All are generated (catalogued); a rigged mesh may hold at most
  `RIGGED_MESH_VERT_LIMIT` corners. Each hand is one bone: hanging at the side its palm faces the thigh with the
  thumb forward (+z), so `hand_l` is a left hand at +x; keep that chirality on any hand you model or mirror.
- **Features are the gender's.** The Character Outliner's Features rows (Appearance mode) read and write the previewed
  gender's `genders.<gender>.features`, the features it is made with; a gender naming none wears the race's
  `appearance.features` default, shown with `CHARACTER_EDITOR_FEATURE_DEFAULT_SUFFIX`. A row steps through the default
  (the gender's own removed), then `none` (`EngineSetting.APPEARANCE_FEATURE_NONE`, `""`: the gender wears nothing
  there, so it can go bald under a race whose default gives hair; offered only while the default wears something),
  then every feature that fits. `AppearanceGenderStruct.namesFeature` tells a slot named none from one left to the
  race; read a preset's feature only through it (`AppearanceData.getStartingFeature`).
- **IK handles are rig data.** A rig's `ik` lists handles (`RigIkChainStruct`): three `bones`, root to end, each the
  child of the one before, and the `pole` their middle joint bends toward in the character's space (`RigBuilder`
  refuses anything else). Rig mode's **Add IK Handle** and **Remove IK Handle** (`CharacterRigBranch.addIkChain` /
  `removeIkChain`) work on the selected bone as the end and on its twin across the body; a new handle's pole is the way
  its middle joint stands off the line from root to end, or, for a straight chain, across it from the front (a
  finger's knuckle away from the palm), mirrored on the right. Edit a pole in the Properties tab (Rigs). The Animation
  Editor's **IK Handle** tool (`EditorTool.IK`, `AnimationEditorPoseSystem.applyIk`) picks only handle ends, carries
  the end across the plane facing the camera and solves the two bones analytically, the middle joint bent on the
  pole's side, keying root and middle together through `AnimationEditorManager.beginChainPose` /
  `writeChainPose`, one undo step.
- **Hair is cards.** A hair mesh is a scalp cap and layered cards two quads wide, textured with the strand tile
  `characters/standard/Hair` (its alpha cuts the locks out); each card's raw UV runs across it in u and from root
  (0) to tip (1) in v. The character vertex shader sways hair-zone vertices in the wind by v (`CharacterHair.glsl`:
  slow gusts, a phase per lock read off where it grows in the bind pose, the tip trailing its root, a flutter across
  the wind and a lift on gusts), after skinning and in the world, so it adds to whatever bones carry the lock; keep v
  running root to tip on any card you add, and keep the root at the scalp.
- **Long hair rides spring chains; short hair is the shader's.** A rig's `springs` (`RigSpringChainStruct`) each name
  at least two `bones`, each the child of the one before, the first hung from a bone the body moves, none in another
  chain or an IK handle, with `stiffness` (pull back to the pose, 1/s²), `damping` (settles a joint's swing against
  the joint above it, 1/s), `gravity` (blocks/s²) and `drag` (the air holding it back, 1/s), each within the
  `EngineSetting.RIG_SPRING_*_MAX` range (`RigBuilder` refuses anything else; the schema says the same). Nothing keys
  them: `AnimationStateHandle` simulates them over the finished pose every `update` (`AnimationSpringStruct`: joints
  kept in the body's frame, substeps no longer than `RIG_SPRING_STEP`, each bone held its length and within the swing
  cone, an update longer than `RIG_SPRING_MAX_DELTA` ridden rigidly for the rest, a body moved farther than
  `RIG_SPRING_RESET_DISTANCE` started afresh), then turns each bone about its joint onto its simulated point
  (`RigMathUtility.setPivotTurn`) and skins again only the bones the chains carry. `EntityInstance.moveSprings` hands
  it the body's turn, size and travel across chunks before each update; `CharacterViewPoseSystem.play` hands a
  preview travelling forward at the pace it plays. `poseRest` and `poseClip` never simulate and start the springs
  afresh. A chain runs only while something worn weighs onto it (`AppearanceHandle.resolveSprings`, reading
  `MeshHandle.weighsOnto`, set for every rigged mesh in `MeshManager.applyRiggedMesh`), so a bald or short-haired
  character simulates nothing. Settings apply live (`RigManager.updateRig` keeps a rig in place while its chains keep
  their bones); a new or changed chain is structure and takes hold at the next start.
- **Springs rest against colliders bound to bones** (the physics-asset / spring-bone collider approach). A rig's
  `colliders` (`RigColliderStruct`, at most `RIG_COLLIDER_MAX`) each name a `name`, the `bone` it rides, a `shape`
  (`RigColliderShape`: `sphere` about its `offset`, or `capsule` from `start` to `end`, all in the bone's bind frame)
  and a `radius`; each chain names the colliders it rests against (`colliders`, none passes through everything) and
  its own thickness (`radius`, default `RIG_SPRING_RADIUS_DEFAULT`). `RigBuilder` refuses unknown bones and collider
  names, repeats and radii out of range; the Rigs schema carries the same rules. `AnimationSpringStruct.capture` places
  every collider by the pose's world matrices in the body's frame (its radius sized with the bone and body); each
  substep, after the length and swing cone, `collide` pushes the joint so the bone from the joint above it (a capsule of
  the chain's radius) clears every named collider, the push divided by how far down the bone the contact lies, then the
  length is held again. Colliders are not structure: they apply live.
- **Hair tools.** On the Hair layers, Rig mode's **Rig Hair Locks** (`CharacterRigBranch.addHairLocks`) finds the
  cards (`CharacterHairBranch.detectCards`: hair faces joined corner to corner whose v spans
  `CHARACTER_HAIR_CARD_MIN_SPAN`), keeps those at least `CHARACTER_HAIR_LOCK_MIN_LENGTH` long (shorter hair stays
  shader-only), joins cards rooted and hanging together into locks (`detectLocks`, matched point for point within
  `CHARACTER_HAIR_LOCK_RADIUS`) and lays one chain per lock from the bone its card roots weigh onto, one bone per
  `CHARACTER_HAIR_LOCK_SEGMENT` and a tip, named `hair_<lock>_<joint>`, its spring named `hair_<lock>` with
  `CHARACTER_RIG_SPRING_DEFAULTS`; a lock rooted within the lock radius of a hair chain already rigged takes that
  chain, so every style shares one set. **Add Spring Chain** / **Remove Spring Chain** make or drop a chain from the
  selected bone down its single children (and its twin), for hand-built chains. Weights mode's **Weigh Onto Locks**
  (`CharacterWeightBranch.weighLocks`) hands each long card to the hair chain whose joints lie nearest its line
  (`CharacterHairBranch.findChain`, roots within `CHARACTER_HAIR_WEIGHT_REACH`), by how far down the card each vertex
  grows: the head near the root, then each segment's bone, blended at joints; spring weights held before are let go
  first. The chains are bones, so save the rig and restart between the two. The selected bone's chain lists its five
  settings (stiffness, damping, gravity, drag, radius) in the Properties tab (`CharacterRigRowBranch.addSpringRows`),
  stepped and typed live, then a Yes or No per rig collider it rests against. **Add Collider** and **Remove Collider**
  (Rig mode's Colliders group, `CharacterColliderBranch`) add a sphere on the selected bone, halfway to its first
  child, that every chain rests against, and remove the collider selected among the bone's; while mirroring is on a
  sided bone's collider is added, removed, reshaped and moved with its twin (named as its mirror). The selected bone's
  colliders are listed in the Properties tab; the selected one shows its Shape (a capsule runs from the joint out past
  the sphere's centre as far again; back to a sphere at the capsule's middle), Radius, Offset or Start and End, and
  Spring Chains (All or None). Rig mode draws every collider over the body as a wire sphere or capsule
  (`CharacterEditorOverlaySystem`, the selected one in the selection colour), rebuilt only when the editor's state
  moves. **Preview Springs**
  (Rig and Weights) makes every modelling layer play the pose chosen (Idle at rest) with the springs simulated, until
  pressed again. The Animation Editor never lists, picks or keys a spring bone (`AnimationEditorManager.isKeyedBone`).
- **Assets.** `HumanoidRig` carries 14 hair chains (`hair_1` to `hair_14`, five joints each, radius 0.012) laid by Rig
  Hair Locks on `FemaleLongHairMesh`, every one resting against its five colliders: `head` (sphere on `head`), `neck`
  (capsule on `head`), `shoulder_l` / `shoulder_r` (capsules on the upper arms) and `chest` (capsule across
  `upper_torso`), sized inside the female body's skin with every hair joint clear of them at rest;
  `FemaleLongHairMesh` and `FemaleMediumHairMesh` are weighed onto them by Weigh Onto Locks.
  `MaleShortHairMesh` and `MaleMediumHairMesh` are short and stay on the head.
- **Toon lighting.** Characters write a smooth normal (`GBufferNormal.glsl`); the lighting pass lights those pixels
  in toon tones with a rim (`ToonLighting.glsl`), and the editor viewport's character shader uses the same function.

## Who owns what

- Sculpt strokes: `CharacterShapeBranch` (begin, stamp, end). The viewport fills one `CharacterStampStruct` per stamp
  in `CharacterEditorSculptSystem.placeStamp`. Stamps and weight strokes only move the CPU mesh; the sculpt system calls
  `CharacterEditorManager.showStroke` once its frame's stamps are laid, the one rebuild and upload of the part per
  frame (never per stamp: a rebuild re-skins every corner and remakes the morph texture).
- Occlusion: `CharacterEditorMeshViewSystem.isVisible` answers each vertex once a frame. The frame's first test bins
  every worn face into `CharacterEditorSetting.OCCLUSION_CELL_PIXELS` window cells by its projected bounds, so a vertex
  is ray-tested only against the faces over its cell; never test a point against every face.
- Vertex selection and its weights: `CharacterSelectionBranch` (picks, boxes, select strokes, the weighted centre).
  The document keeps a weight beside each selection flag (`CharacterMeshDocumentInstance.getSelectionWeight`, 0 when
  unselected); `syncSelection` holds every unselected vertex whole, undo steps keep the weights, and `removeOrphans`
  carries them over a renumbering.
- Rig: `CharacterRigBranch` owns the draft read from the rig's InfoManager entry, every rig edit (`edit`), its
  history and the live apply (`RigManager.updateRig`). Values and actions: `CharacterRigPropertyBranch`; rows:
  `CharacterRigRowBranch`; viewport: `CharacterEditorRigSystem`.
- Clip edits: `AnimationEditBranch.edit` records every edit in the clip's `CharacterViewHistoryStruct`; pose drags
  go through `AnimationEditorManager.beginPose / writePose / endPose / cancelPose`; viewport:
  `AnimationEditorPoseSystem`.
- Face controls: `FaceEditBranch` performs every edit of the template's shapes through InfoManager, recorded in the
  template's face history (`FaceEditorManager.getHistory`); values and actions: `FacePropertyBranch`; rows:
  `FaceRowBranch`. Bone drags go through `FaceEditorManager.beginGesture / writeGesture / endGesture / cancelGesture`;
  viewport: `FaceEditorGestureSystem` on the Face layer. The face editor always works on the Character Editor's active race, saves
  and reverts through it, and takes its preview gender and slider values from it. Gender starts live under the race's
  `genders`, the controls under its `appearance`.
- The preview look (gender, tones, sliders, blink, build, head) is put on any viewport's appearance by
  `CharacterEditorManager.applyLook` (`CharacterLookBranch`); never set it piece by piece in a viewport.
- Rotation and projection math shared by every viewport gesture: `CharacterPoseUtility`.

## Traps

- **Weighed meshes name the rig's bones.** A hair mesh weighed onto locks names `hair_*` bones; reverting or removing
  them from the rig without the meshes weighed onto them stops every mesh load (and the editor) with an unknown bone.
  Re-lay and re-weigh through the tools, rig first.
- **Colliders sit inside the skin.** A collider poking out of the body pushes hair off the surface at rest, and a hair
  joint inside a collider at rest is pushed out on the first step. Size them against the body's own vertices and the
  chains' rest joints; a collider a chain names must exist, or the rig refuses to load.
- **Spring settings are tuned per real time.** The editor preview under a slow renderer hits the chains with the
  pose's motion in large steps; judge them at the game's frame rate. A chain stepped longer than
  `RIG_SPRING_MAX_DELTA` rides the rest rigidly, by design.

- **One rig per race.** Every gender of a race must name the same rig (`RaceBuilder` refuses otherwise): its sliders,
  anchors, clips and every skinned buffer are laid out by those bones. Genders may differ in mesh, material and tree.
- **Model height is the default gender's.** `RaceData.getModelHeight` is the first gender's mesh height, the height
  every gender is scaled against, so a smaller body stays smaller.
- **Bones are structure.** Clips, masks, skinned buffers, appearances and live poses are all laid out by a rig's bones
  at load. `RigManager.updateRig` applies a rig in place only while its bone names, order and parents are unchanged
  (joint placement, bind rotation, size); adding, renaming, reparenting or mirroring bones is written to the rig and
  takes hold on the next start. Only bones added since the start (`CharacterRigBranch.canRename`) may be renamed or
  removed, because nothing loaded names them yet.
- **`throwException` halts the process** unless an isolation boundary is open. Editor actions, shortcuts and tab
  updates run inside one, which is why `CharacterRigBranch.apply` and `CharacterPreviewBranch.rebuild` can catch
  `InternalException`. Never call a builder that can refuse from outside an isolated path.
- **Re-read entries, never cache them.** A revert replaces an InfoManager document's root, so a held `ArpgObjectStruct`
  goes stale. Fetch the entry through `InfoManager.findEntry` each time.
- **Shape keys clear the history.** Entering or leaving a shape key clears the mesh's undo steps, since they hold the
  surface as it was being sculpted.
- **A mesh's morph count can change under a skinned buffer.** An added shape key widens the palette row;
  `SkinnedBufferInstance.clear` lays the row out again. Never size anything per instance from a morph count read once.
- **Sculpt works on an unrounded copy.** Positions are written rounded to the editor's precision; a stroke keeps its
  own float copy so small stamps accumulate. Topology and mirror pairs are fixed for the stroke's length.
- **A face drag shapes per unit.** A control's bone target is what one unit of its value does, so a drag divides the
  move, turn or growth it makes by the control's preview value; a control previewed near nothing is first taken to the
  far end of its range. A control must always drive a morph or a bone, or the builder refuses the template: its last
  bone is let go only while it has a morph, and its morph cleared only while it drives a bone.
- **Vertices move only in Bind.** A modelling layer struck in any other pose refuses sculpt strokes and transforms;
  the preview poses on the GPU and the mesh view skins on the CPU, but edits are written to the bind-pose file.
- **Part layers need rig parts.** A rig without `parts` leaves the Head, Ears, Hands, Feet, Eyes and Mouth layers
  showing nothing; name the bones there, not in code.
- **Quaternion poses, Euler keys.** Clips keep Euler-degree curves (as DCC tools author them); `AnimationStateHandle`
  turns every sampled turn into a unit quaternion (`RigMathUtility.toQuaternion`, the `Rz * Ry * Rx` order), blends
  brackets, cross-fades and override layers by slerp, multiplies additive layers on, and composes each bone as bind
  rotation, then shaping, then pose. For a bind turned about z alone (the A-pose arms) this equals the old Euler sum, so
  existing clips play unchanged. The editors key against the bind the same way
  (`CharacterPoseUtility.composeBindTurn` / `toKeyEuler`, nearest the key it replaces so keys never spin the long
  way); never add a key to the bind rotation.
