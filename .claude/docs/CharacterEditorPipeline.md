# CharacterEditorPipeline

Where the character tools live, which method owns each operation, and the traps. Read it before working in the character
tool tabs (Appearance, Equipment, Mesh, Sculpt, Weights, Paint, Rig, UV, Stamp), the UV Editor, the Animation Editor,
their docker windows, races and their genders' models, or a character's appearance (morphs, sliders, bone controls,
zones, masks, overlays, shaders). `.claude/CLAUDE.md` still applies in full.

---

## Windows

Every character tool is split into windows, the way Krita splits its dockers: every part of making a character is its
own tool tab, laid out however the owner likes, each working the one character through the shared state (see
`.claude/docs/Editor.md`). A viewport tab holds the status bar and the viewport; saving, reverting, copying, deleting,
undoing and redoing are the editor's File and Edit menus' (`EditorSetting.FILE_MENU_CHARACTER`,
`FILE_MENU_ANIMATION`), shared by each tool's viewport and docker tabs; everything else is its own tab, opened
from the editor's Characters menu (its Tools section holds the Outliner, above the Character section's tool tabs and the
Animation section), the Content menu (Color Wheel, beside Layers) or the Textures menu (Texture Maps), and restored by
layouts like any other.

| Window | Context | Shows |
|---|---|---|
| Character Appearance, Equipment | `CharacterAppearanceContext`, `CharacterEquipmentContext` | the live preview in its look mode, no layers |
| Character Mesh, Sculpt, Weights, Rig, UV | `CharacterMeshContext`, `CharacterSculptContext`, `CharacterWeightsContext`, `CharacterRigContext`, `CharacterUVContext` | the region picked in the Layers tab framed and shown alone, its tool canvas |
| Character Stamp | `CharacterStampContext` | the whole character in the bind pose, the Stamp canvas: the part chosen in the Stamps tab aimed at the body and joined (see Stamps below), no layers |
| UV Editor | `editor.runtime.uveditor.UVEditorContext` | the UVs of the faces selected, over the tile they are textured with (see UV below) |
| Character Paint | `CharacterPaintContext` | the whole character, the Paint canvas; the paint layers of the map painted in the Layers tab |
| Outliner | `CharacterOutlinerContext` | the outliner rows of the character or animation viewport worked in last (`OutlinerProvider`): `CharacterEditorManager.buildListRows`, `AnimationEditorManager.buildListRows` |
| Color Wheel | `ColorWheelContext` | the paint colour: a hue ring with a saturation and brightness triangle turned to the hue (`ColorWheelSystem`), then Hue, Saturation, Brightness and Red, Green, Blue ranged rows (`CharacterEditorManager.buildColorRows`) |
| Texture Maps | `TextureMapsContext` | every map of the texture painted (`CharacterEditorManager.buildMapRows`), the one painted selected |
| Animation Editor | `AnimationEditorContext` | the posed character; the Animation Pose tool canvas while a clip plays on a character |
| Animation Timeline | `AnimationTimelineContext` | transport, summary track and a dope sheet lane per bone |
| Properties | `PropertiesContext` | the value rows of the character or animation tab worked in last |

- **One tab per tool.** Every tool tab extends `CharacterToolContext` and names its `CharacterEditorMode` (`getMode`)
  and title (`getTabTitle`, `EditorSetting.TAB_TITLE_CHARACTER_*`); each opens from its own Characters menu button
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
  region and mode to `CharacterEditorManager.setRegion` / `setMode`, so the Outliner and the Properties tab follow the
  window worked in last. The context creates every viewport system; the mesh view, sculpt, rig, paint and overlay
  systems act only in the modes their flags name. `CharacterToolContext` answers its tool canvas, tool actions,
  Properties rows, templates and File and Edit table for its mode, all the character editor's. The keys of every tool
  tab are one file, `shortcuts/CharacterEditor.arpg`, naming `CharacterToolContext` (a shortcut file naming a base
  context keys every tab built on it, `ShortcutManager.findShortcuts`) and kept to their tools by `canvases`; Undo, Redo
  and Save run the File menu's action (`FileMenuBranch.run`).

- **The Appearance outliner is the player's creator.** In Appearance the Outliner lists everything the game's creator
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
  outliner pick one (`picksPose`: Equipment, Weights). Only Blink, Weight and Head, preview controls, stay in the
  Properties tab.
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
  schema `BoneControls`). Each bone slider row carries Pick For Control; Add Control at the foot of the outliner makes a
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
- **One outliner.** There is one Outliner (`CharacterOutlinerContext`), never one per tool: a viewport that
  lists outliner rows implements `OutlinerProvider` (`editor.bootstrap.propertiespipeline.properties`) beside
  `PropertiesProvider`, and `PropertiesManager.getOutliner` names the one worked in last, the way the Properties tab
  follows. The outliner implements `CharacterViewRowSource` over it and creates `CharacterViewPanelSystem`,
  `CharacterViewDockSystem` and `ToolCanvasSystem`; the dock system opens `menus/editor/CharacterDock.arpg` and
  refills it only when the source's revision moves (the outliner's own, moved when the window followed changes or its
  list does). Row templates stay in the tool's own menu
  (`EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES`, `ANIMATION_EDITOR_ROW_TEMPLATES`) so
  their buttons route to the tool's branch. A tool without a text row template must never list a `TEXT` row.
