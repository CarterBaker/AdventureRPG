package application.core.kernel.syncconsumer;

import application.core.engine.SyncContainerPackage;

@FunctionalInterface
public interface SyncStructConsumer<T extends SyncContainerPackage> {
    void accept(T instance);
}