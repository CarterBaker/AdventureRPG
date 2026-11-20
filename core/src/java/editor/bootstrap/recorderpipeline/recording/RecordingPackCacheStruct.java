package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;
import engine.util.arpg.ArpgElementStruct;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class RecordingPackCacheStruct extends StructPackage {

    /*
     * The packed blocks a media file was last written or read with, each kept
     * by the track, curve or column it packs and the revision that source had,
     * so writing a source that has not changed since gives back the very same
     * block instead of packing it anew; a block a read found is kept the same
     * way while what it was read into stays as read. Undo steps and saves of
     * one recording therefore share every block their edits left alone. Each
     * write keeps only the blocks it used, so a source let go of is let go of
     * here at the next write.
     */

    // Blocks
    private final Reference2ObjectOpenHashMap<Object, ArpgElementStruct> blocks;
    private final Reference2IntOpenHashMap<Object> revisions;

    // Next — the blocks the write under way has used
    private final Reference2ObjectOpenHashMap<Object, ArpgElementStruct> nextBlocks;
    private final Reference2IntOpenHashMap<Object> nextRevisions;

    // Constructor \\

    public RecordingPackCacheStruct() {

        // Blocks
        this.blocks = new Reference2ObjectOpenHashMap<>();
        this.revisions = new Reference2IntOpenHashMap<>();

        // Next
        this.nextBlocks = new Reference2ObjectOpenHashMap<>();
        this.nextRevisions = new Reference2IntOpenHashMap<>();
    }

    // Write \\

    // The block kept for the source at its revision, used by the write under way; null when it must be packed
    public ArpgElementStruct find(Object source, int revision) {

        ArpgElementStruct block = blocks.get(source);

        if (block == null || revisions.getInt(source) != revision)
            return null;

        keep(source, revision, block);

        return block;
    }

    // The block the write under way packed for the source
    public void keep(Object source, int revision, ArpgElementStruct block) {
        nextBlocks.put(source, block);
        nextRevisions.put(source, revision);
    }

    // The write done: the blocks it used are the ones kept, every other let go
    public void finish() {

        blocks.clear();
        revisions.clear();
        blocks.putAll(nextBlocks);
        revisions.putAll(nextRevisions);
        nextBlocks.clear();
        nextRevisions.clear();
    }

    // Read \\

    // The block a read unpacked into the source, at the revision the source stands at once read
    public void remember(Object source, int revision, ArpgElementStruct block) {
        blocks.put(source, block);
        revisions.put(source, revision);
    }
}
