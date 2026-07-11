package application.bootstrap.weatherpipeline.weatherrendersystem;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadCellStruct;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class CloudRenderSystem extends SystemPackage {

    /*
     * Owns exactly one ModelInstance per active, cloud-bearing overhead cell
     * — created the moment a cell first streams in with a cloud, refreshed
     * in place every frame, and dropped the moment that cell disappears from
     * OverheadManager's registry. Each instance draws through the exact same
     * generic path terrain already uses (ModelManager.createModel() +
     * RenderManager.pushRenderCall()) — no cloud-specific instanced-buffer
     * system, and no cloud-specific hooks anywhere in RenderQueueHandle,
     * RenderSystem, or RenderManager. As far as the render pipeline is
     * concerned, a cloud object is just another model sharing a mesh and a
     * material definition with its neighbors, exactly like a chunk sharing
     * StandardSurfaceMaterial.
     *
     * Every cloud instance's MaterialInstance is its own clone of
     * EngineSetting.CLOUD_VOLUME_MATERIAL_NAME — one shared shader/material
     * DEFINITION per CloudType, individually cloned per physical cloud
     * object, mirroring exactly how WorldRenderManager clones one shared
     * terrain material per chunk mesh. Archetype-level values that never
     * change for the life of the instance (color, scale, density, toon
     * shading numbers — see bakeArchetypeUniforms()) are baked in once, at
     * creation. Per-instance values that DO change every frame — render
     * position, streaming fade alpha, and live weather intensity — are
     * refreshed every frame in updateInstance() via ordinary
     * MaterialInstance.setUniform() calls, the same mechanism
     * EntityRenderSystem already uses for u_hiddenBone.
     *
     * Positioning is resolved ENTIRELY on the CPU, once per instance per
     * frame — see updateInstance()'s own doc comment. Nothing about cloud
     * placement is read back out of a GPU-side UBO (PlayerPositionData or
     * otherwise), and nothing about cloud movement touches WindData either
     * — wind drift is already applied CPU-side by
     * OverheadManager.advanceWindDrift() before this class ever sees a
     * cell's position. WindData exists purely for later GPU-visual effects
     * (foliage sway and similar) and is deliberately never read here.
     *
     * There is deliberately no clear-and-rebuild-every-frame pass here
     * (unlike the old GPU-instanced buffer this replaces) — a cloud's
     * ModelInstance is stable for the entire life of its cell, so a cell
     * that was already streamed in last frame costs nothing more than a
     * handful of setUniform() calls this frame.
     *
     * Owned by WeatherRenderSystem, which supplies the per-window
     * fbo/window pairs to submit() — this class has no window/grid
     * awareness of its own, mirroring how FrustumCullingSystem never
     * touches WorldStreamManager directly and is instead handed a
     * GridInstance by WorldRenderManager each call.
     */

    // Internal
    private OverheadManager overheadManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private UBOManager uboManager;
    private RenderManager renderManager;
    private WeatherManager weatherManager;
    private WorldManager worldManager;

    // Mesh
    private MeshHandle cloudMeshHandle;

    // Registry — one ModelInstance per active, cloud-bearing cell, keyed by
    // OverheadCellStruct.getCellKey(). Populated/pruned in updateInstances().
    private Long2ObjectOpenHashMap<ModelInstance> cellKey2Model;

    // Internal \\

    @Override
    protected void create() {
        this.cellKey2Model = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.overheadManager = get(OverheadManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.uboManager = get(UBOManager.class);
        this.renderManager = get(RenderManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void awake() {

        this.cloudMeshHandle = meshManager.getMeshHandleFromMeshName(EngineSetting.CLOUD_VOLUME_MESH_NAME);

        pushCloudSettings();
    }

    // Settings \\

    /*
     * Pushed once at bootstrap, never per frame — see CloudSettingsData.glsl's
     * own doc comment. Real cloud OBJECTS are capped to whichever is smaller:
     * the actual configured render distance (Settings.maxRenderDistance), or
     * the weather simulation's own design-intent near range
     * (EngineSetting.WEATHER_NEAR_RANGE_CHUNKS) — render distance is almost
     * always the smaller of the two in practice, since terrain itself never
     * draws past it, which is exactly what guarantees a real cloud object is
     * never rendered floating over ground that was never drawn. Individual
     * clouds shrink toward CLOUD_HORIZON_MIN_SCALE as they approach the edge
     * of that radius, so a cloud dissolving into the sky-dome preview and a
     * cloud streaming out of OverheadManager's registry always happen at the
     * same boundary — see OverheadManager, which derives its own streaming
     * radius from this identical expression.
     *
     * skyViewDistanceBlocks is the world-unit version of
     * WEATHER_FAR_RANGE_CHUNKS — the same radius RegionSampleBranch already
     * samples its 8 compass directions within, and deliberately untouched
     * by render distance — so the sky dome never implies weather exists
     * farther out than the simulation actually resolves, regardless of how
     * far terrain itself is currently configured to draw.
     * transitionStartBlocks marks where, within the near radius, real cloud
     * objects should start taking over from that sky representation
     * (CLOUD_VOLUME_FADE_START_RATIO of the way to the streaming edge).
     * Neither is consumed by any shader yet — see CloudSettingsData.glsl.
     */
    private void pushCloudSettings() {

        UBOHandle cloudSettingsData = uboManager.getUBOHandleFromUBOName(EngineSetting.CLOUD_SETTINGS_DATA_UBO);

        float cloudObjectRangeChunks = Math.min(
                settings.maxRenderDistance,
                (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);

        float horizonDistanceBlocks = cloudObjectRangeChunks * EngineSetting.CHUNK_SIZE
                * EngineSetting.CLOUD_HORIZON_RENDER_DISTANCE_SCALE;

        // Safety clamp — see EngineSetting.CLOUD_HORIZON_FAR_PLANE_SAFETY_MARGIN's
        // own doc comment. A large configured render distance previously let
        // this horizon sit beyond the camera's own far clip plane, silently
        // clipping every cloud object before it was ever drawn — visually
        // indistinguishable from "overhead clouds are not rendering at all".
        float maxSafeHorizonBlocks = EngineSetting.CAMERA_FAR_PLANE
                * EngineSetting.CLOUD_HORIZON_FAR_PLANE_SAFETY_MARGIN;
        horizonDistanceBlocks = Math.min(horizonDistanceBlocks, maxSafeHorizonBlocks);

        float skyViewDistanceBlocks = EngineSetting.WEATHER_FAR_RANGE_CHUNKS * EngineSetting.CHUNK_SIZE;
        float transitionStartBlocks = horizonDistanceBlocks * EngineSetting.CLOUD_VOLUME_FADE_START_RATIO;

        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_HORIZON_DISTANCE, horizonDistanceBlocks);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_MIN_SCALE, EngineSetting.CLOUD_HORIZON_MIN_SCALE);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_MAX_SCALE, EngineSetting.CLOUD_HORIZON_MAX_SCALE);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_SKY_VIEW_DISTANCE, skyViewDistanceBlocks);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_TRANSITION_START, transitionStartBlocks);

        uboManager.push(cloudSettingsData);
    }

    // Update \\

    /*
     * Prunes any ModelInstance whose cell no longer exists, then walks
     * every currently active overhead cell and creates or refreshes its
     * instance. Called once per frame, before any window submits — never
     * per-window, since the underlying cell grid is itself global (see
     * OverheadManager's own doc comment on why a cell's identity must never
     * re-roll per frame).
     *
     * The reference position is resolved once here, up front, as a
     * CONTINUOUS chunk-space coordinate — WeatherManager
     * .getReferenceChunkXContinuous()/getReferenceChunkZContinuous(), not
     * the chunk-granular getReferenceCoordinate() OverheadManager itself
     * streams cells around. Cell EXISTENCE only ever needs chunk
     * granularity (which cells should be active), but a cell's actual
     * RENDER position needs the player's exact sub-chunk position too —
     * using only the integer chunk here previously froze every cloud's
     * contribution to its own render offset for the player's entire time
     * inside one chunk, then snapped it by a full chunk width the instant
     * the player's chunk index changed. That was exactly the "clouds jump
     * when the player wraps inside a chunk" bug; see updateInstance()'s own
     * doc comment for the fix. The active world is likewise resolved once
     * here and threaded through to updateInstance() — every cell's render
     * position is rebuilt from scratch against these two fresh values in
     * updateInstance() — nothing about cloud placement is cached from a
     * previous frame, and nothing about it is read back out of a GPU-side
     * UBO. See updateInstance()'s own doc comment.
     */
    void updateInstances() {

        pruneRetiredCells();

        double referenceChunkX = weatherManager.getReferenceChunkXContinuous();
        double referenceChunkZ = weatherManager.getReferenceChunkZContinuous();

        WorldHandle activeWorld = worldManager.getActiveWorld();

        for (OverheadCellStruct cell : overheadManager.getActiveCells().values())
            updateInstance(cell, referenceChunkX, referenceChunkZ, activeWorld);
    }

    private void pruneRetiredCells() {

        if (cellKey2Model.isEmpty())
            return;

        ObjectIterator<Long2ObjectMap.Entry<ModelInstance>> iterator = cellKey2Model.long2ObjectEntrySet()
                .iterator();

        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<ModelInstance> entry = iterator.next();
            if (!overheadManager.getActiveCells().containsKey(entry.getLongKey()))
                iterator.remove();
        }
    }

    /*
     * Clear (or otherwise cloudless) cells still exist in OverheadManager's
     * registry so their patch of weather stays trackable — see
     * OverheadManager's own doc comment — but there is nothing to render
     * here, and no ModelInstance is ever created for one. A cell's cloud
     * choice is fixed for its entire lifetime (see OverheadCellStruct), so
     * once an instance exists its archetype-level uniforms never need
     * re-baking — only position/fade/intensity are refreshed below.
     *
     * Render position is fully resolved here, on the CPU, every frame.
     * cell.getCurrentChunkX()/Z() is this cell's absolute chunk-space
     * position — its fixed home chunk (wrapped into the world's own bounds
     * at stream-in time, since the world is a torus for weather purposes —
     * see OverheadManager.wrapChunkCoordinate()) plus accumulated wind
     * drift, both tracked in real chunk units by OverheadCellStruct /
     * OverheadManager.advanceWindDrift(). The offset against the player's
     * own CONTINUOUS chunk-space position (referenceChunkX/Z — a double,
     * carrying the player's exact sub-chunk position rather than merely
     * which chunk they currently stand in) is resolved via WorldWrapUtility
     * .wrappedDeltaX()/Z() — the same toroidal shortest-distance
     * correction OverheadManager.advanceFadesAndRetire() already applies
     * for its own retirement check — rather than a plain subtraction. A
     * cell whose home chunk wrapped around one edge of the world (any cell
     * west/north of a player spawned near the world's chunk-space origin,
     * for instance) sits numerically near the OPPOSITE edge of the world;
     * naive subtraction against the player's own small, unwrapped chunk
     * index then read that cell as almost an entire world-width away
     * instead of the few chunks away it actually was, which is exactly
     * what previously kept the overhead cloud grid confined to a single
     * quadrant of the map. Scaling up to block units only after this
     * correction is what keeps the result safe to store in a float: the
     * outcome is bounded by the streaming radius (at most a few thousand
     * blocks) no matter how large the absolute chunk coordinate itself has
     * grown over a long session — exactly mirroring how the terrain grid's
     * own u_gridPosition offset stays small by construction (see
     * GridBuildSystem).
     *
     * Using referenceChunkX/Z's fractional part is the actual fix for the
     * chunk-wrap position jump: previously this method compared against a
     * plain integer chunk coordinate, which is constant for as long as the
     * player stays inside one chunk, while the camera itself keeps moving
     * continuously within that same chunk. The cloud's contribution to the
     * render offset silently drifted out of sync with the camera for the
     * player's entire time inside a chunk, then corrected itself in one
     * visible jump the instant the player's chunk index changed. Carrying
     * the fractional chunk position through here removes that entirely —
     * the cloud's contribution to the offset now moves continuously in
     * lockstep with the camera's own motion, with no special case needed
     * at the chunk boundary itself.
     *
     * The vertex shader receives that one resolved vec3 directly as
     * u_cloudInstancePosition and does no chunk-index math, no ivec2 split,
     * and no PlayerPositionData UBO read of its own — the previous split
     * (an ivec2 chunk index + a shader-side subtraction against the
     * terrain's own player-chunk uniform) was also where a latent Z/X mixup
     * lived, silently placing every cloud thousands of blocks away from
     * where it belonged. Resolving the whole offset here, in one place, in
     * Java, removes that entire class of bug.
     */
    private void updateInstance(
            OverheadCellStruct cell,
            double referenceChunkX,
            double referenceChunkZ,
            WorldHandle activeWorld) {

        if (!cell.hasCloud())
            return;

        ModelInstance model = cellKey2Model.get(cell.getCellKey());

        if (model == null) {
            model = createInstance(cell);
            cellKey2Model.put(cell.getCellKey(), model);
        }

        MaterialInstance material = model.getMaterial();

        double relativeChunkX = WorldWrapUtility.wrappedDeltaX(activeWorld, cell.getCurrentChunkX(), referenceChunkX);
        double relativeChunkZ = WorldWrapUtility.wrappedDeltaZ(activeWorld, cell.getCurrentChunkZ(), referenceChunkZ);

        float renderX = (float) (relativeChunkX * EngineSetting.CHUNK_SIZE);
        float renderZ = (float) (relativeChunkZ * EngineSetting.CHUNK_SIZE);

        material.setUniform(EngineSetting.UNIFORM_CLOUD_INSTANCE_POSITION,
                new Vector3(renderX, cell.getEffectiveAltitude(), renderZ));
        material.setUniform(EngineSetting.UNIFORM_CLOUD_INSTANCE_FADE_ALPHA, cell.getFadeAlpha());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_INSTANCE_INTENSITY, cell.getIntensity());
    }

    private ModelInstance createInstance(OverheadCellStruct cell) {

        MaterialInstance material = materialManager.cloneMaterial(EngineSetting.CLOUD_VOLUME_MATERIAL_NAME);

        bakeArchetypeUniforms(material, cell.getCloudHandle());

        // Stable for the cell's entire lifetime — baked once here rather
        // than refreshed alongside position/fade/intensity every frame.
        material.setUniform(EngineSetting.UNIFORM_CLOUD_INSTANCE_RANDOM_SEED, cell.getRandomSeed());

        return modelManager.createModel(cloudMeshHandle, material);
    }

    // Material Resolution \\

    /*
     * Bakes every archetype-level (never-per-instance) value off CloudData
     * into this instance's own cloned MaterialInstance. Called exactly once,
     * when a cell's ModelInstance is first created — see createInstance().
     * Split into two groups purely for readability:
     * - legacy card-shader fields (still read by the current
     * CloudVolumeShader.fsh)
     * - volumetric/toon fields (declared as uniforms in the shader but not
     * yet consumed by its shading logic — staged ahead of the raymarch
     * rework so that rework only has to change GLSL, not this method).
     */
    private void bakeArchetypeUniforms(MaterialInstance material, CloudHandle cloudHandle) {

        // Legacy card-shader fields
        material.setUniform(EngineSetting.UNIFORM_CLOUD_COLOR, cloudHandle.getCloudColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SCALE, cloudHandle.getScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_VERTICAL_THICKNESS, cloudHandle.getVerticalThickness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY, cloudHandle.getDensity());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_EDGE_SOFTNESS, cloudHandle.getEdgeSoftness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_PUFF_JITTER, cloudHandle.getPuffJitter());

        // Volumetric / toon fields
        material.setUniform(EngineSetting.UNIFORM_CLOUD_TOP_COLOR, cloudHandle.getTopColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_TOON_BANDS, cloudHandle.getToonBands());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY_NOISE_SCALE, cloudHandle.getDensityNoiseScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_NOISE_WARP_STRENGTH, cloudHandle.getNoiseWarpStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_COVERAGE_BIAS, cloudHandle.getCoverageBias());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SILHOUETTE_SOFTNESS, cloudHandle.getSilhouetteSoftness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SHADOW_COLOR, cloudHandle.getShadowColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SHADE_STRENGTH, cloudHandle.getShadeStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_RIM_LIGHT_STRENGTH, cloudHandle.getRimLightStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_AMBIENT_OCCLUSION_STRENGTH,
                cloudHandle.getAmbientOcclusionStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_BRIGHTNESS_MULTIPLIER,
                cloudHandle.getBrightnessMultiplier());
    }

    // Submit \\

    /*
     * Pushes every active cloud instance into this window's render queue
     * for the given target fbo, through the exact same
     * RenderManager.pushRenderCall() every other opaque-ish world-space
     * draw (terrain, entities) already goes through. Safe to call once per
     * active grid window each frame.
     */
    void submit(FboInstance fbo, WindowInstance window) {

        for (ModelInstance model : cellKey2Model.values())
            renderManager.pushRenderCall(model, fbo, 0, window);
    }
}