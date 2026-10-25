package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingAssetStruct extends StructPackage {

    /*
     * One asset a recording uses: its name, everything in the recording that
     * brings it in — a take, a character, an item or a vehicle — each named
     * once, and whether the game can no longer find it.
     */

    // Asset
    private final String name;
    private final ObjectArrayList<String> owners;
    private boolean missing;

    // Constructor \\

    public RecordingAssetStruct(String name) {

        // Asset
        this.name = name;
        this.owners = new ObjectArrayList<>();
    }

    // Management \\

    public void addOwner(String owner) {
        if (!owners.contains(owner))
            owners.add(owner);
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getOwnerCount() {
        return owners.size();
    }

    public String getFirstOwner() {
        return owners.get(0);
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }
}
