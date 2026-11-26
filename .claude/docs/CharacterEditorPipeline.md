# CharacterEditorPipeline

Where the character tools live, which method owns each operation, and the traps. Read it before working in the character
tool tabs (Appearance, Equipment, Hair, Mesh, Topology, Sculpt, Weights, Paint, Rig, UV), the Materials tab, the UV
Editor, the Animation Editor, their docker windows, races and their genders' models, or a character's appearance
(morphs, sliders, bone controls, zones, masks, overlays, shaders). `.claude/CLAUDE.md` still applies in full.

---

## Windows

Every character tool is split into windows, the way Krita splits its dockers: every part of making a character is its
own tool tab, laid out however the owner likes, each working the one character through the shared state (see
`.claude/docs/Editor.md`). A viewport tab holds the status bar and the viewport; saving, reverting, copying, deleting,
undoing and redoing are the editor's File and Edit menus' (`EditorSetting.FILE_MENU_CHARACTER`,
`FILE_MENU_ANIMATION`), shared by each tool's viewport and docker tabs; everything else is its own tab, opened
from the editor's Entities menu (its Entity section: Appearance, Equipment and Hair; the Animation section), the Mesh
menu (its Mesh section: Mesh, Topology, Sculpt, Paint, UV and the UV Editor; its Rigging section: Rig and Weights), the
Tools menu (Properties; Tools, Brushes, Stamps and the Palette in its Canvas section), and restored by
layouts like any other. Whatever the owner sees says
Entity, never Character (the Animation Editor's row, the command console's Entity group,
`assets/commands/Entity.arpg`); the code keeps its `Character*` names.

| Window | Context | Shows |
|---|---|---|
| Character Appearance, Equipment | `CharacterAppearanceContext`, `CharacterEquipmentContext` | the live preview in its look mode, no layers |
| Character Mesh, Topology, Sculpt, Weights, Rig, UV | `CharacterMeshContext`, `CharacterTopologyContext`, `CharacterSculptContext`, `CharacterWeightsContext`, `CharacterRigContext`, `CharacterUVContext` | the region picked in the Layers tab framed and shown alone, its tool canvas |
| Hair | `CharacterHairContext` | the whole character with the groom open drawn over it, the Hair canvas; its own File and Edit table (`FILE_MENU_HAIR`) |
| Materials | `editor.runtime.materials.MaterialsContext` (Materials menu) | the workspace material open and every texture of every baked pack, the one strands grow in, the Hair Brush paints and the Paint tab's Brush and Fill lay chosen (`MaterialsManager`, see `.claude/docs/Editor.md`) |
| Material Viewer | `editor.runtime.materialviewer.MaterialViewerContext` (Materials menu) | the material chosen on the shape the Shape option names, lit by the light its Light tool turns |
| UV Editor | `editor.runtime.uveditor.UVEditorContext` | the UVs of the faces selected, over the tile they are textured with (see UV below) |
| Character Paint | `CharacterPaintContext` | the whole character, the Paint canvas; the paint layers of the map painted in the Layers tab |
| Properties | `editor.runtime.properties.PropertiesContext` (Tools menu) | the settings of the window worked in last: the preview's choices and Appearance's rows (`CharacterEditorManager.buildSettingRows`), the values of what is picked (`buildPropertyRows`), the clip's settings (`AnimationEditorManager.buildClipRows`); the material's maps and parameters for the Materials and Material Viewer tabs (`MaterialsManager.buildRows`); the colour's rows for the Palette |
| Palette | `editor.runtime.palette.PaletteContext` | what the mode picks from (`CharacterEditorManager.buildPickRows`, `CharacterEditorMode.getPaletteContent`: Equipment's gear and anchors, the modelling tabs' parts, Mesh, Weights and Rig's bones; `AnimationEditorManager.buildPickRows`, the clip's bones) and, for Paint and UV, the paint colour, a hue ring with a saturation and brightness triangle turned to the hue (`ColorWheelSystem`), over the maps of the texture painted while a character tab or the UV Editor was worked in last (`MapsProvider`, `CharacterEditorManager.buildMapRows`): the one painted selected, its settings, Generate and Clear; the colour's Hue, Saturation, Brightness and Red, Green, Blue ranged rows (`CharacterEditorManager.buildColorRows`) are the Properties tab's while it was worked in last |
| Animation Editor | `AnimationEditorContext` | the character chosen (the player at first) posed at the recorder Timeline's playhead; the Animation Pose tool canvas while a clip plays on a character |
| Properties | `PropertiesContext` | the value rows of the character or animation tab worked in last |

- **One tab per tool.** Every tool tab extends `CharacterToolContext` and names its `CharacterEditorMode` (`getMode`)
  and title (`getTabTitle`, `EditorSetting.TAB_TITLE_CHARACTER_*`: the tool's own name, Mesh or Sculpt, never
  "Character Mesh"); each opens from its own Characters menu button
  (`EditorBranch.openCharacter<Tool>`, `TabManager.openCharacter<Tool>`). Add a tool as a mode with its flags, a
  context naming it and a menu button, never as a mode switch inside a tab. `CharacterEditorModeSystem` holds the
  tab's mode, read from its context, and its region (`getRegion`).
- **Layers.** A tab lists layers only where its mode `listsLayers`, and creates its `LayersSourceSystem` only then:
  a mode that `takesRegions` (Mesh, Sculpt, Weights, Rig, UV) lists the Body (`CharacterRegionStruct.WHOLE`) and every
  region nested under it by `getParent`, keyed by region key; Paint lists the paint layers of the map painted, top
  first, keyed by number, each above the base with an eye (`CharacterPaintCanvasStruct.isLayerVisible`; a hidden layer
  stays out of the composite, so Save writes only what is shown), as Substance Painter and Krita list theirs. The look
  tabs list none. Add a region as a rig part, never in code.
- **Shared choices.** The region picked is one choice for every tab that takes regions
  (`CharacterEditorManager.pickRegion` / `getPickedRegion`, kept by key and found again whenever the regions are made
  again); the look tabs and Paint work on the whole character. A face is a region like any other, the rig's part that
  takes it, so every tab reaches it through the Layers tab. The bone selected is one choice too: the weights' bone
  (`CharacterEditorManager.selectBone`) and the rig's (`CharacterRigBranch.pickBone`) follow each other by name
  (`followRigBone`, `followBone`), so a bone selected in the Rig tab is the bone the Weights tab paints. The vertex
  selection lives in the part's mesh document, shared by every tab.
- **One window, one manager.** Each tab draws its preview in its own mode (`CharacterEditorPreviewSystem` reads the
  window's mode, never the manager's); while it is the active window, and when a layer is picked for it, it hands its
  region and mode to `CharacterEditorManager.setRegion` / `setMode`, so the Properties tab follows the window
  worked in last. The context creates every viewport system; the mesh view, sculpt, rig, paint and overlay
  systems act only in the modes their flags name. `CharacterToolContext` answers its tool canvas, tool actions,
  Properties rows, templates and File and Edit table for its mode, all the character editor's. The keys of every tool
  tab are one file, `shortcuts/CharacterEditor.arpg`, naming `CharacterToolContext` (a shortcut file naming a base
  context keys every tab built on it, `ShortcutManager.findShortcuts`) and kept to their tools by `canvases`; Undo, Redo
  and Save run the File menu's action (`FileMenuBranch.run`).

- **The Appearance rows are the player's creator.** In Appearance the Properties tab lists first everything the game's creator
  offers the player: whether the race is Playable (`playable`, `CharacterEditBranch.setPlayable`), the gender, the
  features worn (Hair: the race default, none, or any style that fits), under each
  worn feature that offers variants the variant the previewed gender starts it in (eyes, teeth: the feature's own
  default, then each variant; `CharacterPropertyBranch.cycleFeatureVariant`, written as
  `genders.<g>.feature_variants.<slot>` by `CharacterEditBranch.setFeatureVariant`, cleared when the gender's feature in
  that slot changes), the marks (each overlay's style, then its Amount, the overlay's `SkinnedDetail`), every color
  with the race's tones, the detail amounts no mark shows, every slider (below) and the body build of the bone selected
  (Thin, Heavy, Girth). Each feature, mark, color and detail carries a Player row, the flag in the race's
  `appearance.player` block (`colors`, `details`, `overlays`, `features`; true unless it says otherwise,
  `CharacterEditBranch.setOffered`, read through `AppearanceData.isOffered`); a mark's amount is offered with the mark,
  never flagged on its own (`SkinnedOverlay.findByDetail`). Appearance lists no pose: a mode names the pose
  it stands in (`CharacterEditorMode.getStandingPose`, Appearance idles as the creator shows the player) or lets its
  rows pick one (`picksPose`: Equipment, Weights). Blink, Weight and Head, preview controls, follow them as the
  values.
