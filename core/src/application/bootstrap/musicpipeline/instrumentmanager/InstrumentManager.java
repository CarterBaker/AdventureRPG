package application.bootstrap.musicpipeline.instrumentmanager;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InstrumentManager extends ManagerPackage {

    /*
     * Owns the instrument palette for the engine lifetime, with IDs assigned
     * in registration order. An instrument is a thing that plays notes — the
     * note set it plays from, the pitches it reaches, how many notes it holds
     * at once and how every rig that can play it holds and plays it — and an
     * item names the one it is played as, as it names the weapon it is wielded
     * with. Instruments load on demand, when an item, a song or a tool first
     * names one, at bootstrap or while the game runs. INSTRUMENT_NONE (0) is
     * the reserved sentinel for an item that names none. Auto-triggers an
     * on-demand load via InstrumentLoader on a name-based cache miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> instrumentName2InstrumentID;
    private ObjectArrayList<InstrumentHandle> instrumentID2InstrumentHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.instrumentName2InstrumentID = RegistryUtility.createNameIndex();
        this.instrumentID2InstrumentHandle = RegistryUtility.createPalette();
        create(InstrumentLoader.class);
    }

    // Management \\

    short registerInstrumentName(String instrumentName) {
        return (short) RegistryUtility.registerID(
                instrumentName2InstrumentID, instrumentID2InstrumentHandle, instrumentName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addInstrument(InstrumentHandle handle) {
        instrumentID2InstrumentHandle.set(handle.getInstrumentID(), handle);
    }

    // Accessible \\

    public boolean hasInstrument(String instrumentName) {
        return RegistryUtility.getHandle(
                instrumentName2InstrumentID, instrumentID2InstrumentHandle, instrumentName) != null;
    }

    public short getInstrumentIDFromInstrumentName(String instrumentName) {

        if (!hasInstrument(instrumentName))
            ((InstrumentLoader) internalLoader).request(instrumentName);

        if (!hasInstrument(instrumentName))
            throwException("Instrument could not be loaded: \"" + instrumentName + "\"");

        return (short) instrumentName2InstrumentID.getInt(instrumentName);
    }

    public InstrumentHandle getInstrumentHandleFromInstrumentID(short instrumentID) {

        InstrumentHandle handle = RegistryUtility.getHandle(instrumentID2InstrumentHandle, instrumentID);

        if (handle == null)
            throwException("Instrument ID not found: " + instrumentID);

        return handle;
    }

    public InstrumentHandle getInstrumentHandleFromInstrumentName(String instrumentName) {
        return getInstrumentHandleFromInstrumentID(getInstrumentIDFromInstrumentName(instrumentName));
    }
}
