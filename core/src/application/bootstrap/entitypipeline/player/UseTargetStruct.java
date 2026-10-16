package application.bootstrap.entitypipeline.player;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCargoInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import engine.root.StructPackage;

public class UseTargetStruct extends StructPackage {

    /*
     * The item an entity would use with the activate binding: a world item, or
     * cargo aboard a vehicle with the vehicle it is aboard, whichever lies
     * nearer. Filled by PlayerManager and read by everything the activate
     * binding opens — a chest's container or a drafting table — so each asks
     * the same question the same way. Reused in place by its owner.
     */

    // Target
    private WorldItemInstance worldItem;
    private VehicleCargoInstance cargo;
    private VehicleInstance vehicle;

    // Management \\

    public void clear() {
        this.worldItem = null;
        this.cargo = null;
        this.vehicle = null;
    }

    public void setWorldItem(WorldItemInstance worldItem) {
        clear();
        this.worldItem = worldItem;
    }

    public void setCargo(VehicleInstance vehicle, VehicleCargoInstance cargo) {
        clear();
        this.vehicle = vehicle;
        this.cargo = cargo;
    }

    // Accessible \\

    public boolean hasTarget() {
        return worldItem != null || cargo != null;
    }

    public boolean isAboard() {
        return cargo != null;
    }

    // The definition of the item targeted; there must be one
    public ItemDefinitionHandle getItemDefinitionHandle() {
        return isAboard()
                ? cargo.getItemInstance().getItemDefinitionHandle()
                : worldItem.getItemDefinitionHandle();
    }

    public WorldItemInstance getWorldItem() {
        return worldItem;
    }

    public VehicleCargoInstance getCargo() {
        return cargo;
    }

    public VehicleInstance getVehicle() {
        return vehicle;
    }
}
