package engine.util.arpg;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public final class ArpgObjectStruct extends ArpgElementStruct {

    /*
     * Keyed node of the ARPG document tree. Keys keep the order they were
     * first added in, so a file decodes, edits and re-encodes without
     * reshuffling; replacing a key keeps its position. A null member is stored
     * as the explicit ArpgNullStruct.
     */

    // Members
    private final Object2ObjectLinkedOpenHashMap<String, ArpgElementStruct> key2Element;

    // Constructor \\

    public ArpgObjectStruct() {
        this.key2Element = new Object2ObjectLinkedOpenHashMap<>();
    }

    ArpgObjectStruct(int expectedSize) {
        this.key2Element = new Object2ObjectLinkedOpenHashMap<>(expectedSize);
    }

    // Type \\

    @Override
    public boolean isObject() {
        return true;
    }

    // Conversion \\

    @Override
    public ArpgObjectStruct getAsObject() {
        return this;
    }

    // Management \\

    public void add(String key, ArpgElementStruct element) {
        key2Element.put(key, element != null ? element : ArpgNullStruct.INSTANCE);
    }

    public void addProperty(String key, String value) {
        add(key, value != null ? new ArpgValueStruct(value) : ArpgNullStruct.INSTANCE);
    }

    public void addProperty(String key, boolean value) {
        add(key, new ArpgValueStruct(value));
    }

    public void addProperty(String key, long value) {
        add(key, new ArpgValueStruct(value));
    }

    public void addProperty(String key, float value) {
        add(key, new ArpgValueStruct(value));
    }

    public void addProperty(String key, double value) {
        add(key, new ArpgValueStruct(value));
    }

    boolean addUnique(String key, ArpgElementStruct element) {
        return key2Element.putIfAbsent(key, element) == null;
    }

    public ArpgElementStruct remove(String key) {
        return key2Element.remove(key);
    }

    // Accessible \\

    public boolean has(String key) {
        return key2Element.containsKey(key);
    }

    public ArpgElementStruct get(String key) {
        return key2Element.get(key);
    }

    public ArpgObjectStruct getAsObject(String key) {

        ArpgElementStruct element = key2Element.get(key);
        return element != null ? element.getAsObject() : null;
    }

    public ArpgArrayStruct getAsArray(String key) {

        ArpgElementStruct element = key2Element.get(key);
        return element != null ? element.getAsArray() : null;
    }

    public ArpgValueStruct getAsValue(String key) {

        ArpgElementStruct element = key2Element.get(key);
        return element != null ? element.getAsValue() : null;
    }

    public ObjectSet<String> keySet() {
        return key2Element.keySet();
    }

    public ObjectSet<Object2ObjectMap.Entry<String, ArpgElementStruct>> entrySet() {
        return key2Element.object2ObjectEntrySet();
    }

    public int size() {
        return key2Element.size();
    }

    public boolean isEmpty() {
        return key2Element.isEmpty();
    }

    // Copy \\

    @Override
    public ArpgObjectStruct deepCopy() {

        ArpgObjectStruct copy = new ArpgObjectStruct(key2Element.size());

        for (Object2ObjectMap.Entry<String, ArpgElementStruct> entry : key2Element.object2ObjectEntrySet())
            copy.key2Element.put(entry.getKey(), entry.getValue().deepCopy());

        return copy;
    }

    // Utility \\

    @Override
    String describe() {
        return "object";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ArpgObjectStruct object && key2Element.equals(object.key2Element));
    }

    @Override
    public int hashCode() {
        return key2Element.hashCode();
    }
}
