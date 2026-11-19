package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterGroomStruct extends StructPackage {

    /*
     * A groom, the hair tool's project as a grooming tool keeps its guide
     * curves apart from the cards a game draws: the rig it is worn on, the
     * hair feature it was taken from (empty for one grown from nothing,
     * whose scalp cap then comes with every export), and its strands. A copy
     * is a whole undo step.
     */

    // Groom
    private final String rigName;
    private String sourceFeature;
    private final ObjectArrayList<CharacterStrandStruct> strands;

    // Constructor \\

    public CharacterGroomStruct(String rigName, String sourceFeature) {

        // Groom
        this.rigName = rigName;
        this.sourceFeature = sourceFeature;
        this.strands = new ObjectArrayList<>();
    }

    public CharacterGroomStruct(CharacterGroomStruct source) {

        this(source.rigName, source.sourceFeature);

        for (int i = 0; i < source.strands.size(); i++)
            strands.add(new CharacterStrandStruct(source.strands.get(i)));
    }

    // Accessible \\

    public String getRigName() {
        return rigName;
    }

    public String getSourceFeature() {
        return sourceFeature;
    }

    public void setSourceFeature(String sourceFeature) {
        this.sourceFeature = sourceFeature;
    }

    public ObjectArrayList<CharacterStrandStruct> getStrands() {
        return strands;
    }

    public int getStrandCount() {
        return strands.size();
    }

    public CharacterStrandStruct getStrand(int strandIndex) {
        return strands.get(strandIndex);
    }

    public int getSelectedCount() {

        int selected = 0;

        for (int i = 0; i < strands.size(); i++)
            if (strands.get(i).isSelected())
                selected++;

        return selected;
    }
}
