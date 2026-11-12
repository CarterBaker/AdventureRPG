# CharacterEditorPipeline

Where the character tools live, which method owns each operation, and the traps. Read it before working in the
Character Editor, the part editors (Hair, Hand, Foot, Eye, Mouth), the Face Editor, the Animation Editor, their docker
windows, races and their genders' models, or a character's appearance (morphs, face controls, zones, masks, overlays,
shaders). `.claude/CLAUDE.md` still
applies in full.

---

## Windows

Every character tool is split into windows, the way Krita splits its dockers. A viewport tab holds the status bar and
the viewport (the modelling tabs also their mode toolbar); saving, reverting, copying, deleting, undoing and
redoing are the editor's File and Edit menus' (`EditorSetting.FILE_MENU_CHARACTER`, `FILE_MENU_FACE`,
`FILE_MENU_ANIMATION`), shared by each tool's viewport and docker tabs; everything else is its own tab, opened from the editor's Characters menu and restored by
layouts like any other.

| Window | Context | Shows |
|---|---|---|
| Character Appearance | `CharacterAppearanceContext` | the live preview in the Appearance mode |
| Character Build | `CharacterBuildContext` | the live preview in the Build mode, its joints marked |
| Character Equipment | `CharacterEquipmentContext` | the live preview in the Equipment mode, its anchors marked |
| Character Mesh Editor | `CharacterMeshEditorContext` | modelling the whole character: the bind-pose preview, its overlay and the Mesh, Sculpt, Weights and Rig toolbar; a tool canvas |
| Hair Editor | `HairEditorContext` | modelling the worn hair, framed on the head, its card roots pinned; its Rig mode holds the hair's spring chains |
| Hand Editor | `HandEditorContext` | modelling the hands and finger bones, one side shown and framed, edits mirrored |
| Foot Editor | `FootEditorContext` | modelling the feet and toe bones, one side shown and framed, edits mirrored |
| Eye Editor | `EyeEditorContext` | modelling one eye socket and its lids on the head with the eye worn in it, edits mirrored |
| Mouth Editor | `MouthEditorContext` | modelling the lips, jaw and mouth's inside on the head with the teeth and tongue |
| Character Outliner | `CharacterOutlinerContext` | `CharacterEditorManager.buildListRows` |
| Face Editor | `FaceEditorContext` | the open character framed on its head, its face bones marked; the Character Face tool canvas |
| Face Outliner | `FaceOutlinerContext` | `FaceEditorManager.buildListRows` |
| Animation Editor | `AnimationEditorContext` | the posed character; the Animation Pose tool canvas while a clip plays on a character |
| Animation Outliner | `AnimationOutlinerContext` | `AnimationEditorManager.buildListRows` |
| Animation Timeline | `AnimationTimelineContext` | transport, summary track and a dope sheet lane per bone |
| Properties | `PropertiesContext` | the value rows of the character, face or animation tab worked in last |

- **One mode per look tab, one manager.** Character Appearance, Build and Equipment each keep to their one mode
  and share `CharacterLookContext` (abstract; each subclass only names its mode); the Character Mesh Editor keeps to
  the modes that take tools (`CharacterEditorMode.isModeling`). Each tab's `CharacterEditorModeSystem` remembers its
  own mode and draws its preview in it (`CharacterEditorPreviewSystem` reads the tab's mode, never the manager's);
  while a tab is the active window it hands its mode to `CharacterEditorManager.setMode`, so the Outliner and the Properties
  tab follow the tab worked in last. The viewport systems live in `editor.runtime.charactereditor` and every such tab
  creates them; only the mesh tab creates the mesh view, sculpt, rig and overlay systems and a `ToolCanvasSystem`,
  and its `runToolAction` adopts its mode first. Each tab names its starting mode, the modes it holds and its toolbar
  through `CharacterEditorWindowProvider`; their keys are `shortcuts/CharacterAppearance.arpg`,
  `CharacterBuild.arpg`, `CharacterEquipment.arpg` and `CharacterMeshEditor.arpg`.
