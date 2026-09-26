package engine.root;

import java.io.File;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public abstract class LoaderPackage extends ManagerPackage {

    /*
     * Self-releasing file loader. On CREATE it scans and queues its files
     * through queueFile(), each UPDATE it loads up to LOADER_BATCH_SIZE of
     * them, and once the queue is empty it fires onComplete() and releases
     * itself and its builders in the next RELEASE phase. request() loads one
     * queued file immediately, for on-demand lookups while the loader lives.
     */

    // Queue
    private final ObjectLinkedOpenHashSet<File> fileQueue;

    // State
    private boolean pendingRelease;

    // Internal \\

    protected LoaderPackage() {

        super();

        // Queue
        this.fileQueue = new ObjectLinkedOpenHashSet<>();

        // State
        this.pendingRelease = false;
    }

    // Create \\

    @Override
    void internalCreate() {

        if (!this.verifyContext(SystemContext.CREATE))
            return;

        this.internalScan();
        this.create();
        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    private void internalScan() {

        File directory = this.directory();

        if (directory != null && directory.isDirectory()) {

            File[] entries = directory.listFiles();

            if (entries != null)
                for (File entry : entries)
                    this.queueFile(entry);
        }

        this.scan();
    }

    protected File directory() {
        return null;
    }

    protected void scan() {
    }

    // Queue \\

    protected final void queueFile(File file) {
        this.fileQueue.add(file);
    }

    public final void requestAll() {

        File[] pendingFiles = this.fileQueue.toArray(new File[0]);

        for (int i = 0; i < pendingFiles.length; i++)
            this.request(pendingFiles[i]);
    }

    // Update \\

    @Override
    void internalUpdate() {

        if (!this.verifyContext(SystemContext.UPDATE))
            return;

        if (!this.pendingRelease) {

            int processed = 0;

            while (!this.fileQueue.isEmpty() && processed < EngineSetting.LOADER_BATCH_SIZE) {
                this.load(this.fileQueue.removeFirst());
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

    protected void load(File file) {
    }

    protected void onComplete() {
    }

    // On-Demand \\

    protected final void request(File file) {
        this.fileQueue.remove(file);
        this.load(file);
    }

    // Release \\

    @Override
    void internalRelease() {

        if (!this.verifyContext(SystemContext.RELEASE))
            return;

        if (this.pendingRelease) {

            this.release();

            for (int i = 0; i < this.systemArray.length; i++)
                if (this.systemArray[i] instanceof BuilderPackage)
                    this.release(this.systemArray[i].getClass());

            this.local.release(this.getClass());
        }

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRelease();

        this.clearGarbage();
    }
}
