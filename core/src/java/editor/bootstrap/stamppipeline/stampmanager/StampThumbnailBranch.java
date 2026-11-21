package editor.bootstrap.stamppipeline.stampmanager;

import java.nio.ByteBuffer;
import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class StampThumbnailBranch extends BranchPackage {

    /*
     * Main thread — draws each stamp's emblem the way a creature kit shows
     * its parts: the part seen from the front, fitted to a square with a
     * margin, every face's two triangles laid over the texels they cover,
     * the nearest one kept, each shaded by how far it turns toward the
     * viewer. Texels hold the shade with the coverage as alpha, so the tile's
     * colour tints them. A texture is made the first time a tile asks for
     * one, drawn again only after its stamp is rebuilt, and deleted with its
     * stamp; rows are bottom first, as every sprite's are.
     */

    // Internal
    private TextureManager textureManager;

    // Thumbnails — one texture per stamp ID, 0 until first drawn, and whether it waits to be drawn again
    private IntArrayList stampID2Texture;
    private BooleanArrayList stampID2Stale;

    // Scratch
    private float[] shades;
    private float[] depths;
    private ByteBuffer texels;
    private float[] cornerX;
    private float[] cornerY;
    private float[] cornerZ;
    private Vector3 edgeA;
    private Vector3 edgeB;

    // Base \\

    @Override
    protected void create() {

        int texelCount = EditorSetting.STAMP_THUMBNAIL_SIZE * EditorSetting.STAMP_THUMBNAIL_SIZE;

        // Thumbnails
        this.stampID2Texture = new IntArrayList();
        this.stampID2Stale = new BooleanArrayList();

        // Scratch
        this.shades = new float[texelCount];
        this.depths = new float[texelCount];
        this.texels = BufferUtility.newByteBuffer(texelCount * EngineSetting.COLOR_CHANNEL_COUNT);
        this.cornerX = new float[EngineSetting.QUAD_VERTEX_COUNT];
        this.cornerY = new float[EngineSetting.QUAD_VERTEX_COUNT];
        this.cornerZ = new float[EngineSetting.QUAD_VERTEX_COUNT];
        this.edgeA = new Vector3();
        this.edgeB = new Vector3();
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void dispose() {

        for (int i = 0; i < stampID2Texture.size(); i++)
            if (stampID2Texture.getInt(i) != 0)
                textureManager.deleteTexture2D(stampID2Texture.getInt(i));

        stampID2Texture.clear();
        stampID2Stale.clear();
    }

    // Management \\

    // A rebuilt stamp is drawn again the next time a tile shows it
    void invalidate(int stampID) {

        reserve(stampID);
        stampID2Stale.set(stampID, true);
    }

    void release(int stampID) {

        if (stampID >= stampID2Texture.size() || stampID2Texture.getInt(stampID) == 0)
            return;

        textureManager.deleteTexture2D(stampID2Texture.getInt(stampID));
        stampID2Texture.set(stampID, 0);
        stampID2Stale.set(stampID, true);
    }

    private void reserve(int stampID) {

        while (stampID2Texture.size() <= stampID) {
            stampID2Texture.add(0);
            stampID2Stale.add(true);
        }
    }

    // Accessible \\

    // The stamp's emblem texture, made or drawn again first when it is missing or stale
    int getThumbnail(StampHandle stamp) {

        int stampID = stamp.getStampID();

        reserve(stampID);

        if (stampID2Texture.getInt(stampID) == 0)
            stampID2Texture.set(stampID, textureManager.createTexture2D(
                    EditorSetting.STAMP_THUMBNAIL_SIZE,
                    EditorSetting.STAMP_THUMBNAIL_SIZE,
                    EngineSetting.GL_CLAMP_TO_EDGE,
                    EngineSetting.GL_LINEAR));

        if (stampID2Stale.getBoolean(stampID)) {
            draw(stamp, stampID2Texture.getInt(stampID));
            stampID2Stale.set(stampID, false);
        }

        return stampID2Texture.getInt(stampID);
    }

    // Draw \\

    // Every face laid over the square from the front, the part's larger side spanning it within the margin
    private void draw(StampHandle stamp, int texture) {

        RiggedMeshStruct mesh = stamp.getMesh();
        Vector3 min = stamp.getBoundsMin();
        Vector3 max = stamp.getBoundsMax();
        int size = EditorSetting.STAMP_THUMBNAIL_SIZE;
        float span = Math.max(Math.max(max.x - min.x, max.y - min.y), Float.MIN_NORMAL);
        float scale = size * (1f - EditorSetting.STAMP_THUMBNAIL_MARGIN * 2f) / span;
        float offsetX = size * 0.5f - (min.x + max.x) * 0.5f * scale;
        float offsetY = size * 0.5f - (min.y + max.y) * 0.5f * scale;

        Arrays.fill(shades, 0f);
        Arrays.fill(depths, Float.NEGATIVE_INFINITY);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            for (int c = 0; c < mesh.getFaceCornerCount(f); c++) {

                int vertexIndex = mesh.getFaceVertex(f, c);

                cornerX[c] = mesh.getX(vertexIndex) * scale + offsetX;
                cornerY[c] = mesh.getY(vertexIndex) * scale + offsetY;
                cornerZ[c] = mesh.getZ(vertexIndex);
            }

            float shade = resolveShade(mesh, f);

            fillTriangle(0, 1, 2, shade);
            fillTriangle(0, 2, 3, shade);
        }

        upload(texture);
    }

    // How far a face turns toward the viewer, from the least shade at edge-on to full facing it
    private float resolveShade(RiggedMeshStruct mesh, int faceIndex) {

        int a = mesh.getFaceVertex(faceIndex, 0);
        int b = mesh.getFaceVertex(faceIndex, 1);
        int c = mesh.getFaceVertex(faceIndex, 2);
        int d = mesh.getFaceVertex(faceIndex, 3);

        edgeA.set(mesh.getX(c) - mesh.getX(a), mesh.getY(c) - mesh.getY(a), mesh.getZ(c) - mesh.getZ(a));
        edgeB.set(mesh.getX(d) - mesh.getX(b), mesh.getY(d) - mesh.getY(b), mesh.getZ(d) - mesh.getZ(b));

        float nx = edgeA.y * edgeB.z - edgeA.z * edgeB.y;
        float ny = edgeA.z * edgeB.x - edgeA.x * edgeB.z;
        float nz = edgeA.x * edgeB.y - edgeA.y * edgeB.x;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        float facing = length > 0f ? Math.abs(nz) / length : 0f;

        return EditorSetting.STAMP_THUMBNAIL_SHADE_MIN + (1f - EditorSetting.STAMP_THUMBNAIL_SHADE_MIN) * facing;
    }

    // One triangle of a face over every texel whose centre it covers, kept where it stands nearer than what is there
    private void fillTriangle(int a, int b, int c, float shade) {

        int size = EditorSetting.STAMP_THUMBNAIL_SIZE;
        float area = (cornerX[b] - cornerX[a]) * (cornerY[c] - cornerY[a])
                - (cornerY[b] - cornerY[a]) * (cornerX[c] - cornerX[a]);

        if (area == 0f)
            return;

        int minX = Math.max(0, (int) Math.floor(Math.min(cornerX[a], Math.min(cornerX[b], cornerX[c]))));
        int maxX = Math.min(size - 1, (int) Math.ceil(Math.max(cornerX[a], Math.max(cornerX[b], cornerX[c]))));
        int minY = Math.max(0, (int) Math.floor(Math.min(cornerY[a], Math.min(cornerY[b], cornerY[c]))));
        int maxY = Math.min(size - 1, (int) Math.ceil(Math.max(cornerY[a], Math.max(cornerY[b], cornerY[c]))));

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {

                float px = x + 0.5f;
                float py = y + 0.5f;
                float wa = ((cornerX[b] - px) * (cornerY[c] - py) - (cornerY[b] - py) * (cornerX[c] - px)) / area;
                float wb = ((cornerX[c] - px) * (cornerY[a] - py) - (cornerY[c] - py) * (cornerX[a] - px)) / area;
                float wc = 1f - wa - wb;

                if (wa < 0f || wb < 0f || wc < 0f)
                    continue;

                int index = y * size + x;
                float depth = wa * cornerZ[a] + wb * cornerZ[b] + wc * cornerZ[c];

                if (depth <= depths[index])
                    continue;

                depths[index] = depth;
                shades[index] = shade;
            }
    }

    private void upload(int texture) {

        int channelMax = EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        texels.clear();

        for (int i = 0; i < shades.length; i++) {

            byte shade = (byte) Math.round(shades[i] * channelMax);

            texels.put(shade)
                    .put(shade)
                    .put(shade)
                    .put((byte) (shades[i] > 0f ? channelMax : 0));
        }

        texels.flip();
        textureManager.updateTexture2D(
                texture, 0, 0, EditorSetting.STAMP_THUMBNAIL_SIZE, EditorSetting.STAMP_THUMBNAIL_SIZE, texels);
    }
}