- **The game's creator is laid out by the race.** `appearance.creator` lists the creator's look tabs, each with its
  sections, each listing options by `kind` (`AppearanceOptionKind`: race, gender, build, head, color, detail, overlay,
  feature, sliders) and `name` (the member's lower-case enum name, or the slider group);
  `AppearanceBuilder.parseCreator` makes `AppearanceTabStruct`s, and any offered member or slider group the layout
  leaves out gains a section of its kind in the last tab, so nothing offered is lost. A race without `creator` gets a
  Body tab (race, gender, build, head shape) and a Look tab holding the rest. The creator
  (`CreatorAppearanceBranch.populateLook`) lists the race's tabs, then Attributes and Profession, and in each section
  only what this character has to offer (an overlay only where its gender's model places it; a section of several
  options heads each color by name). Race is the race's `playable` flag: the Race row lists the playable races
  (`RaceManager.collectPlayableRaces`) only when there are several, and picking one makes the player that race in place
  (`EntityInstance.setRaceData`, its size carried over by the races' model heights, `getRaceScale`); saves keep the race
  and gender by name.
- **Sliders are derived, never listed by hand.** `CharacterSliderBranch` owns every slider row. The race's sliders are
  whatever its rig and bodies offer (see Appearance below): a bone slider per channel the Rig tab ticks on a bone, a
  control per control of the rig's bone controls, a shape key per morph no expression plays. Each is listed under its
  group in the order the race's `appearance.sliders` names it, the rest after, as a ranged row
  (`CharacterViewRowStruct.ranged`) holding the value the game starts the previewed gender at (the gender preset's
  `shapes` value where it names the slider, `CharacterEditBranch.setGenderShape`; the race's `default` otherwise,
  `CharacterEditBranch.setSliderField`), then a pair row for the gender's Min and Max
  (`genders.<g>.ranges.<slider>`, read only through `AppearanceData.getShapeMin(genderID, i)` / `getShapeMax`, which
  fall back to the slider's own; an end typed past the slider's own range widens it, `setRangeEnd`), then whether the
  player is offered it (`player`, `setSliderPlayer`; one the player is not offered, NPC variation skips too, and it
  stands at its default, moved only by the controls that drive it: the converted Human's bone sliders are all so).
  Adding or removing a bone, a tick or a morph changes the list the next time the race is built; nothing else to edit.
- **Controls.** A control is one slider that moves several bone sliders, the Set Driven Key of Maya and the driver of
  Blender, kept per rig in `assets/bonecontrols/<Rig>.arpg` (`BoneControlManager`, `entitypipeline.bonecontrolmanager`;
  schema `BoneControls`). Each bone slider row carries Pick For Control; Add Control at the foot of the list makes a
  control over the two or more picked (`CharacterControlBranch.addControl`, named `control_<n>`, in the default group
  until the race's `sliders` gives it one; rename and regroup it in the Info Panel). Under a control each bone slider it
  drives lists an adjust slider (where the bone stands on the preview now: its own value plus every control's push, held
  to its range), its Increase choice (on by default: the control raises the bone slider; off, it lowers it) and the bone
  slider's own Min and Max, the same values as the bone slider's rows. Dragging the adjust slider keys the drive's
  hidden curve where the control stands (`CharacterSliderBranch.adjustDrive` solves the share,
  `CharacterControlBranch.setKey` keeps at most `BONE_CONTROL_MAX_KEYS`, moving the key within
  `CHARACTER_CONTROL_KEY_REACH` or else the nearest); the curve is a monotone cubic through (0, 0), the keys and (1, 1)
  (`BoneControlCurveUtility`, Fritsch–Carlson), so it never overshoots; there is no curve view, the face shows it. A
  push is measured from control value 0 and scaled by the bone slider's own range, added on top of the bone slider's
  value and held within its range, so a key cannot be set at zero. Every change, here or in the Info Panel, goes to
  `BoneControlManager.updateBoneControls` and the preview is built again
  (`CharacterEditorManager.rebuildActivePreview`); Save and Revert of the character take the rig's controls document
  with them.
- **Values live in the shared Properties tab.** The character and animation viewport contexts implement
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
- **Two shared tabs, never a docker per tool.** What a tool picks from is the Palette tab's and the settings of what is
  picked the Properties tab's (see `.claude/docs/Editor.md`, The Palette tab): a viewport implements
  `PropertiesProvider` with `PropertiesContent.ROWS` and `CharacterViewRowSource`, its `buildRows` listing the values
  of what is picked and its own settings, and `PaletteRowSource`, its `buildPaletteRows` listing what it picks from,
  each under a revision that moves with what it lists. `RowPropertiesSystem` fills the tab through `CharacterViewPanelSystem` only when that revision moves, and
  the tab's File and Edit menus act on the tool whose rows it shows. Row templates stay in the tool's own menu
  (`EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES`, `ANIMATION_EDITOR_ROW_TEMPLATES`) so
  their buttons route to the tool's branch. A tool without a text row template must never list a `TEXT` row.
- Tools are the Tools window's (`ToolCanvas.CHARACTER_MESH`, `CHARACTER_TOPOLOGY`, `CHARACTER_SCULPT`,
  `CHARACTER_WEIGHTS`, `CHARACTER_RIG`, `CHARACTER_UV`, `CHARACTER_HAIR`, `UV_EDITOR`, `ANIMATION_POSE`); brushes the
  Brushes window's. A tool strokes the
  brush where the canvas sizes one and the tool carries Size (`ToolCanvas.strokes`), so on the mesh and the weights Select sweeps the
  brush over vertices, each selected as much as the tip covers it (a hard brush whole, a soft one fading at its edge;
  Shift adds, Ctrl takes away, a press where the brush covers nothing still clears or boxes). Move, Rotate, Scale and
  the numeric transforms carry each vertex as far as it is selected, and Assign, Add, Subtract and Smooth weights reach
  it as far. Add a tool to a canvas in `ToolCanvas`, never as a panel button. The mesh's one-off operations (transform,
  geometry, weights, selection) are the canvas's action groups (`ToolActionGroupStruct`), shown in the Tools window
  under the tools and run by `CharacterToolContext.runToolAction` through `CharacterEditorManager.act`, the same key
  their shortcuts carry. Properties keeps values only.
- Every viewport (Character, Animation, Animation Tree Editor) looks through the editor's `ViewCameraSystem`
  (zoomed in as far as it goes, the wheel dollies the pivot forward, and the near plane follows the distance, so close
  detail is never clipped),
  framing `CharacterViewPoseSystem` (every character tool tab frames its region through `CharacterRegionFrameSystem`).
  **Stacked modelling tabs share one view.** Every mode that takes tools (`CharacterEditorMode.sharesView`: Mesh, Sculpt,
  Weights, Paint, Rig and UV) frames the region picked (`CharacterEditorModeSystem.getFramedRegion`, Paint's frame
  included though it paints the whole character) and names `EditorSetting.VIEW_CAMERA_SHARE_CHARACTER` as its
  `ViewCameraShareProvider` kind, so the modelling tabs stacked in one dock look through one camera: switching between
  them keeps the same part in sight, while modelling tabs in separate docks each keep their own view. Appearance and
  Equipment name none and keep a view of their own on the whole character.
  A viewport
  system takes a click only when it acts on something (a vertex, a joint, the mesh under the brush) and claims it with
  `EditorInputSystem.claim`; any other left press orbits and a right press pans. Cancel clicks
  (`CHARACTER_VIEW_BUTTON_CANCEL`) are claimed too. Test `ViewCameraSystem.isDragging`, never a held button, to stay
  out of a camera drag.

## Regions

- **Regions are the rig's parts.** A rig's `parts` (`RigPartStruct`, a list in the rig file, the way a rig groups its
  bones into collections) each name a `name`, the `bones` they hold with every bone below them, and optionally the worn
  slot they `holds` (eyes, teeth), the worn slots they `shows` (hair, eyes, teeth) and whether the `face` is shaped
  there; `RigBuilder` refuses an unknown bone, an empty part, a repeated name and the editor's own names (`whole`,
  `hair`). `CharacterRegionBranch.resolveRegions` makes the regions (`CharacterRegionStruct`) once per rig data: the
  Body, one per part in the rig's order (labelled by `LabelUtility.toWords`, sided when every bone has a `_l`/`_r` twin,
  listed under the part holding the nearest bone above its own), then Hair (the worn hair, its roots pinned, its own
  bones named by prefix, `hair_`, and the head they hang from, listed under the part that shows hair). Any creature's
  rig gets its layers by naming its parts; the Rig tab's **Add Layer From Bone** (`CharacterRigBranch.addPart`) makes a
  part of the selected bone and its twin, named after the bone without its side, and **Remove Layer** drops the part
  holding it; the rig applies live, so the Layers tab follows at once. Bone membership is read only through
  `CharacterRegionBranch.holdsBone` / `holdsRigBone`. The active window hands its region to
  `CharacterEditorManager.setRegion`, which keeps the part picked while the region holds it, else picks its body, else
  its feature. A region holding both lists only those two in the Properties tab's Parts (`holdsPartKey`), so the Eyes layers
  sculpt the socket on the head and the eye itself, and the Mouth layers the lips and the teeth.
- **Sides.** A region whose every part bone has a twin (`_l`/`_r`) is sided (`CharacterRegionBranch.isSided`, cached
  per rig data): the Properties tab lists Side (`CharacterEditorSide`, kept per region in the manager), the window frames the
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
  `CHARACTER_RIG_BALL_SHARE` of its length. The Rig tab's **Rig Digits** lays a chain of `CHARACTER_RIG_DIGIT_SHARES`
  joints on each (`CharacterRigBranch.addDigits`, named `<digit>_<n>_<side>`, a toe `toe_<side>`); the Weights tab's
  **Weigh Onto Digits** (`CharacterWeightBranch.weighDigits`) then hands each vertex of the hand or foot bone to them,
  a digit's own vertices wholly, blended across each joint, the palm near each digit's root partly. The digits are
  added bones, so they apply live and the two run one after the other.

## Modes

`CharacterEditorMode` carries its behaviour as data: its tool canvas, whether it edits a worn mesh, whether it holds
the bind pose, whether it draws vertices, and its status hint. Check those flags, never `mode == X`, except where a
mode's own branch is chosen (`SCULPT` strokes the shape, `RIG` undoes the rig). `editsVertices` is every mesh mode but
Paint, the modes whose tools select and shape vertices (`CharacterEditorSculptSystem` acts only there); `placesParts`
is Sculpt, whose Brush places the part chosen in the Stamps tab instead of stroking
(`CharacterEditorManager.armsStamp`; see Stamps below).

- **Poses.** Every mode that picks a pose (Equipment, Mesh, Sculpt, Weights) lists one Pose row, stepped by
  `CharacterEditorManager.stepPose` through Bind (the default: the mesh exactly as its file holds it in a mesh mode),
  each clip the rig names in `poses` (struck still at its start, `CharacterEditorPreviewSystem.strikeClip`, labelled
  by `CharacterViewUtility.toResourceLabel`), then Idle, Walk and Run played. Rig holds the bind pose. Grab, Rotate
  and Scale work in any pose, on the cage as the view shows it (Maya's and Blender's on-cage editing of a skinned mesh):
  `CharacterEditorSculptSystem` hands `CharacterEditorManager.beginTransform` the pose's skinning matrices
  (`CharacterEditorMeshViewSystem.getSkinningMatrices`) while `isPosed`, and `CharacterSculptBranch.holdVertex` keeps
  each held vertex's posed start and the inverse of its blended matrix B = Σ wᵢ Mᵢ (`resolveBlend`; affine under
  linear blend skinning, so exact); the pivot is the weighted centre of the posed starts, every move is made posed and
  written back to the bind pose through B⁻¹ (`toBindPose`) before rounding, and twins are then placed mirrored in the
  bind pose, so symmetry holds whatever the pose. Numeric transforms (typed Move, Size, Rotate) stay in the bind pose.
  Sculpt strokes work only in Bind: one struck posed is refused with `CHARACTER_EDITOR_MESSAGE_POSED`; selecting and
  painting weights work in any pose, the mesh view skinning every vertex by it. A pose is a
  plain clip on the rig (`animations/humanoid/poses/`); add one by naming it in the rig's `poses`.
- **Mirror pairs.** `CharacterSelectionBranch.resolveMirror` pairs each vertex with the one at its mirrored position
  on the decimal grid, then pairs every vertex left without a twin with the nearest one likewise left across x within
  `CHARACTER_MESH_MIRROR_REACH` (`pairNearest`, binned by reach-sized cells), so a mesh not modelled exactly
  symmetric still mirrors every stroke. Vertices standing on one point (an extrusion or a copy before it moves) pair
  in index order with those on the mirrored point, so an original pairs with the original and a copy with the copy.
  `CharacterViewUtility.round` rounds half away from zero, so a point and its mirror always round alike; every new
  vertex a tool places from corners (Topology, Subdivide) sums them in double, so a face and its mirror, whose corners
  run the other way round, find the same point. Every mesh action honours the canvas's Mirror switch: Extrude,
  Duplicate, Subdivide, Delete and Set Face Texture take the selection's twins too (Extrude pulls a region reaching
  the centre plane straight out of it), Make Face makes the twin face wound back, and Merge joins the twins at the
  mirrored point, or both sets as one on the plane where they meet.
- **Symmetrize** (Blender's Mesh > Symmetrize, Maya's Symmetrize): `CharacterSymmetryBranch.symmetrize(document,
  positiveToNegative)` is the one operation that makes a mesh exactly symmetric, the Mesh canvas's two Symmetrize
  actions (+X to -X, -X to +X) and the Command Console, whose view lists the commands whose `target` is `character`
  while a character tab was worked in after any Dev window (`CommandManager.resolveViewTarget`; the Entity group,
  `assets/commands/Entity.arpg`: Mirror Body and Mirror Hair, Left onto Right or Right onto Left,
  left being +x, run by `CharacterCommandBranch` on the open character's body or worn hair through
  `CharacterEditorManager.executeCommand`) both calling it. It always mirrors, whatever the canvas's Mirror switch. It never moves one side's vertices onto the other's: the side given up is removed and the side kept
  is mirrored and welded on x = 0, so every vertex gets an exact twin and every face a twin wound back. A face crossing
  the plane along two edges is cut there (`CharacterTopologyBranch.addBetween`, the cut shared by both faces beside the
  edge); one crossing any other way first has the corners on the side it holds fewer of laid on the plane. Twins take
  their side's weights with sided bones swapped (`CharacterWeightBranch.copyMirrored`; an unsided bone off the plane,
  a hair chain, swaps with the unsided bone whose joint is nearest its mirror within `CHARACTER_SYMMETRY_BONE_REACH`,
  and the status line counts those left without one), so the rig and every clip stay as they are; a `_l`/`_r` shape
  key swaps with its twin and the rest mirror; vertices on the plane take the even blend of their weights and deltas
  and their mirror's (`symmetrizeWeights`); masks, seams, pins, textures and zones carry over. UV islands stay whole,
  fitted from each kept face's old twin face (the face on the side removed over its corners' old twins,
  `CharacterSelectionBranch.resolveMirror` taken before the edit): an island that crossed the plane is reflected
  across the line fitted to its old pairs, its centre corners within `CHARACTER_SYMMETRY_AXIS_WELD` laid on it; any
  other is laid where its old twin island stood, by a glide reflection or, for a layout that shared one, a move,
  whichever fits; with no old twin it is mirrored in place and counted. One undo step, refused on a shape key and past
  the mesh limit. Run on its own result it changes nothing.
