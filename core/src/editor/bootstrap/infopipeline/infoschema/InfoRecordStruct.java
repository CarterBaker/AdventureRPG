package editor.bootstrap.infopipeline.infoschema;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoRecordStruct extends StructPackage {

    /*
     * How a flat array of whole numbers reads as records, as a song clip's
     * notes do: a fixed number of slots to a record, each named and held to
     * its own bounds, so the array holds whole records only; and, when it
     * declares a span, the slots holding each record's start and length,
     * whose end keeps within the sibling number named.
     */

    // Slots
    private final ObjectArrayList<String> slotKeys;
    private final ObjectArrayList<InfoRuleStruct> slotRules;

    // Span — the slots holding a record's start and length, and the sibling its end keeps within
    private final int spanStartSlot;
    private final int spanLengthSlot;
    private final String spanWithin;

    // Constructor \\

    public InfoRecordStruct(
            ObjectArrayList<String> slotKeys,
            ObjectArrayList<InfoRuleStruct> slotRules,
            int spanStartSlot,
            int spanLengthSlot,
            String spanWithin) {

        // Slots
        this.slotKeys = slotKeys;
        this.slotRules = slotRules;

        // Span
        this.spanStartSlot = spanStartSlot;
        this.spanLengthSlot = spanLengthSlot;
        this.spanWithin = spanWithin;
    }

    // Slots \\

    public int getStride() {
        return slotKeys.size();
    }

    public String getSlotKey(int slot) {
        return slotKeys.get(slot);
    }

    public InfoRuleStruct getSlotRule(int slot) {
        return slotRules.get(slot);
    }

    public ObjectArrayList<String> getSlotKeys() {
        return slotKeys;
    }

    // Span \\

    public boolean hasSpan() {
        return spanStartSlot != EngineSetting.INDEX_NOT_FOUND;
    }

    public int getSpanStartSlot() {
        return spanStartSlot;
    }

    public int getSpanLengthSlot() {
        return spanLengthSlot;
    }

    public String getSpanWithin() {
        return spanWithin;
    }
}
