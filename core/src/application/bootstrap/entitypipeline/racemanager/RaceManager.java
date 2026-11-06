package application.bootstrap.entitypipeline.racemanager;

import application.bootstrap.entitypipeline.race.RaceHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RaceManager extends ManagerPackage {

    /*
     * Owns the race palette for the engine lifetime. Every entity template
     * names the race it is made of, and every race the genders it may be
     * made as, each with the character model — mesh, rig, material and
     * animation tree — it requires. Auto-triggers an on-demand load via
     * RaceLoader on a name-based cache miss, so templates resolve their race
     * while they load, and builds unregistered previews of a race an editor
     * is changing.
     */

    // Palette
    private Object2IntOpenHashMap<String> raceName2RaceID;
    private ObjectArrayList<RaceHandle> raceID2RaceHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.raceName2RaceID = RegistryUtility.createNameIndex();
        this.raceID2RaceHandle = RegistryUtility.createPalette();
        create(RaceLoader.class);
    }

    // Management \\

    void addRace(String raceName, RaceHandle raceHandle) {
        RegistryUtility.registerHandle(
                raceName2RaceID, raceID2RaceHandle, raceName, raceHandle, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    // Accessible \\

    public boolean hasRace(String raceName) {
        return RegistryUtility.getHandle(raceName2RaceID, raceID2RaceHandle, raceName) != null;
    }

    public RaceHandle getRaceHandleFromRaceName(String raceName) {

        RaceHandle handle = RegistryUtility.getHandle(raceName2RaceID, raceID2RaceHandle, raceName);

        if (handle == null) {
            ((RaceLoader) internalLoader).request(raceName);
            handle = RegistryUtility.getHandle(raceName2RaceID, raceID2RaceHandle, raceName);
        }

        if (handle == null)
            throwException("Race could not be loaded: \"" + raceName + "\"");

        return handle;
    }

    // Preview \\

    // Builds a race from an ARPG tree without registering it, so an editor can show an edit before it is saved
    public RaceHandle buildPreviewRace(String raceName, ArpgObjectStruct raceArpg) {
        return ((RaceLoader) internalLoader).buildPreview(raceName, raceArpg);
    }
}
