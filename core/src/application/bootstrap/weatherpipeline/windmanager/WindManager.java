package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.wind.WindData;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class WindManager extends ManagerPackage {

    /*
     * Owns the world's wind simulation. Global wind is the planet's fixed
     * prevailing airflow, resolved once by GlobalWindBranch. Local wind is
     * that global airflow blended with season and the currently active
     * weather, recomputed every frame by LocalWindBranch. Consumed by
     * RegionSampleBranch (weather drift) and, eventually, cloud streaming
     * and rendering.
     *
     * Also owns the WindData GPU UBO — the live local wind direction/speed
     * plus a continuously-accumulated sky-dome drift offset (see
     * advanceSkyDrift()). Previously nothing pushed wind to the GPU at all;
     * the sky dome's distant cloud preview (sky/util/Clouds.glsl) faked its
     * own scroll with a fixed synthetic direction, completely decoupled from
     * the same wind driving the physical overhead cloud layer. The drift
     * offset is integrated incrementally every frame (direction * speed *
     * SKY_WIND_DRIFT_SCALE * deltaTime), exactly like
     * RegionSampleBranch.advanceWindDrift() / OverheadManager.advanceWindDrift()
     * already do for their own drift accumulators — never recomputed as
     * direction * speed * elapsedTime, which would retroactively replay this
     * instant's wind across the whole session's history the moment wind
     * changes, a visible pop.
     */

    // Internal
    private UBOManager uboManager;

    // Branches
    private GlobalWindBranch globalWindBranch;
    private LocalWindBranch localWindBranch;

    // Wind
    private WindHandle windHandle;

    // GPU
    private UBOHandle windData;

    // Sky Drift — see advanceSkyDrift()
    private double skyDriftX;
    private double skyDriftZ;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.globalWindBranch = create(GlobalWindBranch.class);
        this.localWindBranch = create(LocalWindBranch.class);

        // Wind
        this.windHandle = create(WindHandle.class);
        this.windHandle.constructor(new WindData());
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        globalWindBranch.assignData(windHandle);
        localWindBranch.assignData(windHandle);

        this.windData = uboManager.getUBOHandleFromUBOName(EngineSetting.WIND_DATA_UBO);
    }

    @Override
    protected void update() {
        localWindBranch.updateLocalWind();
        advanceSkyDrift();
        pushWindData();
    }

    // Sky Drift \\

    /*
     * Advances the sky dome's own wind-driven drift accumulator — see the
     * class comment for why this is a running integral rather than a raw
     * direction*speed*time product. SKY_WIND_DRIFT_SCALE is independently
     * tunable from OVERHEAD_DRIFT_SPEED_SCALE / WEATHER_WIND_DRIFT_SCALE
     * since the sky raymarch's domain (see SKY_LAYER_SCALE in Clouds.glsl)
     * is its own synthetic radius, not real chunk/block units.
     */
    private void advanceSkyDrift() {

        Vector3 direction = windHandle.getLocalWindDirection();
        float speed = windHandle.getLocalWindSpeed();
        float deltaTime = internal.getDeltaTime();

        skyDriftX += direction.x * speed * EngineSetting.SKY_WIND_DRIFT_SCALE * deltaTime;
        skyDriftZ += direction.z * speed * EngineSetting.SKY_WIND_DRIFT_SCALE * deltaTime;

        // Bounded purely to keep the accumulator from growing unbounded over
        // an arbitrarily long session — mirrors RegionSampleBranch's own
        // evolutionElapsedSeconds wrap, which never produces a visible seam
        // in practice since the wrap period is enormous relative to any
        // per-frame drift step.
        skyDriftX %= EngineSetting.SKY_WIND_DRIFT_WRAP;
        skyDriftZ %= EngineSetting.SKY_WIND_DRIFT_WRAP;
    }

    // GPU Push \\

    private void pushWindData() {

        windData.updateUniform(EngineSetting.UNIFORM_WIND_DIRECTION, windHandle.getLocalWindDirection());
        windData.updateUniform(EngineSetting.UNIFORM_WIND_SPEED, windHandle.getLocalWindSpeed());
        windData.updateUniform(EngineSetting.UNIFORM_WIND_DRIFT_OFFSET,
                new Vector2((float) skyDriftX, (float) skyDriftZ));

        uboManager.push(windData);
    }

    // Accessible \\

    public WindHandle getWindHandle() {
        return windHandle;
    }
}