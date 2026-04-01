package program.core.kernel.syncconsumer;

import program.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumer<T extends AsyncContainerPackage> {
    void accept(T instance);
}