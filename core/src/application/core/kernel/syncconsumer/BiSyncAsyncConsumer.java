package application.core.kernel.syncconsumer;

import application.core.engine.AsyncContainerPackage;
import application.core.engine.SyncContainerPackage;

@FunctionalInterface
public interface BiSyncAsyncConsumer<T extends AsyncContainerPackage, S extends SyncContainerPackage> {
    void accept(T asyncInstance, S syncInstance);
}