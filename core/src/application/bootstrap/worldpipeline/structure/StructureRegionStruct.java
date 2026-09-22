package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureRegionStruct extends StructPackage {

    /*
     * Everything that can write a block inside one structure region — every
     * placement plan whose bounding circle reaches it and every world road
     * whose band crosses it. A region spans many chunks, so the expensive
     * part of structure resolution (candidate rolls, validity checks, road
     * graph, path planning) happens once per region and every chunk inside
     * only filters this short list. Immutable once published.
     */

    public static final StructureRegionStruct EMPTY = new StructureRegionStruct();

    private final ObjectArrayList<StructurePlanStruct> plans = new ObjectArrayList<>();
    private final ObjectArrayList<RoadPathStruct> roads = new ObjectArrayList<>();

    public void addPlan(StructurePlanStruct plan) {
        plans.add(plan);
    }

    public void addRoad(RoadPathStruct road) {
        roads.add(road);
    }

    public ObjectArrayList<StructurePlanStruct> getPlans() {
        return plans;
    }

    public ObjectArrayList<RoadPathStruct> getRoads() {
        return roads;
    }

    public boolean isEmpty() {
        return plans.isEmpty() && roads.isEmpty();
    }
}
