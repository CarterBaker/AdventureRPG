package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;

public class CharacterMeshUtility extends EngineUtility {

    /*
     * Stateless geometry the character editor's mesh tools share: the smooth
     * falloff of proportional edits and brushes, turning a point about an
     * axis, the exact key of a position on the editor's decimal grid so a
     * point and its mirror across x find each other, the mirrored name of a
     * left or right bone, and the starting shapes of a new part. Every
     * position written is rounded to the editor's precision first.
     */

    // Falloff \\

    // Full strength at the centre easing smoothly to none at the radius
    public static float falloff(float distance, float radius) {

        if (radius <= 0f)
            return distance <= 0f ? 1f : 0f;

        float t = CharacterViewUtility.clamp(1f - distance / radius, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    // Rotation \\

    // A point turned about a unit axis through a pivot by an angle in radians
    public static void rotate(
            float x,
            float y,
            float z,
            Vector3 pivot,
            Vector3 axis,
            float angle,
            Vector3 out) {

        float vx = x - pivot.x;
        float vy = y - pivot.y;
        float vz = z - pivot.z;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float dot = (axis.x * vx + axis.y * vy + axis.z * vz) * (1f - cos);

        out.set(
                pivot.x + vx * cos + (axis.y * vz - axis.z * vy) * sin + axis.x * dot,
                pivot.y + vy * cos + (axis.z * vx - axis.x * vz) * sin + axis.y * dot,
                pivot.z + vz * cos + (axis.x * vy - axis.y * vx) * sin + axis.z * dot);
    }

    // Positions \\

    // A position's place on the editor's decimal grid packed into one key, the same for every point that rounds alike
    public static long toPositionKey(float x, float y, float z) {
        return pack(x) << (EditorSetting.CHARACTER_MESH_KEY_BITS * 2)
                | pack(y) << EditorSetting.CHARACTER_MESH_KEY_BITS
                | pack(z);
    }

    private static long pack(float value) {
        return Math.round(value * EditorSetting.CHARACTER_VIEW_DECIMAL_SCALE) & EditorSetting.CHARACTER_MESH_KEY_MASK;
    }

    public static void setRoundedPosition(RiggedMeshStruct mesh, int vertexIndex, float x, float y, float z) {
        mesh.setPosition(
                vertexIndex,
                CharacterViewUtility.round(x),
                CharacterViewUtility.round(y),
                CharacterViewUtility.round(z));
    }

    // Bones \\

    // A left bone's right twin or a right bone's left twin by name; any other bone mirrors onto itself
    public static String toMirrorBoneName(String boneName) {

        if (boneName.endsWith(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT))
            return boneName.substring(0, boneName.length() - EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT.length())
                    + EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT;

        if (boneName.endsWith(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT))
            return boneName.substring(0, boneName.length() - EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT.length())
                    + EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT;

        return boneName;
    }

    // Shapes \\

    // Adds a shape centred on a point, weighted wholly to one bone; returns the index of its first vertex
    public static int addShape(
            RiggedMeshStruct mesh,
            CharacterEditorShape shape,
            Vector3 center,
            float size,
            int boneIndex,
            int textureIndex) {

        int firstVertex = mesh.getVertexCount();
        float half = size * 0.5f;

        switch (shape) {
            case PLANE -> addPlane(mesh, center, half, boneIndex, textureIndex);
            case BOX -> addBox(mesh, center, half, boneIndex, textureIndex);
            case CYLINDER -> addCylinder(mesh, center, half, boneIndex, textureIndex);
            case SPHERE -> addSphere(mesh, center, half, boneIndex, textureIndex);
        }

        return firstVertex;
    }

    private static void addPlane(RiggedMeshStruct mesh, Vector3 center, float half, int boneIndex, int textureIndex) {

        int v0 = addShapeVertex(mesh, center, -half, -half, 0f, boneIndex);
        int v1 = addShapeVertex(mesh, center, half, -half, 0f, boneIndex);
        int v2 = addShapeVertex(mesh, center, half, half, 0f, boneIndex);
        int v3 = addShapeVertex(mesh, center, -half, half, 0f, boneIndex);

        mesh.addFace(v0, v1, v2, v3, textureIndex);
    }

    private static void addBox(RiggedMeshStruct mesh, Vector3 center, float half, int boneIndex, int textureIndex) {

        int v0 = addShapeVertex(mesh, center, -half, -half, -half, boneIndex);
        int v1 = addShapeVertex(mesh, center, half, -half, -half, boneIndex);
        int v2 = addShapeVertex(mesh, center, half, half, -half, boneIndex);
        int v3 = addShapeVertex(mesh, center, -half, half, -half, boneIndex);
        int v4 = addShapeVertex(mesh, center, -half, -half, half, boneIndex);
        int v5 = addShapeVertex(mesh, center, half, -half, half, boneIndex);
        int v6 = addShapeVertex(mesh, center, half, half, half, boneIndex);
        int v7 = addShapeVertex(mesh, center, -half, half, half, boneIndex);

        mesh.addFace(v4, v5, v6, v7, textureIndex);
        mesh.addFace(v1, v0, v3, v2, textureIndex);
        mesh.addFace(v5, v1, v2, v6, textureIndex);
        mesh.addFace(v0, v4, v7, v3, textureIndex);
        mesh.addFace(v7, v6, v2, v3, textureIndex);
        mesh.addFace(v0, v1, v5, v4, textureIndex);
    }

    // A cylinder standing along y, its caps closed by quads fanned from their centres
    private static void addCylinder(
            RiggedMeshStruct mesh,
            Vector3 center,
            float half,
            int boneIndex,
            int textureIndex) {

        int segments = EditorSetting.CHARACTER_MESH_SHAPE_SEGMENTS;
        int bottomCenter = addShapeVertex(mesh, center, 0f, -half, 0f, boneIndex);
        int topCenter = addShapeVertex(mesh, center, 0f, half, 0f, boneIndex);
        int firstRing = mesh.getVertexCount();

        for (int i = 0; i < segments; i++) {

            double angle = Math.PI * 2.0 * i / segments;
            float x = (float) Math.cos(angle) * half;
            float z = (float) Math.sin(angle) * half;

            addShapeVertex(mesh, center, x, -half, z, boneIndex);
            addShapeVertex(mesh, center, x, half, z, boneIndex);
        }

        for (int i = 0; i < segments; i++) {

            int bottom = firstRing + i * 2;
            int nextBottom = firstRing + (i + 1) % segments * 2;

            mesh.addFace(bottom, bottom + 1, nextBottom + 1, nextBottom, textureIndex);
        }

        for (int i = 0; i < segments; i += 2) {

            int a = firstRing + i * 2;
            int b = firstRing + (i + 1) % segments * 2;
            int c = firstRing + (i + 2) % segments * 2;

            mesh.addFace(topCenter, c + 1, b + 1, a + 1, textureIndex);
            mesh.addFace(bottomCenter, a, b, c, textureIndex);
        }
    }

    // A sphere of latitude rings, its polar bands closed by quads standing twice on their pole
    private static void addSphere(
            RiggedMeshStruct mesh,
            Vector3 center,
            float half,
            int boneIndex,
            int textureIndex) {

        int segments = EditorSetting.CHARACTER_MESH_SHAPE_SEGMENTS;
        int rings = EditorSetting.CHARACTER_MESH_SHAPE_RINGS;
        int top = addShapeVertex(mesh, center, 0f, half, 0f, boneIndex);
        int bottom = addShapeVertex(mesh, center, 0f, -half, 0f, boneIndex);
        int firstRing = mesh.getVertexCount();

        for (int r = 1; r < rings; r++) {

            double polar = Math.PI * r / rings;
            float y = (float) Math.cos(polar) * half;
            float radius = (float) Math.sin(polar) * half;

            for (int i = 0; i < segments; i++) {

                double angle = Math.PI * 2.0 * i / segments;
                addShapeVertex(mesh, center, (float) Math.cos(angle) * radius, y,
                        (float) Math.sin(angle) * radius, boneIndex);
            }
        }

        int lastRing = firstRing + (rings - 2) * segments;

        for (int i = 0; i < segments; i++) {

            int next = (i + 1) % segments;

            mesh.addFace(top, firstRing + next, firstRing + i, top, textureIndex);
            mesh.addFace(lastRing + i, lastRing + next, bottom, bottom, textureIndex);

            for (int r = 0; r < rings - 2; r++) {

                int upper = firstRing + r * segments;
                int lower = upper + segments;

                mesh.addFace(upper + i, upper + next, lower + next, lower + i, textureIndex);
            }
        }
    }

    private static int addShapeVertex(
            RiggedMeshStruct mesh,
            Vector3 center,
            float x,
            float y,
            float z,
            int boneIndex) {

        int vertexIndex = mesh.addVertex(0f, 0f, 0f);

        setRoundedPosition(mesh, vertexIndex, center.x + x, center.y + y, center.z + z);
        mesh.addInfluence(vertexIndex, boneIndex, 1f);

        return vertexIndex;
    }

    // Faces \\

    // Whether every corner of a face is selected
    public static boolean isFaceSelected(RiggedMeshStruct mesh, int faceIndex, BooleanArrayList selection) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (!selection.getBoolean(mesh.getFaceVertex(faceIndex, c)))
                return false;

        return true;
    }
}
