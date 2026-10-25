package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingAssetSectionStruct extends StructPackage {

    /*
     * One section of a recording's assets: every asset of its kind, each
     * listed once however often the recording uses it, in name order once
     * sorted.
     */

    // Section
    private final RecorderAssetKind kind;
    private final ObjectArrayList<RecordingAssetStruct> assets;

    // Palette
    private final Object2ObjectOpenHashMap<String, RecordingAssetStruct> assetName2Asset;

    // Constructor \\

    public RecordingAssetSectionStruct(RecorderAssetKind kind) {

        // Section
        this.kind = kind;
        this.assets = new ObjectArrayList<>();

        // Palette
        this.assetName2Asset = new Object2ObjectOpenHashMap<>();
    }

    // Management \\

    // The asset by its name, listed the first time it is named, with what brought it in
    public RecordingAssetStruct add(String assetName, String owner) {

        RecordingAssetStruct asset = assetName2Asset.get(assetName);

        if (asset == null) {
            asset = new RecordingAssetStruct(assetName);
            assetName2Asset.put(assetName, asset);
            assets.add(asset);
        }

        asset.addOwner(owner);
        return asset;
    }

    public boolean contains(String assetName) {
        return assetName2Asset.containsKey(assetName);
    }

    public void sort() {
        assets.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
    }

    public void clear() {
        assets.clear();
        assetName2Asset.clear();
    }

    // Accessible \\

    public RecorderAssetKind getKind() {
        return kind;
    }

    public int size() {
        return assets.size();
    }

    public RecordingAssetStruct get(int index) {
        return assets.get(index);
    }
}
