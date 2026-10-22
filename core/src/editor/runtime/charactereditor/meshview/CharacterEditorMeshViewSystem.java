package editor.runtime.charactereditor.meshview;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorMeshViewSystem extends SystemPackage {

    /*
     * Places the character's mesh parts where this tab's viewport shows them
     * while a mesh mode is open: every vertex of every worn part skinned on
     * the CPU by the pose the preview strikes, exactly as the shader skins
     * it. From those positions it projects vertices onto the window, finds
     * the active part's vertex nearest the pointer, the ones inside a box or
     * under a brush, and — unless X-Ray is on — leaves out any a worn face
     * hides from the camera. Positions are refreshed each frame and again on
     * demand after an edit; their storage only ever grows.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPoseSystem characterViewPoseSystem;

    // Parts
    private ObjectArrayList<CharacterMeshDocumentInstance> wornDocuments;
    private ObjectArrayList<FloatArrayList> wornPositions;
    private CharacterMeshDocumentInstance activeDocument;
    private FloatArrayList activePositions;

    // Scratch
    private Vector2 screenScratch;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

    // Base \\

    @Override
    protected void create() {

        // Parts
        this.wornDocuments = new ObjectArrayList<>();
        this.wornPositions = new ObjectArrayList<>();

        // Scratch
        this.screenScratch = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
    }

    // Update \\

    @Override
    protected void update() {
        refresh();
    }

    // Skins every worn part's vertices by the pose struck this frame
    public void refresh() {

        this.activeDocument = null;
        this.activePositions = null;
        wornDocuments.clear();

        if (!characterEditorManager.getMode().isMeshMode() || !characterViewPoseSystem.hasSubject())
            return;

        characterEditorManager.collectWornMeshDocuments(wornDocuments);
        CharacterMeshDocumentInstance active = characterEditorManager.getActiveMeshDocument();

        while (wornPositions.size() < wornDocuments.size())
            wornPositions.add(new FloatArrayList());

        for (int i = 0; i < wornDocuments.size(); i++) {

            skin(wornDocuments.get(i).getMesh(), wornPositions.get(i));

            if (wornDocuments.get(i) == active) {
                this.activeDocument = active;
                this.activePositions = wornPositions.get(i);
            }
        }
    }

    // Each vertex moved by its bones' skinning matrices in proportion to their weights
    private void skin(RiggedMeshStruct mesh, FloatArrayList positions) {

        Matrix4[] skinningMatrices = characterViewPoseSystem.getAnimationStateHandle().getSkinningMatrices();
        int vertexCount = mesh.getVertexCount();

        positions.clear();
        positions.ensureCapacity(vertexCount * EngineSetting.RIGGED_MESH_POSITION_FLOATS);

        for (int v = 0; v < vertexCount; v++) {

            float x = mesh.getX(v);
            float y = mesh.getY(v);
            float z = mesh.getZ(v);
            int influenceCount = mesh.getInfluenceCount(v);

            if (influenceCount == 0) {
                positions.add(x);
                positions.add(y);
                positions.add(z);
                continue;
            }

            float sx = 0f;
            float sy = 0f;
            float sz = 0f;

            for (int i = 0; i < influenceCount; i++) {

                float[] m = skinningMatrices[mesh.getInfluenceBone(v, i)].val;
                float w = mesh.getInfluenceWeight(v, i);

                sx += (m[0] * x + m[4] * y + m[8] * z + m[12]) * w;
                sy += (m[1] * x + m[5] * y + m[9] * z + m[13]) * w;
                sz += (m[2] * x + m[6] * y + m[10] * z + m[14]) * w;
            }

            positions.add(sx);
            positions.add(sy);
            positions.add(sz);
        }
    }

    // Screen \\

    // A world point in window pixels, the origin bottom left as the pointer reads; false behind the camera
    public boolean project(float x, float y, float z, Vector2 out) {

        WindowInstance window = context.getWindow();
        float[] m = window.getActiveCamera().getViewProjection().val;
        float clipX = m[0] * x + m[4] * y + m[8] * z + m[12];
        float clipY = m[1] * x + m[5] * y + m[9] * z + m[13];
        float clipW = m[3] * x + m[7] * y + m[11] * z + m[15];

        if (clipW <= 0f)
            return false;

        out.set(
                (clipX / clipW + 1f) * 0.5f * window.getWidth(),
                (clipY / clipW + 1f) * 0.5f * window.getHeight());
        return true;
    }

    // The ray from the camera through a window pixel
    public void resolveRay(float pixelX, float pixelY, Vector3 origin, Vector3 direction) {

        WindowInstance window = context.getWindow();

        window.getActiveCamera().getPickRay(
                pixelX / window.getWidth() * 2f - 1f,
                pixelY / window.getHeight() * 2f - 1f,
                origin,
                direction);
    }

    // Picking \\

    // The active part's visible vertex nearest a pixel within the pick radius, or INDEX_NOT_FOUND
    public int pickVertex(float pixelX, float pixelY) {

        if (activeDocument == null)
            return EngineSetting.INDEX_NOT_FOUND;

        float nearest = CharacterEditorSetting.PICK_RADIUS_PIXELS * CharacterEditorSetting.PICK_RADIUS_PIXELS;
        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int v = 0; v < getVertexCount(); v++) {

            if (!project(getX(v), getY(v), getZ(v), screenScratch))
                continue;

            float dx = screenScratch.x - pixelX;
            float dy = screenScratch.y - pixelY;
            float distance = dx * dx + dy * dy;

            if (distance <= nearest && isVisible(v)) {
                nearest = distance;
                found = v;
            }
        }

        return found;
    }

    // Every visible vertex of the active part inside a window rectangle given by two opposite corners
    public void collectBox(float x0, float y0, float x1, float y1, IntArrayList vertices) {

        vertices.clear();

        if (activeDocument == null)
            return;

        float minX = Math.min(x0, x1);
        float maxX = Math.max(x0, x1);
        float minY = Math.min(y0, y1);
        float maxY = Math.max(y0, y1);

        for (int v = 0; v < getVertexCount(); v++) {

            if (!project(getX(v), getY(v), getZ(v), screenScratch))
                continue;

            if (screenScratch.x >= minX && screenScratch.x <= maxX && screenScratch.y >= minY
                    && screenScratch.y <= maxY && isVisible(v))
                vertices.add(v);
        }
    }

    // Every visible vertex of the active part under a round brush, each with the strength it falls off to there
    public void collectBrush(
            float pixelX,
            float pixelY,
            float radius,
            float strength,
            IntArrayList vertices,
            FloatArrayList strengths) {

        vertices.clear();
        strengths.clear();

        if (activeDocument == null)
            return;

        for (int v = 0; v < getVertexCount(); v++) {

            if (!project(getX(v), getY(v), getZ(v), screenScratch))
                continue;

            float dx = screenScratch.x - pixelX;
            float dy = screenScratch.y - pixelY;
            float falloff = CharacterMeshUtility.falloff((float) Math.sqrt(dx * dx + dy * dy), radius);

            if (falloff > 0f && isVisible(v)) {
                vertices.add(v);
                strengths.add(falloff * strength);
            }
        }
    }

    // Occlusion \\

    private boolean isVisible(int vertexIndex) {
        return characterEditorManager.isXray() || !isOccluded(getX(vertexIndex), getY(vertexIndex), getZ(vertexIndex));
    }

    // Whether any worn face stands between the camera and a point, short of the faces meeting at the point itself
    private boolean isOccluded(float x, float y, float z) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 eye = camera.getPosition();
        float dx = x - eye.x;
        float dy = y - eye.y;
        float dz = z - eye.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= 0f)
            return false;

        rayOrigin.set(eye);
        rayDirection.set(dx / distance, dy / distance, dz / distance);

        float reach = distance - CharacterEditorSetting.OCCLUSION_BIAS;

        for (int d = 0; d < wornDocuments.size(); d++)
            if (hitsMesh(wornDocuments.get(d).getMesh(), wornPositions.get(d), reach))
                return true;

        return false;
    }

    private boolean hitsMesh(RiggedMeshStruct mesh, FloatArrayList positions, float reach) {

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            int c0 = mesh.getFaceVertex(f, 0);
            int c1 = mesh.getFaceVertex(f, 1);
            int c2 = mesh.getFaceVertex(f, 2);
            int c3 = mesh.getFaceVertex(f, 3);

            if (hitsTriangle(positions, c0, c1, c2, reach) || hitsTriangle(positions, c2, c3, c0, reach))
                return true;
        }

        return false;
    }

    // Moller-Trumbore: whether the ray meets a triangle closer than the reach
    private boolean hitsTriangle(FloatArrayList positions, int a, int b, int c, float reach) {

        float[] p = positions.elements();
        int ia = a * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int ib = b * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int ic = c * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        float e1x = p[ib] - p[ia];
        float e1y = p[ib + 1] - p[ia + 1];
        float e1z = p[ib + 2] - p[ia + 2];
        float e2x = p[ic] - p[ia];
        float e2y = p[ic + 1] - p[ia + 1];
        float e2z = p[ic + 2] - p[ia + 2];
        float px = rayDirection.y * e2z - rayDirection.z * e2y;
        float py = rayDirection.z * e2x - rayDirection.x * e2z;
        float pz = rayDirection.x * e2y - rayDirection.y * e2x;
        float determinant = e1x * px + e1y * py + e1z * pz;

        if (Math.abs(determinant) < CharacterEditorSetting.OCCLUSION_EPSILON)
            return false;

        float inverse = 1f / determinant;
        float tx = rayOrigin.x - p[ia];
        float ty = rayOrigin.y - p[ia + 1];
        float tz = rayOrigin.z - p[ia + 2];
        float u = (tx * px + ty * py + tz * pz) * inverse;

        if (u < 0f || u > 1f)
            return false;

        float qx = ty * e1z - tz * e1y;
        float qy = tz * e1x - tx * e1z;
        float qz = tx * e1y - ty * e1x;
        float v = (rayDirection.x * qx + rayDirection.y * qy + rayDirection.z * qz) * inverse;

        if (v < 0f || u + v > 1f)
            return false;

        float t = (e2x * qx + e2y * qy + e2z * qz) * inverse;
        return t > CharacterEditorSetting.OCCLUSION_EPSILON && t < reach;
    }

    // Accessible \\

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    public CharacterMeshDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public int getVertexCount() {
        return activePositions.size() / EngineSetting.RIGGED_MESH_POSITION_FLOATS;
    }

    public float getX(int vertexIndex) {
        return activePositions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
    }

    public float getY(int vertexIndex) {
        return activePositions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + 1);
    }

    public float getZ(int vertexIndex) {
        return activePositions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + 2);
    }
}