- **The Appearance outliner is the player's creator.** In Appearance the Character Outliner lists what the game's
  creator offers the player: the gender, the features worn (Hair: the race default, none, or any style that fits),
  then every shape slider under its group in the order the race first lists each, each a ranged row
  (`CharacterViewRowStruct.ranged`) holding the value the game starts the previewed gender at, across the shape's
  own `min` to `max`: the gender preset's `shapes` value where it names the shape (`CharacterEditBranch.setGenderShape`),
  the race's `default` otherwise (`FaceEditorManager.setDefault`, the Face Editor's default row path), the preview's
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
  canvas's, run by `FaceEditorContext.runToolAction` through `FaceEditorManager.act`. Properties keeps values only.
- Every viewport (Character, Face, Animation, Animation Tree Editor) looks through the editor's `ViewCameraSystem`,
  framing `CharacterViewPoseSystem` (the Face Editor frames the head through `FaceEditorPreviewSystem`). A viewport
  system takes a click only when it acts on something (a vertex, a joint, the mesh under the brush) and claims it with
  `EditorInputSystem.claim`; any other left press orbits and a right press pans. Cancel clicks
  (`CHARACTER_VIEW_BUTTON_CANCEL`) are claimed too. Test `ViewCameraSystem.isDragging`, never a held button, to stay
  out of a camera drag.

## Modelling tabs and regions

- **One modelling tab, six regions.** `CharacterModelingContext` (abstract) is every modelling tab; the Character Mesh
  Editor, Hair, Hand, Foot, Eye and Mouth Editors only name their `CharacterEditorRegion` (through
  `CharacterEditorWindowProvider.getRegion`) and starting mode. Their keys are one file,
  `shortcuts/CharacterMeshEditor.arpg`, naming the base class: `ShortcutManager` finds a context's pressed shortcuts on
  its own class or the nearest class it extends.
- **A region is data, its bones the rig's.** `CharacterEditorRegion` names the worn feature it holds (Hair: `HAIR`,
  Eyes: `EYES`, Mouth: `TEETH`) and the `RigPart` whose bones it holds on the body with every bone below them (Hands,
  Feet, Eyes, Mouth). The bones come from the rig's own `parts` (`RigData.getPartBones`), never from names in code, so
  any creature's rig gets every part editor by naming its parts; a rig that names no such part leaves that editor
  empty. Hair also names its own bones by prefix (`hair_`, and the head they hang from). Bone membership is read only
  through `CharacterRegionBranch.holdsBone` / `holdsRigBone`. The active tab hands its region to
  `CharacterEditorManager.setRegion`, which keeps the part picked while the region holds it, else picks its body, else
  its feature. A region holding both lists only those two in the outliner's Parts (`holdsPartKey`), so the Eye Editor
  sculpts the socket on the head and the eye itself, and the Mouth Editor the lips and the teeth.
- **Sides.** A region whose every part bone has a twin (`_l`/`_r`) is sided (`CharacterRegionBranch.isSided`, cached
  per rig data): the outliner lists Side (`CharacterEditorSide`, kept per region in the manager), the tab frames the
  side's first part bone (`getRegionFrameBone`), shows and picks only that side (`isOnRegionSide`, `isBoneShown`,
  `isRigBoneShown`), and `isMirror` is held on, so every edit is made to the hidden twin as well.
