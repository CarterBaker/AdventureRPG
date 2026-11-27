package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.io.File;
import java.util.function.Consumer;

import application.bootstrap.geometrypipeline.mesh.MeshZone;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterGroomStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterHairCardStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStrandStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.charactereditorpipeline.util.CharacterStrandUtility;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgBufferStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterGroomBranch extends BranchPackage {

    /*
     * The Hair tab's groom, the industry's guide-curve grooming turned into
     * game hair cards as a groom is in Blender, Maya XGen or Unreal's Groom:
     * strands are drawn from the surface, planted, combed, smoothed, grown,
     * shortened, cut and painted with the material chosen in the Materials
     * tab, each a curve of points with its width and taper, and
     * Export lays each one out as a card two quads wide along it
     * (CharacterStrandUtility), its UVs running across in u and from the root
     * at v 1 to the tip at v 0 as the strand image hangs, wholly weighed onto
     * the bone its root grows from, and beside them the scalp cap of the
     * hair it was taken from. A groom is a workspace file in the Hair folder,
     * the way a grooming tool keeps its project apart from the engine:
     * Export writes its cards as a rigged mesh to the workspace's Exports,
     * and Import brings an export into the game as a hair feature and its
     * mesh, the spring chains of its long strands laid and weighed by the
     * Rig and Weights tabs' own Rig Hair Locks and Weigh Onto Locks. Taking
     * strands from the hair worn turns each of its cards back into a strand
     * along the card's middle, as wide as the card. Every edit is one undo
     * step, a whole copy of the groom; with mirror on, a new strand comes
     * with its twin across x and every stroke reaches each strand's twin the
     * mirrored way. On a posed body each strand is shown, drawn and combed
     * where the pose carries it, by the skinning of the bone it grows from
     * as its card is weighed, and every point and drag the pointer gives is
     * carried back through that bone to the bind pose the groom keeps, so a
     * mirrored edit stays mirrored whatever the pose.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHairBranch characterHairBranch;
    private CharacterRigBranch characterRigBranch;
    private CharacterWeightBranch characterWeightBranch;
    private CharacterWorkspaceBranch characterWorkspaceBranch;
    private MeshManager meshManager;
    private RigManager rigManager;
    private InfoManager infoManager;
    private MaterialsManager materialsManager;

    // Directory
    private File groomRoot;
    private File exportRoot;

    // Groom — the one open and its name in the Hair folder
    private CharacterGroomStruct groom;
    private String groomName;
    private boolean dirty;
    private int revision;

    // History — whole copies of the groom, the one the open step started from and whether it changed since
    private ObjectArrayList<CharacterGroomStruct> undoSteps;
    private ObjectArrayList<CharacterGroomStruct> redoSteps;
    private CharacterGroomStruct openStep;
    private boolean stepChanged;

    // Stroke — each strand's twin across x for the stroke's length, the strands the frame reached and those it cut
    // away, and the strand a drag draws
    private IntArrayList twins;
    private CharacterEditorSelection strokeSelection;
    private BooleanArrayList touched;
    private BooleanArrayList removed;
    private int drawnStrand;

    // Status — why the last open, export or import could not be done
    private String refusal;

    // Scratch
    private FloatArrayList lengths;
    private FloatArrayList cardRows;
    private IntArrayList groupPoints;
    private FloatArrayList groupWeights;
    private ObjectArrayList<CharacterHairCardStruct> cards;
    private Vector3 pointA;
    private Vector3 pointB;
    private Vector3 tangent;
    private Vector3 side;
    private Vector3 mirrored;
    private Vector3 bindDrag;
    private Matrix4 inverseScratch;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.groomRoot = WorkspaceUtility.resolveFolder(
                internal.path, EditorSetting.CHARACTER_HAIR_WORKSPACE_DIRECTORY);
        this.exportRoot = new File(
                WorkspaceUtility.resolveFolder(internal.path, EditorSetting.IMAGE_EXPORT_DIRECTORY),
                EditorSetting.CHARACTER_HAIR_EXPORT_FOLDER);

        // History
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();

        // Stroke
        this.twins = new IntArrayList();
        this.touched = new BooleanArrayList();
        this.removed = new BooleanArrayList();
        this.drawnStrand = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.lengths = new FloatArrayList();
        this.cardRows = new FloatArrayList();
        this.groupPoints = new IntArrayList();
        this.groupWeights = new FloatArrayList();
        this.cards = new ObjectArrayList<>();
        this.pointA = new Vector3();
        this.pointB = new Vector3();
        this.tangent = new Vector3();
        this.side = new Vector3();
        this.mirrored = new Vector3();
        this.bindDrag = new Vector3();
        this.inverseScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHairBranch = get(CharacterHairBranch.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
        this.characterWorkspaceBranch = get(CharacterWorkspaceBranch.class);
        this.meshManager = get(MeshManager.class);
        this.rigManager = get(RigManager.class);
        this.infoManager = get(InfoManager.class);
        this.materialsManager = get(MaterialsManager.class);
    }

    // Files \\

    File getFolder() {
        return groomRoot;
    }

    File getExportFolder() {
        return exportRoot;
    }

    boolean isNameAvailable(String name) {
        return FileUtility.isValidFileName(name, EditorSetting.NAME_INPUT_MAX_LENGTH) && !resolveFile(name).exists();
    }

    private File resolveFile(String name) {
        return ArpgUtility.resolveFile(groomRoot, name);
    }

    // A new groom with no strands on the rig, written at once and opened; false when it could not be written
    boolean create(String name, String rigName) {

        CharacterGroomStruct created = new CharacterGroomStruct(rigName, "");

        if (!write(created, resolveFile(name)))
            return false;

        adopt(created, name);
        return true;
    }

    // The groom a file in the Hair folder holds, opened in place of the one open; false, with the refusal kept, for a
    // file outside the folder or one that cannot be read
    boolean open(File file) {

        String name = WorkspaceUtility.toName(groomRoot, file);

        if (name == null) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_OUTSIDE;
            return false;
        }

        CharacterGroomStruct read = read(file);

        if (read == null) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_READ + name;
            return false;
        }

        adopt(read, name);
        return true;
    }

    boolean save() {

        if (groom == null || !write(groom, resolveFile(groomName))) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_WRITTEN + groomName;
            return false;
        }

        this.dirty = false;
        changed();
        return true;
    }

    // The groom read back from its file, every unsaved change and the history dropped
    boolean revert() {
        return groom != null && open(resolveFile(groomName));
    }

    private void adopt(CharacterGroomStruct opened, String name) {

        this.groom = opened;
        this.groomName = name;
        this.dirty = false;
        this.openStep = null;
        this.drawnStrand = EngineSetting.INDEX_NOT_FOUND;
        undoSteps.clear();
        redoSteps.clear();
        changed();
    }

    // Format \\

    private boolean write(CharacterGroomStruct written, File file) {

        ArpgObjectStruct groomArpg = new ArpgObjectStruct();
        ArpgArrayStruct strandsArpg = new ArpgArrayStruct();

        groomArpg.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_RIG, written.getRigName());
        groomArpg.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_SOURCE, written.getSourceFeature());

        for (int i = 0; i < written.getStrandCount(); i++)
            strandsArpg.add(toStrandArpg(written.getStrand(i)));

        groomArpg.add(EditorSetting.CHARACTER_HAIR_FIELD_STRANDS, strandsArpg);
        file.getParentFile().mkdirs();

        return ArpgUtility.tryWriteObject(file, groomArpg);
    }

    private static ArpgObjectStruct toStrandArpg(CharacterStrandStruct strand) {

        ArpgObjectStruct strandArpg = new ArpgObjectStruct();
        ArpgArrayStruct normalArpg = new ArpgArrayStruct();

        normalArpg.add(strand.getNormal().x);
        normalArpg.add(strand.getNormal().y);
        normalArpg.add(strand.getNormal().z);
        strandArpg.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_TEXTURE, strand.getTextureName());
        strandArpg.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_BONE, strand.getBoneName());
        strandArpg.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_WIDTH, strand.getWidth());
        strandArpg.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_TAPER, strand.getTaper());
        strandArpg.add(EditorSetting.CHARACTER_HAIR_FIELD_NORMAL, normalArpg);
        strandArpg.add(EditorSetting.CHARACTER_HAIR_FIELD_POINTS, ArpgUtility.toBuffer(
                strand.getPoints().elements(), strand.getPoints().size()));

        return strandArpg;
    }

    // A groom file's contents, or null for one that cannot be read
    private static CharacterGroomStruct read(File file) {

        ArpgObjectStruct groomArpg = ArpgUtility.tryLoadObject(file);

        if (groomArpg == null || !ArpgUtility.hasString(groomArpg, EditorSetting.CHARACTER_HAIR_FIELD_RIG))
            return null;

        CharacterGroomStruct read = new CharacterGroomStruct(
                groomArpg.get(EditorSetting.CHARACTER_HAIR_FIELD_RIG).getAsString(),
                ArpgUtility.getString(groomArpg, EditorSetting.CHARACTER_HAIR_FIELD_SOURCE, ""));

        if (!ArpgUtility.hasArray(groomArpg, EditorSetting.CHARACTER_HAIR_FIELD_STRANDS))
            return read;

        try {
            for (ArpgElementStruct element : groomArpg.getAsArray(EditorSetting.CHARACTER_HAIR_FIELD_STRANDS))
                read.getStrands().add(toStrand(element.getAsObject()));
        } catch (InternalException e) {
            return null;
        }

        return read;
    }

    private static CharacterStrandStruct toStrand(ArpgObjectStruct strandArpg) {

        ArpgArrayStruct normalArpg = strandArpg.getAsArray(EditorSetting.CHARACTER_HAIR_FIELD_NORMAL);
        ArpgBufferStruct points = strandArpg.get(EditorSetting.CHARACTER_HAIR_FIELD_POINTS).getAsBuffer();
        CharacterStrandStruct strand = new CharacterStrandStruct(
                new Vector3(
                        normalArpg.get(0).getAsFloat(),
                        normalArpg.get(1).getAsFloat(),
                        normalArpg.get(2).getAsFloat()),
                ArpgUtility.getString(strandArpg, EditorSetting.CHARACTER_HAIR_FIELD_BONE,
                        EditorSetting.CHARACTER_HAIR_DEFAULT_BONE),
                ArpgUtility.getString(strandArpg, EditorSetting.CHARACTER_HAIR_FIELD_TEXTURE,
                        EditorSetting.CHARACTER_HAIR_DEFAULT_TEXTURE),
                ArpgUtility.getFloat(strandArpg, EditorSetting.CHARACTER_HAIR_FIELD_WIDTH,
                        EditorSetting.TOOL_HAIR_WIDTH_DEFAULT * EditorSetting.CHARACTER_HAIR_WIDTH_UNIT),
                ArpgUtility.getFloat(strandArpg, EditorSetting.CHARACTER_HAIR_FIELD_TAPER,
                        EditorSetting.CHARACTER_HAIR_TAPER_DEFAULT));

        for (int i = 0; i + 2 < points.size(); i += 3)
            strand.addPoint(points.getFloat(i), points.getFloat(i + 1), points.getFloat(i + 2));

        return strand;
    }

    // History \\

    // Opens one undo step, the groom as it stands kept to go back to
    void beginStep() {

        if (groom == null)
            return;

        this.openStep = new CharacterGroomStruct(groom);
        this.stepChanged = false;
    }

    // Keeps the open step when the groom changed under it, at most the steps the history holds
    void endStep() {

        if (openStep != null && stepChanged) {

            undoSteps.add(openStep);

            if (undoSteps.size() > EditorSetting.CHARACTER_HAIR_HISTORY_MAX)
                undoSteps.remove(0);

            redoSteps.clear();
        }

        this.openStep = null;
        this.stepChanged = false;
    }

    boolean undo() {
        return step(undoSteps, redoSteps);
    }

    boolean redo() {
        return step(redoSteps, undoSteps);
    }

    private boolean step(ObjectArrayList<CharacterGroomStruct> from, ObjectArrayList<CharacterGroomStruct> to) {

        if (groom == null || from.isEmpty() || openStep != null)
            return false;

        to.add(new CharacterGroomStruct(groom));
        this.groom = from.remove(from.size() - 1);
        this.dirty = true;
        changed();

        return true;
    }

    private void edited() {
        this.stepChanged = true;
        this.dirty = true;
        changed();
    }

    private void changed() {
        revision++;
        characterEditorManager.notifyChanged();
    }

    // Material \\

    // The material of the strand picked becomes the one chosen in the Materials tab, which new strands and the Brush
    // lay
    void pickMaterial(int strandIndex) {

        if (groom == null || strandIndex < 0 || strandIndex >= groom.getStrandCount())
            return;

        materialsManager.selectMaterial(groom.getStrand(strandIndex).getTextureName());
        changed();
    }

    // Draw \\

    // A new strand rooted where the surface was pressed, just off it along its normal, drawn on by extend; one undo
    // step from the press until end
    void beginDraw(Vector3 root, Vector3 normal, String boneName, float width) {

        if (groom == null)
            return;

        beginStep();
        this.drawnStrand = addStrand(root, normal, boneName, width);
        edited();
    }

    // The strand drawn carried on toward a point, a point laid at every spacing along the way; the point is where the
    // view poses it through the skinning matrices when given, carried back to the bind pose first
    void extendDraw(Vector3 point, Matrix4[] skinningMatrices) {

        if (drawnStrand == EngineSetting.INDEX_NOT_FOUND)
            return;

        CharacterStrandStruct strand = groom.getStrand(drawnStrand);
        Matrix4 skinning = resolveSkinning(strand, skinningMatrices);

        if (skinning != null) {
            inverseScratch.set(skinning).inverse();
            CharacterPoseUtility.transformPoint(inverseScratch, point.x, point.y, point.z, point);
        }

        float spacing = EditorSetting.CHARACTER_HAIR_POINT_SPACING;
        boolean extended = false;

        while (true) {

            int last = strand.getPointCount() - 1;

            strand.getPoint(last, pointA);
            tangent.set(point).subtract(pointA);

            float distance = tangent.length();

            if (distance < spacing)
                break;

            tangent.multiply(spacing / distance);
            strand.addPoint(pointA.x + tangent.x, pointA.y + tangent.y, pointA.z + tangent.z);
            extended = true;
        }

        if (extended)
            edited();
    }

    // The drag let go: a strand too short to be a card goes, and with mirror on the strand drawn gains its twin
    void endDraw(boolean mirror) {

        if (drawnStrand == EngineSetting.INDEX_NOT_FOUND)
            return;

        CharacterStrandStruct strand = groom.getStrand(drawnStrand);

        if (strand.getPointCount() < EditorSetting.CHARACTER_HAIR_MIN_POINTS)
            groom.getStrands().remove(drawnStrand);
        else if (mirror)
            addTwin(strand);

        this.drawnStrand = EngineSetting.INDEX_NOT_FOUND;
        edited();
        endStep();
    }

    boolean isDrawing() {
        return drawnStrand != EngineSetting.INDEX_NOT_FOUND;
    }

    // Place \\

    // A straight strand the given length standing along the surface's normal where it was clicked, with its twin
    // while mirroring; one undo step
    void place(Vector3 root, Vector3 normal, String boneName, float length, float width, boolean mirror) {

        if (groom == null)
            return;

        beginStep();

        CharacterStrandStruct strand = groom.getStrand(addStrand(root, normal, boneName, width));
        int segments = Math.max(1, (int) Math.ceil(length / EditorSetting.CHARACTER_HAIR_POINT_SPACING));

        strand.getPoint(0, pointA);

        for (int i = 1; i <= segments; i++) {

            float reach = length * i / segments;

            strand.addPoint(pointA.x + normal.x * reach, pointA.y + normal.y * reach, pointA.z + normal.z * reach);
        }

        if (mirror)
            addTwin(strand);

        edited();
        endStep();
    }

    // A strand of the material picked rooted just off the surface, its index
    private int addStrand(Vector3 root, Vector3 normal, String boneName, float width) {

        CharacterStrandStruct strand = new CharacterStrandStruct(
                normal,
                boneName,
                materialsManager.getSelectedMaterial(),
                width,
                EditorSetting.CHARACTER_HAIR_TAPER_DEFAULT);
        float lift = EditorSetting.CHARACTER_HAIR_ROOT_LIFT;

        strand.addPoint(root.x + normal.x * lift, root.y + normal.y * lift, root.z + normal.z * lift);
        groom.getStrands().add(strand);

        return groom.getStrandCount() - 1;
    }

    // A strand's mirror across x, rooted on the twin of its bone
    private void addTwin(CharacterStrandStruct strand) {

        mirrored.set(-strand.getNormal().x, strand.getNormal().y, strand.getNormal().z);

        CharacterStrandStruct twin = new CharacterStrandStruct(
                mirrored,
                CharacterMeshUtility.toMirrorBoneName(strand.getBoneName()),
                strand.getTextureName(),
                strand.getWidth(),
                strand.getTaper());

        for (int i = 0; i < strand.getPointCount(); i++)
            twin.addPoint(-strand.getX(i), strand.getY(i), strand.getZ(i));

        groom.getStrands().add(twin);
    }

    // Stroke \\

    // Opens a stroke's undo step, a selecting stroke that replaces the selection clearing it first, and finds each
    // strand's twin while mirroring
    void beginStroke(boolean mirror, CharacterEditorSelection selection) {

        if (groom == null)
            return;

        beginStep();
        resolveTwins(mirror);
        this.strokeSelection = selection;

        if (selection == CharacterEditorSelection.REPLACE)
            selectAll(false);
    }

    void endStroke() {
        endStep();
    }

    // Each strand's twin: the strand whose root stands at its mirrored root within the twin reach, never itself
    private void resolveTwins(boolean mirror) {

        twins.clear();
        twins.size(groom.getStrandCount());

        for (int i = 0; i < groom.getStrandCount(); i++) {

            twins.set(i, EngineSetting.INDEX_NOT_FOUND);

            if (!mirror)
                continue;

            CharacterStrandStruct strand = groom.getStrand(i);
            float reach = EditorSetting.CHARACTER_HAIR_TWIN_REACH * EditorSetting.CHARACTER_HAIR_TWIN_REACH;

            mirrored.set(-strand.getX(0), strand.getY(0), strand.getZ(0));

            for (int j = 0; j < groom.getStrandCount() && twins.getInt(i) == EngineSetting.INDEX_NOT_FOUND; j++) {

                groom.getStrand(j).getPoint(0, pointA);

                if (j != i && CharacterMeshUtility.distanceSquared(pointA, mirrored) <= reach)
                    twins.set(i, j);
            }
        }
    }

    // One frame of a stroke: the points under the brush listed strand by strand with the share of the brush each
    // takes, worked by the tool, and the same points of each strand's twin the mirrored way; drag is how far the
    // pointer moved in the world as the view shows it, for Nudge, carried back to the bind pose through each strand's
    // skinning when the skinning matrices are given
    void stroke(
            EditorTool tool,
            IntArrayList strands,
            IntArrayList points,
            FloatArrayList weights,
            Vector3 drag,
            Matrix4[] skinningMatrices) {

        if (groom == null || openStep == null || strands.isEmpty())
            return;

        touched.clear();
        touched.size(groom.getStrandCount());
        removed.clear();
        removed.size(groom.getStrandCount());

        int start = 0;

        while (start < strands.size()) {

            int strandIndex = strands.getInt(start);
            int end = start;

            groupPoints.clear();
            groupWeights.clear();

            while (end < strands.size() && strands.getInt(end) == strandIndex) {
                groupPoints.add(points.getInt(end));
                groupWeights.add(weights.getFloat(end));
                end++;
            }

            toBindDrag(groom.getStrand(strandIndex), drag, skinningMatrices);
            work(tool, strandIndex, bindDrag, false);

            int twin = strandIndex < twins.size() ? twins.getInt(strandIndex) : EngineSetting.INDEX_NOT_FOUND;

            if (twin != EngineSetting.INDEX_NOT_FOUND)
                work(tool, twin, bindDrag, true);

            start = end;
        }

        removeCut();

        if (tool == EditorTool.SELECT)
            changed();
        else
            edited();
    }

    // The tool's work on one strand at the points grouped, once a frame however many times it is reached
    private void work(EditorTool tool, int strandIndex, Vector3 drag, boolean twin) {

        if (touched.getBoolean(strandIndex))
            return;

        touched.set(strandIndex, true);

        CharacterStrandStruct strand = groom.getStrand(strandIndex);

        switch (tool) {
            case SELECT -> strand.setSelected(strokeSelection != CharacterEditorSelection.SUBTRACT);
            case BRUSH -> strand.setTextureName(materialsManager.getSelectedMaterial());
            case NUDGE -> comb(strand, drag, twin);
            case SMOOTH -> smooth(strand);
            case RAISE -> grow(strand, resolveMostWeight());
            case LOWER -> shorten(strand, resolveMostWeight());
            case ERASE -> cut(strandIndex, strand);
            default -> {
            }
        }
    }

    // The drag the view shows carried back to the bind pose through the turn and scale of a strand's skinning, kept
    // in the bind drag scratch; the twin takes it mirrored there, so the pair stays mirrored whatever the pose
    private void toBindDrag(CharacterStrandStruct strand, Vector3 drag, Matrix4[] skinningMatrices) {

        Matrix4 skinning = resolveSkinning(strand, skinningMatrices);

        bindDrag.set(drag);

        if (skinning == null)
            return;

        inverseScratch.set(skinning).inverse();
        CharacterPoseUtility.transformDirection(inverseScratch, drag.x, drag.y, drag.z, bindDrag);
    }

    private float resolveMostWeight() {

        float most = 0f;

        for (int i = 0; i < groupWeights.size(); i++)
            most = Math.max(most, groupWeights.getFloat(i));

        return most;
    }

    // Every point but the root carried along with the pointer by the share of the brush it takes, the strand's
    // segments then held to the lengths they had, as a comb keeps hair its length
    private void comb(CharacterStrandStruct strand, Vector3 drag, boolean twin) {

        float dragX = twin ? -drag.x : drag.x;

        holdLengths(strand);

        for (int i = 0; i < groupPoints.size(); i++) {

            int pointIndex = groupPoints.getInt(i);
            float weight = groupWeights.getFloat(i);

            if (pointIndex == 0)
                continue;

            strand.setPoint(
                    pointIndex,
                    strand.getX(pointIndex) + dragX * weight,
                    strand.getY(pointIndex) + drag.y * weight,
                    strand.getZ(pointIndex) + drag.z * weight);
        }

        restoreLengths(strand);
    }

    // Every inner point eased toward the middle of its neighbours by its share of the brush, lengths held
    private void smooth(CharacterStrandStruct strand) {

        holdLengths(strand);

        for (int i = 0; i < groupPoints.size(); i++) {

            int pointIndex = groupPoints.getInt(i);

            if (pointIndex == 0 || pointIndex + 1 >= strand.getPointCount())
                continue;

            float share = groupWeights.getFloat(i) * EditorSetting.CHARACTER_HAIR_SMOOTH_RATE;
            float middleX = (strand.getX(pointIndex - 1) + strand.getX(pointIndex + 1)) * 0.5f;
            float middleY = (strand.getY(pointIndex - 1) + strand.getY(pointIndex + 1)) * 0.5f;
            float middleZ = (strand.getZ(pointIndex - 1) + strand.getZ(pointIndex + 1)) * 0.5f;

            strand.setPoint(
                    pointIndex,
                    strand.getX(pointIndex) + (middleX - strand.getX(pointIndex)) * share,
                    strand.getY(pointIndex) + (middleY - strand.getY(pointIndex)) * share,
                    strand.getZ(pointIndex) + (middleZ - strand.getZ(pointIndex)) * share);
        }

        restoreLengths(strand);
    }

    private void holdLengths(CharacterStrandStruct strand) {

        lengths.clear();

        for (int i = 0; i + 1 < strand.getPointCount(); i++)
            lengths.add(strand.getSegmentLength(i));
    }

    // Each segment laid from the point before it, along the way it now runs, at the length it had
    private void restoreLengths(CharacterStrandStruct strand) {

        for (int i = 1; i < strand.getPointCount(); i++) {

            strand.getPoint(i - 1, pointA);
            strand.getPoint(i, pointB);
            tangent.set(pointB).subtract(pointA);

            float distance = tangent.length();

            if (distance < EditorSetting.CHARACTER_HAIR_EPSILON)
                continue;

            tangent.multiply(lengths.getFloat(i - 1) / distance);
            strand.setPoint(i, pointA.x + tangent.x, pointA.y + tangent.y, pointA.z + tangent.z);
        }
    }

    // The tip carried on along the way its last segment runs, a point laid at every spacing
    private void grow(CharacterStrandStruct strand, float weight) {

        float amount = EditorSetting.CHARACTER_HAIR_GROW_RATE * weight;
        float spacing = EditorSetting.CHARACTER_HAIR_POINT_SPACING;

        while (amount > 0f) {

            int last = strand.getPointCount() - 1;
            float segment = strand.getSegmentLength(last - 1);

            strand.getPoint(last - 1, pointA);
            strand.getPoint(last, pointB);
            tangent.set(pointB).subtract(pointA);

            if (segment < EditorSetting.CHARACTER_HAIR_EPSILON)
                return;

            tangent.multiply(1f / segment);

            float step = segment < spacing ? Math.min(amount, spacing - segment) : Math.min(amount, spacing);

            if (segment < spacing)
                strand.setPoint(last, pointB.x + tangent.x * step, pointB.y + tangent.y * step,
                        pointB.z + tangent.z * step);
            else
                strand.addPoint(pointB.x + tangent.x * step, pointB.y + tangent.y * step, pointB.z + tangent.z * step);

            amount -= step;
        }
    }

    // The tip drawn back along its last segment, a point given up whenever a segment runs out, never below the
    // shortest strand
    private void shorten(CharacterStrandStruct strand, float weight) {

        float amount = EditorSetting.CHARACTER_HAIR_GROW_RATE * weight;

        while (amount > 0f) {

            int last = strand.getPointCount() - 1;
            float segment = strand.getSegmentLength(last - 1);
            float kept = segment - amount;

            if (kept > EditorSetting.CHARACTER_HAIR_SEGMENT_MIN || last < EditorSetting.CHARACTER_HAIR_MIN_POINTS) {

                kept = Math.max(kept, EditorSetting.CHARACTER_HAIR_SEGMENT_MIN);
                strand.getPoint(last - 1, pointA);
                strand.getPoint(last, pointB);
                tangent.set(pointB).subtract(pointA)
                        .multiply(kept / Math.max(segment, EditorSetting.CHARACTER_HAIR_EPSILON));
                strand.setPoint(last, pointA.x + tangent.x, pointA.y + tangent.y, pointA.z + tangent.z);
                return;
            }

            strand.keepPoints(last);
            amount -= segment;
        }
    }

    // The strand cut where the brush first covers it from the root, gone whole where it covers the root or the first
    // point past it
    private void cut(int strandIndex, CharacterStrandStruct strand) {

        int first = strand.getPointCount();

        for (int i = 0; i < groupPoints.size(); i++)
            first = Math.min(first, groupPoints.getInt(i));

        if (first < EditorSetting.CHARACTER_HAIR_MIN_POINTS)
            removed.set(strandIndex, true);
        else
            strand.keepPoints(first);
    }

    // Every strand a cut took whole removed, the twins found again for what remains
    private void removeCut() {

        boolean any = false;

        for (int i = groom.getStrandCount() - 1; i >= 0; i--)
            if (removed.getBoolean(i)) {
                groom.getStrands().remove(i);
                any = true;
            }

        if (any)
            resolveTwins(characterEditorManager.isMirror());
    }

    // Pose \\

    // The skinning matrix a strand is posed by, that of the bone it grows from on the body's rig, as its card is
    // weighed wholly onto it; null in the bind pose (no skinning matrices) or for a bone the body's rig lacks, where
    // the strand stands as the groom holds it
    Matrix4 resolveSkinning(CharacterStrandStruct strand, Matrix4[] skinningMatrices) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (skinningMatrices == null || document == null)
            return null;

        RigHandle rigHandle = document.getMesh().getRigHandle();

        return rigHandle.hasBone(strand.getBoneName())
                ? skinningMatrices[rigHandle.getBoneIndex(strand.getBoneName())]
                : null;
    }

    // Selection \\

    void selectAll(boolean selected) {

        if (groom == null)
            return;

        for (int i = 0; i < groom.getStrandCount(); i++)
            groom.getStrand(i).setSelected(selected);

        changed();
    }

    void deleteSelected() {

        if (groom == null || groom.getSelectedCount() == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_NO_SELECTION);
            return;
        }

        int count = groom.getSelectedCount();

        beginStep();

        for (int i = groom.getStrandCount() - 1; i >= 0; i--)
            if (groom.getStrand(i).isSelected())
                groom.getStrands().remove(i);

        edited();
        endStep();
        characterEditorManager.setStatusMessage(count + EditorSetting.CHARACTER_HAIR_MESSAGE_DELETED);
    }

    // Selected Values \\

    // The first selected strand's width in millimetres, or its taper; zero with none selected
    float getSelectedWidth() {
        return findSelected() != null ? findSelected().getWidth() / EditorSetting.CHARACTER_HAIR_WIDTH_UNIT : 0f;
    }

    float getSelectedTaper() {
        return findSelected() != null ? findSelected().getTaper() : 0f;
    }

    // Every selected strand's width set, in millimetres within the Width option's range, as one undo step
    void setSelectedWidth(float millimetres) {

        float width = Math.max(EditorSetting.TOOL_HAIR_WIDTH_MIN, Math.min(EditorSetting.TOOL_HAIR_WIDTH_MAX,
                millimetres)) * EditorSetting.CHARACTER_HAIR_WIDTH_UNIT;

        editSelected(strand -> strand.setWidth(width));
    }

    void setSelectedTaper(float taper) {

        float clamped = Math.max(0f, Math.min(1f, taper));

        editSelected(strand -> strand.setTaper(clamped));
    }

    private void editSelected(Consumer<CharacterStrandStruct> edit) {

        if (groom == null || groom.getSelectedCount() == 0)
            return;

        beginStep();

        for (int i = 0; i < groom.getStrandCount(); i++)
            if (groom.getStrand(i).isSelected())
                edit.accept(groom.getStrand(i));

        edited();
        endStep();
    }

    private CharacterStrandStruct findSelected() {

        if (groom == null)
            return null;

        for (int i = 0; i < groom.getStrandCount(); i++)
            if (groom.getStrand(i).isSelected())
                return groom.getStrand(i);

        return null;
    }

    // From Worn \\

    // Every card of a worn hair mesh made a strand along the card's middle, as wide as the card and of its texture,
    // rooted on the bone weighing most on its root; a groom named after the feature is opened first when none is
    void takeFromWorn(RiggedMeshStruct mesh, String featureName) {

        characterHairBranch.detectCards(mesh, cards);

        if (cards.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_NO_WORN);
            return;
        }

        if (groom == null)
            adopt(new CharacterGroomStruct(mesh.getRigHandle().getRigName(), featureName),
                    resolveFreeName(FileUtility.getFileName(featureName)));

        beginStep();

        for (int c = 0; c < cards.size(); c++)
            groom.getStrands().add(toStrand(mesh, cards.get(c)));

        if (groom.getSourceFeature().isEmpty())
            groom.setSourceFeature(featureName);

        edited();
        endStep();
        characterEditorManager.setStatusMessage(cards.size() + EditorSetting.CHARACTER_HAIR_MESSAGE_FROM_WORN);
    }

    // A name in the Hair folder no file holds yet, the one given or it numbered on
    private String resolveFreeName(String name) {

        String free = name;

        for (int n = 2; resolveFile(free).exists(); n++)
            free = name + n;

        return free;
    }

    private CharacterStrandStruct toStrand(RiggedMeshStruct mesh, CharacterHairCardStruct card) {

        int rootVertex = card.getVertex(0);
        int face = findFace(mesh, rootVertex);
        float width = 0f;

        for (int i = 0; i < card.getVertices().size(); i++) {

            card.sampleAt(card.reachAt(card.getVertexV(i)), pointA);
            pointB.set(mesh.getX(card.getVertex(i)), mesh.getY(card.getVertex(i)), mesh.getZ(card.getVertex(i)));
            width = Math.max(width, (float) Math.sqrt(CharacterMeshUtility.distanceSquared(pointA, pointB)) * 2f);
        }

        CharacterMeshUtility.resolveFaceNormal(mesh, face, side);

        CharacterStrandStruct strand = new CharacterStrandStruct(
                side,
                resolveHeaviestBone(mesh, rootVertex),
                face != EngineSetting.INDEX_NOT_FOUND
                        ? mesh.getTextureName(mesh.getFaceTexture(face))
                        : materialsManager.getSelectedMaterial(),
                Math.max(width, EditorSetting.TOOL_HAIR_WIDTH_MIN * EditorSetting.CHARACTER_HAIR_WIDTH_UNIT),
                EditorSetting.CHARACTER_HAIR_TAPER_DEFAULT);
        int segments = Math.max(1, (int) Math.ceil(card.getLength() / EditorSetting.CHARACTER_HAIR_POINT_SPACING));

        for (int i = 0; i <= segments; i++) {
            card.sampleAt(card.getLength() * i / segments, pointA);
            strand.addPoint(pointA.x, pointA.y, pointA.z);
        }

        return strand;
    }

    private static int findFace(RiggedMeshStruct mesh, int vertexIndex) {

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (mesh.usesVertex(f, vertexIndex))
                return f;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private static String resolveHeaviestBone(RiggedMeshStruct mesh, int vertexIndex) {

        int heaviest = EngineSetting.INDEX_NOT_FOUND;
        float most = 0f;

        for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++)
            if (mesh.getInfluenceWeight(vertexIndex, i) > most) {
                most = mesh.getInfluenceWeight(vertexIndex, i);
                heaviest = mesh.getInfluenceBone(vertexIndex, i);
            }

        return heaviest != EngineSetting.INDEX_NOT_FOUND
                ? mesh.getRigHandle().getBone(heaviest).getName()
                : EditorSetting.CHARACTER_HAIR_DEFAULT_BONE;
    }

    // Export \\

    // The groom's cards written to the workspace's Exports as a rigged mesh on its rig, beside the scalp cap of the
    // hair it was taken from; false, with the refusal kept, when there is nothing to write or it could not be written
    boolean export() {

        if (groom == null) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NO_GROOM;
            return false;
        }

        RigHandle rigHandle;

        try {
            rigHandle = rigManager.getRigHandleFromRigName(groom.getRigName());
        } catch (InternalException e) {
            this.refusal = e.getMessage();
            return false;
        }

        RiggedMeshStruct mesh = buildCards(rigHandle);

        if (mesh.getFaceCount() == 0) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NO_STRANDS;
            return false;
        }

        ArpgObjectStruct sourceArpg = new ArpgObjectStruct();
        ArpgArrayStruct layout = new ArpgArrayStruct();
        File file = ArpgUtility.resolveFile(exportRoot, groomName);

        layout.add(EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        layout.add(EngineSetting.RIGGED_MESH_UV_FLOATS);
        sourceArpg.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigHandle.getRigName());
        sourceArpg.add(EditorSetting.CHARACTER_MODEL_FIELD_VAO, layout);
        file.getParentFile().mkdirs();

        if (!ArpgUtility.tryWriteObject(file, meshManager.toRiggedMeshArpg(mesh, sourceArpg))) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_WRITTEN + file.getName();
            return false;
        }

        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_EXPORTED + file.getName());
        return true;
    }

    // The scalp cap of the source hair, its cards taken away, then a card along every strand
    private RiggedMeshStruct buildCards(RigHandle rigHandle) {

        RiggedMeshStruct mesh = loadCap(rigHandle);

        for (int i = 0; i < groom.getStrandCount(); i++)
            if (groom.getStrand(i).getPointCount() >= EditorSetting.CHARACTER_HAIR_MIN_POINTS)
                layCard(mesh, rigHandle, groom.getStrand(i));

        return mesh;
    }

    // The source feature's mesh with every card face and the vertices only they used removed, an empty mesh on the
    // rig when the groom has no source or its mesh cannot be read
    private RiggedMeshStruct loadCap(RigHandle rigHandle) {

        RiggedMeshStruct empty = new RiggedMeshStruct(rigHandle);
        InfoEntryStruct feature = groom.getSourceFeature().isEmpty()
                ? null
                : infoManager.findEntry(
                        EditorSetting.INFO_SCHEMA_FEATURES, groom.getSourceFeature(), groom.getSourceFeature());

        if (feature == null)
            return empty;

        String meshName = ArpgUtility.getString(feature.getArpg(), EditorSetting.CHARACTER_HAIR_FIELD_MESH, "");
        File meshFile = ArpgUtility.resolveFile(new File(EngineSetting.MESH_PATH), meshName);
        ArpgObjectStruct meshArpg = meshFile.isFile() ? ArpgUtility.tryLoadObject(meshFile) : null;

        if (meshArpg == null || !meshManager.isRiggedMesh(meshArpg))
            return empty;

        RiggedMeshStruct mesh;

        try {
            mesh = meshManager.parseRiggedMesh(meshArpg, rigHandle, meshFile.getName());
        } catch (InternalException e) {
            return empty;
        }

        BooleanArrayList cardVertices = new BooleanArrayList();
        BooleanArrayList faceFlags = new BooleanArrayList();
        BooleanArrayList unused = new BooleanArrayList();

        characterHairBranch.detectCards(mesh, cards);
        cardVertices.size(mesh.getVertexCount());

        for (int c = 0; c < cards.size(); c++)
            for (int i = 0; i < cards.get(c).getVertices().size(); i++)
                cardVertices.set(cards.get(c).getVertex(i), true);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            boolean card = true;

            for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                card &= cardVertices.getBoolean(mesh.getFaceVertex(f, c));

            faceFlags.add(card);
        }

        mesh.removeFaces(faceFlags);
        unused.size(mesh.getVertexCount());

        for (int v = 0; v < mesh.getVertexCount(); v++)
            unused.set(v, true);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                unused.set(mesh.getFaceVertex(f, c), false);

        mesh.removeVertices(unused, new IntArrayList());

        return mesh;
    }

    // A card two quads wide along a strand, its rows of corners laid out by CharacterStrandUtility, every corner
    // wholly weighed onto the bone the strand grows from
    private void layCard(RiggedMeshStruct mesh, RigHandle rigHandle, CharacterStrandStruct strand) {

        int columns = EditorSetting.CHARACTER_HAIR_CARD_U.length;
        int rowFloats = CharacterStrandUtility.getRowFloats();
        int texture = mesh.resolveTexture(strand.getTextureName());
        int bone = rigHandle.getBoneIndex(strand.getBoneName());
        int first = mesh.getVertexCount();

        if (bone == EngineSetting.INDEX_NOT_FOUND)
            bone = Math.max(0, rigHandle.getBoneIndex(EditorSetting.CHARACTER_HAIR_DEFAULT_BONE));

        CharacterStrandUtility.resolveCardRows(strand, cardRows);

        for (int i = 0; i < strand.getPointCount(); i++) {

            int row = i * rowFloats;

            for (int u = 0; u < columns; u++) {

                int vertex = mesh.addVertex(0f, 0f, 0f);

                CharacterMeshUtility.setRoundedPosition(
                        mesh,
                        vertex,
                        cardRows.getFloat(row + u * 3),
                        cardRows.getFloat(row + u * 3 + 1),
                        cardRows.getFloat(row + u * 3 + 2));
                mesh.addInfluence(vertex, bone, 1f);
            }

            if (i == 0)
                continue;

            float vBefore = cardRows.getFloat(row - 1);
            float vAfter = cardRows.getFloat(row + rowFloats - 1);

            for (int u = 0; u + 1 < columns; u++)
                layCardFace(mesh, texture, first, i, u, vBefore, vAfter);
        }
    }

    // One quad of a card between a point's row and the one before it, wound to face along the root's normal
    private void layCardFace(
            RiggedMeshStruct mesh,
            int texture,
            int first,
            int row,
            int column,
            float vBefore,
            float vAfter) {

        int columns = EditorSetting.CHARACTER_HAIR_CARD_U.length;
        int before = first + (row - 1) * columns + column;
        int after = first + row * columns + column;
        float uLeft = EditorSetting.CHARACTER_HAIR_CARD_U[column];
        float uRight = EditorSetting.CHARACTER_HAIR_CARD_U[column + 1];
        int face = mesh.addFace(before, before + 1, after + 1, after, texture);

        mesh.setFaceZone(face, MeshZone.HAIR);
        mesh.setFaceUV(face, 0, uLeft, vBefore);
        mesh.setFaceUV(face, 1, uRight, vBefore);
        mesh.setFaceUV(face, 2, uRight, vAfter);
        mesh.setFaceUV(face, 3, uLeft, vAfter);
    }

    // Import \\

    // Whether a name may take an export: a feature path of valid names, one no feature holds or a hair feature
    boolean isFeatureName(String featureName) {

        if (featureName.isEmpty())
            return false;

        String[] segments = featureName.split(EditorSetting.INFO_FOLDER_SEPARATOR);

        for (int i = 0; i < segments.length; i++)
            if (!FileUtility.isValidFileName(segments[i], EditorSetting.NAME_INPUT_MAX_LENGTH))
                return false;

        InfoEntryStruct feature = infoManager.findEntry(EditorSetting.INFO_SCHEMA_FEATURES, featureName, featureName);

        return feature == null || EditorSetting.CHARACTER_HAIR_SLOT.equals(
                ArpgUtility.getString(feature.getArpg(), EditorSetting.CHARACTER_HAIR_FIELD_SLOT, ""));
    }

    // The feature an import is offered to become: the hair the groom was taken from, or one named after it
    String getDefaultFeatureName() {

        if (groom != null && !groom.getSourceFeature().isEmpty())
            return groom.getSourceFeature();

        return EditorSetting.CHARACTER_HAIR_DEFAULT_FEATURE_FOLDER + (groom != null ? groomName : "");
    }

    // An export in Exports/Hair brought into the game: its mesh written among the game's meshes under the feature's
    // name, and the feature made, or its mesh replaced; the mesh's name, or null with the refusal kept
    String importExport(File file, String featureName) {

        if (WorkspaceUtility.toName(exportRoot, file) == null) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_EXPORT;
            return null;
        }

        ArpgObjectStruct meshArpg = ArpgUtility.tryLoadObject(file);

        if (meshArpg == null || !meshManager.isRiggedMesh(meshArpg)) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_READ + file.getName();
            return null;
        }

        String meshName = EditorSetting.CHARACTER_MODEL_IMPORT_FOLDER + featureName
                + EditorSetting.CHARACTER_HAIR_MESH_SUFFIX;
        File meshFile = ArpgUtility.resolveFile(new File(EngineSetting.MESH_PATH), meshName);

        this.refusal = assignHairRig(meshArpg, meshName, featureName);

        if (refusal != null)
            return null;

        meshFile.getParentFile().mkdirs();

        if (!ArpgUtility.tryWriteObject(meshFile, meshArpg)) {
            this.refusal = EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_WRITTEN + meshName;
            return null;
        }

        meshManager.addMeshFile(meshName, meshFile);

        if (infoManager.hasDocument(EditorSetting.INFO_SCHEMA_FEATURES, featureName))
            infoManager.editEntry(EditorSetting.INFO_SCHEMA_FEATURES, featureName, featureName, feature ->
                    feature.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_MESH, meshName));
        else
            infoManager.createDocument(EditorSetting.INFO_SCHEMA_FEATURES, featureName, feature ->
                    fillFeature(feature, meshName));

        if (!infoManager.saveDocument(EditorSetting.INFO_SCHEMA_FEATURES, featureName)) {
            this.refusal = infoManager.getRefusal();
            return null;
        }

        return meshName;
    }

    // The rig the imported hair stands on, written into its mesh: the one its mesh is loaded on already where it was,
    // so every handle keeps its rig, else a rig of its own following the export's character rig, written and made
    // known now, as every hair style carries its own skeleton; null when it is set, else why not
    private String assignHairRig(ArpgObjectStruct meshArpg, String meshName, String featureName) {

        if (meshManager.hasMesh(meshName)) {
            meshArpg.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG,
                    meshManager.getMeshHandleFromMeshName(meshName).getRigHandle().getRigName());
            return null;
        }

        RigHandle exportRig = rigManager.getRigHandleFromRigName(
                ArpgUtility.validateString(meshArpg, EditorSetting.CHARACTER_MODEL_FIELD_RIG));

        if (exportRig.isFollower())
            return null;

        String rigName = characterWorkspaceBranch.resolveNewRigName(
                EditorSetting.CHARACTER_HAIR_RIG_FOLDER + FileUtility.getFileName(featureName));
        String rigRefusal = characterWorkspaceBranch.writeFollowerRig(rigName, exportRig);

        if (rigRefusal != null)
            return rigRefusal;

        meshArpg.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigName);
        return null;
    }

    // A new hair feature's fields, every default replaced: its slot and the mesh it wears
    private static void fillFeature(ArpgObjectStruct feature, String meshName) {

        ObjectArrayList<String> defaultKeys = new ObjectArrayList<>(feature.keySet());

        for (int i = 0; i < defaultKeys.size(); i++)
            feature.remove(defaultKeys.get(i));

        feature.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_SLOT, EditorSetting.CHARACTER_HAIR_SLOT);
        feature.addProperty(EditorSetting.CHARACTER_HAIR_FIELD_MESH, meshName);
    }

    // The imported mesh's long strands given spring chains on the hair's own rig and weighed onto them, through the Rig
    // and Weights tabs' own Rig Hair Locks and Weigh Onto Locks, then the mesh and the rig saved
    void laySprings(CharacterMeshDocumentInstance document) {

        characterRigBranch.addHairLocks(document.getMesh());
        characterWeightBranch.weighLocks(document);
        characterMeshBranch.save(document);
    }

    // Rows \\

    // The Properties tab's rows in the Hair tab: the groom, its count, the material picked and the selection's values
    void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (groom == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_HAIR_HEADER_NO_GROOM));
            return;
        }

        rows.add(CharacterViewRowStruct.header(
                dirty ? groomName + EditorSetting.CHARACTER_VIEW_DIRTY_MARKER : groomName));
        rows.add(CharacterViewRowStruct.header(groom.getStrandCount() + EditorSetting.CHARACTER_HAIR_LABEL_STRANDS
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + groom.getSelectedCount() + EditorSetting.CHARACTER_HAIR_LABEL_SELECTED));
        rows.add(CharacterViewRowStruct.choice(
                EditorSetting.CHARACTER_HAIR_KEY_MATERIAL,
                EditorSetting.CHARACTER_HAIR_LABEL_MATERIAL,
                getValueText(EditorSetting.CHARACTER_HAIR_KEY_MATERIAL)));

        if (groom.getSelectedCount() == 0)
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_HAIR_HEADER_SELECTED));
        rows.add(CharacterViewRowStruct.number(
                EditorSetting.CHARACTER_HAIR_KEY_WIDTH,
                EditorSetting.CHARACTER_HAIR_LABEL_WIDTH,
                getValueText(EditorSetting.CHARACTER_HAIR_KEY_WIDTH)));
        rows.add(CharacterViewRowStruct.number(
                EditorSetting.CHARACTER_HAIR_KEY_TAPER,
                EditorSetting.CHARACTER_HAIR_LABEL_TAPER,
                getValueText(EditorSetting.CHARACTER_HAIR_KEY_TAPER)));
    }

    // Keys \\

    boolean handles(String keyName) {
        return keyName.startsWith(EditorSetting.CHARACTER_HAIR_KEY_PREFIX);
    }

    String getValueText(String key) {
        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_HAIR_KEY_MATERIAL -> CharacterViewUtility.toResourceLabel(
                    materialsManager.getSelectedMaterial());
            case EditorSetting.CHARACTER_HAIR_KEY_WIDTH -> CharacterViewUtility.formatNumber(getSelectedWidth());
            case EditorSetting.CHARACTER_HAIR_KEY_TAPER -> CharacterViewUtility.formatNumber(getSelectedTaper());
            default -> "";
        };
    }

    boolean isValueValid(String key, String text) {

        String keyName = CharacterViewUtility.getKeyName(key);

        return (keyName.equals(EditorSetting.CHARACTER_HAIR_KEY_WIDTH)
                || keyName.equals(EditorSetting.CHARACTER_HAIR_KEY_TAPER))
                && findSelected() != null && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {

        float value = CharacterViewUtility.parseNumber(text);

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_HAIR_KEY_WIDTH))
            setSelectedWidth(value);
        else
            setSelectedTaper(value);
    }

    void step(String key, int direction) {

        if (findSelected() == null)
            return;

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_HAIR_KEY_WIDTH))
            setSelectedWidth(getSelectedWidth() + direction * EditorSetting.CHARACTER_HAIR_WIDTH_STEP);
        else if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_HAIR_KEY_TAPER))
            setSelectedTaper(getSelectedTaper() + direction * EditorSetting.CHARACTER_HAIR_TAPER_STEP);
    }

    void act(String key) {
        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_HAIR_KEY_FROM_WORN -> characterEditorManager.takeHairFromWorn();
            case EditorSetting.CHARACTER_HAIR_KEY_SELECT_ALL -> selectAll(true);
            case EditorSetting.CHARACTER_HAIR_KEY_SELECT_NONE -> selectAll(false);
            case EditorSetting.CHARACTER_HAIR_KEY_DELETE -> deleteSelected();
            default -> {
            }
        }
    }

    // The material stepped through every material of the Materials tab
    void cycle(int direction) {
        materialsManager.cycleMaterial(direction);
        changed();
    }

    // Accessible \\

    boolean hasGroom() {
        return groom != null;
    }

    CharacterGroomStruct getGroom() {
        return groom;
    }

    String getGroomName() {
        return groomName;
    }

    int getRevision() {
        return revision;
    }

    String getRefusal() {
        return refusal;
    }
}
