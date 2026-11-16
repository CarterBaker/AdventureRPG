package editor.bootstrap.sceneassetspipeline.sceneassetsmanager;

import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetKind;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetRowStruct;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetSectionStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class SceneAssetsManager extends ManagerPackage {

    /*
     * Owns what every Scene Assets tab shows: the assets the Dev window
     * shows as it runs, one section per SceneAssetKind. While a tab is open
     * it keeps the scene watched, and the first Dev window to see that
     * claims it, its DevSceneAssetsSystem casting the crosshair's ray and a
     * grid of rays across its view and handing each hit here; a claim no
     * longer fed lapses, so another Dev window may take the scene over. The
     * block pointed at is described the moment it changes — its block, the
     * biome it lies in, the face hit and its texture, every face's texture,
     * its material and shader, and where it stands — and the blocks and
     * biomes each scan sees are described again only when they differ from
     * the last scan, every texture, material and shader they are drawn with
     * gathered from them. A row naming content opens it in the Info Panel.
     * Each change moves the revision the tab redraws on.
     */

    // Internal
    private BiomeManager biomeManager;
    private InfoManager infoManager;
    private SceneAssetsRowBranch sceneAssetsRowBranch;

    // Watch — how long the scene stays watched without a tab asking again, the Dev window that claimed it, and how
    // long since it last fed the scene
    private float watchSeconds;
    private int ownerWindowID;
    private float ownerIdleSeconds;

    // Pointed — the cell and face last pointed at, so a ray landing on the same face describes nothing again
    private boolean pointedHit;
    private short pointedBlockID;
    private long pointedChunkCoordinate;
    private int pointedSubChunkY;
    private int pointedX;
    private int pointedY;
    private int pointedZ;
    private Direction3Vector pointedFace;

    // Scan — the world scanned, the blocks, biomes and chunks it has seen, and the blocks and biomes last shown
    private WorldHandle scanWorld;
    private ShortOpenHashSet seenBlockIDs;
    private ShortOpenHashSet seenBiomeIDs;
    private LongOpenHashSet seenChunks;
    private ShortOpenHashSet shownBlockIDs;
    private ShortOpenHashSet shownBiomeIDs;

    // Sections
    private SceneAssetSectionStruct[] sections;

    // Status
    private int revision;
    private String statusText;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.sceneAssetsRowBranch = create(SceneAssetsRowBranch.class);

        // Watch
        this.ownerWindowID = EngineSetting.INDEX_NOT_FOUND;

        // Scan
        this.seenBlockIDs = new ShortOpenHashSet();
        this.seenBiomeIDs = new ShortOpenHashSet();
        this.seenChunks = new LongOpenHashSet();
        this.shownBlockIDs = new ShortOpenHashSet();
        this.shownBiomeIDs = new ShortOpenHashSet();

        // Sections
        this.sections = new SceneAssetSectionStruct[SceneAssetKind.VALUES.length];

        for (int i = 0; i < sections.length; i++)
            sections[i] = new SceneAssetSectionStruct(SceneAssetKind.VALUES[i]);

        // Status
        this.statusText = EditorSetting.SCENE_ASSETS_STATUS_NO_DEV;
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        this.infoManager = get(InfoManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();

        this.watchSeconds = Math.max(0f, watchSeconds - deltaTime);

        if (ownerWindowID == EngineSetting.INDEX_NOT_FOUND)
            return;

        this.ownerIdleSeconds += deltaTime;

        if (ownerIdleSeconds >= EditorSetting.SCENE_ASSETS_OWNER_SECONDS)
            releaseOwner();
    }

    // A lapsed claim leaves the scene to the next Dev window that watches it
    private void releaseOwner() {
        this.ownerWindowID = EngineSetting.INDEX_NOT_FOUND;
        setStatus(EditorSetting.SCENE_ASSETS_STATUS_NO_DEV);
    }

    // Watch \\

    // A Scene Assets tab is open, so the scene stays watched a little longer
    public void watch() {
        this.watchSeconds = EditorSetting.SCENE_ASSETS_WATCH_SECONDS;
    }

    // True when the Dev window should feed the scene this frame: it is watched, and the window holds the claim or
    // takes it while none does
    public boolean claim(int windowID) {

        if (watchSeconds <= 0f
                || ownerWindowID != EngineSetting.INDEX_NOT_FOUND && ownerWindowID != windowID)
            return false;

        this.ownerWindowID = windowID;
        this.ownerIdleSeconds = 0f;
        return true;
    }

    // Pointed \\

    // The block under the crosshair, described again only when the ray lands on another face of another cell
    public void showPointed(WorldHandle worldHandle, BlockCastStruct cast) {

        if (!isPointedChange(cast))
            return;

        rememberPointed(cast);

        SceneAssetSectionStruct section = sections[SceneAssetKind.POINTED.ordinal()];

        if (cast.isHit())
            sceneAssetsRowBranch.describePointed(section, cast,
                    biomeManager.getBiome(worldHandle, cast.getChunkCoordinate()));
        else
            sceneAssetsRowBranch.describeNothing(section);

        revision++;
    }

    private boolean isPointedChange(BlockCastStruct cast) {

        if (cast.isHit() != pointedHit)
            return true;

        return cast.isHit() && (cast.getBlock().getBlockID() != pointedBlockID
                || cast.getChunkCoordinate() != pointedChunkCoordinate
                || cast.getSubChunkY() != pointedSubChunkY
                || cast.getBlockX() != pointedX
                || cast.getBlockY() != pointedY
                || cast.getBlockZ() != pointedZ
                || cast.getHitFace() != pointedFace);
    }

    private void rememberPointed(BlockCastStruct cast) {

        this.pointedHit = cast.isHit();

        if (!pointedHit)
            return;

        this.pointedBlockID = cast.getBlock().getBlockID();
        this.pointedChunkCoordinate = cast.getChunkCoordinate();
        this.pointedSubChunkY = cast.getSubChunkY();
        this.pointedX = cast.getBlockX();
        this.pointedY = cast.getBlockY();
        this.pointedZ = cast.getBlockZ();
        this.pointedFace = cast.getHitFace();
    }

    // Scan \\

    // A scan of the view begins in the world given, nothing seen yet
    public void beginScan(WorldHandle worldHandle) {
        this.scanWorld = worldHandle;
        seenBlockIDs.clear();
        seenBiomeIDs.clear();
        seenChunks.clear();
    }

    // A ray of the scan landed: its block is seen, and the biome of its chunk the first time the chunk is
    public void addSeen(BlockCastStruct cast) {

        if (!cast.isHit())
            return;

        seenBlockIDs.add(cast.getBlock().getBlockID());

        if (seenChunks.add(cast.getChunkCoordinate()))
            seenBiomeIDs.add(biomeManager.getBiome(scanWorld, cast.getChunkCoordinate()).getBiomeID());
    }

    // The scan done: what it saw described again when it differs from what is shown
    public void endScan() {

        setStatus(EditorSetting.SCENE_ASSETS_STATUS_WATCHING + scanWorld.getWorldName());

        if (seenBlockIDs.equals(shownBlockIDs) && seenBiomeIDs.equals(shownBiomeIDs))
            return;

        shownBlockIDs.clear();
        shownBlockIDs.addAll(seenBlockIDs);
        shownBiomeIDs.clear();
        shownBiomeIDs.addAll(seenBiomeIDs);
        sceneAssetsRowBranch.describeSeen(sections, shownBlockIDs, shownBiomeIDs);
        revision++;
    }

    // Sections \\

    public void toggleSection(int sectionIndex) {

        if (sectionIndex < 0 || sectionIndex >= sections.length)
            return;

        sections[sectionIndex].toggle();
        revision++;
    }

    // The content a row names opened in the Info Panel; a row naming none does nothing
    public void selectRow(int sectionIndex, int rowIndex) {

        if (sectionIndex < 0 || sectionIndex >= sections.length
                || rowIndex < 0 || rowIndex >= sections[sectionIndex].size())
            return;

        SceneAssetRowStruct row = sections[sectionIndex].get(rowIndex);

        if (row.isContent())
            infoManager.selectEntry(row.getSchemaName(), row.getDefinitionName(), row.getEntryName());
    }

    // Utility \\

    private void setStatus(String statusText) {

        if (statusText.equals(this.statusText))
            return;

        this.statusText = statusText;
        revision++;
    }

    // Accessible \\

    public SceneAssetSectionStruct[] getSections() {
        return sections;
    }

    public boolean isOwned() {
        return ownerWindowID != EngineSetting.INDEX_NOT_FOUND;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {
        return statusText;
    }
}
