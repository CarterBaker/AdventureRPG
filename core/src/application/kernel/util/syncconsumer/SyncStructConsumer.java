package application.kernel.util.syncconsumer;

import engine.root.SyncContainerPackage;

@FunctionalInterface
public interface SyncStructConsumer<T extends SyncContainerPackage> {
    void accept(T instance);
}