package engine.util.arpg;

import java.util.Iterator;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class ArpgArrayStruct extends ArpgElementStruct implements Iterable<ArpgElementStruct> {

    /*
     * Ordered node of the ARPG document tree. Elements are read by index or
     * iterated in order; a null element is stored as the explicit
     * ArpgNullStruct.
     */

    // Elements
    private final ObjectArrayList<ArpgElementStruct> elements;

    // Constructor \\

    public ArpgArrayStruct() {
        this.elements = new ObjectArrayList<>();
    }

    ArpgArrayStruct(int expectedSize) {
        this.elements = new ObjectArrayList<>(expectedSize);
    }

    // Type \\

    @Override
    public boolean isArray() {
        return true;
    }

    // Conversion \\

    @Override
    public ArpgArrayStruct getAsArray() {
        return this;
    }

    // Management \\

    public void add(ArpgElementStruct element) {
        elements.add(element != null ? element : ArpgNullStruct.INSTANCE);
    }

    public void add(String value) {
        add(value != null ? new ArpgValueStruct(value) : ArpgNullStruct.INSTANCE);
    }

    public void add(boolean value) {
        add(new ArpgValueStruct(value));
    }

    public void add(long value) {
        add(new ArpgValueStruct(value));
    }

    public void add(float value) {
        add(new ArpgValueStruct(value));
    }

    public void add(double value) {
        add(new ArpgValueStruct(value));
    }

    public void addAll(ArpgArrayStruct array) {
        elements.addAll(array.elements);
    }

    public ArpgElementStruct set(int index, ArpgElementStruct element) {
        return elements.set(index, element != null ? element : ArpgNullStruct.INSTANCE);
    }

    public ArpgElementStruct remove(int index) {
        return elements.remove(index);
    }

    public boolean remove(ArpgElementStruct element) {
        return elements.remove(element);
    }

    // Accessible \\

    public ArpgElementStruct get(int index) {
        return elements.get(index);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public Iterator<ArpgElementStruct> iterator() {
        return elements.iterator();
    }

    // Copy \\

    @Override
    public ArpgArrayStruct deepCopy() {

        ArpgArrayStruct copy = new ArpgArrayStruct(elements.size());

        for (int i = 0; i < elements.size(); i++)
            copy.elements.add(elements.get(i).deepCopy());

        return copy;
    }

    // Utility \\

    @Override
    String describe() {
        return "array";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ArpgArrayStruct array && elements.equals(array.elements));
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}