- **Isolation.** `CharacterRegionIsolationSystem` fills the viewport's `AppearanceIsolationStruct`
  (`CharacterViewRenderSystem.getIsolation`): the body only where the region holds a part, drawn through a `SHOW`
  bone mask of the bones shown, and of the worn features only the region's own, masked too where sided. The mesh view
  takes faces hidden this way out of occlusion (`isFaceShown`). The Character Mesh Editor isolates nothing.
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
  painted over the skin, never geometry. Each gender's model places them (`genders.<g>.overlays`: the `styles` it
  offers, each a `name` and a `texture` in the character atlas, the style worn by `default`, the `projection` front,
  back or wrap, the box `center` and `size` on the bind-pose body, `mirror`, and a wrapped image's `repeat` per block;
  `RaceGenderStruct.getOverlay`), written into each draw as three texels, the tile that of the style worn
  (`SkinnedOverlayStruct.write`, `AppearanceHandle.getOverlayStyle`, `INDEX_NOT_FOUND` for the model's default). The
  shader lays them on the position before morphs (`v_bindPosition`), so they stay on the same skin however the body
  is shaped or posed, tinted by the overlay's `SkinnedColor` (`BROWS`, `NIPPLES`, `BODY_HAIR`) and shown as far as
  its `SkinnedDetail`. Saves keep a chosen style by name (`overlay_styles`); the creator lists a style row and an
  amount per overlay, and the Character Appearance outliner's Marks rows set the previewed gender's `default`.
  **Not yet industry standard:** the box projection stands in for UV-space layers until the bodies are unwrapped;
  the planned replacement is in the handoff the owner holds.
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
  foot's ball, and carry the same morphs (blinks, body sliders, `ear_point`). Each head is a smooth grid stitched to
  the body's neck ring, with eye sockets whose lids close over the eye (the upper lid edge in the lash zone), a mouth
  cavity behind the lips (mouth zone) and ears as their own shells; the meshes are exactly symmetric. Both genders
  wear `features/humanoid/eyes/HumanoidEyes` (`HumanoidEyesMesh`, drawn from the eye zone's raw UVs with
  `CharacterEyeMaterial`) and `features/humanoid/teeth/HumanoidTeeth` (`HumanoidTeethMesh`: both rows of teeth and
  the tongue, `CharacterTeethMaterial`). `HumanoidRig` names its `parts` (eyes, mouth, hands, feet) and seven
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
- **Hair tools.** In the Hair Editor, Rig mode's **Rig Hair Locks** (`CharacterRigBranch.addHairLocks`) finds the
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
  first. The chains are bones, so save the rig and restart between the two. The selected bone's chain lists its four
  settings in the Properties tab (`CharacterRigRowBranch.addSpringRows`), stepped and typed live. **Preview Springs**
  (Rig and Weights) makes every modelling tab play the pose chosen (Idle at rest) with the springs simulated, until
  pressed again. The Animation Editor never lists, picks or keys a spring bone (`AnimationEditorManager.isKeyedBone`).
- **Assets.** `HumanoidRig` carries 14 hair chains (`hair_1` to `hair_14`, five joints each) laid by Rig Hair Locks on
  `FemaleLongHairMesh`; `FemaleLongHairMesh` and `FemaleMediumHairMesh` are weighed onto them by Weigh Onto Locks.
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
  viewport: `FaceEditorGestureSystem`. The Face Editor always works on the Character Editor's active race, saves
  and reverts through it, and takes its preview gender and slider values from it. Gender starts live under the race's
  `genders`, the controls under its `appearance`.
- The preview look (gender, tones, sliders, blink, build, head) is put on any viewport's appearance by
  `CharacterEditorManager.applyLook` (`CharacterLookBranch`); never set it piece by piece in a viewport.
- Rotation and projection math shared by every viewport gesture: `CharacterPoseUtility`.

## Traps

- **Weighed meshes name the rig's bones.** A hair mesh weighed onto locks names `hair_*` bones; reverting or removing
  them from the rig without the meshes weighed onto them stops every mesh load (and the editor) with an unknown bone.
  Re-lay and re-weigh through the tools, rig first.
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
- **Vertices move only in Bind.** A modelling tab struck in any other pose refuses sculpt strokes and transforms;
  the preview poses on the GPU and the mesh view skins on the CPU, but edits are written to the bind-pose file.
- **Part editors need rig parts.** A rig without `parts` leaves the Hand, Foot, Eye and Mouth Editors showing
  nothing; name the bones there, not in code.
- **Euler angles.** Bones compose `Rz * Ry * Rx` from degrees and a clip's rotation is added to the bind rotation.
  Read a turn back with `CharacterPoseUtility.toEuler` against the angle it replaces, so keys never spin the long way.
