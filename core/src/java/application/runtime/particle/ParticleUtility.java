package application.runtime.particle;

import java.util.SplittableRandom;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.oceanpipeline.util.OceanWaveUtility;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class ParticleUtility extends EngineUtility {

    /*
     * What every particle kind shares: the one card and the one ribbon their instances are drawn on, made the first
     * time any window asks and looked up by name after; the frame the window's camera sees the world in, the corner
     * of the chunk its player stands in, against which a particle's absolute place is laid across the world's wrap;
     * how hard the wind gusts; a mix and a roll between two bounds; and the push that draws a kind's instances into the
     * precipitation target with the scene's depth bound.
     */

    // Meshes \\

    // The card every leaf and bubble is drawn on, its corners from minus one to one across and along
    public static MeshHandle resolveQuad(MeshManager meshManager, VAOManager vaoManager) {

        if (meshManager.hasMesh(RuntimeSetting.PARTICLE_QUAD_MESH))
            return meshManager.getMeshHandleFromMeshName(RuntimeSetting.PARTICLE_QUAD_MESH);

        FloatArrayList vertices = new FloatArrayList(EngineSetting.QUAD_VERTEX_COUNT * 3);
        ShortArrayList indices = new ShortArrayList(EngineSetting.QUAD_INDEX_COUNT);

        addVertex(vertices, -1f, -1f);
        addVertex(vertices, 1f, -1f);
        addVertex(vertices, 1f, 1f);
        addVertex(vertices, -1f, 1f);
        addQuad(indices, 0, 1, 2, 3);

        return createMesh(meshManager, vaoManager, RuntimeSetting.PARTICLE_QUAD_MESH, vertices, indices);
    }

    // The strip every wind line is bent along: its vertices run from zero to one along it, in pairs a side apart
    public static MeshHandle resolveRibbon(MeshManager meshManager, VAOManager vaoManager) {

        if (meshManager.hasMesh(RuntimeSetting.PARTICLE_RIBBON_MESH))
            return meshManager.getMeshHandleFromMeshName(RuntimeSetting.PARTICLE_RIBBON_MESH);

        int segments = RuntimeSetting.PARTICLE_RIBBON_SEGMENTS;
        FloatArrayList vertices = new FloatArrayList((segments + 1) * 6);
        ShortArrayList indices = new ShortArrayList(segments * EngineSetting.QUAD_INDEX_COUNT);

        for (int i = 0; i <= segments; i++) {

            float along = i / (float) segments;

            addVertex(vertices, along, -1f);
            addVertex(vertices, along, 1f);
        }

        for (int i = 0; i < segments; i++)
            addQuad(indices, i * 2, i * 2 + 2, i * 2 + 3, i * 2 + 1);

        return createMesh(meshManager, vaoManager, RuntimeSetting.PARTICLE_RIBBON_MESH, vertices, indices);
    }

    // A vertex of the particle layout: its place across the card or ribbon, flat in its own plane
    private static void addVertex(FloatArrayList vertices, float x, float y) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(0f);
    }

    private static void addQuad(ShortArrayList indices, int a, int b, int c, int d) {
        indices.add((short) a);
        indices.add((short) b);
        indices.add((short) c);
        indices.add((short) a);
        indices.add((short) c);
        indices.add((short) d);
    }

    private static MeshHandle createMesh(
            MeshManager meshManager,
            VAOManager vaoManager,
            String meshName,
            FloatArrayList vertices,
            ShortArrayList indices) {
        return meshManager.createMeshHandle(
                meshName, vaoManager.getVAOHandleFromVAOName(RuntimeSetting.PARTICLE_VAO), vertices, indices);
    }

    // Frame \\

    // The corner of a chunk in absolute blocks, the frame a window's camera and every particle it draws stand in
    public static double resolveOriginX(long chunkCoordinate) {
        return (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE;
    }

    public static double resolveOriginZ(long chunkCoordinate) {
        return (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE;
    }

    // An absolute place laid in that frame, the short way round the world's wrap
    public static float toFrameX(WorldHandle world, double x, double originX) {
        return (float) WorldWrapUtility.wrappedBlockDeltaX(world, x, originX);
    }

    public static float toFrameZ(WorldHandle world, double z, double originZ) {
        return (float) WorldWrapUtility.wrappedBlockDeltaZ(world, z, originZ);
    }

    // Wind \\

    // How hard the wind gusts, from none at a breeze to one in a gale
    public static float resolveGust(float windSpeed) {
        return OceanWaveUtility.smoothstep(RuntimeSetting.PARTICLE_GUST_START, RuntimeSetting.PARTICLE_GUST_FULL,
                windSpeed);
    }

    // Random \\

    public static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }

    public static float roll(SplittableRandom random, float min, float max) {
        return min + (max - min) * (float) random.nextDouble();
    }

    // Draw \\

    // A kind's material bound to the scene's and the water's depth, which hide a particle behind them
    public static void bindSceneDepth(MaterialInstance material, int sceneDepth, int waterDepth) {
        material.setUniform(RuntimeSetting.UNIFORM_SCENE_DEPTH, sceneDepth);
        material.setUniform(RuntimeSetting.UNIFORM_WATER_DEPTH, waterDepth);
    }

    // A kind's instances drawn into the target this frame, nothing queued while it has none
    public static void draw(
            RenderManager renderManager,
            MaterialInstance material,
            CompositeBufferInstance buffer,
            FBOInstance target,
            WindowInstance window) {

        if (buffer.isEmpty())
            return;

        renderManager.ensureFboRendered(target, window);
        renderManager.pushCompositeCall(material, buffer, target, window);
    }
}
