package editor.bootstrap.materialspipeline.materials;

import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class MaterialDocumentStruct extends StructPackage {

    /*
     * One material of the workspace, as a texturing tool's material is a set
     * of channel maps and the parameters its surface shader takes: the
     * texture it is made from, the texture or the image each of its maps is
     * taken from instead (neither for the base's own; an image wins over a
     * texture), and its parameters, one per MATERIALS_PARAMETER_KEYS entry.
     * Dirty from its first change until it is saved or read again.
     */

    // Identity
    private final String materialName;

    // Maps — the texture every map is taken from, and each map's own texture and image file, empty for none
    private String baseTexture;
    private final String[] mapTextures;
    private final String[] mapImages;

    // Parameters
    private final float[] parameters;

    // State
    private boolean dirty;

    // Constructor \\

    public MaterialDocumentStruct(String materialName, String baseTexture) {

        // Identity
        this.materialName = materialName;

        // Maps
        this.baseTexture = baseTexture;
        this.mapTextures = new String[EditorSetting.CHARACTER_PAINT_MAP_LABELS.length];
        this.mapImages = new String[EditorSetting.CHARACTER_PAINT_MAP_LABELS.length];

        for (int i = 0; i < mapTextures.length; i++) {
            mapTextures[i] = "";
            mapImages[i] = "";
        }

        // Parameters
        this.parameters = EditorSetting.MATERIALS_PARAMETER_DEFAULT.clone();
    }

    // Maps \\

    public void setBaseTexture(String baseTexture) {
        this.baseTexture = baseTexture;
        this.dirty = true;
    }

    // A map's own texture, its image let go; empty to take it from the base again
    public void setMapTexture(int map, String textureName) {
        mapTextures[map] = textureName;
        mapImages[map] = "";
        this.dirty = true;
    }

    // A map's own image file, by its path as the file keeps it, its texture let go
    public void setMapImage(int map, String imagePath) {
        mapTextures[map] = "";
        mapImages[map] = imagePath;
        this.dirty = true;
    }

    // The texture a map is read from: its own, or the base's
    public String resolveMapTexture(int map) {
        return mapTextures[map].isEmpty() ? baseTexture : mapTextures[map];
    }

    // Parameters \\

    public void setParameter(int parameter, float value) {
        parameters[parameter] = Math.clamp(
                value,
                EditorSetting.MATERIALS_PARAMETER_MIN[parameter],
                EditorSetting.MATERIALS_PARAMETER_MAX[parameter]);
        this.dirty = true;
    }

    // History \\

    // Every map and parameter taken from another copy of the material, as an undo step puts one back
    public void copyFrom(MaterialDocumentStruct other) {

        this.baseTexture = other.baseTexture;
        System.arraycopy(other.mapTextures, 0, mapTextures, 0, mapTextures.length);
        System.arraycopy(other.mapImages, 0, mapImages, 0, mapImages.length);
        System.arraycopy(other.parameters, 0, parameters, 0, parameters.length);
        this.dirty = true;
    }

    // A copy of the material as it stands, for its history
    public MaterialDocumentStruct copy() {

        MaterialDocumentStruct copy = new MaterialDocumentStruct(materialName, baseTexture);

        copy.copyFrom(this);
        return copy;
    }

    // State \\

    public void markSaved() {
        this.dirty = false;
    }

    // Accessible \\

    public String getMaterialName() {
        return materialName;
    }

    public String getBaseTexture() {
        return baseTexture;
    }

    public String getMapTexture(int map) {
        return mapTextures[map];
    }

    public String getMapImage(int map) {
        return mapImages[map];
    }

    public boolean hasMapImage(int map) {
        return !mapImages[map].isEmpty();
    }

    public float getParameter(int parameter) {
        return parameters[parameter];
    }

    public boolean isDirty() {
        return dirty;
    }
}