- Tools are the Tools window's (`ToolCanvas.CHARACTER_MESH`, `CHARACTER_SCULPT`, `CHARACTER_WEIGHTS`, `CHARACTER_RIG`,
  `CHARACTER_UV`, `CHARACTER_STAMP`, `UV_EDITOR`, `ANIMATION_POSE`); brushes the Brushes window's. A tool strokes the
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
  `CHARACTER_RIG_BALL_SHARE` of its length. The Rig tab's **Rig Digits** lays a chain of `CHARACTER_RIG_DIGIT_SHARES`
  joints on each (`CharacterRigBranch.addDigits`, named `<digit>_<n>_<side>`, a toe `toe_<side>`); the Weights tab's
  **Weigh Onto Digits** (`CharacterWeightBranch.weighDigits`) then hands each vertex of the hand or foot bone to them,
  a digit's own vertices wholly, blended across each joint, the palm near each digit's root partly. The digits are
  added bones, so they apply live and the two run one after the other.

## Modes

`CharacterEditorMode` carries its behaviour as data: its tool canvas, whether it edits a worn mesh, whether it holds
the bind pose, whether it draws vertices, and its status hint. Check those flags, never `mode == X`, except where a
mode's own branch is chosen (`SCULPT` strokes the shape, `RIG` undoes the rig). `editsVertices` is every mesh mode but
Paint and Stamp, the modes whose tools select and shape vertices (`CharacterEditorSculptSystem` acts only there);
`placesParts` is Stamp, which works on the body alone (`resolveRegionPartKey`) and takes no regions.

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
  symmetric still mirrors every stroke. `CharacterViewUtility.round` rounds half away from zero, so a point and its
  mirror always round alike.
