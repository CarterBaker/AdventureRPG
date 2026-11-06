package editor.bootstrap.entityeditorpipeline.entityeditormanager;

import java.io.File;

import editor.bootstrap.entityeditorpipeline.util.EntityEditorChoice;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class EntityPropertyBranch extends BranchPackage {

    /*
     * Reads, steps and types the Entity Editor's choices, every one described
     * by its EntityEditorChoice: a race, a rig or an animation tree is any
     * entry of its schema, a tree only those on the race's rig; a mesh or a
     * material is any file in the folder of the one chosen now, so a body
     * steps through the bodies beside it. A typed value must name an entry or
     * a file that exists. Values are read from the live ARPG each time and
     * written through EntityEditBranch.
     */

    // Internal
    private InfoManager infoManager;
    private EntityEditorManager entityEditorManager;
    private EntityEditBranch entityEditBranch;

    // Scratch
    private ObjectArrayList<String> options;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.options = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.entityEditorManager = get(EntityEditorManager.class);
        this.entityEditBranch = get(EntityEditBranch.class);
    }

    // Values \\

    // The value a choice holds now, empty when its entity, race or gender has none
    String getValue(EntityEditorChoice choice) {

        ArpgObjectStruct owner = resolveOwner(choice);

        return owner != null && ArpgUtility.hasString(owner, choice.getField())
                ? owner.get(choice.getField()).getAsString()
                : "";
    }

    // The object a choice is read from: the entity, or the gender previewed, whose rig stands for the race's
    private ArpgObjectStruct resolveOwner(EntityEditorChoice choice) {
        return switch (choice.getScope()) {
            case ENTITY -> entityEditorManager.getEntityArpg();
            case RACE, GENDER -> entityEditorManager.getPickedGenderArpg();
        };
    }

    boolean isValueValid(EntityEditorChoice choice, String value) {

        if (value.isEmpty())
            return false;

        return choice.getSchemaName() != null
                ? infoManager.hasDocument(choice.getSchemaName(), value)
                : ArpgUtility.resolveFile(new File(choice.getDirectory()), value).isFile();
    }

    void setValue(EntityEditorChoice choice, String value) {

        String entityName = entityEditorManager.getActiveEntityName();
        String raceName = entityEditorManager.getActiveRaceName();
        String genderName = entityEditorManager.getPickedGenderName();

        switch (choice.getScope()) {
            case ENTITY -> entityEditBranch.setEntityField(entityName, choice.getField(), value);
            case RACE -> entityEditBranch.setRaceField(raceName, choice.getField(), value);
            case GENDER -> entityEditBranch.setGenderField(raceName, genderName, choice.getField(), value);
        }
    }

    // Steps \\

    // The next option one way or the other, wrapping at either end
    void cycle(EntityEditorChoice choice, int direction) {

        collectOptions(choice);

        if (options.isEmpty())
            return;

        int index = options.indexOf(getValue(choice));
        int next = index == EngineSetting.INDEX_NOT_FOUND
                ? (direction > 0 ? 0 : options.size() - 1)
                : Math.floorMod(index + direction, options.size());

        setValue(choice, options.get(next));
    }

    private void collectOptions(EntityEditorChoice choice) {

        options.clear();

        if (choice.listsSiblings())
            collectSiblings(choice);
        else
            collectEntries(choice);

        options.sort(String.CASE_INSENSITIVE_ORDER);
    }

    // Every file in the folder of the value chosen now, by its resource name
    private void collectSiblings(EntityEditorChoice choice) {

        File root = new File(choice.getDirectory());
        String value = getValue(choice);
        int separator = value.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);
        File folder = separator == EngineSetting.INDEX_NOT_FOUND ? root : new File(root, value.substring(0, separator));

        if (!folder.isDirectory())
            return;

        for (File file : FileUtility.collectFilesShallow(folder, EngineSetting.ARPG_FILE_EXTENSIONS))
            options.add(FileUtility.getPathWithFileNameWithoutExtension(root, file));
    }

    // Every entry of the choice's schema, only those on the race's rig for a choice bound to it
    private void collectEntries(EntityEditorChoice choice) {

        String rigName = getValue(EntityEditorChoice.RIG);
        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(choice.getSchemaName());

        for (int i = 0; i < entries.size(); i++)
            if (!choice.isRigBound() || isOnRig(entries.get(i).getArpg(), rigName))
                options.add(entries.get(i).getEntryName());
    }

    private boolean isOnRig(ArpgObjectStruct entryArpg, String rigName) {
        return ArpgUtility.hasString(entryArpg, EditorSetting.ENTITY_EDITOR_FIELD_RIG)
                && entryArpg.get(EditorSetting.ENTITY_EDITOR_FIELD_RIG).getAsString().equals(rigName);
    }
}
