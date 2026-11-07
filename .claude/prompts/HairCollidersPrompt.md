Continue the AdventureRPG character work. Follow .claude/CLAUDE.md and every file it imports exactly: deliver
as AdventureRPG_Delivery.zip, never commit or push, catalogue every AI asset, and check the Version status
(origin/main and refs/delivery/status) before building. main should be on Delivery 47 (SpringHair); if it isn't,
tell me before building anything. Build this delivery as DELIVERY = main + 1, BASE = main.

Already done and on main (do not redo):
- 45: CharacterEditorRegion, CharacterRegionBranch, CharacterModelingContext (Mesh, Hair, Hand and Foot Editors),
  CharacterDigitBranch, HumanoidRig with full fingers.
- 46: IK handles (RigIkChainStruct, Add/Remove IK Handle, the Animation Editor's IK Handle tool).
- 47: Spring-bone hair. RigSpringChainStruct ("springs" in the rig: bones, stiffness, damping, gravity, drag),
  RigBuilder rules and the Rigs schema; AnimationSpringStruct simulated by AnimationStateHandle over the finished
  pose (body frame, substeps, length and 75° swing cone, hitch riding, teleport reset, zero steady-state garbage,
  about 10.6 µs per character for 14 chains); EntityInstance.moveSprings; AppearanceHandle.resolveSprings (a chain
  runs only while something worn weighs onto it, via MeshHandle.weighsOnto); the Hair Editor's Rig mode with Rig
  Hair Locks, Add/Remove Spring Chain, Weigh Onto Locks and Preview Springs (CharacterHairBranch,
  CharacterRigBranch, CharacterWeightBranch); spring rows in the Properties tab; spring bones never keyed in the
  Animation Editor. HumanoidRig carries hair_1 to hair_14; FemaleLongHairMesh and FemaleMediumHairMesh are weighed
  onto them. CharacterHair.glsl reads each lock's sway phase from its bind position.

Task now: hair colliders, the way studio spring-bone systems do it. Today the hair swings freely and passes through
the head, neck, shoulders and back when the head turns or the body runs. Hair must never pass through the body.

- Colliders are rig data. A rig gains a "colliders" array: each names the bone it follows, its shape (sphere, or
  capsule between two points), its radius and its point(s) in that bone's bind frame. Schema in
  assets/schemas/Rigs.arpg plus the matching RigBuilder rules (known bone, radius within an EngineSetting range,
  capsule ends distinct, names unique), mirrored in RigData/RigHandle like the IK handles and springs.
- Each spring chain gains a "radius" (how thick its joints are against a collider, 0 allowed) as rig data, schema
  and builder rule like its other four settings, with a default in EditorSetting.CHARACTER_RIG_SPRING_DEFAULTS.
- Runtime: AnimationSpringStruct places every collider in the body's frame once per step from the captured bone
  matrices, then after each joint's length and cone hold pushes the joint out of every collider by its chain's
  radius, then holds its length again. Zero steady-state garbage, every array sized once; state the numbers
  (µs per character, bytes per update) before and after, measured the same way as Delivery 47.
- Editor tools in the Hair Editor, in the Tools tab only (never panel buttons), mirroring Rig Hair Locks and the IK
  handle actions:
  - Fit Hair Colliders: fits a head sphere, a neck capsule, a chest/back capsule and a shoulder capsule per side
    from the body mesh vertices those bones carry (the body the previewed gender wears), mirrored on the right,
    leaving any collider already present as it is. Its constants go in EditorSetting.
  - Add Collider / Remove Collider on the selected bone.
  - The selected bone's colliders and its chain's radius show as rows in the Properties tab, every number with a
    slider, step buttons and a typed value, applied live.
  - Rig mode draws every collider as a wire marker (sphere and capsule) through CharacterViewRenderSystem; the
    selected one highlighted. Preview Springs shows the hair resting on them.
- The Hair, Mesh, Hand and Foot Editors share the base context: keep collider tools available to every modelling tab
  through the region rules, the same way the hair region names its own bones.
- Make every asset through my own editor tools. Run the editor (see notes below), use Fit Hair Colliders on the
  female and male bodies, tune nothing by hand-editing files, save through the File menu. Never hand-generate rig
  data with scripts.
- Before starting, read .claude/docs/CharacterEditorPipeline.md (its spring and hair-tool sections), Editor.md and
  Shortcut.md, plus AnimationSpringStruct, CharacterHairBranch, CharacterRigBranch, CharacterRigRowBranch,
  CharacterRigPropertyBranch and RigBuilder, and mirror them.
- Update CharacterEditorPipeline.md and the catalogue, then deliver.

Notes from Delivery 47 that will save time:
- The editor runs in the cloud container: start Xvfb on a spare display, run lwjgl3.Lwjgl3LauncherEditor from a
  direct compile (.claude/docs/Build.md, plus the lwjgl *-natives-linux jars) with the working directory set to
  assets/ and XDG_RUNTIME_DIR set, and drive it with python-xlib XTEST clicks, taking screenshots with ImageMagick
  `import -window root` to find each control. The editor's window is 1280x720 at +320+180 on a 1920x1080 screen;
  the layout persists between runs.
- Bones are structure: save the rig and restart the editor between laying bones and weighing onto them. A weighed
  mesh names its bones, so never revert the rig without the meshes weighed onto it.
- Under software rendering the editor runs at about 16 fps, which exaggerates every spring. Prove the physics
  offline first with a scratch harness against the real classes (RigBuilder.buildData through reflection,
  AnimationSpringStruct stepped over bind matrices with a simulated run and head nod), and only then judge it live.
