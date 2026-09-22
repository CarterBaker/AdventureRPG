package application.bootstrap.worldpipeline.structure;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;

import engine.root.StructPackage;

public class StructureCacheStruct<V> extends StructPackage {

    /*
     * Bounded, thread-safe memo of a pure function of a long key. Structure
     * resolution runs on concurrent generation workers and is recursive —
     * resolving one placement's validity resolves the placements that could
     * outrank it, resolving a region resolves road plans — so this never
     * computes inside ConcurrentHashMap.computeIfAbsent, which forbids
     * re-entering the same map. The map only ever creates an empty entry;
     * the value is computed under that entry's own lock, so two workers
     * asking for the same key compute it once and a worker resolving a
     * different key never waits. Every cached value can be recomputed
     * identically, so when the map outgrows its limit it is simply cleared —
     * holders of an evicted entry keep a valid value. Null is a legitimate
     * cached result (a rejected placement).
     */

    private final ConcurrentHashMap<Long, Entry<V>> entries = new ConcurrentHashMap<>();
    private final int limit;

    public StructureCacheStruct(int limit) {
        this.limit = limit;
    }

    public V get(long key, LongFunction<V> compute) {

        Entry<V> entry = entries.get(key);

        if (entry == null) {

            if (entries.size() >= limit)
                entries.clear();

            entry = entries.computeIfAbsent(key, k -> new Entry<>());
        }

        return entry.resolve(key, compute);
    }

    public void clear() {
        entries.clear();
    }

    private static final class Entry<V> {

        private V value;
        private boolean resolved;

        synchronized V resolve(long key, LongFunction<V> compute) {

            if (!resolved) {
                value = compute.apply(key);
                resolved = true;
            }

            return value;
        }
    }
}
