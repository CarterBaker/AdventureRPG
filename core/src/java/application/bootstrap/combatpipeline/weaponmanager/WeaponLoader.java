package application.bootstrap.combatpipeline.weaponmanager;

import java.io.File;

import application.bootstrap.combatpipeline.weapon.WeaponHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class WeaponLoader extends LoaderPackage {

    /*
     * Scans the weapon ARPG directory and loads every weapon into
     * WeaponManager. Supports on-demand loading for weapons not yet in the
     * palette — item definitions and entities resolve the weapon they name
     * through that path while they load.
     */

    // Internal
    private File root;
    private WeaponManager weaponManager;
    private WeaponBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> weaponName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.WEAPON_PATH);
        this.weaponName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Weapon ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String weaponName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            weaponName2File.put(weaponName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(WeaponBuilder.class);
    }

    @Override
    protected void get() {
        this.weaponManager = get(WeaponManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String weaponName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        WeaponHandle handle = internalBuilder.build(file, weaponName);

        if (handle == null)
            throwException("Failed to build weapon from: " + file.getAbsolutePath());

        weaponManager.addWeapon(handle);
    }

    // On-Demand \\

    void request(String weaponName) {

        File file = weaponName2File.get(weaponName);

        if (file == null)
            throwException("On-demand weapon load failed — not found in scan registry: \"" + weaponName + "\"");

        request(file);
    }
}
