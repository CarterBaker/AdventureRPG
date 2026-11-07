# CharacterEditorPipeline

Where the character tools live, which method owns each operation, and the traps. Read it before working in the
Character Editor, the Face Editor, the Animation Editor, the Entity Editor, their docker windows, races and their
genders' models, or a character's appearance (morphs, face controls, zones, masks, shaders). `.claude/CLAUDE.md` still
applies in full.

---

## Windows

Every character tool is split into windows, the way Krita splits its dockers. A viewport tab holds the status bar and
the viewport (the Character Mesh Editor also its mode toolbar); saving, reverting, copying, deleting, undoing and
redoing are the editor's File and Edit menus' (`EditorSetting.FILE_MENU_CHARACTER`, `FILE_MENU_FACE`,
`FILE_MENU_ANIMATION`), shared by each tool's viewport and docker tabs; everything else is its own tab, opened from the editor's Characters menu and restored by
layouts like any other.

| Window | Context | Shows |
|---|---|---|
| Character Appearance | `CharacterAppearanceContext` | the live preview in the Appearance mode |
| Character Build | `CharacterBuildContext` | the live preview in the Build mode, its joints marked |
| Character Equipment | `CharacterEquipmentContext` | the live preview in the Equipment mode, its anchors marked |
| Character Mesh Editor | `CharacterMeshEditorContext` | modelling: the bind-pose preview, its overlay and the Mesh, Sculpt, Weights and Rig toolbar; a tool canvas |
| Character Outliner | `CharacterOutlinerContext` | `CharacterEditorManager.buildListRows` |
| Entity Editor | `EntityEditorContext` | `EntityEditorManager.buildListRows`: entities to make, delete and pick, the picked one's race, its rig and genders, the previewed gender's model, Save and Revert; the entity's other fields in Properties (`INFO`) |
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
  while a tab is focused it hands its mode to `CharacterEditorManager.setMode`, so the Outliner and the Properties
  tab follow the tab worked in last. The viewport systems live in `editor.runtime.charactereditor` and every such tab
  creates them; only the mesh tab creates the mesh view, sculpt, rig and overlay systems and a `ToolCanvasSystem`,
  and its `runToolAction` adopts its mode first. Each tab names its starting mode, the modes it holds and its toolbar
  through `CharacterEditorWindowProvider`; their keys are `shortcuts/CharacterAppearance.arpg`,
  `CharacterBuild.arpg`, `CharacterEquipment.arpg` and `CharacterMeshEditor.arpg`.
- **Values live in the shared Properties tab.** The character, face and animation viewport contexts implement
  `PropertiesProvider` (content `ROWS`) and `CharacterViewRowSource` (their manager's `buildPropertyRows`) and create a
  `PropertiesSourceSystem`; the Properties tab (`RowPropertiesSystem`) shows the rows of the one focused last and hands
  its sliders' steps back to it. There are no per-tool Properties windows.
- **Number rows have a slider.** `CharacterViewPanelSystem` refills a panel only when its rows change by kind, key,
  label or selection; otherwise it sets the moved values in place, so a dragged slider keeps its row. A slider spans
  `EditorSetting.CHARACTER_VIEW_SLIDER_STEPS` of the value's own steps centred where the value stood when the row was
  listed, and hands the tool the steps it moves through the context's `CharacterViewStepTarget.stepRow`, the path the
  step buttons take; the value box still opens for typing. A number row template carries the track and knob ids in
  its `CharacterViewTemplateStruct`.
- An outliner docker context implements `CharacterViewRowSource` and creates `CharacterViewPanelSystem`,
  `CharacterViewDockSystem` and `ToolCanvasSystem`; the dock system opens `menus/editor/CharacterDock.arpg` and
  refills it only when the source's revision moves. Row templates stay in the tool's own menu
  (`EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES`, `FACE_EDITOR_ROW_TEMPLATES`, `ANIMATION_EDITOR_ROW_TEMPLATES`) so
  their buttons route to the tool's branch. A tool without a text row template must never list a `TEXT` row.
- Tools are the Tools window's (`ToolCanvas.CHARACTER_MESH`, `CHARACTER_SCULPT`, `CHARACTER_WEIGHTS`,
  `CHARACTER_RIG`, `CHARACTER_FACE`, `ANIMATION_POSE`); brushes the Brushes window's. Add a tool to a canvas in
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

