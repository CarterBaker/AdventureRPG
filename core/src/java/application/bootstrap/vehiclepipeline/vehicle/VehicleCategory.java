package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.EngineSetting;

public enum VehicleCategory {

    /*
     * The kind of thing a vehicle is. Vehicle types are listed grouped under
     * these headers, in this order, and a vehicle's ARPG names its category by
     * the lower-case constant name.
     */

    SHIP(EngineSetting.VEHICLE_CATEGORY_TITLE_SHIP);

    // Values
    public static final VehicleCategory[] VALUES = values();

    // Internal
    private final String title;

    // Constructor \\

    VehicleCategory(String title) {
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
