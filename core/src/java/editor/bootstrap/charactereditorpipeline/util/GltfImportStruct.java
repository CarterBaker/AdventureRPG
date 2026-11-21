package editor.bootstrap.charactereditorpipeline.util;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GltfImportStruct extends StructPackage {

    /*
     * What a .glb file holds for Import, read before anything is made
     * from it so the user can confirm the choices it needs: the model's name,
     * its materials with the texture and zone each one names in its extras
     * (null where it names none, as outside art does), the joints its skin
     * weighs onto with the joint each hangs from and where each stands in
     * the world, whether it is skinned at all, its morph names, and its
     * geometry as glTF vertices with its faces rebuilt from its triangles:
     * four vertex slots each (a triangle's fourth repeating its third), its
     * corner count and its material.
     */

    // Model
    String meshName;
    boolean skinned;
    final ObjectArrayList<String> materialNames = new ObjectArrayList<>();
    final ObjectArrayList<String> materialTextures = new ObjectArrayList<>();
    final ObjectArrayList<String> materialZones = new ObjectArrayList<>();
    final ObjectArrayList<String> jointNames = new ObjectArrayList<>();
    final ObjectArrayList<String> jointParents = new ObjectArrayList<>();
    float[] jointPoints;
    final ObjectArrayList<String> morphNames = new ObjectArrayList<>();

    // Geometry
    float[] positions;
    float[] uvs;
    float[] weights;
    float[] masks;
    int[] joints;
    float[][] morphDeltas;
    int[] quads;
    int[] quadCorners;
    int[] quadMaterials;

    // Accessible \\

    public String getMeshName() {
        return meshName;
    }

    public boolean isSkinned() {
        return skinned;
    }

    public int getQuadCount() {
        return quadMaterials.length;
    }

    public int getMaterialCount() {
        return materialNames.size();
    }

    public boolean isMaterialUsed(int material) {

        for (int quadMaterial : quadMaterials)
            if (quadMaterial == material)
                return true;

        return false;
    }

    public String getMaterialName(int material) {
        return materialNames.get(material);
    }

    // The texture or zone the material itself names, null when the user must choose
    public String getMaterialTexture(int material) {
        return materialTextures.get(material);
    }

    public String getMaterialZone(int material) {
        return materialZones.get(material);
    }

    public ObjectArrayList<String> getJointNames() {
        return jointNames;
    }

    // The joint a joint hangs from, empty for one that hangs from none
    public String getJointParent(String jointName) {
        return jointParents.get(jointNames.indexOf(jointName));
    }

    // Where a joint stands in the world, three floats from the offset returned
    public float[] getJointPoints() {
        return jointPoints;
    }

    public int getJointPointOffset(String jointName) {
        return jointNames.indexOf(jointName) * 3;
    }

    public ObjectArrayList<String> getMorphNames() {
        return morphNames;
    }
}