- **Canvas options.** Mirror, X-Ray, Wireframe and Bones are `ToolOption`s marked canvas-wide (`isCanvasWide`): every
  tool on a canvas that honours one lists it under its own options, last, in the Tools panel. Mirror (on by default) is
  honoured by the mesh, sculpt, weights, paint and rig canvases, X-Ray by the mesh and weights, Wireframe (the edges of
  the part worked drawn over it by `CharacterEditorOverlaySystem`, depth tested unless X-Ray; on by default on the mesh,
  weights and UV canvases, off on sculpt and paint) by the mesh, sculpt, weights, paint and UV.
  `CharacterEditorManager.isMirror` / `isXray` / `isWireframe` read them from `ToolManager` for the mode's canvas;
  never keep a copy, and never list them as Properties rows.
- **Bone Display** (Blender's armature display), one choice for the editor in `CharacterBoneDisplayBranch`, listed as
  Properties rows by every window that draws bones (the modes whose `drawsBones` is set: Mesh, Weights, Rig; and the
  Animation Editor): the style (`CharacterBoneDisplay`: Octahedral, Stick, B-Bone, Envelope, Wire, Hidden; one width
  formula of a fixed width, a share of the bone's length and a share of its size) and the colour of each kind
  (`CharacterBoneKind`: Hair by the lock prefix, Face below a part that `takes_face`, Body otherwise) from the named
  `BONE_DISPLAY_COLORS`. Every viewport draws its bones through `CharacterViewRenderSystem.addBones`, handing it a
  `CharacterBoneSource` (the posed character, or the rig being edited at its bind joints); a bone runs from its joint
  to each child's in the style's marker shape (`CHARACTER_VIEW_MARKER_*`: the cube, `CharacterViewBoneOctahedral`,
  `CharacterViewBoneSphere`), a root or childless bone is a sphere at its joint, and Envelope adds a sphere at every
  joint as wide as the bone.
- **Hide and Reveal.** The Selection group's Hide (H), Hide Unselected (Shift+H) and Reveal (Alt+H)
  (`CharacterSelectionBranch.hide` / `reveal`) put vertices out of the way, as Blender hides geometry to reach tight
  spaces: the document's hidden flags (`CharacterMeshDocumentInstance.isHidden`, carried through `removeOrphans`,
  cleared when an undo or revert renumbers the mesh) keep a vertex out of every pick, box, brush and selection
  (`CharacterEditorMeshViewSystem.isInRegion`, `CharacterSelectionBranch.changed`), its faces out of occlusion
  (`isFaceShown`) and out of the preview (`CharacterMeshBranch.upload` uploads a copy without them). The file always
  keeps them. Something must stay shown.
- **Pick and Pose.** The mesh and weights canvases take Pick (the bone whose joint is clicked, or the bone weighing most
  on the vertex clicked, becomes the bone every weight action works on) and Pose (turns a bone about its joint, about
  the line of sight, so what it carries follows), run by `CharacterEditorBoneSystem`. Pose writes the edit pose
  (`CharacterEditorManager.setEditPoseTurn`, a turn per bone in its own frame, mirrored onto the twin while mirroring,
  never saved; Reset Pose, Alt+R, clears it); `CharacterEditorPreviewSystem.applyEditPose` lays it on the preview as
  `AnimationStateHandle` postures, which every pose (the tree, a struck clip, the rest) now carries. A tab with an
  edit pose counts as posed: select, weigh and transform in it (pose a thumb out of the way to reach the fingers),
  Reset Pose before sculpting.
- **The Pose tab** (`CharacterPoseContext`, `CharacterEditorMode.POSE`, `ToolCanvas.CHARACTER_POSE`) is the edit pose's
  own tab, as a modeller's pose mode is: Pose and Pick its tools, Reset Pose its action (Alt+R), Mirror its option; the
  Palette tab lists every bone (with its eye) to pick, and the Properties tab the bone picked and its turn about each
  axis in degrees (`CharacterPosePropertyBranch`, key `pose_turn`, stepped by `CHARACTER_EDITOR_POSE_TURN_STEP`), every
  change through `setEditPoseTurn`. It poses the bones and never edits a vertex (`CharacterEditorMode.posesBones`,
  kept out of `editsVertices` and `takesRegions`); the pose it holds is the one every modelling tab shows. Keying a
  pose into a clip is the Animation Editor's (Record, then pose there).
- **Sculpt tools.** Brush, Smooth, Relax (even spacing along the surface without shrinking), Inflate, Pinch, Crease
  (a sharp line drawn in under the brush's centre, a ridge inverted: lid folds and lip lines), Flatten and Nudge, all
  in `CharacterShapeBranch.resolveMove`.
- **Topology tab** (`CharacterEditorMode.TOPOLOGY`, `ToolCanvas.CHARACTER_TOPOLOGY`, `editsEdges`): the retopology
  toolset of Blender's and Maya's modelling kits, its Wireframe on by default and its Properties rows counting quads,
  triangles and poles (`CharacterEdgeBranch.getStatsText`, counted again only when the mesh moves). Its tools are
  Select, the Topology brush (below), and three edge tools picked with the pointer (`CharacterEditorEdgeSystem`: the
  edge of the face under the pointer that passes nearest it, `CharacterEditorMeshViewSystem.pickEdge`, found again
  only when the pointer or mesh moves, drawn as a guide by the overlay; a click runs
  `CharacterEditorManager.applyEdgeTool`): **Flip Edge** (Maya's Spin Edge, Blender's Rotate Edge: the two faces beside
  the edge walked as one polygon and the edge turned one corner round it, Ctrl the other way, so a "/" becomes a "\" and
  quads stay quads; refused on an open edge, a seam, faces wound opposite ways, or corners joined already), **Loop
  Cut** (Blender's Loop Cut, Maya's Insert Edge Loop: the ring of quads across the edge, walked through each quad's
  opposite edge until it closes or meets an open edge or a triangle, cut through every edge's middle, the new loop
  selected) and **Dissolve Loop** (Maya's Delete Edge Loop: the loop walked straight on through every vertex four quads
  meet at and taken away with its vertices, the quads either side joined in pairs; it must close or run to open edges,
  since a pole would leave a face of more than four corners). Its action groups are Faces (Make Face, Merge, Merge by
  Distance, Subdivide, Flip Faces, Triangulate Faces (Ctrl+T: every selected quad split along its shorter diagonal),
  Triangles to Quads (Alt+J: selected triangle pairs sharing an edge, texture and zone joined), Delete Faces) and
  Clean Up (Delete Degenerate Faces, Select Poles, Select Triangles, Select Non-Manifold), then Selection; all of it is
  `CharacterEdgeBranch` but Make Face, Merge and Subdivide
  (`CharacterSculptBranch`). Every edit honours Mirror (the twin edge, ring or loop the mirrored way, skipped where it
  is the same one or runs into it), is one undo step, and builds the faces about each edge once
  (`buildEdgeFaces`), never per face. New vertices blend through `CharacterTopologyBranch.addBetween`; faces keep their
  texture, zone and UVs; a cut seam stays a seam. The Mesh tab keeps transforms, Extrude, Duplicate, the deletes and
  Symmetrize.
- **Topology brush.** The Topology canvas's Topology tool (`D`) changes the faces, not the shape, where the mesh's
  vertices and edges are drawn (`CharacterTopologyBranch`, at the canvas's own Strength, honouring its Mirror; a stroke
  routes there from `CharacterEditorManager.beginStroke` before the mode's own stroke): a stroke opens one undo step,
  gathers how far the brush covers each vertex (and its twin with Mirror on), and once a frame (`CharacterEditorManager.showStroke` calls `apply`) changes every face whose
  corners it all covers and whose size (the side of a square of its area) is off the tool's Detail the way the stroke
  goes: a plain stroke splits only faces above `CHARACTER_TOPOLOGY_SPLIT_ABOVE` times Detail, Ctrl merges only those
  below `CHARACTER_TOPOLOGY_MERGE_BELOW` times it, so strokes even an area toward Detail and then stop changing it, as
  Blender's dynamic topology does (Detail is in millimetres, `getTopologyDetail`; the body's faces run about 6 mm); an edge's two thirds are each measured from its nearer end, and a collapse's twin falls along
  the same diagonal the face itself took, so a stroke with Mirror on leaves the two sides exact twins. Plain strokes
  refine by Schneiders' 3-refinement, as adaptive all-quad meshing does: a face
  splits into nine on a lattice of thirds and each face beside it takes the transition template its refined edges call
  for (`EditorSetting.CHARACTER_TOPOLOGY_TEMPLATE_*`: one edge, two at a corner, two opposite); a face left with three
  or four refined edges joins the refined set until none is, so the mesh stays all quads with no hanging vertex.
  Ctrl reduces by quad-mesh simplification (Tarini et al.): the smallest covered faces collapse across the diagonal
  that leaves the merged vertex and the two beside it nearest four faces each (the shorter on a tie), walking the faces
  about each vertex through an adjacency chained once a frame (never a scan of the mesh) with the vertices locked on
  open edges and seams found once a stroke until a refine adds more, refused on an open edge or UV seam, where the rings would pinch (the link condition) or a face about them
  would fold or shrink (`CHARACTER_TOPOLOGY_FLIP_COSINE`, `AREA_SHARE_MIN`), and a vertex left between two faces that
  share both its edges dissolves with them. New vertices blend position, weights (`CharacterWeightBranch.blend`),
  masks and shape-key deltas from the corners they lie between; faces keep their texture, zone and interpolated UVs;
  a refined seam stays a seam. Faces a stroke made are not refined again by it; unused vertices go when it ends
  (`CharacterSculptBranch.removeOrphans`). Refused while a shape key is sculpted and past the mesh limit.

## UV

The UV tool is two tabs that work together, as Blender's UV Editing workspace pairs its 3D view with its UV Editor:
the Character UV tab (`CharacterEditorMode.UV`, `ToolCanvas.CHARACTER_UV`: Select with the brush, the Seams, Unwrap and
Selection groups) and the UV Editor (`UVEditorContext`, `ToolCanvas.UV_EDITOR`: Select, Move, Rotate, Scale, the
Unwrap, Islands and Selection groups). Both work in the UV mode on the region picked for the modelling tabs
(`CharacterModeProvider`), so the Palette tab lists the parts and the Properties tab the UVs selected.

- **Data.** UVs are per face corner (`RiggedMeshStruct.getFaceU` / `setFaceUV`); seams are vertex pairs
  (`isSeam` / `setSeam`, keyed by `toEdgeKey`, renumbered by `removeVertices`); pins are four bits a face
  (`isCornerPinned` / `setCornerPinned`). All three are saved with the mesh and are part of every undo step.
- **What the UV Editor shows** (`CharacterUVBranch.collectShownFaces`): the faces whose four vertices are selected and
  shown, as Blender shows the faces selected in Edit Mode. Its own selection is of face corners
  (`CharacterMeshDocumentInstance.isCornerSelected`, cleared whenever the faces are renumbered, never in the history);
  a pick takes every corner of the same vertex at the same place. Islands are faces joined across an edge both meet at
  in UV (`UV_WELD_EPSILON`); unwrap islands are faces joined across every edge that is no seam.
- **One branch owns every UV edit:** `CharacterUVBranch` (Mark and Clear Seam on the edges between selected vertices,
  Seams From Islands, Unwrap, Pack, Average Island Scale, Stitch, Pin, Unpin, Flip U and V, Select All and Linked, the
  Move, Rotate and Scale drags, and the Properties rows: the U and V of the selection's centre). Every key routes
  through `CharacterPropertyBranch` like any other; each change to the mesh is one undo step through
  `CharacterHistoryBranch.begin` and `CharacterMeshBranch.finishEdit`.
- **Unwrap** is least squares conformal mapping (`CharacterUnwrapUtility`, Lévy et al. 2002): each quad split into two
  triangles, the Cauchy-Riemann conditions of each weighted by its area, two vertices pinned (the island's own pinned
  corners where at least two vertices are pinned, else its two farthest along its longest side), solved by Jacobi
  preconditioned conjugate gradients. Unwrap then evens the islands' scale and packs them. An island with no
  boundary (a closed surface with no seam) or no area is left as it was and counted in the status line.
- **Pack** lays the islands in shelves, tallest first, `UV_PACK_MARGIN` of the tile apart, scaled together to fit the
  unit square; islands are not turned. **Average Island Scale** gives every island the same UV area per surface area,
  keeping their total. **Stitch** carries each island holding selected corners onto the neighbours those vertices
  also stand on, by the least squares similarity (turn, uniform scale, move), then welds the corners there.
- **Drawing.** The Character UV tab's overlay draws seams red and wider (`COLOR_SEAM`); the UV Editor
  (`UVEditorRenderSystem`) draws the tile of the first face shown over the unit square (`UVEditorBackgroundShader`,
  a checker without one) and the UVs as one overlay mesh in UV space (`UVEditorOverlayShader`, fixed pixel widths),
  rebuilt only when the mesh, the selection, the editor or the box moves. `UVEditorViewSystem` fits the square, zooms
  about the pointer and pans with the right button; `UVEditorToolSystem` picks, boxes and drags, and runs the keys of
  `shortcuts/UVEditor.arpg`.
- **Not done yet, only when the owner asks:** island rotation while packing, UV snapping, and showing every face of
  the region when none is selected (Blender's UV Sync Selection).

## Paint

The Paint tab (`CharacterEditorMode.PAINT`, `ToolCanvas.CHARACTER_PAINT`: Brush, Material, Erase, Smooth, Fill, Pick, the
Brushes tab's brush) paints the open part's texture maps on every region. It is a mesh mode that `paintsMaps`, so the
mesh view skins the part in any pose and the sculpt system stays out of it.

- **Projection painting**, as Blender and Substance paint: `CharacterEditorPaintSystem` lays a stamp every brush
  spacing and hands `CharacterPaintStampStruct` every face of the region under the brush whose middle the camera sees,
  its corners on the window; `CharacterPaintBranch.stamp` rasterises each face's two UV triangles over the map and
  paints each texel by the tip's coverage where the same barycentrics place it on the window, so strokes cross UV
  seams; texels within `CHARACTER_PAINT_BLEED_PIXELS` of an island edge are painted too. Fill covers the island of
  faces joined corner to corner around the face clicked (`pickFace`), Pick takes the flattened map's colour there.
- **Material painting** (Substance Painter's material brush): there is no material tool. While the Materials tab was
  worked in after the Palette (`MaterialsManager.isPaintingMaterial`), Brush and Fill lay the material chosen in
  place of the colour, stamping every map's canvas of each texture they reach (`CharacterPaintBranch.stamp`, `stampCanvas`), each texel mixed by the tip's
  coverage toward `MaterialsManager.sampleMap` at the texel's UV, the material chosen in the Materials tab read from
  its maps' textures or images, repeated by its Tiling and shaped by its parameters (see `.claude/docs/Editor.md`,
  Materials).
  The maps' images are read on first use and let go when the material laid changes (`MaterialSampleBranch.retain`).
- **One undo step per stroke.** The history is a list of steps, each the layers one stroke, fill or map write painted
  across every canvas (`undoSteps`, `redoSteps`); Undo and Redo take a whole step back, and a layer removed leaves
  every step, an empty step dropped (`forgetLayer`).
- **Symmetry painting** (Substance Painter's and Blender's object-space symmetry): with the paint canvas's Mirror on,
  `CharacterEditorPaintSystem.resolveFaceMirrors` finds each face's twin once a stroke (the face whose corners are its
  corners' twins, `CharacterEditorManager.getMirrorVertices`, keyed by the sorted corners), and every stamp lays each
  covered face again on its twin with the twin's corners placed where their own twins show on the window
  (`addMirroredFaces`), so the same coverage lands on the twin's texels; the stroke stays one undo step. Islands shared
  across the centre line (head, torso) mirror within the island. Fill and Pick do not mirror.
- **Canvases** (`CharacterPaintCanvasStruct`): one per texture and map (albedo, normal, height, AO, metallic,
  roughness, tint; `CHARACTER_PAINT_MAP_*`), opened on first use from `<Name>_<suffix>.png` beside the texture or the
  alias's default colour. Its layers are image documents of `ImageManager` (`beginPaint` / `paintPixel` /
  `flushPaint`), the first the image itself, each above laid over by alpha and opacity; every stroke is one undo step
  of the layer it touched (the before colours of the pixels it changed), and each change is laid flat and uploaded
  into the atlas layer the map fills, only its rectangle, rows bottom first (`TextureManager.updateTextureArrayLayer`,
  `glTexSubImage3D`). The base keeps its own alpha; Erase wears the base to the default and a layer to nothing.
- **Values.** The Layers tab picks the layer painted and shows or hides each above the base; the Properties tab names
  it and holds its Opacity; the colour lives in the Palette tab's wheel, its sliders in the Properties tab while the Palette
  was worked in last (Hue, Saturation and Brightness, `CHARACTER_PAINT_KEY_HSB`, a grey keeping the hue last chosen in `CharacterPaintBranch.getHue`; Red,
  Green and Blue, `CHARACTER_PAINT_KEY_COLOR`, which work with no character open, since the Image Editor paints with
  the same colour) and the map painted is picked in the Palette tab
  (`CHARACTER_PAINT_KEY_MAP_ITEM`, `CharacterPaintBranch.setMapIndex`), every key read and written through
  `CharacterPaintPropertyBranch` and listed by `CharacterPaintRowBranch`; Add Layer and Remove Layer are the canvas's
  tool actions. Save (File) writes every
  changed map flat into its image, Revert puts the atlas back as the images hold it, Undo and Redo (Edit) step the
  paint history in the Paint tab. Painted maps are the owner's art: never catalogue them.
- **Maps** (the Palette tab, as Materialize or Bitmap2Material generate maps; the same tab and generators as the Image
  Editor's, see `.claude/docs/ImageEditorPipeline.md`): the texture's maps are `CHARACTER_PAINT_IMAGE_MAPS`, the
  `ImageMap`s its aliases fill. Picking one makes it the map painted; below it are that map's own settings
  (`MapGenerateOption`, each map its own copy per texture for the session, `CharacterMapBranch.getSettings`), then
  Generate, which works the map out of the albedo as painted (layers and all) through `ImageManager.generateMap` at the
  texture's own size, and Clear, which lays the alias's default back; each paints over the map's layer as one undo step
  (`CharacterPaintBranch.writeMap`), so Save writes and bakes it and Undo takes it back. Keys:
  `CHARACTER_PAINT_KEY_MAP_SETTING`, `_GENERATE`, `_CLEAR` in `CharacterPaintPropertyBranch`; rows through
  `MapRowUtility`, shared with the Image Editor.
- **One layered model.** `CharacterPaintCanvasStruct` extends `ImageLayerStackStruct`, the Image Editor's layered
  image, and both are laid flat by `ImageManager.compositeLayers` (a canvas keeps its base's alpha, an image does not).
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
- **Mesh format** (`RiggedMeshArpgUtility`): a rigged mesh file holds a `mesh` object of typed arrays (see
  `.claude/docs/ARPG.md`) of quads and triangles: `positions` (each distinct vertex once), `bones` (the bone names the
  file uses, resolved against the rig by name at load) with `influence_counts`, `influence_bones` and
  `influence_weights`, `quads` (four vertex slots a face, a triangle's fourth repeating its third), `face_corners`
  (each face's corner count, 3 or 4, written only for a mesh holding triangles, so an all-quad file is unchanged)
  beside `quad_textures` and `quad_zones` (numbers into the
  `textures` and `zones` name lists; `MeshZone`: skin, eye, brow, lash, hair, cloth (dyed the cloth color), mouth,
  teeth, gear (its own texture untinted, for armor)), `quad_uv_flags`
  (bit 0 the face authors its UVs, bits 1 to 4 its corners pinned for unwrapping,
  `EngineSetting.RIGGED_MESH_UV_*`; a build older than the pins reads any non-zero flag as authored) with the eight
  `quad_uvs` floats of each face that authors them, the optional `seams` (vertex pairs, the edges cut for unwrapping,
  written only when there are any), sparse `masks` (channels
  `EngineSetting.RIGGED_MESH_MASK_CHANNELS`) and `morphs` (`vertices` beside `values` or three `deltas` each), and
  `normals`, every corner's packed smooth normal. Vertices are numbered as corners first appear, merged by position
  and influences; writing renumbers through `resolveFileVertices`, so never index masks or morphs by
  `RiggedMeshStruct` order in a file. Only this layout is written (the character tools' save, the ARPG tool's
  `compact`); a file still in quad entries (`vbo`) is read by `upgrade`, which rewrites its tree first. Stored normals
  serve the load only (`MeshManager.parseRiggedNormals`); any rebuild of an edited mesh computes them. The finished
  GPU blocks are never stored: they hold tile IDs and bone numbers assigned at run time, and building them takes a
  few milliseconds. Static quad meshes take the same idea through `MeshLayoutUtility`: `mesh.vertices` (every float
  of each distinct vertex) and `mesh.quads`.
- **Import and Export.** The File menu's Import and Export move the mesh being edited to and from a
  `.glb` (`CharacterModelBranch`; see `.claude/docs/GLTF.md`). Import replaces the open mesh as one edit; Save writes it.
- **GPU path.** `RiggedMeshGeometryUtility` packs every corner into `RIGGED_MESH_VERTEX_INTS` words (44 bytes: float
  position, normalised-short UV, byte bone indices, normalised-byte weights summing to one, a 10-bit-per-axis normal,
  the morph entry run, zone with tile, normalised-byte masks; `EngineSetting.RIGGED_MESH_ATTRIBUTE_SIZES`, `TYPES` and
  `NORMALIZED`) and shares one indexed vertex between corners whose words all match, so never count on one vertex per
  corner. The shaders still declare float attributes: GL converts. Indices are unsigned shorts unless a mesh has more
  than `RIGGED_MESH_VERT_LIMIT` shared vertices (`MeshHandle.getIndexType`). Every VAO over a mesh's buffer points its
  attributes through `VAOAttributeUtility.pointAttributes`. The morph deltas live in the mesh's morph
  texture (`MeshHandle.getMorphTexture`). Each instance's palette row (`SkinnedBufferData`) holds its bones, then the
  appearance texels (`SkinnedAppearanceStruct`: hidden bone, one texel per `SkinnedColor`, `SkinnedDetail` amounts),
  then its morph weights. The shaders read them through `characters/includes/CharacterAppearance.glsl` and shade by
  zone in `CharacterSurface.glsl`; keep the GLSL constants in step with the enums.
- **Eyes and teeth are worn parts.** `FeatureSlot` holds `HAIR`, `EYES` and `TEETH`; each feature may name its own
  `material` (`FeatureData.getMaterial`), drawn with it by `EntityRenderSystem.pushBody`, the body's otherwise; an editor
  viewport draws it with its own copy (`CharacterViewRenderSystem`'s `featureMaterial2ViewMaterial`, cloned on first
  use), since skinned batches are keyed by mesh and material and a shared one would draw every viewport's eyes and
  teeth in every other viewport. A
  removable slot (`FeatureSlot.isRemovable`, hair) is offered bare in the game's creator; eyes and teeth are not, and a
  save that names none for them keeps the gender's. The head's sockets, lids, lips and mouth cavity are the body's.
- **Gear is worn as models skinned to the body's rig** (the modular-outfit approach: every piece skinned to the
  skeleton of the body that wears it and posed by the body's own bones). An item lies in the world and in containers
  as its sub-voxel model; worn, an item whose equip type is worn on the body (`EquipmentType.isWornOnBody`: not a
  weapon or shield, which are always held, nor a ring, which rides its finger slot's anchor) is drawn as the model
  its `worn` list names for the wearer's rig (`ItemWornModelStruct`, read by `WornModelBranch`): a rigged `mesh`
  skinned to that rig, the `gender` whose body it is fitted to (none for every gender), the `side` it is worn on
  (`EquipmentSide`, from the slot's `EquipmentSlot.getEquipmentSide`: a pauldron's left and right models; none for
  either), the `material` it is drawn with (the body's when it names none, else its own clone), the body bones it
  `hides` and the features it `hides_features` (a helmet hides the hair, which then neither draws nor simulates). The
  model fitted most closely wins, its own gender outweighing its own side (`ItemDefinitionData.getWornModel`); no two
  may fit one rig, gender and side.
  `AppearanceHandle.wear` (each update, from `EntityInstance`, acting only when the inventory's revision or the gender
  moves) takes on each body slot's model and `resolveWorn` gathers the hidden bones into one mask and the hidden
  features; `EntityRenderSystem.pushWorn`
  draws them after the features through the body's own skinning matrices, and `resolveBodyMask` cuts the hidden bones
  from the body (`HIDE`, so every fragment those bones move at all goes: hide only bones whose skin the piece covers
  past their reach). A worn model's morphs play the body's morphs of the same names
  (`ItemWornModelStruct.getMorphSources`, resolved once per body mesh; `SkinnedAppearanceStruct.setMorphSources`), so
  a piece modelled from the body keeps every shape slider, and bone sliders reach it through the shared bones. A
  worn model weighing onto spring bones runs their chains (`AppearanceHandle.weighsOnto`), so a cape rides spring
  chains as long hair does. Gear with no model for the rig, and everything held, rides its race's anchors rigidly as
  before. Worn models live under `assets/mesh/worn/`. The Equipment tab's Gear rows step the item every preview
  wears in each body slot (`CharacterEditorManager.stepGear`, offering only items with a model for the previewed rig,
  gender and side), worn through `AppearanceHandle.setWornModel` in `CharacterLookBranch` and drawn by
  `EntityRenderSystem.pushPreview`; anchored gear is not previewed.
- **Assets.** Every armor and clothing item wears a quick model per body (`worn/armor/`, `worn/clothing/`, `Male`
  and `Female`, paired pieces `Left` and `Right`): the body's own faces under the garment offset along their normals,
  so each keeps the body's weights and shape morphs, in the gear zone with the item's texture box-projected; the
  helmet hides the hair. The cloak is a sheet hung down the back on `HumanoidRig`'s cape chains (`cape_l`, `cape_c`,
  `cape_r`, five joints each from `upper_torso`, resting against `back`, `hips`, `chest`, the shoulders, thighs and
  shins). Replace them by modelling over the body and importing as worn gear.
- **Bone masks.** A skinned draw's palette row carries a `SkinnedBoneMaskStruct` and its `SkinnedMaskMode`
  (`SkinnedAppearanceStruct.setBoneMask`): `HIDE` cuts every fragment the masked bones move at all (a character seen
  from its own eyes: `AppearanceData.getHeadMask`, the head and everything below it, eyes and teeth included), `SHOW`
  keeps only fragments they carry at least half of (part isolation). A rig may hold at most
  `SKINNED_BONE_MASK_CAPACITY` bones.
- **Overlays.** Makeup (`SkinnedOverlay`: blush, eyeshadow, lips, eyeliner), brows, nipples and body hair (chest,
  stomach, pubic, underarm, arm, leg) are images painted over the skin in the body's own UV layout, never geometry:
  UV-space decal layers, each laid over the ones before it in enum order. Each gender's model places
  them (`genders.<g>.overlays`: the `region` of its UV layout the images cover, u0 v0 u1 v1, the `styles` it offers,
  each a `name`, a `texture` in the character atlas and an optional `relief` in blocks, and the style worn by
  `default`; `RaceGenderStruct.getOverlay`), written into each draw as three texels: the tile of the style worn, the
  region and the relief (`SkinnedOverlayStruct.write`, `AppearanceHandle.getOverlayStyle`, `INDEX_NOT_FOUND` for the
  model's default). The shader maps the corner UV into the region and samples the style's layers there
  (`shadeOverlays`), tinted by the overlay's `SkinnedColor` (`BROWS`, `NIPPLES`, `BODY_HAIR`) and laid over at its
  image's alpha times its `SkinnedDetail` amount, a layer's opacity. A style with relief (the nipples) rises from the skin through its height map by parallax occlusion
  (`marchOverlayRelief`: the ray enters the relief's top and marches down to the skin, so bare skin never shifts) and
  bends the normal through its normal map, both faded by the Character Detail distance. An overlay's images cover only
  its region, so a style is a small image; paint a new style in the body's layout, crop it to the region, and every
  map it carries (A, h, n, ...) goes beside it by suffix. Saves keep a chosen style by name (`overlay_styles`); the
  creator lists a style row and an amount per overlay (Lips under Lip Color, Blush, Eyeshadow and Eyeliner under
  Makeup), and the Appearance rows' Marks set the previewed gender's `default`. Each overlay's `region` is the
  tight UV box of what its styles cover, so its image (at most 512 texels a side, the bake's top tier) spends its
  texels on the mark; the head's UV shell gives the face few body texels, so face marks belong in overlays.
- **Makeup** replaced the vertex-mask lip colour, blush and liner: the `blush`, `lips` and `liner` mask channels stay
  in the mesh format but are no longer shaded (freckles still read theirs). Lips take `SkinnedColor.LIPS` and
  `SkinnedDetail.LIPS` (Natural, Full, Thin, Defined, Ombre), blush `BLUSH` (Apples, Sculpted, Sunkissed), eyeshadow
  its own `EYESHADOW` colour and detail (Soft, Smoky, Halo), eyeliner the `LINER` colour and `SkinnedDetail.LINER`
  (Thin, Bold, Winged, Smudged); liner and eyeshadow start at 0, so a character wears none unless given it. The lid's
  `lash` zone is shaded as skin (it was a band of brow colour that read as mascara); real lashes would be alpha cards,
  not modelled yet.
- **Eyes.** An eye is its variant's image set (`features/humanoid/eyes/HumanoidEyes`: Realistic, the default, Cat,
  Ringed) on the eye zone's raw UVs, the iris centred at `CHARACTER_IRIS_CENTRE` (`shadeEye`): Iris Size scales the
  image about the iris, Pupil Size dilates the pupil by a radial remap inside it, and the iris, `CHARACTER_IRIS_DEPTH`
  behind the cornea, shifts against the view (refraction); the eye color reaches what the tint mask allows. The eye
  writes a specular falling from `CHARACTER_EYE_SPECULAR` toward `CHARACTER_EYE_SPECULAR_FAR` across the Character
  Detail distance, and the lighting pass (and the editor viewport) mirrors the sky on every glossy character pixel by
  that specular, weighted by Fresnel and occlusion (`SkyReflection.glsl`), and lays the light's own crisp catchlight on
  a cornea (`resolveCorneaHighlight`, specular past `CORNEA_GLOSS_START`), as a wet eye shows it. The editor viewport's
  highlight uses the lighting pass's specular power, so both look alike. **Not yet industry standard:** the environment
  is the analytic sky the sky shader paints, not a captured reflection probe of the surroundings.
- **Material variants.** A look offering several image sets is a `SkinnedVariantStruct` (names, tiles, reliefs,
  default), parsed for overlays' `styles` and features' `variants` alike by `SkinnedVariantUtility.parse`. A feature
  naming `variants` and a `default` is worn in the one its appearance picks (`AppearanceHandle.getFeatureVariant`,
  INDEX_NOT_FOUND for the default, cleared when the feature changes, and set from the gender's `feature_variants` when
  an appearance takes its gender's preset: `AppearanceGenderStruct.getFeatureVariant`, parsed and checked against the
  feature the gender starts with by `AppearanceBuilder.parseFeatureVariants`): `EntityRenderSystem.pushBody` writes the
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
- **Appearance.** `AppearanceData` holds the race's body meshes (each gender's `mesh`, in the race's order), colors,
  palettes, details, sliders and the expressions its face plays, the morphs resolved on every body (`getShapeMorph`,
  `AppearanceExpressionStruct.getTrackMorph`). `AppearanceBuilder.parseShapes` derives the sliders
  (`AppearanceShapeStruct`, three kinds): a bone slider per channel a rig bone offers in its `sliders`
  (`BoneSliderChannel`: move, turn and size on x, y and z; named `<bone>/<channel>`, a `_l`/`_r` pair one slider named
  without its side, the right twin taking the channel's mirror sign, `getMirrorSign`) moving its bones
  (`AppearanceBoneTargetStruct`) a block, a degree or its whole size per unit; a control per control of the rig's bone
  controls, its drives (`AppearanceDriveStruct`) naming the bone sliders it pushes; a shape key per morph of the bodies
  that no expression's clip weighs (the blinks are the blink's). The race's `appearance.sliders` map gives each its
  `group`, `min`, `max`, `default` and `player` by name, and its order; a slider it does not name takes the defaults (a
  bone slider its channel's range either way of zero, `getDefaultRange`). `AppearanceHandle` keeps shape key values as
  morph weights of the body worn, totals each bone slider's own value with every control's push
  (`AppearanceData.resolvePush`) held to the slider's range, sums the bone targets into each bone's shaping
  (`AnimationStateHandle.setBoneShape`, laid on the bind pose under every pose) and proportion in `applyProportions`,
  plays the face (`updateFace`, below) and hands everything to a draw in `writeAppearance`. The creator and NPC
  variation list only sliders the player is offered, by group (the body group on the Body tab); saves and gender presets
  store values by name.
- **Expressions are clips.** A race's `appearance.expressions` maps each expression's name to an animation clip made for
  the race's rig (`AppearanceExpressionStruct`), the facial-clip standard: its bone tracks move face bones and its
  `morphs` curves (`MorphTrackStruct`, a curve of keys as a bone axis has, the blink's linear,
  `AnimationClipUtility.sampleMorph`) weigh morphs by name, resolved on every body when the race is built. The one named
  `blink` (`EngineSetting.FACE_EXPRESSION_BLINK`) plays by itself: `AppearanceHandle.updateFace(deltaTime)`, called from
  `EntityInstance.updateAnimation` before the tree updates, places it once at a pseudo-random moment in every interval
  of a length each character draws (`FACE_BLINK_INTERVAL_MIN` to `MAX`, `BiomeFieldUtility.hash01`); every other
  expression fades toward the level asked of it (`setExpression`, over `FACE_EXPRESSION_FADE`), a looping clip playing
  on, any other held at its end. The morphs the expressions weigh are written as the levels times their curves; their
  bones are the pose's face layer (`AnimationStateHandle.setExpressionLayer`, handed over again on every reset, laid
  additively over the tree, a struck clip and the rest pose, beneath the posture). An editor strikes one with
  `poseExpression(index, phase, level)`; the Blink preview row holds the blink at `closed * FACE_BLINK_CLOSED_PHASE`,
  since a blink clip closes the lids fully at its middle. The Human's blink is
  `animations/humanoid/expressions/HumanoidBlink`, each eye closing through its half shape and opening back.
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
  `CharacterEyeMaterial`) and `features/humanoid/teeth/HumanoidTeeth` (`HumanoidTeethMesh`: a single-sided UV-mapped
  strip per row of teeth facing out, the upper on the head in front of the lower on the jaw, as teeth cards are made,
  never a second surface behind them, which shows through the gaps between the teeth as a doubled row; and the tongue,
  lofted inside the lower arch, widest at its root, rounded at its tip, domed with a shallow groove down its middle; `CharacterTeethMaterial`; nine
  variants, from Standard to Braces, each an image set whose tint mask keeps metal from taking the teeth color).
  `HumanoidRig` names its `parts` (head showing hair, eyes and teeth and taking the face; ears; eyes holding the eyes;
  mouth holding the teeth; hands; feet) and seven
  `poses` (`animations/humanoid/poses/`). All are generated (catalogued); the character tools edit a rigged
  mesh of at most `RIGGED_MESH_VERT_LIMIT` corners. Their overlays (`CharacterEditorOverlaySystem`,
  `UVEditorRenderSystem`) index by unsigned int (`MeshManager.createMesh` with an `IntArrayList`), so the wireframe of a
  whole body, four overlay vertices to every edge, is never cut short. Each hand is one bone: hanging at the side its
  palm faces the thigh with the
  thumb forward (+z), so `hand_l` is a left hand at +x; keep that chirality on any hand you model or mirror.
- **Features are the gender's.** The Properties tab's Features rows (Appearance mode) read and write the previewed
  gender's `genders.<gender>.features`, the features it is made with; a gender naming none wears the race's
  `appearance.features` default, shown with `CHARACTER_EDITOR_FEATURE_DEFAULT_SUFFIX`. A row steps through the default
  (the gender's own removed), then `none` (`EngineSetting.APPEARANCE_FEATURE_NONE`, `""`: the gender wears nothing
  there, so it can go bald under a race whose default gives hair; offered only while the default wears something),
  then every feature that fits. `AppearanceGenderStruct.namesFeature` tells a slot named none from one left to the
  race; read a preset's feature only through it (`AppearanceData.getStartingFeature`).
- **IK handles are rig data.** A rig's `ik` lists handles (`RigIkChainStruct`): three `bones`, root to end, each the
  child of the one before, and the `pole` their middle joint bends toward in the character's space (`RigBuilder`
  refuses anything else). The Rig tab's **Add IK Handle** and **Remove IK Handle** (`CharacterRigBranch.addIkChain` /
  `removeIkChain`) work on the selected bone as the end and on its twin across the body; a new handle's pole is the way
  its middle joint stands off the line from root to end, or, for a straight chain, across it from the front (a
  finger's knuckle away from the palm), mirrored on the right. Edit a pole in the Properties tab (Rigs). The Animation
  Editor's **IK Handle** tool (`EditorTool.IK`, `AnimationEditorPoseSystem.applyIk`) picks only handle ends, carries
  the end across the plane facing the camera and solves the two bones analytically, the middle joint bent on the
  pole's side, keying root and middle together through `AnimationEditorManager.beginChainPose` /
  `writeChainPose`, one undo step.
- **Hair is cards.** A hair mesh is a scalp cap and layered cards two quads wide, textured with the strand tile
  `characters/standard/Hair` (its alpha cuts the locks out); each card's raw UV runs across it in u and hangs as the
  image does in v, the root at the image's top (v 1) and the tip at its foot (v 0). Code reads how far along its strand
  a corner stands, root 0 to tip 1, only through `RiggedMeshStruct.getFaceStrand`. The strand image fades to nothing at
  its side columns and pads its colour under transparent texels (edge padding), so filtering never shows a card's
  straight edge or the opposite edge wrapping in. The character vertex shader sways hair-zone vertices in the wind by v
  (`CharacterHair.glsl`: slow gusts, a phase per lock read off where it grows in the bind pose, the tip trailing its
  root, a flutter across the wind and a lift on gusts), after skinning and in the world, so it adds to whatever bones
  carry the lock; keep v running from 1 at the root to 0 at the tip on any card you add, and keep the root at the scalp.
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
  afresh. Drag works against air moving with the wind (`AnimationSpringStruct.setWind`, the global wind's
  `RIG_SPRING_WIND_SHARE` swelling and easing in gusts on the wind's clock, handed by `EntityInstance.blowSprings`), so
  every chain is blown downwind as far as its drag carries it against its stiffness: a cape wants high drag and low
  stiffness. Editor previews blow no wind. On top of the chains, the character vertex shader flutters every
  non-hair vertex the spring bones carry (`CharacterCloth.glsl`, by the share the spring bones carry, read from the
  rig's spring bone mask each draw writes after its variant texel, `SkinnedAppearanceStruct.setSpringBones`), so a
  cape ripples while its chains swing it. A chain runs only while something worn weighs onto it (`AppearanceHandle.resolveSprings`, reading
  `MeshHandle.weighsOnto`, set for every rigged mesh in `MeshManager.applyRiggedMesh`), so a bald or short-haired
  character simulates nothing. Settings apply live, and so does a new or changed chain: a change of structure moves
  the rig's structure revision and every pose lays its springs out again.
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
- **Hair tools.** On the Hair region, the Rig tab's **Rig Hair Locks** (`CharacterRigBranch.addHairLocks`) finds the
  cards (`CharacterHairBranch.detectCards`: hair faces joined corner to corner whose v spans
  `CHARACTER_HAIR_CARD_MIN_SPAN`), keeps those at least `CHARACTER_HAIR_LOCK_MIN_LENGTH` long (shorter hair stays
  shader-only), joins cards rooted and hanging together into locks (`detectLocks`, matched point for point within
  `CHARACTER_HAIR_LOCK_RADIUS`) and lays one chain per lock from the bone its card roots weigh onto, one bone per
  `CHARACTER_HAIR_LOCK_SEGMENT` and a tip, named `hair_<lock>_<joint>`, its spring named `hair_<lock>` with
  `CHARACTER_RIG_SPRING_DEFAULTS`; a lock rooted within the lock radius of a hair chain already rigged takes that
  chain, so every style shares one set. **Add Spring Chain** / **Remove Spring Chain** make or drop a chain from the
  selected bone down its single children (and its twin), for hand-built chains. The Weights tab's **Weigh Onto Locks**
  (`CharacterWeightBranch.weighLocks`) hands each long card to the hair chain whose joints lie nearest its line
  (`CharacterHairBranch.findChain`, roots within `CHARACTER_HAIR_WEIGHT_REACH`), by how far down the card each vertex
  grows: the head near the root, then each segment's bone, blended at joints; spring weights held before are let go
  first. The chains are added bones, so they apply live and the two run one after the other. The selected bone's
  chain lists its five
  settings (stiffness, damping, gravity, drag, radius) in the Properties tab (`CharacterRigRowBranch.addSpringRows`),
  stepped and typed live, then a Yes or No per rig collider it rests against. **Add Collider** and **Remove Collider**
  (The Rig tab's Colliders group, `CharacterColliderBranch`) add a sphere on the selected bone, halfway to its first
  child, that every chain rests against, and remove the collider selected among the bone's; while mirroring is on a
  sided bone's collider is added, removed, reshaped and moved with its twin (named as its mirror). The Rig canvas's
  **Collider** tool does both in the viewport: a click on a bone adds one there, and Ctrl+click
  (`CharacterEditor/EraseCollider`) takes the bone's first off, each through the same actions
  (`CharacterEditorRigSystem.press`). The selected bone's
  colliders are listed in the Properties tab; the selected one shows its Shape (a capsule runs from the joint out past
  the sphere's centre as far again; back to a sphere at the capsule's middle), Radius, Offset or Start and End, and
  Spring Chains (All or None). Rig mode draws every collider over the body as a wire sphere or capsule
  (`CharacterEditorOverlaySystem`, the selected one in the selection colour), rebuilt only when the editor's state
  moves. **Preview Springs**
  (Rig and Weights) makes every modelling tab play the pose chosen (Idle at rest) with the springs simulated, until
  pressed again. The Animation Editor never lists, picks or keys a spring bone (`AnimationEditorManager.isKeyedBone`).
- **Assets.** `HumanoidRig` carries 14 hair chains (`hair_1` to `hair_14`, five joints each, radius 0.012) laid by Rig
  Hair Locks on `FemaleLongHairMesh`, every one resting against its five colliders: `head` (sphere on `head`), `neck`
  (capsule on `head`), `shoulder_l` / `shoulder_r` (capsules on the upper arms) and `chest` (capsule across
  `upper_torso`), sized inside the female body's skin with every hair joint clear of them at rest;
  `FemaleLongHairMesh` and `FemaleMediumHairMesh` are weighed onto them by Weigh Onto Locks.
  `MaleShortHairMesh` and `MaleMediumHairMesh` are short and stay on the head.
- **Toon lighting.** Characters write a smooth normal (`GBufferNormal.glsl`); the lighting pass lights those pixels
  in toon tones with a rim (`ToonLighting.glsl`), and the editor viewport's character shader uses the same function.

## Hair Tab

The Hair tab (`CharacterEditorMode.HAIR`, `ToolCanvas.CHARACTER_HAIR`, `groomsHair`) grooms hair the industry's way,
guide curves turned into game cards (Blender's hair curves, Maya XGen, Unreal's Groom): a **groom**
(`CharacterGroomStruct`, its strands `CharacterStrandStruct`: points root to tip, the root's surface normal, the bone it
grows from, its texture, its width at the root and the taper the tip narrows to) is a workspace file in
`Assets/Hair/<Name>.arpg` (`rig`, `source`, `strands`, each strand's `points` a float32 typed array), owned by
`CharacterGroomBranch` with its own undo history of whole copies. The tab works on the whole character, its mesh mode
reaching the surface of the part picked in the Properties tab's Parts (the body), and holds the bind pose.

- **Tools** (`CharacterEditorGroomSystem`): Draw roots a strand on the surface pressed and carries it along the drag
  across the plane through the root facing the camera, a point every `CHARACTER_HAIR_POINT_SPACING`; Place grows a
  straight strand the Length option long out of the surface clicked; Brush paints the material picked onto the strands
  it covers; Nudge combs (points move with the pointer, lengths held); Smooth; Raise and Lower lengthen and shorten
  tips;
  Erase cuts a strand where the brush first covers it (wholly at its root); Pick takes a strand's material; Select
  selects strands, Shift adding, Ctrl taking away. Draw and Place take Width, the strokes Size and Strength through the
  Brushes tab's brush, only points the camera sees. Mirror gives a new strand its twin across x and reaches each
  strand's twin (the strand rooted at its mirrored root) the mirrored way.
- **Values.** The Properties tab names the groom, counts its strands and selection, steps the material picked, and holds
  the selected strands' Width (mm) and Taper. The material is the one chosen in the **Materials** tab
  (`MaterialsManager.getSelectedMaterial`), which lists every material of the game; Pick on a strand chooses its
  material there, and the Hair tab's rows move with that choice (`CharacterToolContext.getRowRevision`).
- **Actions:** Strands From Worn Hair (each card of the hair worn turned back into a strand along its middle, as wide as
  the card, a groom named after the feature made first when none is open, the feature kept as the groom's `source`),
  Select All, Select None and Delete Selected Strands.
- **Files** (`EditorSetting.FILE_MENU_HAIR`, `CharacterEditorBranch.newHair` and on): New (name, then rig), Open (a
  groom from the Hair folder), Save, Revert, Undo, Redo; **Export** writes the groom's cards to
  `Assets/Exports/Hair/<Name>.arpg`, a rigged mesh on its rig: each strand a card two quads wide
  (`CharacterStrandUtility.resolveCardRows`, a rotation-minimising frame from the root tangent crossed with the root
  normal, the width tapering to the tip; u across 0, 0.5, 1, v from 1 at the root to 0 at the tip), every corner wholly
  on the strand's bone, the hair zone, beside the scalp cap of the `source` feature with its cards removed; **Import**
  picks an export from Exports/Hair, asks the hair feature it becomes (`folder/Name`, the source offered; an existing
  hair feature has its mesh replaced) and whether to lay spring chains, then writes
  `mesh/characters/<feature>Mesh.arpg`, makes or edits `features/<feature>` and, when asked, runs Rig Hair Locks and
  Weigh Onto Locks on it against the open character's rig (which must be the hair's) and saves the mesh and the rig.
  Grooms and exports are the owner's work: never catalogue them.
- The overlay draws every strand (`CharacterEditorOverlaySystem.syncGroomOverlay`, rebuilt only when the groom moves):
  its root, its guide line and its card's two sides, the selected ones in the selection's colour.
- **Not industry standard yet:** the viewport shows the guides and card outlines, not the rendered cards; the cards are
  seen once exported and imported.

## Models

The character tabs open two kinds of character (`CharacterDocumentInstance`): a race, picked in the Hierarchy, and a
**model**, a skinned mesh in the workspace (`Documents/My Games/AdventureRPG/Assets/Characters/<Name>.arpg`), the way
a modelling tool's skeletal mesh is skinned to a shared skeleton. `CharacterWorkspaceBranch` owns them; the File menu's
New, Open and Import reach them (`FILE_MENU_CHARACTER`: `newModel`, `openModel`, `importModel`).

- **The file** is a rigged mesh file like any under `assets/mesh/` (`rig`, `vao`, `mesh`), naming one of the game's
  rigs. A new model holds only `rig` (New asks its name, then its rig, `HumanoidRig` offered): it is blank
  (`isBlank`), with no body to preview, since the engine refuses a rigged mesh without a quad.
- **The first part** placed on a blank model starts its body (`CharacterEditorManager.startModelBody`, a click in the
  viewport while a part is armed; `CharacterEditorStampSystem.updateBlank`). Only a root part starts one. A part whose
  every bone the rig already holds by name (a part cut from a body on that rig) is bound to those bones where the rig
  holds them (`CharacterGraftBranch.bindBody`): the rig and its clips are left as they are, and the part's own
  animations are not joined, since the rig's clips already move those bones. Any other has its bones grafted at the
  rig's root (`startBody`). The body is written to the file (`CharacterWorkspaceBranch.writeBody`) and the model opens
  as any other; later parts join it through the Sculpt Brush as on any body.
- **Opening** makes the model's mesh known under `workspace/<Name>` to the mesh loader (`MeshManager.addMeshFile`,
  loaded on demand like any mesh) and to the mesh tools (`CharacterMeshBranch.addMeshFile`, so Save writes the
  workspace file), and builds the race it is previewed through in memory only (`buildRace`): one gender wearing the
  mesh, with the material and animation tree of the first race's gender on the rig, that race's equipment anchors and
  the part of its look any body takes (`CHARACTER_MODEL_LOOK_FIELDS`: head bone, colors, palettes, details; never the
  sliders, expressions or features, which name a body's own morphs and meshes). A rig no race stands on but an
  animation tree moves (`ItemRig`, the item rig) is previewed as the game's first gender in
  `CHARACTER_MODEL_STANDALONE_MATERIAL` moving by that tree (`resolveGender`), so a baked item opens like any model; a
  rig with neither cannot be previewed. A model's mesh is known as `workspace/<Name>/<Rig>`, so New Rig For The Model
  (`CharacterWorkspaceBranch.moveToNewRig`: the rig copied, a rest clip and tree made for it, `RigManager.addRigFile`,
  `AnimationManager.addAnimationFile`, `AnimationTreeManager.addAnimationTreeFile`) opens it afresh on its own rig. An
  item model's Import paints its item (`importItem`) rather than making a race. The view frames a body from where its
  mesh stands
  (`CharacterViewPoseSystem.getCenter`), so a lone torso is framed at its own height.
- **A model is no race.** Race edits skip it (`CharacterEditBranch.edit`), the Appearance and Equipment tabs say so,
  Duplicate and Delete refuse it, and Save writes its mesh, the rig, controls, clips and paint, never a race.
- **Import as worn gear.** Named an item's full name in place of a race (an item worn on the body,
  `CharacterWorkspaceBranch.isImportWorn`), Import then asks the gender whose body the model fits (empty for every
  gender) and the side it is worn on (left, right, or empty for either) and writes the mesh to `assets/mesh/worn/<Name>.arpg`, listing it in the item's `worn` in place of any it
  held for the same rig, gender and side (`importWorn`). Model a piece by opening a body model on the rig and shaping it
  over the body, so it keeps the body's weights and morphs; set `hides` in the Properties tab.
- **Import** (the open model, saved first) asks the race it joins (a race already on its rig, or a new name), the
  gender it is the model of (a registered gender) and an entity to make (empty for none). Its mesh is written to
  `assets/mesh/characters/<race in lower case>/<Name>.arpg`; an existing race's gender of that name wears it (a gender
  the race lacks is made), a new race is made with that gender alone, looking like the race it was previewed beside
  and not yet `playable`; the entity is made with the race. The race then opens. The workspace file stays.
- **Open** also takes a `.glb`: it is brought into the mesh being edited as glTF Import always was (see
  `.claude/docs/GLTF.md`).

## Stamps

Stamps are body parts a character is built from, the way a modular creature kit's parts are cut from finished bodies
(`editor.bootstrap.stamppipeline`, schema `Stamps`, files under `assets/stamps/`). Each carries its own bone fragment,
skinned mesh (the compact rigged layout, with UVs, seams, masks and morphs), rig layers, and its open edge loops.

- **Format** (`StampBuilder`): `bones` are rig bone entries; one naming no `parent` hangs from the host bone it joins.
  They are built into a detached rig of their own (`StampRigUtility.toRigArpg`, `RigManager.buildDetachedRig`) with
  the placeholder `EditorSetting.STAMP_HOST_BONE` first at the origin, and `mesh` is read against it
  (`MeshManager.parseRiggedMesh(arpg, rig, name)`); weights left to the host rest on the placeholder. `plug` (position,
  `normal` facing out of the part toward its host, `radius`) is the loop it joins by, absent for a root part, which
  starts the body over; `sockets` are the loops other parts join it at, each named and riding one of its
  bones. Vectors are `{x, y, z}` objects, as rig entries hold them. Positions are in the stamp's own space: the plug's
  centre (or a root part's middle) at the origin. A fur stamp names `fur` instead of a plug (`StampFurStruct`: the
  patch `radius`, the `spacing` between roots and the `jitter` its cards vary by), its mesh one strand card rooted at
  the origin, standing along +y and bent toward +z at its two inner rows, hair-zone faces textured with the strand tile,
  v running from 1 at the root to 0 at the tip (`stamps/fur/`: Short Fur and Long Fur). `animations` are the motion
  the part brings, by the clip each joins (`humanoid/HumanoidWalk`): `tracks` keyed on the stamp's own bones for either
  side, and `left` and `right` for a part whose sides move their own way (an arm swings against the other). The
  humanoid parts carry every Humanoid clip that keys their bones, their `left` and `right` taken from the clip's
  `_l` and `_r` tracks.
- **Manager** (`StampManager`): the palette, the stamp in use (shared, picked in the Stamps tab or the Hierarchy), live
  rebuilds from the Info Panel, and each stamp's emblem (`StampThumbnailBranch`: a front view rasterised into a square
  with a z-buffer, shaded by facing).
- **Stamps tab** (`editor.runtime.stamps.StampsContext`, the Textures menu's Tools section): tiles of every stamp, a
  row of `STAMP_TILES_PER_ROW` injected at a time with each slot filled from it (`StampsMenuSystem`), the one in use
  tinted, then its details. Anchor a tile's emblem by its centre: an emblem anchored to the top of a tile in a stacked
  row is placed from the panel's top instead.
- **Fixed.** The Stamps schema sets `fixed_entries`: the Hierarchy edits and deletes the game's stamps but makes and
  copies none, and nothing cuts a new one yet. Stamp tools come later, as their own work.
- **Placing** (the Sculpt tab's Brush, `CharacterEditorMode.placesParts`, while a stamp is in use:
  `CharacterEditorManager.armsStamp`; its Scale, Turn and Comb are the Brush's options on the Sculpt canvas, shown with
  Mirror; `CharacterEditorStampSystem` in every character tool tab, acting only while armed). No stamp is in use until
  one is picked in the Stamps tab or the Hierarchy; joining puts it down (`disarmStamp`), and so does Escape before it
  is set down, so the Brush sculpts again. It places only on the Body in the Bind pose (`placesStamp`), and says so
  otherwise. The stamp in use follows the pointer over the body in the bind pose, snapping to an open loop whose centre projects
  within `CHARACTER_STAMP_LOOP_SNAP_PIXELS`, otherwise aimed at the face under it
  (`CharacterEditorMeshViewSystem.pickSurface`: the hit point and the face's Newell normal).
  `CharacterPlacementBranch.resolve` works the placement out (`CharacterPartPlacementStruct`): it must lie within
  `CHARACTER_STAMP_ATTACH_REACH_SHARE` of the rig's height of an attach point (the joint of a bone that carries the body
  and is at least `CHARACTER_STAMP_JOINT_SIZE_SHARE` of that height across, or an open loop's centre: the yellow dots),
  and fit the mesh limit, or it is red with the reason in the status line. Its bones hang from the bone weighing most on
  the face aimed at, or on the loop. The side is the sign of x past `CHARACTER_STAMP_CENTRE_SHARE`; a part whose first
  sided top bone names the other side is mirrored (the stamp's space reflected in x, its faces wound back), so a left
  arm joins the right shoulder facing forward. The matrix carrying the stamp's space into the bind pose is T(point) ·
  tilt · twist · align · scale · mirror: align turns the plug's normal onto the surface's inward normal by the shortest
  arc, twist turns about that axis by Turn (`,` and `.`), scale is Scale, tilt is
  the pointer's turn once set down. A click where it is green sets it down, moving the pointer turns it
  (`CHARACTER_STAMP_TURN_DEGREES_PER_PIXEL`, about the view's up and side), a second click joins it, a right click or
  Escape lifts it. While mirroring, an off-centre part's twin (`resolveTwin`, the matrix reflected in x) is shown and
  joined with it. A root part stands at the rig's root and joins at once, replacing the whole body (Undo brings it
  back). The overlay draws the attach points and the part's edges where it would join, green or red, over everything
  (`CharacterEditorOverlaySystem.syncStampOverlay`).
- **Joining** (`CharacterGraftBranch.graft`, one edit of the body and its rig): every stamp bone is added to a copy of
  the rig entry where the placement carries it (its frame the placement's turn times its own, reflected when mirrored,
  its joint carried by the whole matrix, sizes scaled), its top bones under the host bone, each renamed for the side it
  joins and numbered past every name the rig holds (`upper_arm_2_l`), with the part's layers (a twin adding its bones to
  the same layers) and a record of the bones it brought (the rig's `stamps`: the part's name, numbered past any record
  of it, and its bones; a twin's bones join the same record, and Remove Bone and Rename Bone keep every record in step,
  `CharacterRigBranch.followStampBone`); the rig is applied live (`CharacterRigBranch.replaceRig`). The body then opens where it joins: the
  open loop aimed at, or a hole of the faces facing out within the plug's radius of the point, flooded across edges
  from the nearest and closed over any face left enclosed. The stamp's vertices, faces, weights (onto the grafted bones
  by name, the host placeholder onto the host), masks, shape keys (by name, their deltas turned), textures, zones, UVs,
  pins and seams come in carried by the matrix. Its plug loop (the open loop of its faces nearest the plug) meets the
  body's rim (`CharacterLoopUtility`: loops run as the faces beside them wind): rims of as many vertices pairing within
  `CHARACTER_STAMP_WELD_SHARE` of the radius are welded vertex to vertex at their midpoints, weights, masks and shape
  keys blended, the rim marked as a seam; any others are bridged, walked along both rims by length, quads where they
  keep pace (`CHARACTER_STAMP_PACE_SHARE`) and triangles where one steps alone (laid with one corner doubled and
  settled into triangles as the edit ends),
  the strip textured and laid out in UV as the body's face beside it, the part's rim marked as a seam. A part cut from
  the same body back into its own hole welds exactly. Cards (hair-zone faces) are strands over the surface, never the
  surface: their rims are never loops (`CharacterLoopUtility.isSurface`), a hole never starts on one, and a hole cuts
  away every card standing in it.
- **Animations** (`CharacterClipBranch`, the modular kit's animation layers): once joined, each clip the part names
  that plays on the character's rig gains one entry under `parts`, named after the part's top bone as it joined
  (`upper_arm_2_l`), holding its tracks on the renamed bones: the side's own group where the part brings one,
  otherwise `tracks`, mirrored (a turn's y and z and a move's x negated) when the part was. The clip plays as one
  (`AnimationBuilder` lays every part's tracks in with the clip's own; a bone may be keyed only once across them),
  and the Animation Editor edits a part's bones in that part (`AnimationEditBranch.findBoneTracks`), so each part's
  motion can be adjusted apart later. Joined clips are built again live (`AnimationManager.updateClip`) at once and
  whenever the rig's bones change (`CharacterRigBranch.apply`), and are saved and reverted with the character.
- **Fur** (`CharacterFurBranch`, the hair-card standard applied to a body): a fur stamp follows the pointer over any
  surface face, never snapping, and lays at the first click. `resolveCards` scatters roots over the surface faces
  facing it within its patch, one per square of the spacing each face's area holds, each placed on its face by a hash
  of the face and the card (`BiomeFieldUtility.hash01`, `CHARACTER_FUR_SALT`), so the same aim lays the same fur. Each
  card stands along its face's normal, combs along the surface toward `CHARACTER_FUR_COMB` (down and back on a body
  facing +z; the tool's Comb, flipped by `/`, sends it forward and up), turned about the normal by Turn (the other way
  for a mirrored twin) and its jitter, sized by Scale and its jitter; its frame and root are kept in the placement for
  the ghost and the laying. `lay` brings every card in as one plain mesh edit, each weighed as its root is (the root's
  face corners blended bilinearly through `CharacterWeightBranch.blend`) and moved by every shape key as far as its root
  is, its faces the card's. The bends are modelled into the card, as game hair cards are; `CharacterHair.glsl` sways
  fur in the wind as it sways hair, coloured by the hair colour. While mirroring the twin lays with it.

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
  `CharacterRigRowBranch`; viewport: `CharacterEditorRigSystem`. The Rig tab's tools are Select, Move, Rotate (turns the
  selected bone's bind `rotation` about the line of sight, children carried, the twin turned the mirrored way while
  mirroring: `CharacterRigBranch.rotateGesture`, one undo step), Scale, Place and Erase. A bind edit never moves the
  mesh, as a rig tool keeps the skin where it is; Pose on the mesh and weights layers moves what a bone carries.
- Stamp placement: `CharacterEditorStampSystem` (pointer, set down, tilt) asks `CharacterEditorManager.resolvePlacement`
  (`CharacterPlacementBranch`, the body's open loops and attach points cached per edit) and joins through
  `placeStamp` (`CharacterGraftBranch`). The placement is worked out again only when the pointer, the view, the stamp,
  the tool options or the body moved; the ghost is rebuilt only when the placement's revision moves.
- Clip edits: `AnimationEditBranch.edit` makes every edit on a copy of the clip and puts it in place through
  InfoManager (a clip that cannot be read is refused with the reason); pose drags go through
  `AnimationEditorManager.beginPose / writePose / endPose / cancelPose`, each one step of the recorder's history
  (`RecorderManager.beginGesture / endGesture / cancelGesture`); viewport: `AnimationEditorPoseSystem`.
- Sliders: `CharacterSliderBranch` lists, reads, steps, types, cycles and acts on every slider row (keys
  `CHARACTER_EDITOR_KEY_SHAPE*`, `DRIVE_*`, `CONTROL_*`; a drive's key index is its control's slider number times
  `CHARACTER_EDITOR_DRIVE_KEY_STRIDE` plus the drive's place), writing the race through `CharacterEditBranch`
  (`setSliderField`, `setSliderPlayer`, `setGenderShape`) and the controls through `CharacterControlBranch`, which owns
  the rig's Bone Controls document (`INFO_SCHEMA_BONE_CONTROLS`, made the first time a control is added) and the picks.
- Slider channels: the Rig tab lists, under the selected bone's Sliders header, a choice per `BoneSliderChannel`
  (`CharacterRigRowBranch.addSliderRows`); `CharacterRigBranch.toggleSlider` writes the bone's `sliders` and its twin's,
  since a pair offers one slider, as one rig edit. The rig applies live, and `CharacterRigBranch.refreshEntry` builds
  the race's preview again only when the channels' signature (`resolveSliderSignature`) moved, so a joint drag never
  rebuilds the race.
- The preview look (gender, tones, sliders, blink, build, head) is put on any viewport's appearance by
  `CharacterEditorManager.applyLook` (`CharacterLookBranch`); never set it piece by piece in a viewport.
- Rotation and projection math shared by every viewport gesture: `CharacterPoseUtility`.

## Traps

- **Faces are quads or triangles** (`RiggedMeshStruct.getFaceCornerCount`, `isTriangle`, `getNextCorner`): walk a
  face's corners up to its count and step round it with `getNextCorner`, never `% QUAD_VERTEX_COUNT`. A triangle keeps
  four slots, its fourth repeating its third (`setFaceVertex` and `setFaceUV` on corner 2 keep it so), so storage stays
  four slots a face. An edit may lay a triangle as a quad with one corner doubled, the way the older tools do;
  `CharacterMeshBranch.finishEdit` settles every such face into a true triangle (`settleTriangles`), and a tool
  rewriting a triangle as four slots first sets it back to a quad (`setFaceCornerCount`). Quad-only operations (Loop
  Cut, Dissolve Loop, the topology brush's refine) stop at or refuse triangles.

- **A joined part undoes with the rig.** Its mesh step holds the rig as it stood before and after
  (`CharacterMeshHistoryStruct.getRigRestore` / `getRigExpected`); undo and redo put the rig back first, through
  `CharacterRigBranch.replaceRig`, with the open mesh's other steps held out of the relay
  (`CharacterMeshBranch.holdHistory`), since each was recorded against the rig it comes back to. That happens only while
  the rig still stands exactly as the joining left it: after another rig edit, undo takes back the mesh alone, the
  part's added bones left unused for the Rig tab to remove, and a joining waiting to be redone is dropped, as is any
  step waiting to be redone whose weights name bones the rig no longer holds.
- **A part taken back leaves its clip parts behind.** Undo takes the part's bones out of the rig, but its entries
  under the clips' `parts` stay; a part naming a bone the rig does not hold is left out of the clip whole, so it plays
  nothing, and a part joined again under the same top bone name replaces it.
- **The body is near the mesh limit.** The Human body holds about 15,000 of the 16,383 quads a rigged mesh takes, so
  only small parts join it whole; cut parts away first (Mesh tab) or join them to a smaller body.

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
- **Bones load frozen; added bones are live.** Clips, masks, skinned buffers, appearances and live poses are laid out
  by a rig's bones at load. `RigManager.updateRig` applies a rig in place while the bones it was loaded with keep
  their names, places and parents (`RigData.keepsBones`, `RigHandle.getLoadedBoneCount`); bones after them may come,
  go, be renamed and reparented live, as a skeleton merges new bones (Unreal's Skeleton tree), each change moving
  `RigHandle.getStructureRevision`. Everything laid out by bones follows: `AnimationStateHandle.holdRig` lays its
  per-bone arrays out again (bones it held keep their state), `SkinnedBufferData.matchesMesh` relays the palette row,
  a clip built before has no track for an added bone (`AnimationClipData.getBoneTrack`), a layer's mask reaches it
  through its nearest known ancestor (`AnimationLayerStruct.resolveBoneMask`), an appearance shapes only the bones it
  was built with (`AppearanceData.getBoneCount`), and the editor moves every open mesh's weights, undo steps
  included, onto their bones by name (`CharacterRigBranch.resolveBoneRemap`, `CharacterMeshBranch.relayBones`; a bone
  gone hands its weight to its nearest ancestor) and previews the race again. A change to a loaded bone (reparenting
  or mirroring one) is written to the rig and takes hold on the next start. Only added bones
  (`CharacterRigBranch.canRename`, `RigHandle.isAddedBone`) may be renamed or removed, because nothing loaded names
  them.
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
- **Posed transforms map back through the skinning.** A transform held posed writes bind positions through each
  vertex's inverse blended matrix, taken when it begins; a vertex weighed onto no bone moves as it is. Sculpt strokes
  stay bind-only and are refused posed: the preview poses on the GPU and the mesh view skins on the CPU, but edits are
  written to the bind-pose file.
- **Symmetrize after a lopsided hair rig.** Hair chains laid on asymmetric cards have no mirrored twin chain, so a
  symmetrized hair's new side weighs onto the kept side's chains (the status line counts them); run Rig Hair Locks and
  Weigh Onto Locks on the symmetric hair to give it chains of its own.
- **Part layers need rig parts.** A rig without `parts` lists only the Body and Hair; name the parts in the rig (or with
  Add Layer From Bone), never in code. Region keys are part names, so renaming a part moves a window picked on it back
  to the Body.
- **Quaternion poses, Euler keys.** Clips keep Euler-degree curves (as DCC tools author them); `AnimationStateHandle`
  turns every sampled turn into a unit quaternion (`RigMathUtility.toQuaternion`, the `Rz * Ry * Rx` order), blends
  brackets, cross-fades and override layers by slerp, multiplies additive layers on, and composes each bone as bind
  rotation, then shaping, then pose. For a bind turned about z alone (the A-pose arms) this equals the old Euler sum, so
  existing clips play unchanged. The editors key against the bind the same way
  (`CharacterPoseUtility.composeBindTurn` / `toKeyEuler`, nearest the key it replaces so keys never spin the long
  way); never add a key to the bind rotation.

## Animation

- **Clips key curves, one per axis of each channel.** A bone's track in a clip (and in a joined part's tracks, and a
  stamp's `animations`) is an object of channels (`rotation`, `position`, `scale`), each of the axes it keys (`x`,
  `y`, `z`), each a list of keys: `time`, `value`, a `mode` when not `auto` (`aligned`, `free`, `linear`,
  `constant`) and, for aligned and free keys, `in` and `out` handles (a moment and a value from the key), as Blender's
  F-curves and glTF's cubic splines hold them. An axis with no curve holds its channel's default. A clip's `morphs`
  are curves of the same keys, one per morph (the converted expressions keep `linear` keys). The one reader and
  writer is `AnimationCurveArpgUtility`; the curve is the engine's `CurveStruct` (`engine.util.mathematics.curves`),
  the same Bezier curve the recorder's keys are (`RecordingCurveStruct` extends it), so what the Keys Graph shows is
  exactly what the game plays (`AnimationClipUtility.sampleClip`). Auto keys at a looping clip's ends run round its
  seam (`CurveEnds.CYCLIC`, the last key repeating the first) and a one-shot's ease flat into its holds (`CLAMPED`).
- **The timeline is the recorder's.** `AnimationEditorManager` opens its active clip there
  (`RecorderManager.openAnimation`, `RecordingAnimationBranch.parse`): one take and one clip for each layer, each on a
  lane of its own named after it, as a sequencer stacks one character's motion: Base (the clip's own tracks and its
  morphs, `RecordingMorphTrackStruct`) on the first lane, then the clip's own tracks of each body part a stamp joined,
  one lane to each record in the rig's `stamps` while lanes remain (`readStamps`), then every part a stamp joined with
  motion of its own (`parts`, the modular kit's animation layers) in the clip's order, all spanning the clip's whole
  length so they read as the one clip they play as. A stamp's lane is the timeline's view only: its tracks are written
  back into the clip's own `tracks`. A layer holds a `RecordingBoneTrackStruct` for every bone it keys (Base: every bone
  no other layer holds; never spring bones), each holding a curve for every slot and nested in the Layers tab under the nearest bone above it the layer
  holds. The Keys tab lays the layer out as a dope sheet (`RecorderKeysSystem.injectDopeSheet`): a summary row for
  every bone and morph track marking each step any of its curves keys, a press on its name picking it
  (`RECORDER_KEY_ROW_SUMMARY`) and showing its first curve on the Keys Graph; the picked one's curves are the
  Properties tab's rows (`RecordingRowBranch.buildValueRows`), each its value at the playhead, keyed there when
  stepped or typed (`RecorderManager.stepCurveAtPlayhead`, `setCurveAtPlayhead`), and a Graph choice picking the one
  the Keys Graph shows (`cycleGraphCurve`). A press on a lane picks its layer
  (`pressAnimationLayer`), and a bone picked in the Animation Editor picks
  the layer that keys it (`selectBoneLayer`); the pose tools and pose actions work across every layer at once, each
  bone keyed in its own layer, and only while the Recorder's Record is lit (`RecorderManager.record` toggles
  `isAutoKeying` on an animation clip; `AnimationEditorManager.beginPose` refuses otherwise). A click picks a bone
  at its joint or anywhere along the segment drawn from it, in every character viewport alike
  (`CharacterBonePickUtility`). Keys edited on
  the Keys tabs are written back into the clip on the frame they change (`RecorderManager.writeAnimation`), every
  other field and every track of a bone the timeline does not show kept as read; a change made anywhere else (the pose
  tools, the Properties rows, the Info Panel) is read back as a step of the same history (`refreshAnimation`). The
  playhead, playback, undo and redo are the recorder's; `AnimationEditorManager.getTime` reads its playhead. The
  Animation Editor's Properties rows key a bone's pose at the playhead on every axis it keys or that leaves its
  default (at least its first rotation axis), and its bone's keys are every moment any of its curves holds one.
- **The Tracks tab** (`RecordingsPanelSystem`) lists `AnimationEditorManager.collectCharacterClips` while animations
  are worked on, turning there and back as the work moves and by its Media / Animations switch in between
  (`RecorderManager.showAnimationTracks`); a row clicked runs `AnimationEditorManager.openClip`, through `InfoManager.selectEntry`, so the
  Hierarchy, the Info Panel, the Animation Editor and the Timeline all follow.
- **Traps.** A clip open on the timeline refuses what only a media file does (laying, moving, cutting or recording
  clips, tempo and lanes, export, rename, copy and delete: `canArrangeTimeline`, `refuseAnimation`); its layers are
  made by joining stamps, never on the timeline. A clip holds at most `SONG_LANE_COUNT` layers. A curve keeps at least
  one key on the Keys tab; to unkey a bone, delete its key in the Animation Editor's rows.

