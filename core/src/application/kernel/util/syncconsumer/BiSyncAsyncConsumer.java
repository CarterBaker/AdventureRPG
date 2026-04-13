package application.kernel.util.syncconsumer;

import engine.root.AsyncContainerPackage;
import engine.root.SyncContainerPackage;

@FunctionalInterface
public interface BiSyncAsyncConsumer<T extends AsyncContainerPackage, S extends SyncContainerPackage> {
    void accept(T asyncInstance, S syncInstance);
}