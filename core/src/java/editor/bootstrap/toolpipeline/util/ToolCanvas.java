package editor.bootstrap.toolpipeline.util;

import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.toolpipeline.tool.ToolActionGroupStruct;
import editor.bootstrap.toolpipeline.tool.ToolActionStruct;
import editor.bootstrap.toolpipeline.tool.ToolOptionStruct;
import editor.runtime.EditorSetting;

public enum ToolCanvas {

    /*
     * What a window's tools work on right now, each with the tools it
     * takes, the first being the one it starts with, the actions it lists
     * under them in titled groups — one-off operations such as a mesh's
     * selection and geometry edits, run by its window — whether what it
     * holds can blend between values, and every tool option it honours
     * with its range there: a brush is sized in image pixels on the
     * world's layers, in window pixels on a character and in sub-voxels on
     * an item, whose Brush builds and Erase digs the stamp of its tip, Wall
     * stands a wall, Fill moves a run of one part's cubes into the part laid
     * and Pick takes up the part struck, and whose parts are added and
     * deleted. A tool shows
     * only the options both it and the canvas have, and a canvas without
     * Size takes no brush; on one with it, every tool that carries Size
     * strokes with the brush, so a mesh's Select sweeps the brush over its
     * vertices. Biome colors name biomes, so they never blend; heights,
     * bone weights and a sculpted surface do. The world's heights are
     * their own canvas, raised, lowered, smoothed, flattened and erased
     * back to level with the brush, never painted from a palette. A
     * character's rig and an animation's pose take the tools that pick and
     * drag its joints. A character's UV tab selects the vertices whose
     * edges it marks as seams and whose faces it unwraps; the UV Editor
     * selects, moves, turns and scales their UVs and packs, evens,
     * stitches and pins their islands. The Image Editor paints an image's
     * layers and maps as the world's images are painted, its brush sized
     * in image pixels, and adds and deletes layers. A character's mesh and
     * weights also take Pick, which picks the bone under the pointer to weigh, and
     * Pose, which turns a bone out of the way while the mesh follows;
     * every modelling canvas of a character mirrors (how the bones are drawn
     * is the Properties tab's Bone Display). A character's Topology tab
     * flips, cuts and dissolves the edges clicked and evens the faces under
     * the brush, and cleans what a retopology pass leaves; its Hair tab draws,
     * grows, combs, smooths, lengthens, shortens and cuts strands of hair
     * and paints them with the material chosen. A character's Stamp tab joins
     * body parts to it, each scaled and twisted, and lays fur combed one way
     * or the other, with their twins across the body while mirroring. A
     * character's Paint tab's Brush and Fill lay the material chosen instead
     * of the colour, every map at once, while the Materials tab was worked in
     * after the Color Wheel.
     * The Material Viewer turns its light and shows the material on the
     * shape chosen. The tree tabs only look, and grow their preview from a
     * new seed or to another age. The recorder's Timeline selects and cuts
     * clips, nests the selection or opens a nested clip, and exports the
     * media file, as a video or as music, or as MIDI.
     */

