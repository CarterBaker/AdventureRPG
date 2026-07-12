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
     * that airflow blended with season and the active weather, recomputed
     * every frame by LocalWindBranch. Also owns the WindData GPU UBO — live
     * local wind plus a continuously accumulated sky-dome drift offset,
     * integrated incrementally each frame so a wind change never
     * retroactively replays the session's drift history as a visible pop.
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

    // Sky Drift
    private double skyDriftX;
    private double skyDriftZ;

    // Internal \\

    @Override
    protected void create() {

        this.globalWindBranch = create(GlobalWindBranch.class);
        this.localWindBranch = create(LocalWindBranch.class);

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

    private void advanceSkyDrift() {

        Vector3 direction = windHandle.getLocalWindDirection();
        float speed = windHandle.getLocalWindSpeed();
        float deltaTime = internal.getDeltaTime();

        skyDriftX += direction.x * speed * EngineSetting.SKY_WIND_DRIFT_SCALE * deltaTime;
        skyDriftZ += direction.z * speed * EngineSetting.SKY_WIND_DRIFT_SCALE * deltaTime;

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