- **Canvas options.** Mirror, X-Ray and Bones are `ToolOption`s marked canvas-wide (`isCanvasWide`): every tool on a
  canvas that honours one lists it under its own options, last, in the Tools panel. Mirror (on by default) is honoured
  by the mesh, sculpt, weights, paint, rig and stamp canvases, X-Ray by the mesh and weights, Bones
  (`CharacterBoneDisplay`: joints, sticks from each joint to its parent's, both, or hidden) by the mesh, weights and
  rig. `CharacterEditorManager.isMirror` / `isXray` / `getBoneDisplay` read them from
  `ToolManager` for the mode's canvas; never keep a copy, and never list them as Properties rows.
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
- **Sculpt tools.** Brush, Smooth, Relax (even spacing along the surface without shrinking), Inflate, Pinch, Crease
  (a sharp line drawn in under the brush's centre, a ridge inverted: lid folds and lip lines), Flatten and Nudge, all
  in `CharacterShapeBranch.resolveMove`.
- **Topology brush.** The Sculpt canvas's Topology tool (`D`) changes the faces, not the shape
  (`CharacterTopologyBranch`): a stroke opens one undo step, gathers how far the brush covers each vertex (and its
  twin with Mirror on), and once a frame (`CharacterEditorManager.showStroke` calls `apply`) changes every face whose
  corners it all covers. Plain strokes refine by Schneiders' 3-refinement, as adaptive all-quad meshing does: a face
  splits into nine on a lattice of thirds and each face beside it takes the transition template its refined edges call
  for (`EditorSetting.CHARACTER_TOPOLOGY_TEMPLATE_*`: one edge, two at a corner, two opposite); a face left with three
  or four refined edges joins the refined set until none is, so the mesh stays all quads with no hanging vertex.
  Ctrl reduces by quad-mesh simplification (Tarini et al.): the smallest covered faces collapse across their shorter
  diagonal, refused on an open edge or UV seam, where the rings would pinch (the link condition) or a face about them
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
(`CharacterModeProvider`), so the Outliner lists the parts and the Properties tab the UVs selected.

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

The Paint tab (`CharacterEditorMode.PAINT`, `ToolCanvas.CHARACTER_PAINT`: Brush, Erase, Smooth, Fill, Pick, the Brushes
tab's brush) paints the open part's texture maps on every region. It is a mesh mode that `paintsMaps`, so the
mesh view skins the part in any pose and the sculpt system stays out of it.

- **Projection painting**, as Blender and Substance paint: `CharacterEditorPaintSystem` lays a stamp every brush
  spacing and hands `CharacterPaintStampStruct` every face of the region under the brush whose middle the camera sees,
  its corners on the window; `CharacterPaintBranch.stamp` rasterises each face's two UV triangles over the map and
  paints each texel by the tip's coverage where the same barycentrics place it on the window, so strokes cross UV
  seams; texels within `CHARACTER_PAINT_BLEED_PIXELS` of an island edge are painted too. Fill covers the island of
  faces joined corner to corner around the face clicked (`pickFace`), Pick takes the flattened map's colour there.
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
  it and holds its Opacity; the colour lives in the Color Wheel tab (Hue, Saturation and
  Brightness, `CHARACTER_PAINT_KEY_HSB`, a grey keeping the hue last chosen in `CharacterPaintBranch.getHue`; Red,
  Green and Blue, `CHARACTER_PAINT_KEY_COLOR`) and the map painted is picked in the Texture Maps tab
  (`CHARACTER_PAINT_KEY_MAP_ITEM`, `CharacterPaintBranch.setMapIndex`), every key read and written through
  `CharacterPaintPropertyBranch` and listed by `CharacterPaintRowBranch`; Add Layer and Remove Layer are the canvas's
  tool actions. Save (File) writes every
  changed map flat into its image, Revert puts the atlas back as the images hold it, Undo and Redo (Edit) step the
  paint history in the Paint tab. Painted maps are the owner's art: never catalogue them.
- **Generate Maps** (File menu, `FileMenuAction.GENERATE_MAPS`, Paint tab only; as Materialize or Bitmap2Material
  generate maps): `GenerateMapsDialogBranch` opens a dialog over the window on the albedo of the part painted (layers
  and all), listing every `MapGenerateOption` (height detail, softness, contrast and Dark Is High; normal strength;
  occlusion strength and radius; roughness and its variation; metallic; which maps are written) beside a preview
  worked out at `MAP_GENERATE_PREVIEW_SIZE` whenever a setting moves (`CharacterMapBranch.renderPreview`): the result
  lit through the generated normal and occlusion, or any one map. `MapGenerateUtility` holds the image work
  (luminance, a high-pass height, Sobel normals with green up, occlusion from the height's hollows, roughness from the
  darks; running box blurs, so any radius costs the same). Generate works every map out at the texture's own size and
  paints each over its map's layer as one undo step (`CharacterPaintBranch.writeMap`), so Save writes and bakes them
  and Undo takes each back; radii are in the texture's pixels and shrink with the preview.
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
  `.claude/docs/ARPG.md`), quads still the unit: `positions` (each distinct vertex once), `bones` (the bone names the
  file uses, resolved against the rig by name at load) with `influence_counts`, `influence_bones` and
  `influence_weights`, `quads` (four vertex numbers a face) beside `quad_textures` and `quad_zones` (numbers into the
  `textures` and `zones` name lists; `MeshZone`: skin, eye, brow, lash, hair, cloth, mouth, teeth), `quad_uv_flags`
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
- **Import and Export.** The File menu's Import Model and Export Model move the mesh being edited to and from a
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
  Makeup), and the Appearance outliner's Marks rows set the previewed gender's `default`. Each overlay's `region` is the
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
  `morphs` curves (`MorphTrackStruct`, keyed `time` and `value`, linear between keys,
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
  `CharacterEyeMaterial`) and `features/humanoid/teeth/HumanoidTeeth` (`HumanoidTeethMesh`: a UV-mapped strip per
  row of teeth, the upper on the head in front of the lower on the jaw, and the tongue; `CharacterTeethMaterial`; nine
  variants, from Standard to Braces, each an image set whose tint mask keeps metal from taking the teeth color).
  `HumanoidRig` names its `parts` (head showing hair, eyes and teeth and taking the face; ears; eyes holding the eyes;
  mouth holding the teeth; hands; feet) and seven
  `poses` (`animations/humanoid/poses/`). All are generated (catalogued); the character tools edit a rigged
  mesh of at most `RIGGED_MESH_VERT_LIMIT` corners (their overlays draw every corner). Each hand is one bone: hanging at the side its palm faces the thigh with the
  thumb forward (+z), so `hand_l` is a left hand at +x; keep that chirality on any hand you model or mirror.
- **Features are the gender's.** The Outliner's Features rows (Appearance mode) read and write the previewed
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
  afresh. A chain runs only while something worn weighs onto it (`AppearanceHandle.resolveSprings`, reading
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
  sided bone's collider is added, removed, reshaped and moved with its twin (named as its mirror). The selected bone's
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
  v running from 1 at the root to 0 at the tip (`stamps/fur/`: Short Fur and Long Fur).
- **Manager** (`StampManager`): the palette, the stamp in use (shared, picked in the Stamps tab or the Hierarchy), live
  rebuilds from the Info Panel, and each stamp's emblem (`StampThumbnailBranch`: a front view rasterised into a square
  with a z-buffer, shaded by facing).
- **Stamps tab** (`editor.runtime.stamps.StampsContext`, the Characters menu's Tools section): tiles of every stamp, a
  row of `STAMP_TILES_PER_ROW` injected at a time with each slot filled from it (`StampsMenuSystem`), the one in use
  tinted, then its details. Anchor a tile's emblem by its centre: an emblem anchored to the top of a tile in a stacked
  row is placed from the panel's top instead.
- **Save Selection As Stamp** (File menu, `FileMenuAction.SAVE_STAMP`, `CharacterStampBranch`): the whole faces selected
  on the open part become a new stamp. A bone is the part's own when the part holds `CHARACTER_STAMP_BONE_SHARE` of its
  influence over the whole mesh; the bone they all hang from is the host (none for a root part), and a bone mostly
  outside the part hands its weight to its nearest own ancestor, or the host. The cut's open loops (edges the part uses
  once where the mesh uses twice) become the plug (the loop weighing most on the host) and sockets (named after the
  bone the cut took away beyond each). Top bones are written placed and turned in the part's space as the bind pose
  holds them, and every layer whose bones the part holds all of comes with it. The kind is the first `StampCategory`
  its top bone's name carries.
- **Placing** (the Character Stamp tab, `CharacterEditorMode.STAMP`, `ToolCanvas.CHARACTER_STAMP`, tool `STAMP` with
  Scale, Twist, Comb and Mirror; `CharacterEditorStampSystem` in every character tool tab, acting only in that mode):
  the stamp in use follows the pointer over the body in the bind pose, snapping to an open loop whose centre projects
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
  arc, twist turns about that axis by Twist (`,` and `.`), scale is Scale (`[` and `]`, `ToolManager.stepSize`), tilt is
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
  the same layers); the rig is applied live (`CharacterRigBranch.replaceRig`). The body then opens where it joins: the
  open loop aimed at, or a hole of the faces facing out within the plug's radius of the point, flooded across edges
  from the nearest and closed over any face left enclosed. The stamp's vertices, faces, weights (onto the grafted bones
  by name, the host placeholder onto the host), masks, shape keys (by name, their deltas turned), textures, zones, UVs,
  pins and seams come in carried by the matrix. Its plug loop (the open loop of its faces nearest the plug) meets the
  body's rim (`CharacterLoopUtility`: loops run as the faces beside them wind): rims of as many vertices pairing within
  `CHARACTER_STAMP_WELD_SHARE` of the radius are welded vertex to vertex at their midpoints, weights, masks and shape
  keys blended, the rim marked as a seam; any others are bridged, walked along both rims by length, quads where they
  keep pace (`CHARACTER_STAMP_PACE_SHARE`) and triangles held as quads with one corner doubled where one steps alone,
  the strip textured and laid out in UV as the body's face beside it, the part's rim marked as a seam. A part cut from
  the same body back into its own hole welds exactly. Cards (hair-zone faces) are strands over the surface, never the
  surface: their rims are never loops (`CharacterLoopUtility.isSurface`), a hole never starts on one, and a hole cuts
  away every card standing in it.
