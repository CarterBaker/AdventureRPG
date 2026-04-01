package program.core.kernel.syncconsumer;

import program.core.engine.AsyncContainerPackage;
import program.core.engine.SyncContainerPackage;

@FunctionalInterface
public interface BiSyncAsyncConsumer<T extends AsyncContainerPackage, S extends SyncContainerPackage> {
    void accept(T asyncInstance, S syncInstance);
}