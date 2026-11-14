package application.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureLoadStruct;
import application.bootstrap.shaderpipeline.texture.TextureMapType;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.kernel.profilerpipeline.profilermanager.ProfilerManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class TextureStreamBranch extends BranchPackage {

    /*
     * Decides which tiers of which tiles are resident, in the pattern chunk
     * streaming uses. Texture users (a loaded chunk, a spawned entity, an
     * open inventory) register the tiles their meshes use with the bounds
     * they occupy, and release them when they go; every frame each user's
     * distance from the viewer, and from where the viewer is heading, places
     * it in the band of tiers it needs, and every tile wants the highest band
     * any of its users holds. Missing tiers are read on the thread pipeline,
     * lowest tier first, and uploaded under a per-frame byte budget. A tile
     * whose references fall to zero stays cached until a slot is needed;
     * then the least recently released goes first, and only after those a
     * tile in use farther away gives up its top tier, stepping down a tier
     * before it falls back. Nothing blocks the main thread: a tile with no
     * tier resident reads the fallback pool, and base tier slots no user
     * needs are warmed with every other tile at the lowest priority. Every
     * table is a preallocated int array, so streaming makes no garbage once
     * warm.
     */

    // Internal
    private TexturePoolBranch texturePoolBranch;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private ProfilerManager profilerManager;
    private ThreadHandle threadHandle;

    // Tiles
    private int tileCount;
    private int[] tileTierCounts;
    private File[][] tileTierFiles;
    private int[][] tileReferences;
    private int[] tileTops;
    private int[] tileStamps;
    private boolean[][] tilePending;
    private int[] tierPendingCounts;
    private int[] tierRequestRoom;

    // Users
    private IntArrayList[] userTiles;
    private float[] userBounds;
    private boolean[] userScreens;
    private int[] userBands;
    private boolean[] userActive;
    private int[] freeUsers;
    private int freeUserCount;
    private int userHigh;

    // Loads
    private TextureLoadStruct[] loads;
    private boolean[] loadBusy;
    private int generation;

    // Viewer
    private boolean hasViewer;
    private float viewerX;
    private float viewerZ;
    private float aheadX;
    private float aheadZ;
    private float[] bandDistances;

    // Frame
    private int frame;
    private int evictions;
    private int uploads;
    private long uploadNanos;

    // Base \\

    @Override
    protected void create() {

        int tileCapacity = EngineSetting.TEXTURE_TILE_CAPACITY + EngineSetting.TEXTURE_SENTINEL_LAYERS;
        int tierCount = EngineSetting.TEXTURE_TIER_COUNT;
        int userCapacity = EngineSetting.TEXTURE_STREAM_USER_CAPACITY;

        // Tiles
        this.tileTierCounts = new int[tileCapacity];
        this.tileTierFiles = new File[tierCount][tileCapacity];
        this.tileReferences = new int[tierCount][tileCapacity];
        this.tileTops = new int[tileCapacity];
        this.tileStamps = new int[tileCapacity];
        this.tilePending = new boolean[tierCount][tileCapacity];
        this.tierPendingCounts = new int[tierCount];
        this.tierRequestRoom = new int[tierCount];
        Arrays.fill(tileTops, EngineSetting.TEXTURE_TIER_NONE);

        // Users
        this.userTiles = new IntArrayList[userCapacity];
        this.userBounds = new float[userCapacity * EngineSetting.TEXTURE_USER_BOUNDS_FLOATS];
        this.userScreens = new boolean[userCapacity];
        this.userBands = new int[userCapacity];
        this.userActive = new boolean[userCapacity];
        this.freeUsers = new int[userCapacity];

        for (int u = 0; u < userCapacity; u++)
            freeUsers[u] = userCapacity - 1 - u;

        this.freeUserCount = userCapacity;

        // Loads
        this.loads = new TextureLoadStruct[EngineSetting.TEXTURE_STREAM_MAX_IN_FLIGHT];
        this.loadBusy = new boolean[loads.length];

        for (int i = 0; i < loads.length; i++)
            loads[i] = new TextureLoadStruct();

        // Viewer
        this.bandDistances = new float[tierCount];
    }

    @Override
    protected void get() {
        this.texturePoolBranch = get(TexturePoolBranch.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.profilerManager = get(ProfilerManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.TEXTURE_STREAM_THREAD_NAME);
    }

    // Tiles \\

    // A tile the bake filled up to the given tier count, streamed from the given tier files
    void addTile(int tileID, int tierCount, File[] tierFiles) {

        if (tileID > EngineSetting.TEXTURE_TILE_CAPACITY)
            throwException("Texture tile ID " + tileID + " is past the streaming capacity of "
                    + EngineSetting.TEXTURE_TILE_CAPACITY + " tiles.");

        tileTierCounts[tileID] = tierCount;

        for (int tier = 0; tier < tierCount; tier++)
            tileTierFiles[tier][tileID] = tierFiles[tier];

        tileCount = Math.max(tileCount, tileID + 1);
    }

    boolean isStreamed(int tileID) {
        return tileID > 0 && tileID < tileCount && tileTierCounts[tileID] > 0;
    }

    // Users \\

    int registerUser(IntArrayList tileIDs, boolean screen, float minX, float minZ, float maxX, float maxZ) {

        if (freeUserCount == 0)
            throwException("More than " + EngineSetting.TEXTURE_STREAM_USER_CAPACITY
                    + " texture users are registered at once.");

        int user = freeUsers[--freeUserCount];

        if (userTiles[user] == null)
            userTiles[user] = new IntArrayList();

        userActive[user] = true;
        userScreens[user] = screen;
        userHigh = Math.max(userHigh, user + 1);
        writeBounds(user, minX, minZ, maxX, maxZ);
        userBands[user] = resolveBand(user);
        copyTiles(user, tileIDs);
        shiftReferences(user, userBands[user], 1);

        return user;
    }

    void setUserTiles(int user, IntArrayList tileIDs) {

        verifyUser(user);
        shiftReferences(user, userBands[user], -1);
        copyTiles(user, tileIDs);
        shiftReferences(user, userBands[user], 1);
    }

    void setUserBounds(int user, float minX, float minZ, float maxX, float maxZ) {
        verifyUser(user);
        writeBounds(user, minX, minZ, maxX, maxZ);
    }

    void removeUser(int user) {

        verifyUser(user);
        shiftReferences(user, userBands[user], -1);
        userTiles[user].clear();
        userActive[user] = false;
        freeUsers[freeUserCount++] = user;
    }

    private void verifyUser(int user) {
        if (user < 0 || user >= userActive.length || !userActive[user])
            throwException("Texture user " + user + " is not registered.");
    }

    private void copyTiles(int user, IntArrayList tileIDs) {

        IntArrayList tiles = userTiles[user];
        tiles.clear();
        tiles.ensureCapacity(tileIDs.size());

        for (int i = 0; i < tileIDs.size(); i++) {

            int tileID = tileIDs.getInt(i);

            if (!isStreamed(tileID))
                throwException("Tile ID " + tileID + " names no streamed texture.");

            tiles.add(tileID);
        }
    }

    private void writeBounds(int user, float minX, float minZ, float maxX, float maxZ) {

        int base = user * EngineSetting.TEXTURE_USER_BOUNDS_FLOATS;
        userBounds[base] = minX;
        userBounds[base + 1] = minZ;
        userBounds[base + 2] = maxX;
        userBounds[base + 3] = maxZ;
    }

    // Adds or removes the user's references to its tiles in a band, stamping a tile whose last reference goes
    private void shiftReferences(int user, int band, int delta) {

        IntArrayList tiles = userTiles[user];

        for (int i = 0; i < tiles.size(); i++) {

            int tileID = tiles.getInt(i);
            tileReferences[band][tileID] += delta;

            if (delta < 0 && resolveDesiredTier(tileID) == EngineSetting.TEXTURE_TIER_NONE)
                tileStamps[tileID] = frame;
        }
    }

    // Update \\

    @Override
    protected void update() {

        frame++;
        evictions = 0;
        uploads = 0;
        uploadNanos = 0L;

        resolveViewer();
        updateBands();
        requestTiers();
        uploadTiers(EngineSetting.TEXTURE_STREAM_UPLOAD_BYTES_PER_FRAME);
        recordCounters();
    }

    // The main window's player, where it is heading, and how near a block must be to need each tier
    private void resolveViewer() {

        WindowInstance window = windowManager.getMainWindow();
        this.hasViewer = window != null && playerManager.hasPlayerForWindow(window.getWindowID());

        if (!hasViewer)
            return;

        int windowID = window.getWindowID();
        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldPositionStruct position = player.getWorldPositionStruct();
        long chunk = position.getChunkCoordinate();
        Vector2 velocity = player.getEntityStateHandle().getHorizontalVelocity();
        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        this.viewerX = Coordinate2Long.unpackX(chunk) * EngineSetting.CHUNK_SIZE + position.getPosition().x;
        this.viewerZ = Coordinate2Long.unpackY(chunk) * EngineSetting.CHUNK_SIZE + position.getPosition().z;
        this.aheadX = viewerX + velocity.x * EngineSetting.TEXTURE_STREAM_PREFETCH_SECONDS;
        this.aheadZ = viewerZ + velocity.y * EngineSetting.TEXTURE_STREAM_PREFETCH_SECONDS;

        float focalPixels = window.getHeight() * 0.5f
                / (float) Math.tan(Math.toRadians(camera.getFOV()) * 0.5);

        for (int band = 1; band < EngineSetting.TEXTURE_TIER_COUNT; band++)
            bandDistances[band] = focalPixels / EngineSetting.TEXTURE_TIER_SIZES[band - 1]
                    * EngineSetting.TEXTURE_STREAM_TIER_DISTANCE_BIAS;
    }

    // Moves every user whose band changed, its references going with it
    private void updateBands() {

        for (int user = 0; user < userHigh; user++) {

            if (!userActive[user])
                continue;

            int band = resolveBand(user);

            if (band == userBands[user])
                continue;

            shiftReferences(user, band, 1);
            shiftReferences(user, userBands[user], -1);
            userBands[user] = band;
        }
    }

    // A screen user wants every tier; a world user the tiers its nearer distance, now or ahead, calls for
    private int resolveBand(int user) {

        int top = EngineSetting.TEXTURE_TIER_COUNT - 1;

        if (userScreens[user])
            return top;

        if (!hasViewer)
            return 0;

        float distance = Math.min(measureDistance(user, viewerX, viewerZ), measureDistance(user, aheadX, aheadZ));
        int band = 0;

        while (band < top && distance <= bandDistances[band + 1])
            band++;

        return band;
    }

    private float measureDistance(int user, float x, float z) {

        int base = user * EngineSetting.TEXTURE_USER_BOUNDS_FLOATS;
        float dx = Math.max(0f, Math.max(userBounds[base] - x, x - userBounds[base + 2]));
        float dz = Math.max(0f, Math.max(userBounds[base + 1] - z, z - userBounds[base + 3]));

        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    // The highest tier any reference asks for, held within the tile's own tiers and the quality
    private int resolveDesiredTier(int tileID) {

        for (int band = EngineSetting.TEXTURE_TIER_COUNT - 1; band >= 0; band--)
            if (tileReferences[band][tileID] > 0)
                return Math.min(band, Math.min(tileTierCounts[tileID] - 1, texturePoolBranch.getQuality()));

        return EngineSetting.TEXTURE_TIER_NONE;
    }

    // Loads \\

    // Starts a read for every tile short of the tier it wants, every base tier before any higher one, and only as
    // many per tier as it has slots free or a slot a less needed tile can give up
    private void requestTiers() {

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++) {

            int free = texturePoolBranch.getFreeSlotCount(tier);
            int room = free > 0 ? free : (findVictim(tier) != 0 ? 1 : 0);
            tierRequestRoom[tier] = room - tierPendingCounts[tier];
        }

        for (int pass = 0; pass < EngineSetting.TEXTURE_TIER_COUNT; pass++) {

            for (int tileID = 1; tileID < tileCount && tierRequestRoom[pass] > 0; tileID++) {

                int next = tileTops[tileID] + 1;

                if (next != pass || resolveDesiredTier(tileID) < next || tilePending[next][tileID])
                    continue;

                if (!startLoad(tileID, next))
                    return;

                tierRequestRoom[pass]--;
            }
        }

        warmBaseTier();
    }

    // Base tier slots no reference needs are filled with tiles nothing references yet, so a tile any draw reaches
    // without a user of its own shows its base tier; the first tile that needs such a slot takes it back
    private void warmBaseTier() {

        int room = texturePoolBranch.getFreeSlotCount(0) - tierPendingCounts[0];

        for (int tileID = 1; tileID < tileCount && room > 0; tileID++) {

            if (tileTops[tileID] != EngineSetting.TEXTURE_TIER_NONE || tilePending[0][tileID])
                continue;

            if (!startLoad(tileID, 0))
                return;

            room--;
        }
    }

    private boolean startLoad(int tileID, int tier) {

        if (!threadHandle.hasCapacity())
            return false;

        for (int i = 0; i < loads.length; i++) {

            if (loadBusy[i])
                continue;

            TextureLoadStruct load = loads[i];
            load.begin(tileID, tier, generation, tileTierFiles[tier][tileID]);
            load.setFuture(executeAsync(threadHandle, load));
            loadBusy[i] = true;
            tilePending[tier][tileID] = true;
            tierPendingCounts[tier]++;

            return true;
        }

        return false;
    }

    // Uploads finished reads still wanted until the byte budget is spent; every read is waited on when unbounded
    private void uploadTiers(int byteBudget) {

        long start = System.nanoTime();
        int budget = byteBudget;

        for (int i = 0; i < loads.length && budget > 0; i++) {

            if (!loadBusy[i] || !loads[i].isDone())
                continue;

            budget -= finishLoad(i);
        }

        texturePoolBranch.flush();
        texturePoolBranch.pushLookup();
        uploadNanos += System.nanoTime() - start;
    }

    // Places one finished read in its tier, returning the bytes staged
    private int finishLoad(int index) {

        TextureLoadStruct load = loads[index];
        int tileID = load.getTileID();
        int tier = load.getTier();
        int bytes = 0;

        if (load.getFailure() != null)
            throwException("Baked texture tier could not be read: " + load.getFailure());

        tilePending[tier][tileID] = false;
        tierPendingCounts[tier]--;

        boolean current = load.getGeneration() == generation && tileTops[tileID] == tier - 1;
        boolean needed = resolveDesiredTier(tileID) >= tier;

        if (current && (needed || tier == 0)) {

            int slot = needed ? acquireSlot(tier, tileID) : texturePoolBranch.placeTile(tier, tileID);

            if (slot != 0) {

                texturePoolBranch.stageTier(tier, slot, load.getMaps());
                tileTops[tileID] = tier;
                bytes = TextureCompressionUtility.getChainBytes(EngineSetting.TEXTURE_TIER_SIZES[tier])
                        * load.getMaps().length;
                uploads++;
            }
        }

        load.finish();
        loadBusy[index] = false;

        return bytes;
    }

    // Slots \\

    // A slot of the tier for the tile: a free one, else one a less needed tile gives up
    private int acquireSlot(int tier, int tileID) {

        int slot = texturePoolBranch.placeTile(tier, tileID);

        if (slot != 0)
            return slot;

        int victim = findVictim(tier);

        if (victim == 0)
            return 0;

        texturePoolBranch.removeTile(tier, victim);
        tileTops[victim] = tier - 1;
        evictions++;

        return texturePoolBranch.placeTile(tier, tileID);
    }

    // The tile holding the tier at its top that needs it least: unreferenced first, least recently released
    // first among them, then the one wanting the lowest tier, the farthest away
    private int findVictim(int tier) {

        int victim = 0;
        int victimDesire = Integer.MAX_VALUE;
        int victimStamp = Integer.MAX_VALUE;

        for (int slot = 1; slot <= texturePoolBranch.getSlotCount(tier); slot++) {

            int tileID = texturePoolBranch.getSlotTile(tier, slot);

            if (tileID == 0 || tileTops[tileID] != tier)
                continue;

            int desire = resolveDesiredTier(tileID);

            if (desire >= tier)
                continue;

            if (desire < victimDesire || (desire == victimDesire && tileStamps[tileID] < victimStamp)) {
                victim = tileID;
                victimDesire = desire;
                victimStamp = tileStamps[tileID];
            }
        }

        return victim;
    }

    // Editing \\

    // A tile baked again replaces its fallback, extra and every tier it holds, in place
    void replaceBake(int tileID, TextureBakeStruct bake) {

        if (!isStreamed(tileID) || bake.getTierCount() != tileTierCounts[tileID])
            throwException("A new bake of tile " + tileID + " fills " + bake.getTierCount()
                    + " tiers where the tile streams " + tileTierCounts[tileID]);

        byte[][] maps = new byte[TextureMapType.TIERED_COUNT][];

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            maps[m] = bake.getFallbackMap(TextureMapType.VALUES[m]);

        texturePoolBranch.stageFallback(texturePoolBranch.getFallbackLayer(tileID), maps);

        if (bake.getExtraMap() != null) {

            int layer = texturePoolBranch.getExtraLayer(tileID);
            texturePoolBranch.stageExtra(layer != 0 ? layer : texturePoolBranch.assignExtraLayer(tileID),
                    bake.getExtraMap());
        }

        for (int tier = 0; tier <= tileTops[tileID]; tier++) {

            for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
                maps[m] = bake.getTierMap(tier, TextureMapType.VALUES[m]);

            texturePoolBranch.stageTier(tier, texturePoolBranch.getTileSlot(tier, tileID), maps);
        }

        texturePoolBranch.flush();
        texturePoolBranch.pushLookup();
    }

    // Quality \\

    // Reallocates the pools for a new quality; reads in flight are dropped and every tile streams in again
    void applyQuality(int quality) {

        texturePoolBranch.applyQuality(quality);
        generation++;

        for (int tileID = 0; tileID < tileCount; tileID++)
            tileTops[tileID] = EngineSetting.TEXTURE_TIER_NONE;
    }

    // Fills every tier every user wants before play, waiting on each read in turn
    void completeStreaming() {

        boolean pending = true;

        while (pending) {

            requestTiers();
            pending = false;

            for (int i = 0; i < loads.length; i++) {

                if (!loadBusy[i])
                    continue;

                awaitLoad(loads[i]);
                finishLoad(i);
                pending = true;
            }

            texturePoolBranch.flush();
        }

        texturePoolBranch.pushLookup();
    }

    private void awaitLoad(TextureLoadStruct load) {

        try {
            load.getFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throwException("Texture streaming was interrupted while filling the pools", e);
        } catch (ExecutionException e) {
            throwException("Baked texture tier could not be read", e);
        }
    }

    // Profiler \\

    private void recordCounters() {

        if (!profilerManager.isSampling())
            return;

        int referenced = 0;
        int fallback = 0;

        for (int tileID = 1; tileID < tileCount; tileID++) {

            if (resolveDesiredTier(tileID) == EngineSetting.TEXTURE_TIER_NONE)
                continue;

            referenced++;

            if (tileTops[tileID] == EngineSetting.TEXTURE_TIER_NONE)
                fallback++;
        }

        profilerManager.recordCount(EngineSetting.PROFILER_COUNTER_TEXTURE_TILES, referenced);
        profilerManager.recordCount(EngineSetting.PROFILER_COUNTER_TEXTURE_FALLBACK, fallback);
        profilerManager.recordCount(EngineSetting.PROFILER_COUNTER_TEXTURE_EVICTIONS, evictions);
        profilerManager.recordCount(EngineSetting.PROFILER_COUNTER_TEXTURE_UPLOADS, uploads);
        profilerManager.recordCount(EngineSetting.PROFILER_COUNTER_TEXTURE_UPLOAD_MILLIS,
                uploadNanos / (float) EngineSetting.NANOS_PER_MILLI);

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            profilerManager.recordCount(EngineSetting.PROFILER_COUNTER_TEXTURE_RESIDENT[tier],
                    texturePoolBranch.getSlotCount(tier) - texturePoolBranch.getFreeSlotCount(tier));
    }
}