    BIOMES(
            EditorSetting.TOOL_CANVAS_BIOMES,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.BRUSH, EditorTool.SMOOTH, EditorTool.SWIRL, EditorTool.SCATTER,
                    EditorTool.FILL, EditorTool.PICK },
            false,
            imageSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT),
            twist()),
    HEIGHTS(
            EditorSetting.TOOL_CANVAS_HEIGHTS,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.RAISE, EditorTool.SMOOTH, EditorTool.FLATTEN, EditorTool.ERASE },
            true,
            imageSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT)),
    SETTLEMENTS(
            EditorSetting.TOOL_CANVAS_SETTLEMENTS,
            new EditorTool[] { EditorTool.HAND, EditorTool.SELECT, EditorTool.PLACE, EditorTool.ERASE },
            false),
    CAMERAS(
            EditorSetting.TOOL_CANVAS_CAMERAS,
            new EditorTool[] { EditorTool.HAND, EditorTool.SELECT, EditorTool.PLACE, EditorTool.ERASE },
            false),
    CHARACTER_MESH(
            EditorSetting.TOOL_CANVAS_CHARACTER_MESH,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.SELECT, EditorTool.MOVE, EditorTool.ROTATE, EditorTool.SCALE,
                    EditorTool.PICK, EditorTool.POSE },
            new ToolActionGroupStruct[] { meshTransform(), meshGeometry(), meshSelection(), meshPose() },
            false,
            screenSize(),
            mirror(),
            xray(),
            wireframe(EditorSetting.TOOL_OPTION_SWITCH_ON)),
    CHARACTER_TOPOLOGY(
            EditorSetting.TOOL_CANVAS_CHARACTER_TOPOLOGY,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.SELECT, EditorTool.FLIP_EDGE, EditorTool.LOOP_CUT,
                    EditorTool.DISSOLVE_LOOP, EditorTool.TOPOLOGY },
            new ToolActionGroupStruct[] { topologyFaces(), topologyCleanUp(), meshSelection() },
            false,
            screenSize(),
            strength(EditorSetting.TOOL_TOPOLOGY_STRENGTH_DEFAULT),
            detail(),
            mirror(),
            xray(),
            wireframe(EditorSetting.TOOL_OPTION_SWITCH_ON)),
    CHARACTER_HAIR(
            EditorSetting.TOOL_CANVAS_CHARACTER_HAIR,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.BRUSH, EditorTool.SELECT, EditorTool.DRAW, EditorTool.PLACE,
                    EditorTool.NUDGE, EditorTool.SMOOTH, EditorTool.RAISE, EditorTool.LOWER, EditorTool.ERASE,
                    EditorTool.PICK },
            new ToolActionGroupStruct[] { hairStrands() },
            true,
            screenSize(),
            strength(EditorSetting.TOOL_HAIR_STRENGTH_DEFAULT),
            length(),
            width(),
            mirror()),
    CHARACTER_SCULPT(
            EditorSetting.TOOL_CANVAS_CHARACTER_SCULPT,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.BRUSH, EditorTool.SMOOTH, EditorTool.RELAX, EditorTool.INFLATE,
                    EditorTool.PINCH, EditorTool.CREASE, EditorTool.FLATTEN, EditorTool.NUDGE },
            true,
            screenSize(),
            strength(EditorSetting.TOOL_SCULPT_STRENGTH_DEFAULT),
            stampScale(),
            stampTurn(),
            comb(),
            mirror(),
            wireframe(EditorSetting.TOOL_OPTION_SWITCH_OFF)),
    CHARACTER_WEIGHTS(
            EditorSetting.TOOL_CANVAS_CHARACTER_WEIGHTS,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.BRUSH, EditorTool.SELECT, EditorTool.ERASE, EditorTool.SMOOTH,
                    EditorTool.PICK, EditorTool.POSE },
            new ToolActionGroupStruct[] { meshWeights(), meshSelection(), meshPose() },
            true,
            screenSize(),
            strength(EditorSetting.TOOL_WEIGHT_STRENGTH_DEFAULT),
            mirror(),
            xray(),
            wireframe(EditorSetting.TOOL_OPTION_SWITCH_ON)),
    CHARACTER_POSE(
            EditorSetting.TOOL_CANVAS_CHARACTER_POSE,
            new EditorTool[] { EditorTool.HAND, EditorTool.POSE, EditorTool.PICK },
            new ToolActionGroupStruct[] { meshPose() },
            false,
            mirror()),
    CHARACTER_PAINT(
            EditorSetting.TOOL_CANVAS_CHARACTER_PAINT,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.BRUSH, EditorTool.ERASE, EditorTool.SMOOTH, EditorTool.FILL,
                    EditorTool.PICK },
            new ToolActionGroupStruct[] { paintLayers() },
            true,
            screenSize(),
            strength(EditorSetting.TOOL_PAINT_STRENGTH_DEFAULT),
            mirror(),
            wireframe(EditorSetting.TOOL_OPTION_SWITCH_OFF)),
    CHARACTER_RIG(
            EditorSetting.TOOL_CANVAS_CHARACTER_RIG,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.SELECT, EditorTool.MOVE, EditorTool.ROTATE, EditorTool.SCALE,
                    EditorTool.PLACE, EditorTool.ERASE, EditorTool.COLLIDER },
            new ToolActionGroupStruct[] { rigActions(), rigLayers(), colliderActions(), hairActions() },
            false,
            mirror()),
    CHARACTER_UV(
            EditorSetting.TOOL_CANVAS_CHARACTER_UV,
            new EditorTool[] { EditorTool.HAND, EditorTool.SELECT },
            new ToolActionGroupStruct[] { uvSeams(), uvUnwrap(), meshSelection() },
            false,
            screenSize(),
            mirror(),
            xray(),
            wireframe(EditorSetting.TOOL_OPTION_SWITCH_ON)),
    UV_EDITOR(
            EditorSetting.TOOL_CANVAS_UV_EDITOR,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.SELECT, EditorTool.MOVE, EditorTool.ROTATE, EditorTool.SCALE },
            new ToolActionGroupStruct[] { uvUnwrap(), uvIslands(), uvSelection() },
            false),
    IMAGE_EDITOR(
            EditorSetting.TOOL_CANVAS_IMAGE_EDITOR,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.BRUSH, EditorTool.ERASE, EditorTool.SMOOTH, EditorTool.SWIRL,
                    EditorTool.SCATTER, EditorTool.FILL, EditorTool.PICK },
            new ToolActionGroupStruct[] { imageLayers() },
            true,
            imageSize(),
            strength(EditorSetting.TOOL_PAINT_STRENGTH_DEFAULT),
            twist(),
            stampScale(),
            stampTurn()),
    MATERIAL_VIEWER(
            EditorSetting.TOOL_CANVAS_MATERIAL_VIEWER,
            new EditorTool[] { EditorTool.HAND, EditorTool.LIGHT },
            false,
            shape()),
    TREE(
            EditorSetting.TOOL_CANVAS_TREE,
            new EditorTool[] { EditorTool.HAND },
            new ToolActionGroupStruct[] { treePreview() },
            false),
    ANIMATION_POSE(
            EditorSetting.TOOL_CANVAS_ANIMATION_POSE,
            new EditorTool[] {
                    EditorTool.HAND, EditorTool.ROTATE, EditorTool.SELECT, EditorTool.MOVE, EditorTool.SCALE,
                    EditorTool.IK },
            false),
    ITEM(
            EditorSetting.TOOL_CANVAS_ITEM,
            new EditorTool[] { EditorTool.HAND, EditorTool.BRUSH, EditorTool.WALL, EditorTool.FILL, EditorTool.PICK },
            new ToolActionGroupStruct[] { itemParts(), itemPaint() },
            false,
            itemSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT),
            itemDepth(),
            itemSpacing()),
    STRUCTURE(
            EditorSetting.TOOL_CANVAS_STRUCTURE,
            new EditorTool[] { EditorTool.HAND, EditorTool.BRUSH, EditorTool.ERASE, EditorTool.FILL, EditorTool.PICK },
            new ToolActionGroupStruct[] { structureLayers() },
            false,
            structureSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT)),
    LAYOUT(
            EditorSetting.TOOL_CANVAS_LAYOUT,
            new EditorTool[] { EditorTool.HAND },
            new ToolActionGroupStruct[] { layoutPreview() },
            false),
    RECORDER(
            EditorSetting.TOOL_CANVAS_RECORDER,
            new EditorTool[] { EditorTool.HAND, EditorTool.SELECT, EditorTool.CUT },
            new ToolActionGroupStruct[] {
                    recorderTakes(), recorderClip(), recorderKeys(), recorderSounds(), recorderTransition(),
                    recorderNode(), timelineNesting(), timelineExport() },
            false);

    // Values
    public static final ToolCanvas[] VALUES = values();

    // Internal
    private final String label;
    private final EditorTool[] tools;
    private final ToolActionGroupStruct[] actionGroups;
    private final boolean blending;
    private final ToolOptionStruct[] options;

    // Constructor \\

    ToolCanvas(String label, EditorTool[] tools, boolean blending, ToolOptionStruct... options) {
        this(label, tools, new ToolActionGroupStruct[0], blending, options);
    }

    ToolCanvas(
            String label,
            EditorTool[] tools,
            ToolActionGroupStruct[] actionGroups,
            boolean blending,
            ToolOptionStruct... options) {
        this.label = label;
        this.tools = tools;
        this.actionGroups = actionGroups;
        this.blending = blending;
        this.options = options;
    }

    // Actions \\

    private static ToolActionGroupStruct meshTransform() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_TRANSFORM,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_GRAB, EditorSetting.CHARACTER_EDITOR_KEY_GRAB),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_ROTATE, EditorSetting.CHARACTER_EDITOR_KEY_ROTATE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_SCALE, EditorSetting.CHARACTER_EDITOR_KEY_SCALE));
    }

    private static ToolActionGroupStruct meshGeometry() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_GEOMETRY,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_EXTRUDE, EditorSetting.CHARACTER_EDITOR_KEY_EXTRUDE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_DUPLICATE, EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_VERTICES,
                        EditorSetting.CHARACTER_EDITOR_KEY_DELETE_VERTICES),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_FACES,
                        EditorSetting.CHARACTER_EDITOR_KEY_DELETE_FACES),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SYMMETRIZE_POSITIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_POSITIVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SYMMETRIZE_NEGATIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_NEGATIVE));
    }

    // The faces' own topology: made, joined, cut finer and turned over
    private static ToolActionGroupStruct topologyFaces() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_TOPOLOGY_HEADER_FACES,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_MAKE_FACE, EditorSetting.CHARACTER_EDITOR_KEY_MAKE_FACE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_MERGE, EditorSetting.CHARACTER_EDITOR_KEY_MERGE),
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_MERGE_DISTANCE,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_MERGE_DISTANCE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_SUBDIVIDE, EditorSetting.CHARACTER_EDITOR_KEY_SUBDIVIDE),
                action(EditorSetting.CHARACTER_TOPOLOGY_LABEL_FLIP_FACES, EditorSetting.CHARACTER_TOPOLOGY_KEY_FLIP_FACES),
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_TRIANGULATE,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_TRIANGULATE),
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_JOIN_TRIANGLES,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_JOIN_TRIANGLES),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_FACES,
                        EditorSetting.CHARACTER_EDITOR_KEY_DELETE_FACES));
    }

    // The checks a retopology pass runs before it is done: what is left broken removed, what breaks the flow selected
    private static ToolActionGroupStruct topologyCleanUp() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_TOPOLOGY_HEADER_CLEAN_UP,
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_DELETE_DEGENERATE,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_DELETE_DEGENERATE),
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_SELECT_POLES,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_SELECT_POLES),
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_SELECT_TRIANGLES,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_SELECT_TRIANGLES),
                action(
                        EditorSetting.CHARACTER_TOPOLOGY_LABEL_SELECT_NON_MANIFOLD,
                        EditorSetting.CHARACTER_TOPOLOGY_KEY_SELECT_NON_MANIFOLD));
    }

    // The strands of the groom open: taken from the hair worn, selected and removed
    private static ToolActionGroupStruct hairStrands() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_HAIR_HEADER_STRANDS,
                action(EditorSetting.CHARACTER_HAIR_LABEL_FROM_WORN, EditorSetting.CHARACTER_HAIR_KEY_FROM_WORN),
                action(EditorSetting.CHARACTER_HAIR_LABEL_SELECT_ALL, EditorSetting.CHARACTER_HAIR_KEY_SELECT_ALL),
                action(EditorSetting.CHARACTER_HAIR_LABEL_SELECT_NONE, EditorSetting.CHARACTER_HAIR_KEY_SELECT_NONE),
                action(EditorSetting.CHARACTER_HAIR_LABEL_DELETE, EditorSetting.CHARACTER_HAIR_KEY_DELETE));
    }

    private static ToolActionGroupStruct meshWeights() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_WEIGHTS,
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_ASSIGN,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_ADD, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ADD),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_SUBTRACT,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_REMOVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_REMOVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_NORMALIZE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_SMOOTH,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_POSITIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_NEGATIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_NEGATIVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_DIGITS,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_DIGITS),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_LOCKS,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_LOCKS),
                action(
                        EditorSetting.CHARACTER_RIG_LABEL_SPRING_PREVIEW,
                        EditorSetting.CHARACTER_RIG_KEY_SPRING_PREVIEW));
    }

    private static ToolActionGroupStruct meshSelection() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_SELECTION,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_ALL, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_ALL),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_NONE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_NONE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_INVERT,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_INVERT),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_LINKED,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LINKED),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_MORE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MORE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_LESS,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LESS),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_MIRROR,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MIRROR),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_BONE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_BONE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_HIDE, EditorSetting.CHARACTER_EDITOR_KEY_HIDE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_HIDE_UNSELECTED,
                        EditorSetting.CHARACTER_EDITOR_KEY_HIDE_UNSELECTED),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_REVEAL, EditorSetting.CHARACTER_EDITOR_KEY_REVEAL));
    }

    private static ToolActionGroupStruct meshPose() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_POSE,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_POSE_RESET, EditorSetting.CHARACTER_EDITOR_KEY_POSE_RESET));
    }

    private static ToolActionGroupStruct rigActions() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_RIG_HEADER_ACTIONS,
                action(EditorSetting.CHARACTER_RIG_LABEL_ADD_BONE, EditorSetting.CHARACTER_RIG_KEY_ADD_BONE),
                action(EditorSetting.CHARACTER_RIG_LABEL_REMOVE_BONE, EditorSetting.CHARACTER_RIG_KEY_REMOVE_BONE),
                action(EditorSetting.CHARACTER_RIG_LABEL_RIG_NEW, EditorSetting.CHARACTER_RIG_KEY_RIG_NEW),
                action(EditorSetting.CHARACTER_RIG_LABEL_RIG_OWN, EditorSetting.CHARACTER_RIG_KEY_RIG_OWN),
                action(EditorSetting.CHARACTER_RIG_LABEL_DIGITS, EditorSetting.CHARACTER_RIG_KEY_DIGITS),
                action(EditorSetting.CHARACTER_RIG_LABEL_IK_ADD, EditorSetting.CHARACTER_RIG_KEY_IK_ADD),
                action(EditorSetting.CHARACTER_RIG_LABEL_IK_REMOVE, EditorSetting.CHARACTER_RIG_KEY_IK_REMOVE));
    }

    private static ToolActionGroupStruct rigLayers() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_RIG_HEADER_LAYERS,
                action(EditorSetting.CHARACTER_RIG_LABEL_PART_ADD, EditorSetting.CHARACTER_RIG_KEY_PART_ADD),
                action(EditorSetting.CHARACTER_RIG_LABEL_PART_REMOVE, EditorSetting.CHARACTER_RIG_KEY_PART_REMOVE));
    }

    private static ToolActionGroupStruct paintLayers() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_PAINT_HEADER_LAYERS,
                action(EditorSetting.CHARACTER_PAINT_LABEL_LAYER_ADD, EditorSetting.CHARACTER_PAINT_KEY_LAYER_ADD),
                action(
                        EditorSetting.CHARACTER_PAINT_LABEL_LAYER_REMOVE,
                        EditorSetting.CHARACTER_PAINT_KEY_LAYER_REMOVE));
    }

    private static ToolActionGroupStruct imageLayers() {
        return new ToolActionGroupStruct(
                EditorSetting.IMAGE_EDITOR_HEADER_LAYERS,
                action(EditorSetting.IMAGE_EDITOR_LABEL_LAYER_ADD, EditorSetting.IMAGE_EDITOR_KEY_LAYER_ADD),
                action(EditorSetting.IMAGE_EDITOR_LABEL_LAYER_REMOVE, EditorSetting.IMAGE_EDITOR_KEY_LAYER_REMOVE));
    }

    private static ToolActionGroupStruct itemParts() {
        return new ToolActionGroupStruct(
                EditorSetting.ITEM_EDITOR_HEADER_PARTS,
                action(EditorSetting.ITEM_EDITOR_LABEL_PART_ADD, EditorSetting.ITEM_EDITOR_KEY_PART_ADD),
                action(EditorSetting.ITEM_EDITOR_LABEL_PART_REMOVE, EditorSetting.ITEM_EDITOR_KEY_PART_REMOVE));
    }

    private static ToolActionGroupStruct structureLayers() {
        return new ToolActionGroupStruct(
                EditorSetting.STRUCTURE_EDITOR_HEADER_LAYERS,
                action(EditorSetting.STRUCTURE_EDITOR_LABEL_LAYER_ADD, EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_ADD),
                action(EditorSetting.STRUCTURE_EDITOR_LABEL_LAYER_REMOVE,
                        EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_REMOVE),
                action(EditorSetting.STRUCTURE_EDITOR_LABEL_LAYER_UP, EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_UP),
                action(EditorSetting.STRUCTURE_EDITOR_LABEL_LAYER_DOWN, EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_DOWN),
                action(EditorSetting.STRUCTURE_EDITOR_LABEL_LAYER_RENAME,
                        EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_RENAME));
    }

    private static ToolActionGroupStruct layoutPreview() {
        return new ToolActionGroupStruct(
                EditorSetting.LAYOUT_PLANNER_HEADER_PREVIEW,
                action(EditorSetting.LAYOUT_PLANNER_LABEL_NEXT_LAYOUT, EditorSetting.LAYOUT_PLANNER_KEY_NEXT_LAYOUT),
                action(EditorSetting.LAYOUT_PLANNER_LABEL_NEXT_SITE, EditorSetting.LAYOUT_PLANNER_KEY_NEXT_SITE));
    }

    private static ToolActionGroupStruct treePreview() {
        return new ToolActionGroupStruct(
                EditorSetting.TREE_VIEWER_HEADER_PREVIEW,
                action(EditorSetting.TREE_VIEWER_LABEL_RESEED, EditorSetting.TREE_VIEWER_KEY_RESEED),
                action(EditorSetting.TREE_VIEWER_LABEL_YOUNGER, EditorSetting.TREE_VIEWER_KEY_YOUNGER),
                action(EditorSetting.TREE_VIEWER_LABEL_OLDER, EditorSetting.TREE_VIEWER_KEY_OLDER));
    }

    private static ToolActionGroupStruct itemPaint() {
        return new ToolActionGroupStruct(
                EditorSetting.ITEM_EDITOR_HEADER_PAINT,
                action(EditorSetting.ITEM_EDITOR_LABEL_PAINT, EditorSetting.ITEM_EDITOR_KEY_PAINT));
    }

    private static ToolActionGroupStruct uvSeams() {
        return new ToolActionGroupStruct(
                EditorSetting.UV_HEADER_SEAMS,
                action(EditorSetting.UV_LABEL_MARK_SEAM, EditorSetting.UV_KEY_MARK_SEAM),
                action(EditorSetting.UV_LABEL_CLEAR_SEAM, EditorSetting.UV_KEY_CLEAR_SEAM),
                action(EditorSetting.UV_LABEL_SEAMS_FROM_ISLANDS, EditorSetting.UV_KEY_SEAMS_FROM_ISLANDS));
    }

    private static ToolActionGroupStruct uvUnwrap() {
        return new ToolActionGroupStruct(
                EditorSetting.UV_HEADER_UNWRAP,
                action(EditorSetting.UV_LABEL_UNWRAP, EditorSetting.UV_KEY_UNWRAP));
    }

    private static ToolActionGroupStruct uvIslands() {
        return new ToolActionGroupStruct(
                EditorSetting.UV_HEADER_ISLANDS,
                action(EditorSetting.UV_LABEL_PACK, EditorSetting.UV_KEY_PACK),
                action(EditorSetting.UV_LABEL_AVERAGE_SCALE, EditorSetting.UV_KEY_AVERAGE_SCALE),
                action(EditorSetting.UV_LABEL_STITCH, EditorSetting.UV_KEY_STITCH),
                action(EditorSetting.UV_LABEL_PIN, EditorSetting.UV_KEY_PIN),
                action(EditorSetting.UV_LABEL_UNPIN, EditorSetting.UV_KEY_UNPIN),
                action(EditorSetting.UV_LABEL_FLIP_U, EditorSetting.UV_KEY_FLIP_U),
                action(EditorSetting.UV_LABEL_FLIP_V, EditorSetting.UV_KEY_FLIP_V));
    }

    private static ToolActionGroupStruct uvSelection() {
        return new ToolActionGroupStruct(
                EditorSetting.UV_HEADER_SELECTION,
                action(EditorSetting.UV_LABEL_SELECT_ALL, EditorSetting.UV_KEY_SELECT_ALL),
                action(EditorSetting.UV_LABEL_SELECT_LINKED, EditorSetting.UV_KEY_SELECT_LINKED));
    }

    private static ToolActionGroupStruct colliderActions() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_RIG_HEADER_COLLIDERS,
                action(EditorSetting.CHARACTER_RIG_LABEL_COLLIDER_ADD, EditorSetting.CHARACTER_RIG_KEY_COLLIDER_ADD),
                action(
                        EditorSetting.CHARACTER_RIG_LABEL_COLLIDER_REMOVE,
                        EditorSetting.CHARACTER_RIG_KEY_COLLIDER_REMOVE));
    }

    private static ToolActionGroupStruct hairActions() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_RIG_HEADER_HAIR,
                action(EditorSetting.CHARACTER_RIG_LABEL_HAIR, EditorSetting.CHARACTER_RIG_KEY_HAIR),
                action(EditorSetting.CHARACTER_RIG_LABEL_SPRING_ADD, EditorSetting.CHARACTER_RIG_KEY_SPRING_ADD),
                action(EditorSetting.CHARACTER_RIG_LABEL_SPRING_REMOVE, EditorSetting.CHARACTER_RIG_KEY_SPRING_REMOVE),
                action(
                        EditorSetting.CHARACTER_RIG_LABEL_SPRING_PREVIEW,
                        EditorSetting.CHARACTER_RIG_KEY_SPRING_PREVIEW));
    }

    private static ToolActionGroupStruct timelineNesting() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_NESTING,
                action(EditorSetting.TOOL_ACTION_LABEL_NEST_SELECTION, EditorSetting.RECORDER_KEY_CLIP_NEST),
                action(EditorSetting.TOOL_ACTION_LABEL_OPEN_NESTED, EditorSetting.RECORDER_KEY_CLIP_OPEN));
    }

    // A video or music exports through the File menu's Export; MIDI is a format of its own
    private static ToolActionGroupStruct timelineExport() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_EXPORT,
                action(EditorSetting.TOOL_ACTION_LABEL_EXPORT_MIDI, EditorSetting.RECORDER_KEY_EXPORT_MIDI));
    }

    // The take picked in the Palette tab
    private static ToolActionGroupStruct recorderTakes() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_TAKES,
                action(EditorSetting.TOOL_ACTION_LABEL_TAKE_PLACE, EditorSetting.RECORDER_KEY_TAKE_PLACE),
                action(EditorSetting.TOOL_ACTION_LABEL_TAKE_RENAME, EditorSetting.RECORDER_KEY_TAKE_RENAME),
                action(EditorSetting.TOOL_ACTION_LABEL_TAKE_DELETE, EditorSetting.RECORDER_KEY_TAKE_DELETE));
    }

    // The clip selected on the Timeline, edited at the playhead
    private static ToolActionGroupStruct recorderClip() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_CLIP,
                action(EditorSetting.TOOL_ACTION_LABEL_TRIM_START, EditorSetting.RECORDER_KEY_CLIP_TRIM_START),
                action(EditorSetting.TOOL_ACTION_LABEL_TRIM_END, EditorSetting.RECORDER_KEY_CLIP_TRIM_END),
                action(EditorSetting.TOOL_ACTION_LABEL_SPLIT, EditorSetting.RECORDER_KEY_CLIP_SPLIT),
                action(EditorSetting.TOOL_ACTION_LABEL_STAMP, EditorSetting.RECORDER_KEY_CLIP_STAMP),
                action(EditorSetting.TOOL_ACTION_LABEL_TRANSITION_IN,
                        EditorSetting.RECORDER_KEY_TRANSITION_PREFIX + RecorderClipEdge.START.getKey()),
                action(EditorSetting.TOOL_ACTION_LABEL_TRANSITION_OUT,
                        EditorSetting.RECORDER_KEY_TRANSITION_PREFIX + RecorderClipEdge.END.getKey()),
                action(EditorSetting.TOOL_ACTION_LABEL_FILTER_APPLY, EditorSetting.RECORDER_KEY_FILTER_APPLY),
                action(EditorSetting.TOOL_ACTION_LABEL_FILTER_REMOVE, EditorSetting.RECORDER_KEY_FILTER_REMOVE),
                action(EditorSetting.TOOL_ACTION_LABEL_REMOVE_CLIP, EditorSetting.RECORDER_KEY_CLIP_REMOVE));
    }

    // The keys of the curve selected on the Keys tab or its graph
    private static ToolActionGroupStruct recorderKeys() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_KEYS,
                action(EditorSetting.TOOL_ACTION_LABEL_KEY_ADD, EditorSetting.RECORDER_KEY_KEY_ADD),
                action(EditorSetting.TOOL_ACTION_LABEL_KEY_DELETE, EditorSetting.RECORDER_KEY_KEY_DELETE),
                action(EditorSetting.TOOL_ACTION_LABEL_KEY_COPY, EditorSetting.RECORDER_KEY_KEY_COPY),
                action(EditorSetting.TOOL_ACTION_LABEL_KEY_PASTE, EditorSetting.RECORDER_KEY_KEY_PASTE),
                action(EditorSetting.TOOL_ACTION_LABEL_KEY_MODE, EditorSetting.RECORDER_KEY_KEY_MODE),
                action(EditorSetting.TOOL_ACTION_LABEL_KEY_VALUE, EditorSetting.RECORDER_KEY_KEY_VALUE),
                action(EditorSetting.TOOL_ACTION_LABEL_FRAME_KEYS, EditorSetting.RECORDER_KEY_FRAME_KEYS),
                action(EditorSetting.TOOL_ACTION_LABEL_CURVE_REFIT, EditorSetting.RECORDER_KEY_CURVE_REFIT),
                action(EditorSetting.TOOL_ACTION_LABEL_TAKE_REFIT, EditorSetting.RECORDER_KEY_TAKE_REFIT));
    }

    // The sound selected on the Sounds layer, and a sound added from the Sound Library
    private static ToolActionGroupStruct recorderSounds() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_SOUNDS,
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_ADD, EditorSetting.RECORDER_KEY_SOUND_ADD),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_VOLUME, EditorSetting.RECORDER_KEY_SOUND_VOLUME),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_PITCH, EditorSetting.RECORDER_KEY_SOUND_PITCH),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_LENGTH, EditorSetting.RECORDER_KEY_SOUND_LENGTH),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_SEEK, EditorSetting.RECORDER_KEY_SOUND_SEEK),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_COPY, EditorSetting.RECORDER_KEY_SOUND_COPY),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_REPLACE, EditorSetting.RECORDER_KEY_SOUND_REPLACE),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_REMOVE, EditorSetting.RECORDER_KEY_SOUND_REMOVE),
                action(EditorSetting.TOOL_ACTION_LABEL_SOUND_REMOVE_ALL, EditorSetting.RECORDER_KEY_SOUND_REMOVE_ALL));
    }

    // The transition selected on a clip's edge
    private static ToolActionGroupStruct recorderTransition() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_TRANSITION,
                action(EditorSetting.TOOL_ACTION_LABEL_TRANSITION_LENGTH,
                        EditorSetting.RECORDER_KEY_TRANSITION_LENGTH),
                action(EditorSetting.TOOL_ACTION_LABEL_TRANSITION_REPLACE,
                        EditorSetting.RECORDER_KEY_TRANSITION_REPLACE),
                action(EditorSetting.TOOL_ACTION_LABEL_TRANSITION_REMOVE,
                        EditorSetting.RECORDER_KEY_TRANSITION_REMOVE));
    }

    // The movement or gesture selected on a clip
    private static ToolActionGroupStruct recorderNode() {
        return new ToolActionGroupStruct(
                EditorSetting.TOOL_ACTION_HEADER_NODE,
                action(EditorSetting.TOOL_ACTION_LABEL_NODE_PREVIOUS, EditorSetting.RECORDER_KEY_NODE_PREVIOUS),
                action(EditorSetting.TOOL_ACTION_LABEL_NODE_NEXT, EditorSetting.RECORDER_KEY_NODE_NEXT),
                action(EditorSetting.TOOL_ACTION_LABEL_NODE_REMOVE, EditorSetting.RECORDER_KEY_NODE_REMOVE));
    }

    private static ToolActionStruct action(String label, String key) {
        return new ToolActionStruct(label, key);
    }

    // Options \\

    private static ToolOptionStruct imageSize() {
        return new ToolOptionStruct(
                ToolOption.SIZE,
                EditorSetting.TOOL_IMAGE_SIZE_MIN,
                EditorSetting.TOOL_IMAGE_SIZE_MAX,
                EditorSetting.TOOL_IMAGE_SIZE_STEP,
                EditorSetting.TOOL_IMAGE_SIZE_DEFAULT);
    }

    // An item's brush, its radius in sub-voxels, none being one sub-voxel alone
    private static ToolOptionStruct itemSize() {
        return new ToolOptionStruct(
                ToolOption.SIZE,
                EditorSetting.TOOL_ITEM_SIZE_MIN,
                EditorSetting.TOOL_ITEM_SIZE_MAX,
                EditorSetting.TOOL_ITEM_SIZE_STEP,
                EditorSetting.TOOL_ITEM_SIZE_DEFAULT);
    }

    // How many layers of cubes one stamp of an item's brush builds or digs at most, one being a layer at a time
    private static ToolOptionStruct itemDepth() {
        return new ToolOptionStruct(
                ToolOption.DEPTH,
                EditorSetting.TOOL_ITEM_DEPTH_MIN,
                EditorSetting.TOOL_ITEM_DEPTH_MAX,
                EditorSetting.TOOL_ITEM_DEPTH_STEP,
                EditorSetting.TOOL_ITEM_DEPTH_DEFAULT);
    }

    // How far apart an item brush's stamps lie along a drag, in sub-voxels
    private static ToolOptionStruct itemSpacing() {
        return new ToolOptionStruct(
                ToolOption.SPACING,
                EditorSetting.TOOL_ITEM_SPACING_MIN,
                EditorSetting.TOOL_ITEM_SPACING_MAX,
                EditorSetting.TOOL_ITEM_SPACING_STEP,
                EditorSetting.TOOL_ITEM_SPACING_DEFAULT);
    }

    private static ToolOptionStruct structureSize() {
        return new ToolOptionStruct(
                ToolOption.SIZE,
                EditorSetting.TOOL_STRUCTURE_SIZE_MIN,
                EditorSetting.TOOL_STRUCTURE_SIZE_MAX,
                EditorSetting.TOOL_STRUCTURE_SIZE_STEP,
                EditorSetting.TOOL_STRUCTURE_SIZE_DEFAULT);
    }

    private static ToolOptionStruct screenSize() {
        return new ToolOptionStruct(
                ToolOption.SIZE,
                EditorSetting.TOOL_SCREEN_SIZE_MIN,
                EditorSetting.TOOL_SCREEN_SIZE_MAX,
                EditorSetting.TOOL_SCREEN_SIZE_STEP,
                EditorSetting.TOOL_SCREEN_SIZE_DEFAULT);
    }

    private static ToolOptionStruct strength(float defaultValue) {
        return new ToolOptionStruct(
                ToolOption.STRENGTH,
                EditorSetting.TOOL_STRENGTH_STEP,
                1f,
                EditorSetting.TOOL_STRENGTH_STEP,
                defaultValue);
    }

    private static ToolOptionStruct toggle(ToolOption option, int defaultIndex) {
        return new ToolOptionStruct(
                option, ToolOptionKind.SWITCH, EditorSetting.TOOL_OPTION_SWITCH_NAMES, defaultIndex);
    }

    private static ToolOptionStruct choice(ToolOption option, String[] names, int defaultIndex) {
        return new ToolOptionStruct(option, ToolOptionKind.CHOICE, names, defaultIndex);
    }

    private static ToolOptionStruct mirror() {
        return toggle(ToolOption.MIRROR, EditorSetting.TOOL_OPTION_SWITCH_ON);
    }

    private static ToolOptionStruct xray() {
        return toggle(ToolOption.XRAY, EditorSetting.TOOL_OPTION_SWITCH_OFF);
    }

    private static ToolOptionStruct wireframe(int defaultIndex) {
        return toggle(ToolOption.WIREFRAME, defaultIndex);
    }

    // The face size the topology brush evens faces toward, in millimetres
    private static ToolOptionStruct detail() {
        return new ToolOptionStruct(
                ToolOption.DETAIL,
                EditorSetting.TOOL_TOPOLOGY_DETAIL_MIN,
                EditorSetting.TOOL_TOPOLOGY_DETAIL_MAX,
                EditorSetting.TOOL_TOPOLOGY_DETAIL_STEP,
                EditorSetting.TOOL_TOPOLOGY_DETAIL_DEFAULT);
    }

    // How long a new strand of hair grows, in centimetres
    private static ToolOptionStruct length() {
        return new ToolOptionStruct(
                ToolOption.LENGTH,
                EditorSetting.TOOL_HAIR_LENGTH_MIN,
                EditorSetting.TOOL_HAIR_LENGTH_MAX,
                EditorSetting.TOOL_HAIR_LENGTH_STEP,
                EditorSetting.TOOL_HAIR_LENGTH_DEFAULT);
    }

    // How wide a new strand's card is at its root, in millimetres
    private static ToolOptionStruct width() {
        return new ToolOptionStruct(
                ToolOption.WIDTH,
                EditorSetting.TOOL_HAIR_WIDTH_MIN,
                EditorSetting.TOOL_HAIR_WIDTH_MAX,
                EditorSetting.TOOL_HAIR_WIDTH_STEP,
                EditorSetting.TOOL_HAIR_WIDTH_DEFAULT);
    }

    private static ToolOptionStruct shape() {
        return choice(ToolOption.SHAPE, EditorSetting.TOOL_SHAPE_NAMES, EditorSetting.TOOL_SHAPE_DEFAULT);
    }

    private static ToolOptionStruct comb() {
        return choice(ToolOption.COMB, EditorSetting.TOOL_COMB_NAMES, EditorSetting.TOOL_COMB_DEFAULT);
    }

    private static ToolOptionStruct twist() {
        return new ToolOptionStruct(
                ToolOption.TWIST,
                -EditorSetting.TOOL_TWIST_MAX,
                EditorSetting.TOOL_TWIST_MAX,
                EditorSetting.TOOL_TWIST_STEP,
                EditorSetting.TOOL_TWIST_DEFAULT);
    }

    private static ToolOptionStruct stampScale() {
        return new ToolOptionStruct(
                ToolOption.SCALE,
                EditorSetting.TOOL_STAMP_SCALE_MIN,
                EditorSetting.TOOL_STAMP_SCALE_MAX,
                EditorSetting.TOOL_STAMP_SCALE_STEP,
                EditorSetting.TOOL_STAMP_SCALE_DEFAULT);
    }

    private static ToolOptionStruct stampTurn() {
        return new ToolOptionStruct(
                ToolOption.TURN,
                -EditorSetting.TOOL_STAMP_TURN_MAX,
                EditorSetting.TOOL_STAMP_TURN_MAX,
                EditorSetting.TOOL_TWIST_STEP,
                EditorSetting.TOOL_STAMP_TURN_DEFAULT);
    }

    // Support \\

    public boolean supports(EditorTool tool) {

        for (int i = 0; i < tools.length; i++)
            if (tools[i] == tool)
                return true;

        return false;
    }

    // Whether the canvas lists an action under a key, the only actions the Tools panel runs on it
    public boolean hasAction(String actionKey) {

        for (int i = 0; i < actionGroups.length; i++) {

            ToolActionStruct[] actions = actionGroups[i].getActions();

            for (int j = 0; j < actions.length; j++)
                if (actions[j].getKey().equals(actionKey))
                    return true;
        }

        return false;
    }

    // The canvas's own range for an option, null for an option it does not honour
    public ToolOptionStruct findOption(ToolOption option) {

        for (int i = 0; i < options.length; i++)
            if (options[i].getOption() == option)
                return options[i];

        return null;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public EditorTool[] getTools() {
        return tools;
    }

    public ToolActionGroupStruct[] getActionGroups() {
        return actionGroups;
    }

    public EditorTool getDefaultTool() {
        return tools[0];
    }

    public boolean isBlending() {
        return blending;
    }

    // A canvas strokes with the brush when its tools have a size to stroke at
    public boolean takesBrush() {
        return findOption(ToolOption.SIZE) != null;
    }

    // Whether a tool strokes the brush here: every tool sized by the brush on a canvas that takes one
    public boolean strokes(EditorTool tool) {
        return takesBrush() && tool.hasOption(ToolOption.SIZE);
    }
}
