# glTF Import and Export

The glTF subset the editor reads and writes, how it maps to a rigged `.arpg` mesh both ways, and the traps. Read it
before touching Import Model, Export Model or any `Gltf*` class. `.claude/CLAUDE.md` still applies in full.

---

## Where glTF stands

- **Only at the editor's edge.** `.arpg` stays the only format the engine and the tools read and write. A `.glb` is
  something the character tool tabs import from and export to so models can move to and from Blender, Maya, ZBrush or
  an asset store. The game never reads one, none goes under `assets/`, and `.gitattributes` marks `*.glb binary`.
- **Outside art is the owner's.** A model brought in through Import Model is not AI-generated: never catalogue it.
- **The specification is the authority:** glTF 2.0 (registry.khronos.org/glTF/specs/2.0/glTF-2.0.html).

## Code

All of it is in `editor.bootstrap.charactereditorpipeline`:

- `util/GltfReadUtility`: opens a `.glb` (`read`), its JSON chunk parsed by `ArpgUtility.parseText` (never a JSON
  library), and reads accessors as floats or whole numbers through offset, stride, normalisation and sparse values.
- `util/GltfImportUtility`: `inspect` (the model's vertices, quads, materials, joints and morph names, as a
  `GltfImportStruct`), `findMissingBones`, `build` (a `RiggedMeshStruct` against a rig).
- `util/GltfExportUtility`: `export(mesh, rig, name, normals)`, the `.glb` bytes.
- `charactereditormanager/CharacterModelBranch`: the two actions and the steps Import walks.
  `CharacterEditorBranch.importModel` / `exportModel` are the File menu's methods (`FileMenuAction.IMPORT_MODEL`,
  `EXPORT_MODEL`, in `EditorSetting.FILE_MENU_CHARACTER`).
- Constants: the `// glTF` and `// Model Import and Export` groups of `EditorSetting`.
- Refusals are catchable `InternalException`s whose message is shown in the status line. Never `throwException`
  for a bad file: that halts the editor.

## The File menu actions

- **Files live in `Documents/My Games/AdventureRPG/Models/`.** Export Model writes `<MeshName>.glb` there. Import
  Model asks for a file name in that folder through the editor's text dialog. There is no file browser yet.
- **Export Model** writes the mesh being edited, its rig and its morphs.
- **Import Model** replaces the mesh being edited, as one edit: Undo brings the old mesh back and Save writes it
  (compact layout). It never writes a file itself and never makes a new mesh file. It refuses a model past the
  character tools' quad limit like any other edit.
- **Nothing is assumed silently.** Bones the rig lacks are listed and added only when the user types `add` (through
  `CharacterRigBranch.addBones`, each under the joint it hangs from in the file, at its place in the world), after
  which Import is run again. A model with no skin asks for the bone it hangs from. A material that does not name a
  texture and zone of its own asks for each, by name.

## Export mapping

| `.arpg` mesh | glTF |
|---|---|
| mesh vertex, per corner UV and normal | one vertex per distinct (mesh vertex, UV, normal): `POSITION`, `NORMAL`, `TEXCOORD_0` |
| UV | `TEXCOORD_0` with v flipped (`1 - v`): glTF's v runs down |
| influences | `JOINTS_0` (unsigned short, the rig's bone number) and `WEIGHTS_0` (float), unused slots zero |
| masks | `_MASKS`, a float VEC4 in `RIGGED_MESH_MASK_CHANNELS` order |
| texture and zone pair | one material, `extras.texture` and `extras.zone`, and one primitive |
| quad | two triangles fanned from one corner, `FB_ngon_encoding` on the primitive |
| rig | one skin: every bone a node in bone order with its `matrix` against its parent, `inverseBindMatrices` from the rig |
| morphs | dense `POSITION` targets on every primitive, names in `mesh.extras.targetNames` |

One vertex set is shared by every primitive. The same mesh always writes the same bytes.

## Import mapping

- The first node with a mesh and a skin is the model (the first with a mesh when none is skinned). All its
  primitives are joined; each must be indexed or plain `TRIANGLES`.
- **Quads.** With `FB_ngon_encoding`, consecutive triangles sharing their first index are one polygon; exactly two
  make a quad, anything else is refused. Without it, a triangle joins the one listed right after it when they share
  an edge, and otherwise the next in the list that shares an edge and lies flat against it
  (`GLTF_QUAD_FLAT_COSINE`); a triangle left alone refuses the import with the count.
- **Vertices.** glTF vertices on one position with the same influences become one mesh vertex, as the mesh format
  merges corners. Weights are scaled to sum to one only when they are off by more than `BONE_WEIGHT_SUM_EPSILON`.
- **Joints** become bones by node name. `JOINTS_1` (more than four influences) is refused.
- **Materials** give their texture and zone from `extras` when they name ones that exist; otherwise the user does.
- **Morph targets** keep their names (`target_<n>` when the file gives none); deltas below
  `RIGGED_MESH_MORPH_DELTA_EPSILON` are dropped.
- `NORMAL` is ignored: normals are always computed from the quads.

## Traps

- **A quad fans from its opposite corner when the quad before it in the same primitive fans from the same vertex.**
  That keeps the two quads apart for an importer and keeps the same two triangles; the imported quad's corners then
  start two places round. Never rely on corner numbers surviving a round trip.
- **Face order follows the primitives**, so faces come back grouped by material.
- **Every imported face authors its UVs**, and a flipped v comes back within one float step, not bit for bit.
- **Only a self-contained `.glb` is read.** External buffers, data URIs, Draco and meshopt compression are refused.
- **Seams and UV pins stay in the `.arpg` mesh.** glTF has no place for them: Export writes neither and Import brings
  none, so mark seams again after an import.
- **Bone transforms are written, never read.** Import takes joint names and, for bones it adds, their positions.
- **Not done yet, only when the owner asks:** animation clips, sub-voxel item export, writing a new mesh file from
  an import, and a file browser.
