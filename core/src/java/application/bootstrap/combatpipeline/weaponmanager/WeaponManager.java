package application.bootstrap.combatpipeline.weaponmanager;

import application.bootstrap.combatpipeline.weapon.WeaponHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeaponManager extends ManagerPackage {

    /*
     * Owns the weapon palette for the engine lifetime, with IDs assigned in
     * registration order. A weapon is how one kind of held thing fights —
     * the attack it makes from each side and the guard it holds toward each
     * — and an item names the one it is wielded with, as an entity names the
     * one its own body fights with. WEAPON_NONE (0) is the reserved sentinel
     * for an item that names none and fights with the default item weapon.
     * Auto-triggers an on-demand load via WeaponLoader on a name-based cache
     * miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> weaponName2WeaponID;
    private ObjectArrayList<WeaponHandle> weaponID2WeaponHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.weaponName2WeaponID = RegistryUtility.createNameIndex();
        this.weaponID2WeaponHandle = RegistryUtility.createPalette();
        create(WeaponLoader.class);
    }

    // Management \\

    short registerWeaponName(String weaponName) {
        return (short) RegistryUtility.registerID(
                weaponName2WeaponID, weaponID2WeaponHandle, weaponName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addWeapon(WeaponHandle handle) {
        weaponID2WeaponHandle.set(handle.getWeaponID(), handle);
    }

    // Accessible \\

    public boolean hasWeapon(String weaponName) {
        return RegistryUtility.getHandle(weaponName2WeaponID, weaponID2WeaponHandle, weaponName) != null;
    }

    public short getWeaponIDFromWeaponName(String weaponName) {

        if (!hasWeapon(weaponName))
            ((WeaponLoader) internalLoader).request(weaponName);

        if (!hasWeapon(weaponName))
            throwException("Weapon could not be loaded: \"" + weaponName + "\"");

        return (short) weaponName2WeaponID.getInt(weaponName);
    }

    public WeaponHandle getWeaponHandleFromWeaponID(short weaponID) {

        WeaponHandle handle = RegistryUtility.getHandle(weaponID2WeaponHandle, weaponID);

        if (handle == null)
            throwException("Weapon ID not found: " + weaponID);

        return handle;
    }

    public WeaponHandle getWeaponHandleFromWeaponName(String weaponName) {
        return getWeaponHandleFromWeaponID(getWeaponIDFromWeaponName(weaponName));
    }
}
