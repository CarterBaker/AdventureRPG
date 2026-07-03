package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.weatherpipeline.wind.WindData;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import engine.root.ManagerPackage;

public class WindManager extends ManagerPackage {

    /*
     * Owns the world's wind simulation. Global wind is the planet's fixed
     * prevailing airflow, resolved once by GlobalWindBranch. Local wind is
     * that global airflow blended with season and the currently active
     * weather, recomputed every frame by LocalWindBranch. Consumed by
     * RegionSampleBranch (weather drift) and, eventually, cloud streaming
     * and rendering.
     */

    // Branches
    private GlobalWindBranch globalWindBranch;
    private LocalWindBranch localWindBranch;

    // Wind
    private WindHandle windHandle;

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
    protected void awake() {
        globalWindBranch.assignData(windHandle);
        localWindBranch.assignData(windHandle);
    }

    @Override
    protected void update() {
        localWindBranch.updateLocalWind();
    }

    // Accessible \\

    public WindHandle getWindHandle() {
        return windHandle;
    }
}