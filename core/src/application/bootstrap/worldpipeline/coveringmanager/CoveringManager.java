package application.bootstrap.worldpipeline.coveringmanager;

import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CoveringManager extends ManagerPackage {

    /*
     * Owns the covering palette by name and ID. Coverings are the grass, moss
     * and litter that grow over the faces of their host blocks; a cell's
     * coverage lives in its subchunk's coverage palette, never in the block
     * palette, so one block takes any covering its hosts allow. Every
     * covering is loaded in awake(), before any chunk generates, and the
     * table of their tiles is pushed to the surface shader through
     * CoveringBufferSystem in the same breath, so the streaming threads only
     * ever read the palette. Covering IDs are assigned in registration order
     * and capped at the table's size.
     */

    // Internal
    private CoveringBufferSystem coveringBufferSystem;

    // Palette
    private Object2IntOpenHashMap<String> coveringName2CoveringID;
    private ObjectArrayList<CoveringHandle> coveringID2CoveringHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.coveringName2CoveringID = RegistryUtility.createNameIndex();
        this.coveringID2CoveringHandle = RegistryUtility.createPalette();

        // Internal
        this.coveringBufferSystem = create(CoveringBufferSystem.class);

        create(CoveringLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
        coveringBufferSystem.pushCoveringTable(coveringID2CoveringHandle);
    }

    // Management \\

    short registerCoveringName(String coveringName) {
        return (short) RegistryUtility.registerID(
                coveringName2CoveringID, coveringID2CoveringHandle, coveringName, EngineSetting.COVERING_ID_COUNT);
    }

    void addCoveringHandle(CoveringHandle coveringHandle) {

        short coveringID = coveringHandle.getCoveringID();

        if (coveringID2CoveringHandle.get(coveringID) != null)
            throwException("Duplicate covering name: '" + coveringHandle.getCoveringName()
                    + "' was registered more than once");

        coveringID2CoveringHandle.set(coveringID, coveringHandle);
    }

    // On-Demand \\

    public void request(String coveringName) {
        ((CoveringLoader) internalLoader).request(coveringName);
    }

    // Coverage \\

    // A named covering at a level, packed for a coverage palette, refused naming its owner when it cannot grow there
    public short resolveCoverage(String coveringName, int level, short hostBlockID, String ownerName) {

        CoveringHandle coveringHandle = getCoveringHandleFromCoveringName(coveringName);

        if (!coveringHandle.canHost(hostBlockID))
            throwException("\"" + ownerName + "\" lays covering \"" + coveringName
                    + "\" over a block it does not name among its \"hosts\".");

        if (level < 1 || level > CoverageUtility.LEVEL_MAX)
            throwException("\"" + ownerName + "\" lays covering \"" + coveringName + "\" at level " + level
                    + " — a covering's level lies between 1 and " + CoverageUtility.LEVEL_MAX + ".");

        return CoverageUtility.pack(coveringHandle.getCoveringID(), level);
    }

    // A face's color with its coverage laid over it by the share of its tile it shows at its level, tinted as far
    // as each of its texels takes a tint — the same approximation the surface shader draws distant faces with. An
    // upright face only shows a covering that has side tiles
    public int resolveCoveredColor(int faceColor, short coverage, int tint, boolean side) {

        CoveringHandle coveringHandle = getCoveringHandleFromCoverage(coverage);

        if (coveringHandle == null || (side && !coveringHandle.hasSide()))
            return faceColor;

        TextureRevealStruct reveal = side ? coveringHandle.getSideReveal() : coveringHandle.getTopReveal();
        int coveringColor = resolveRevealColor(reveal, tint, coveringHandle.getTintStrength());

        return PackedColorUtility.mix(
                faceColor, coveringColor, reveal.getLevelShare(CoverageUtility.getLevel(coverage)));
    }

    // The average a tile shows, its tintable part carried toward the tint as far as the covering takes one
    private int resolveRevealColor(TextureRevealStruct reveal, int tint, float tintStrength) {

        int revealColor = reveal.getRevealColor();
        int tintableColor = reveal.getTintableColor();

        return PackedColorUtility.pack(
                resolveRevealChannel(
                        PackedColorUtility.red(revealColor), PackedColorUtility.red(tintableColor),
                        PackedColorUtility.red(tint), tintStrength),
                resolveRevealChannel(
                        PackedColorUtility.green(revealColor), PackedColorUtility.green(tintableColor),
                        PackedColorUtility.green(tint), tintStrength),
                resolveRevealChannel(
                        PackedColorUtility.blue(revealColor), PackedColorUtility.blue(tintableColor),
                        PackedColorUtility.blue(tint), tintStrength));
    }

    private float resolveRevealChannel(int reveal, int tintable, int tint, float tintStrength) {
        return reveal + tintStrength * tintable * (tint / EngineSetting.COLOR_CHANNEL_BYTE_MAX - 1f);
    }

    // The covering a packed coverage carries, null when it is bare
    public CoveringHandle getCoveringHandleFromCoverage(short coverage) {

        if (!CoverageUtility.isCovered(coverage))
            return null;

        return getCoveringHandleFromCoveringID(CoverageUtility.getCoveringID(coverage));
    }

    // Accessible \\

    public boolean hasCovering(String coveringName) {
        return findCoveringHandle(coveringName) != null;
    }

    // A loaded covering by name, null when none carries it — never loads, so any thread may ask
    public CoveringHandle findCoveringHandle(String coveringName) {
        return RegistryUtility.getHandle(coveringName2CoveringID, coveringID2CoveringHandle, coveringName);
    }

    public CoveringHandle getCoveringHandleFromCoveringName(String coveringName) {

        CoveringHandle handle = findCoveringHandle(coveringName);

        if (handle == null) {
            request(coveringName);
            handle = findCoveringHandle(coveringName);
        }

        if (handle == null)
            throwException("Covering \"" + coveringName
                    + "\" was not registered after its on-demand load completed — "
                    + "check for a resource-name/path mismatch in the covering directory.");

        return handle;
    }

    public CoveringHandle getCoveringHandleFromCoveringID(int coveringID) {

        CoveringHandle handle = RegistryUtility.getHandle(coveringID2CoveringHandle, coveringID);

        if (handle == null)
            throwException("No handle registered for covering ID: " + coveringID);

        return handle;
    }
}
