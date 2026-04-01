package com.internal.core.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.internal.core.settings.EngineSetting;

public abstract class LoaderPackage extends ManagerPackage {

    /*
     * A self-contained, self-releasing file loader system.
     *
     * On CREATE, the loader scans a directory and populates an internal
     * file queue. Each UPDATE frame it drains up to Settings.LOADER_BATCH_SIZE
     * entries from that queue, calling `load(File)` for each one. When the
     * queue is exhausted, the loader marks itself for release, disposes all
     * child builders it owns, and removes itself from its parent manager
     * during the next RELEASE phase — no external cleanup required.
     *
     * On-demand loading:
     * A parent manager can call requestFromLoader(File) at any point while
     * the loader is alive. request(File) pulls that file from the pending
     * queue immediately (if still present) and calls load() right now,
     * in addition to whatever batch work happens that frame. If the file
     * has already been processed, the call is a no-op.
     *
     * Lifecycle:
     * CREATE → internalScan() populates queue, then create() fires normally
     * UPDATE → batched load(File) calls, pendingRelease flagged when empty
     * RELEASE → release() fires, then builders and loader self-release
     *
     * Override points (in intended call order):
     * directory() — return the root File to scan
     * scan() — filter or extend fileQueue after auto-population
     * create() — post-scan create phase; call create(Builder.class) here
     * load(File) — called per-file each frame, up to batch limit
     * onComplete() — called once when the queue first empties
     * release() — called before builders and loader are torn down
     */

    /*
     * A self-contained, self-releasing file loader system.
     *
     * On CREATE, the loader scans a directory and populates an internal
     * file queue. Each UPDATE frame it drains up to Settings.LOADER_BATCH_SIZE
     * entries from that queue, calling `load(File)` for each one. When the
     * queue is exhausted, the loader marks itself for release, disposes all
     * child builders it owns, and removes itself from its parent manager
     * during the next RELEASE phase — no external cleanup required.
     *
     * Builders are created and retrieved through `createBuilder` and
     * `getBuilder`. Any builder created via these methods is tracked
     * automatically and released alongside the loader.
     *
     * Lifecycle:
     * CREATE → internalScan() populates queue, then create() fires normally
     * UPDATE → batched load(File) calls, pendingRelease flagged when empty
     * RELEASE → release() fires, then builders and loader self-release
     *
     * Override points (in intended call order):
     * directory() — return the root File to scan
     * scan() — filter or extend fileQueue after auto-population
     * create() — post-scan create phase; call createBuilder() here
     * load(File) — called per-file each frame, up to batch limit
     * onComplete()— called once when the queue first empties
     * release() — called before builders and loader are torn down
     */

    // Queue
    protected final Queue<File> fileQueue;

    // State
    private boolean pendingRelease;

    // Internal \\

    protected LoaderPackage() {
        super();
        this.fileQueue = new LinkedList<>();
        this.pendingRelease = false;
    }

    // Create \\

    /*
     * Intercepts the create phase to populate the queue before
     * the user's create() fires, then propagates to child builders.
     */
    @Override
    void internalCreate() {

        if (!this.verifyContext(SystemContext.CREATE))
            return;

        this.internalScan(); // populate queue first
        this.create(); // user: call createBuilder() here
        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    private void internalScan() {

        File dir = this.directory();

        if (dir != null && dir.isDirectory()) {

            File[] entries = dir.listFiles();

            if (entries != null) {
                for (File entry : entries)
                    this.fileQueue.offer(entry);
            }
        }

        this.scan();
    }

    /*
     * Return the directory whose contents should be queued for loading.
     * Return null to skip auto-population (manage the queue manually in scan()).
     */
    protected File directory() {
        return null;
    }

    /*
     * Called after the directory has been scanned and entries added to
     * fileQueue. Override to filter entries, add additional files, or
     * replace the queue entirely with custom logic.
     */
    protected void scan() {
    }

    // Update \\

    /*
     * Drives the batch-load loop each frame, then fires the standard
     * update() for any additional per-frame logic.
     */
    @Override
    void internalUpdate() {

        if (!this.verifyContext(SystemContext.UPDATE))
            return;

        if (!this.pendingRelease) {

            int limit = EngineSetting.LOADER_BATCH_SIZE;
            int processed = 0;

            while (!this.fileQueue.isEmpty() && processed < limit) {
                this.load(this.fileQueue.poll());
                processed++;
            }

            if (this.fileQueue.isEmpty()) {
                this.onComplete();
                this.pendingRelease = true;
            }
        }

        this.update();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalUpdate();
    }

    /*
     * Called once per file, up to Settings.LOADER_BATCH_SIZE times per frame.
     * Override to define per-file processing logic — retrieve a builder here
     * via getBuilder(MyBuilder.class) and call builder.build(file).
     */
    protected void load(File file) {
    }

    /*
     * Called once when the file queue is exhausted, before the release flag
     * is set. Override for any post-load finalization that must happen in
     * the same frame the queue empties.
     */
    protected void onComplete() {
    }

    // On-Demand Loading \\

    /*
     * Immediately loads a specific file outside the normal batch cadence.
     * If the file is still pending in the queue it is removed first so it
     * is not processed a second time during the normal drain. If it has
     * already been processed this frame or in a prior frame, the call is
     * a no-op — load() implementations are expected to be idempotent
     * (check before registering, same as the batch path).
     *
     * Called by the parent manager via requestFromLoader(File).
     * Concrete loaders expose a typed request(String resourceName) that
     * resolves the name to a File and delegates here.
     */
    protected final void request(File file) {
        this.fileQueue.remove(file);
        this.load(file);
    }

    // Release \\

    /*
     * Only tears down when the queue has been fully drained.
     * Fires the user's release(), then disposes child builders
     * and self-releases from the parent manager.
     */
    @Override
    void internalRelease() {

        if (!this.verifyContext(SystemContext.RELEASE))
            return;

        if (this.pendingRelease) {

            this.release(); // user cleanup before teardown

            // Release all child builders tracked under this loader.
            // Iterating a snapshot since release() modifies garbageCollection.
            for (SystemPackage system : new ArrayList<>(this.systemCollection)) {
                if (system instanceof BuilderPackage)
                    this.release(system.getClass());
            }

            // Self-release from parent manager
            this.local.release(this.getClass());
        }

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRelease();

        this.clearGarbage();
    }

}