## Modes

`CharacterEditorMode` carries its behaviour as data: its tool canvas, whether it edits a worn mesh, whether it holds
the bind pose, whether it draws vertices, and its status hint. Check those flags, never `mode == X`, except where a
mode's own branch is chosen (`SCULPT` strokes the shape, `RIG` undoes the rig).

## Characters

Every entity template names its race (`race`, required), every race lists the genders it may be made as (`genders`, at
least one, the first the default), and every gender names the character model it requires: its `mesh`, the `rig` that
mesh is skinned to, its `material` and its `animation_tree`, with the `shapes` and `features` it starts with. The race
also holds the `equipment` anchors and the optional `appearance` (sliders, colors, features, build). The game reads
the race through `EntityData.getRaceData()`; `RaceManager` (`entitypipeline.racemanager`) loads races with
`RaceBuilder` and `AppearanceBuilder`. A character is a race's rigged body mesh per gender with its whole head
modelled in, plus worn feature meshes (`FeatureSlot`: hair). Nothing is a texture swap.

- **The character tools edit races.** A `CharacterDocumentInstance` is one race (`INFO_SCHEMA_RACES`), its preview a
  `RaceHandle` from `RaceManager.buildPreviewRace`. Picking a race opens it; picking an entity, in the Hierarchy or the
  Entity Editor, opens its race through `CharacterEditorManager.openRace`, the one path that opens a race by name.
- **The Entity Editor** (`editor.bootstrap.entityeditorpipeline`) makes, deletes and picks entities; picking one
  selects it through `InfoManager.selectEntry`, so the Properties tab edits its other fields and the character tabs
  open its race. Picking a gender row calls `CharacterEditorManager.selectPreviewGender`, so every character tab wears
  that gender's model, and the model rows edit that gender. Every write goes through `EntityEditBranch` and
  InfoManager; every choice is an `EntityEditorChoice` (its field, scope, schema or sibling folder, rig binding), so a
  new one is a new constant, not new code. A race's rig is written on every gender at once; Add Gender copies the
  previewed gender's model so the race still builds; Copy Race goes through `CharacterEditorManager.copyRace` and
  makes the entity that race; Save writes the race before the entity. A changed `race` field opens the new race.
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
  `HumanoidFemaleMesh` share the rig with its
  15 face bones (jaw, chin, lips, mouth corners, nose, cheeks, eyes, brows, ears), carry the same morphs (blinks, body
  sliders, `ear_point`) and draw their eyes from the eye zone's raw UVs, brows and lash line as their own strips.
  They are generated (catalogued); a rigged mesh may hold at most `RIGGED_MESH_VERT_LIMIT` corners. Each hand is
  one bone: hanging at the side its palm faces the thigh with the thumb forward (+z), so `hand_l` is a left hand at
  +x; keep that chirality on any hand you model or mirror.
- **Features are the gender's.** The Character Outliner's Features rows (Appearance mode) read and write the previewed
  gender's `genders.<gender>.features`, the features it is made with; a gender naming none wears the race's
  `appearance.features` default, shown with `CHARACTER_EDITOR_FEATURE_DEFAULT_SUFFIX`, and None removes the gender's
  own so the default returns.
- **Hair is cards.** A hair mesh is a scalp cap and layered cards two quads wide, textured with the strand tile
  `characters/standard/Hair` (its alpha cuts the locks out); each card's raw UV runs across it in u and from root
  (0) to tip (1) in v. The character vertex shader sways hair-zone vertices in the wind by v (`CharacterHair.glsl`),
  so keep v running root to tip on any card you add, and keep the root at the scalp.
- **Toon lighting.** Characters write a smooth normal (`GBufferNormal.glsl`); the lighting pass lights those pixels
  in toon tones with a rim (`ToonLighting.glsl`), and the editor viewport's character shader uses the same function.

## Who owns what

- Sculpt strokes: `CharacterShapeBranch` (begin, stamp, end). The viewport fills one `CharacterStampStruct` per stamp
  in `CharacterEditorSculptSystem.placeStamp`.
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
- **Euler angles.** Bones compose `Rz * Ry * Rx` from degrees and a clip's rotation is added to the bind rotation.
  Read a turn back with `CharacterPoseUtility.toEuler` against the angle it replaces, so keys never spin the long way.
