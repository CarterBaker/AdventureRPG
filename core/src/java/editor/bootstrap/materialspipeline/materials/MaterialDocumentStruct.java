package editor.bootstrap.materialspipeline.materials;

import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class MaterialDocumentStruct extends StructPackage {

    /*
     * One material of the workspace, as a texturing tool's material is a set
     * of channel maps and the parameters its surface shader takes: the
     * texture it is made from, the texture each of its maps is taken from
     * instead (none for the base's own), and its parameters, one per
     * MATERIALS_PARAMETER_KEYS entry. Dirty from its first change until it is
     * saved or read again.
     */

    // Identity
    private final String materialName;

    // Maps — the texture every map is taken from, and each map's own texture, empty for the base's
    private String baseTexture;
    private final String[] mapTextures;

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

        for (int i = 0; i < mapTextures.length; i++)
            mapTextures[i] = "";

        // Parameters
        this.parameters = EditorSetting.MATERIALS_PARAMETER_DEFAULT.clone();
    }

    // Maps \\

    public void setBaseTexture(String baseTexture) {
        this.baseTexture = baseTexture;
        this.dirty = true;
    }

    // A map's own texture, empty to take it from the base again
    public void setMapTexture(int map, String textureName) {
        mapTextures[map] = textureName;
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

    public float getParameter(int parameter) {
        return parameters[parameter];
    }

    public boolean isDirty() {
        return dirty;
    }
}
