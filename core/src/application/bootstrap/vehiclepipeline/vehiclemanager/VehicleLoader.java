package application.bootstrap.vehiclepipeline.vehiclemanager;

import java.io.File;

import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class VehicleLoader extends LoaderPackage {

    /*
     * Scans the vehicle ARPG directory and loads every vehicle type into
     * VehicleManager, one type per file, named by its path below the
     * directory. Supports on-demand loading for a type not yet in the palette
     * while the loader lives.
     */

    // Internal
    private File root;
    private VehicleManager vehicleManager;
    private VehicleBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> vehicleName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.VEHICLE_PATH);
        this.vehicleName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Vehicle directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String vehicleName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            vehicleName2File.put(vehicleName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        create(VehicleGeometryBuilder.class);
        create(VehicleHullBuilder.class);
        this.internalBuilder = create(VehicleBuilder.class);
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String vehicleName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        VehicleHandle vehicleHandle = internalBuilder.build(file, vehicleName);

        vehicleManager.addVehicleHandle(vehicleHandle);
    }

    // On-Demand \\

    void request(String vehicleName) {

        File file = vehicleName2File.get(vehicleName);

        if (file == null)
            throwException("On-demand vehicle load failed — not found in scan registry: \"" + vehicleName + "\"");

        request(file);
    }
}