- **Fur** (`CharacterFurBranch`, the hair-card standard applied to a body): a fur stamp follows the pointer over any
  surface face, never snapping, and lays at the first click. `resolveCards` scatters roots over the surface faces
  facing it within its patch, one per square of the spacing each face's area holds, each placed on its face by a hash
  of the face and the card (`BiomeFieldUtility.hash01`, `CHARACTER_FUR_SALT`), so the same aim lays the same fur. Each
  card stands along its face's normal, combs along the surface toward `CHARACTER_FUR_COMB` (down and back on a body
  facing +z; the tool's Comb, flipped by `/`, sends it forward and up), turned about the normal by Twist (the other way
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
- Clip edits: `AnimationEditBranch.edit` records every edit in the clip's `CharacterViewHistoryStruct`; pose drags
  go through `AnimationEditorManager.beginPose / writePose / endPose / cancelPose`; viewport:
  `AnimationEditorPoseSystem`.
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

- **A joined part undoes with the rig.** Its mesh step holds the rig as it stood before and after
  (`CharacterMeshHistoryStruct.getRigRestore` / `getRigExpected`); undo and redo put the rig back first, through
  `CharacterRigBranch.replaceRig`, with the open mesh's other steps held out of the relay
  (`CharacterMeshBranch.holdHistory`), since each was recorded against the rig it comes back to. That happens only while
  the rig still stands exactly as the joining left it: after another rig edit, undo takes back the mesh alone, the
  part's added bones left unused for the Rig tab to remove, and a joining waiting to be redone is dropped, as is any
  step waiting to be redone whose weights name bones the rig no longer holds.
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
