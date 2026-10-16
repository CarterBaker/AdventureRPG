package application.bootstrap.progressionpipeline.skillmanager;

import application.bootstrap.progressionpipeline.skill.SkillHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SkillManager extends ManagerPackage {

    /*
     * Owns the skill palette: the exact kinds of item a being grows skilled
     * with, each feeding the trades it belongs to. IDs are assigned in
     * registration order, and a name declared twice is rejected. Every skill
     * is resolved in awake(), before any item definition names one, so a
     * being's skills can be held in one array indexed by ID.
     */

    // Palette
    private Object2IntOpenHashMap<String> skillName2SkillID;
    private ObjectArrayList<SkillHandle> skillID2SkillHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.skillName2SkillID = RegistryUtility.createNameIndex();
        this.skillID2SkillHandle = RegistryUtility.createPalette();
        create(SkillLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerSkillName(String skillName) {
        return (short) RegistryUtility.registerID(
                skillName2SkillID, skillID2SkillHandle, skillName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addSkillHandle(SkillHandle skillHandle) {

        short skillID = skillHandle.getSkillID();

        if (skillID2SkillHandle.get(skillID) != null)
            throwException("Duplicate skill name: '" + skillHandle.getSkillName()
                    + "' is declared more than once — every skill name must be unique");

        skillID2SkillHandle.set(skillID, skillHandle);
    }

    // Accessible \\

    public boolean hasSkill(String skillName) {
        return RegistryUtility.getHandle(skillName2SkillID, skillID2SkillHandle, skillName) != null;
    }

    public short getSkillIDFromSkillName(String skillName) {

        if (!hasSkill(skillName) && internalLoader != null)
            ((SkillLoader) internalLoader).request(skillName);

        if (!hasSkill(skillName))
            throwException("Skill not found after load: \"" + skillName + "\"");

        return (short) skillName2SkillID.getInt(skillName);
    }

    public SkillHandle getSkillHandleFromSkillID(short skillID) {

        SkillHandle handle = RegistryUtility.getHandle(skillID2SkillHandle, skillID);

        if (handle == null)
            throwException("Skill ID not found: " + skillID);

        return handle;
    }

    public SkillHandle getSkillHandleFromSkillName(String skillName) {
        return getSkillHandleFromSkillID(getSkillIDFromSkillName(skillName));
    }

    // Every skill ID lies below this — the length of an array indexed by skill ID
    public int getSkillIDCount() {
        return skillID2SkillHandle.size();
    }
}
