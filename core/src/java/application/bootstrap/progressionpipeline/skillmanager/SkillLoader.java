package application.bootstrap.progressionpipeline.skillmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.skill.SkillHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SkillLoader extends LoaderPackage {

    /*
     * Scans the skill ARPG directory and loads every skill into SkillManager.
     * Each skill's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private SkillManager skillManager;
    private SkillBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> skillName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SKILL_PATH);
        this.skillName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Skill directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(SkillBuilder.class);
    }

    @Override
    protected void get() {
        this.skillManager = get(SkillManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct skillArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "skills");

        for (int i = 0; i < skillArray.size(); i++) {
            ArpgObjectStruct skillArpg = skillArray.get(i).getAsObject();
            skillName2File.put(ArpgUtility.validateString(skillArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<SkillHandle> skills = internalBuilder.build(file);

        for (int i = 0; i < skills.size(); i++)
            skillManager.addSkillHandle(skills.get(i));
    }

    // On-Demand \\

    void request(String skillName) {

        File file = skillName2File.get(skillName);

        if (file == null)
            throwException("On-demand skill load failed — no file found for: \"" + skillName + "\"");

        request(file);
    }
}
