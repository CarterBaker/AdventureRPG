package application.bootstrap.entitypipeline.race;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RaceData extends DataPackage {

    /*
     * Immutable race loaded from ARPG: the genders it may be made as, at
     * least one, each with the character model it requires, the first the
     * one a new being of it starts as. Every gender is on the race's one rig,
     * so its equipment anchors, its optional appearance — sliders, colors,
     * features and the starting look of each gender — and every clip it plays
     * are laid out by one set of bones. The model height is the default
     * gender's mesh's, the height every gender is scaled against.
     */

    // Identity
    private final String raceName;

    // Rig
    private final RigHandle rigHandle;
    private final float modelHeight;

    // Genders — in the order the race lists them, the default first
    private final RaceGenderStruct[] genders;

    // Appearance — optional
    private final AppearanceData appearanceData;

    // Equipment
    private final ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors;

    // Constructor \\

    public RaceData(
            String raceName,
            RigHandle rigHandle,
            RaceGenderStruct[] genders,
            AppearanceData appearanceData,
            ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors) {

        // Identity
        this.raceName = raceName;

        // Rig
        this.rigHandle = rigHandle;
        this.modelHeight = genders[0].getMeshHandle().getHeight();

        // Genders
        this.genders = genders;

        // Appearance
        this.appearanceData = appearanceData;

        // Equipment
        this.equipmentAnchors = equipmentAnchors;
    }

    // Genders \\

    public int getGenderCount() {
        return genders.length;
    }

    public short getGenderID(int index) {
        return genders[index].getGenderID();
    }

    public short getDefaultGenderID() {
        return genders[0].getGenderID();
    }

    public boolean allowsGender(short genderID) {
        return findGender(genderID) != null;
    }

    // The model a gender requires, the default gender's for one the race does not allow
    public RaceGenderStruct getGender(short genderID) {

        RaceGenderStruct gender = findGender(genderID);
        return gender != null ? gender : genders[0];
    }

    public RaceGenderStruct getGenderAt(int index) {
        return genders[index];
    }

    private RaceGenderStruct findGender(short genderID) {

        for (int i = 0; i < genders.length; i++)
            if (genders[i].getGenderID() == genderID)
                return genders[i];

        return null;
    }

    // Accessible \\

    public String getRaceName() {
        return raceName;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public float getModelHeight() {
        return modelHeight;
    }

    public boolean hasAppearance() {
        return appearanceData != null;
    }

    public AppearanceData getAppearanceData() {
        return appearanceData;
    }

    public ObjectArrayList<EquipmentAnchorStruct> getEquipmentAnchors() {
        return equipmentAnchors;
    }
}
