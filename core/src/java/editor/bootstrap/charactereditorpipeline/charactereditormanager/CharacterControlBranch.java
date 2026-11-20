package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;
import java.util.function.Consumer;

import application.bootstrap.entitypipeline.bonecontrolmanager.BoneControlManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterControlBranch extends BranchPackage {

    /*
     * Owns the controls of the active race's rig as the Appearance rows
     * edit them: the rig's controls document (InfoManager's Bone Controls
     * entry named after the rig, made the first time a control is added),
     * the bone sliders picked to make the next control from, and every edit
     * of a control — added over the sliders picked, removed, a drive's
     * Increase flipped, a key set where the control stands. Every change,
     * made here or in the Info Panel, is handed to BoneControlManager and
     * the race's preview built again, so the preview shows the controls as
     * they now stand. Saving and reverting the character saves and reverts
     * the document with it.
     */

    // Internal
    private InfoManager infoManager;
    private BoneControlManager boneControlManager;
    private CharacterEditorManager characterEditorManager;

    // Picked — the bone sliders the next control is made from, by name
    private ObjectArrayList<String> pickedSliders;

    // Base \\

    @Override
    protected void create() {

        // Picked
        this.pickedSliders = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.boneControlManager = get(BoneControlManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    // Entry \\

    // A change to a rig's controls, made here or in the Info Panel, taken up by the game and shown on the preview
    void refreshEntry(InfoEntryStruct entry) {

        try {
            boneControlManager.updateBoneControls(entry.getEntryName(), entry.getArpg());
        } catch (InternalException e) {
            errorLog(e.getMessage());
            characterEditorManager.setStatusMessage(e.getMessage());
            return;
        }

        characterEditorManager.rebuildActivePreview();
    }

    // The controls document of the active race's rig, null while the rig has none
    private ArpgObjectStruct getControlsArpg() {

        String rigName = characterEditorManager.getActiveRigName();

        if (rigName == null)
            return null;

        InfoEntryStruct entry = infoManager.findEntry(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName, rigName);

        return entry != null ? entry.getArpg() : null;
    }

    // The controls array of the rig, made with its document the first time a control is added
    private void editControls(Consumer<ArpgArrayStruct> editor) {

        String rigName = characterEditorManager.getActiveRigName();

        if (rigName == null)
            return;

        if (getControlsArpg() == null)
            infoManager.createDocument(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName, root -> {
            });

        infoManager.editEntry(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName, rigName, root -> editor.accept(
                CharacterViewUtility.requireArray(root, EditorSetting.CHARACTER_CONTROL_FIELD_CONTROLS)));
    }

    // One drive of one control, by the control's name and the slider it drives
    private void editDrive(String controlName, String sliderName, Consumer<ArpgObjectStruct> editor) {
        editControls(controls -> {

            ArpgObjectStruct drive = findDrive(controls, controlName, sliderName);

            if (drive != null)
                editor.accept(drive);
        });
    }

    private ArpgObjectStruct findDrive(ArpgArrayStruct controls, String controlName, String sliderName) {

        int controlIndex = findControl(controls, controlName);

        if (controlIndex == EngineSetting.INDEX_NOT_FOUND)
            return null;

        ArpgArrayStruct drives = controls.get(controlIndex).getAsObject()
                .getAsArray(EditorSetting.CHARACTER_CONTROL_FIELD_DRIVES);

        for (int i = 0; i < drives.size(); i++) {

            ArpgObjectStruct drive = drives.get(i).getAsObject();

            if (ArpgUtility.getString(drive, EditorSetting.CHARACTER_CONTROL_FIELD_SLIDER, "").equals(sliderName))
                return drive;
        }

        return null;
    }

    private int findControl(ArpgArrayStruct controls, String controlName) {

        for (int i = 0; i < controls.size(); i++)
            if (ArpgUtility.getString(controls.get(i).getAsObject(), EditorSetting.CHARACTER_CONTROL_FIELD_NAME, "")
                    .equals(controlName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Picked \\

    void togglePick(String sliderName) {

        if (!pickedSliders.remove(sliderName))
            pickedSliders.add(sliderName);

        characterEditorManager.notifyChanged();
    }

    boolean isPicked(String sliderName) {
        return pickedSliders.contains(sliderName);
    }

    // Controls \\

    // A control over every bone slider picked, two at least, named after the first with the next free number
    void addControl(String groupName) {

        if (pickedSliders.size() < EditorSetting.CHARACTER_CONTROL_MIN_SLIDERS) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_CONTROL_MESSAGE_PICK);
            return;
        }

        String controlName = resolveNewName();
        ArpgObjectStruct controlArpg = new ArpgObjectStruct();
        ArpgArrayStruct drivesArpg = new ArpgArrayStruct();

        for (int i = 0; i < pickedSliders.size(); i++) {

            ArpgObjectStruct driveArpg = new ArpgObjectStruct();

            driveArpg.addProperty(EditorSetting.CHARACTER_CONTROL_FIELD_SLIDER, pickedSliders.get(i));
            drivesArpg.add(driveArpg);
        }

        controlArpg.addProperty(EditorSetting.CHARACTER_CONTROL_FIELD_NAME, controlName);
        controlArpg.addProperty(EditorSetting.CHARACTER_CONTROL_FIELD_GROUP, groupName);
        controlArpg.add(EditorSetting.CHARACTER_CONTROL_FIELD_DRIVES, drivesArpg);

        editControls(controls -> controls.add(controlArpg));
        pickedSliders.clear();
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_CONTROL_MESSAGE_ADDED + controlName);
    }

    // The control's name, "control_<n>" with the lowest n no control of the rig holds yet
    private String resolveNewName() {

        ArpgObjectStruct controlsArpg = getControlsArpg();
        ArpgArrayStruct controls = controlsArpg != null
                && ArpgUtility.hasArray(controlsArpg, EditorSetting.CHARACTER_CONTROL_FIELD_CONTROLS)
                        ? controlsArpg.getAsArray(EditorSetting.CHARACTER_CONTROL_FIELD_CONTROLS)
                        : new ArpgArrayStruct();

        for (int n = 1;; n++) {

            String name = EditorSetting.CHARACTER_CONTROL_NEW_NAME + n;

            if (findControl(controls, name) == EngineSetting.INDEX_NOT_FOUND)
                return name;
        }
    }

    void removeControl(String controlName) {

        editControls(controls -> {

            int controlIndex = findControl(controls, controlName);

            if (controlIndex != EngineSetting.INDEX_NOT_FOUND)
                controls.remove(controlIndex);
        });

        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_CONTROL_MESSAGE_REMOVED + controlName);
    }

    // Drives \\

    void setIncrease(String controlName, String sliderName, boolean increase) {
        editDrive(controlName, sliderName, drive -> drive.addProperty(
                EditorSetting.CHARACTER_CONTROL_FIELD_INCREASE, increase));
    }

    // A key of a drive's curve set where the control stands
    void setKey(String controlName, String sliderName, float place, float share) {
        editDrive(controlName, sliderName, drive -> writeKeys(drive, placeKey(readKeys(drive), place, share)));
    }

    // A drive's keys with one set at a place: the key already there moved, else a new one while the curve holds fewer
    // than it may, else the nearest replaced; keys stay in ascending order of their places
    float[] placeKey(float[] keys, float place, float share) {

        int keyCount = keys.length / 2;
        int nearest = EngineSetting.INDEX_NOT_FOUND;

        for (int k = 0; k < keyCount; k++)
            if (nearest == EngineSetting.INDEX_NOT_FOUND
                    || Math.abs(keys[k * 2] - place) < Math.abs(keys[nearest * 2] - place))
                nearest = k;

        boolean onKey = nearest != EngineSetting.INDEX_NOT_FOUND
                && Math.abs(keys[nearest * 2] - place) <= EditorSetting.CHARACTER_CONTROL_KEY_REACH;
        float[] placed = !onKey && keyCount < EngineSetting.BONE_CONTROL_MAX_KEYS
                ? Arrays.copyOf(keys, keys.length + 2)
                : Arrays.copyOf(keys, keys.length);

        if (placed.length > keys.length)
            nearest = keyCount;

        placed[nearest * 2] = place;
        placed[nearest * 2 + 1] = share;

        return sortKeys(placed);
    }

    // A drive's keys as the curve reads them, place and share pairs
    float[] readKeys(ArpgObjectStruct drive) {

        if (!ArpgUtility.hasArray(drive, EditorSetting.CHARACTER_CONTROL_FIELD_KEYS))
            return new float[0];

        ArpgArrayStruct keysArpg = drive.getAsArray(EditorSetting.CHARACTER_CONTROL_FIELD_KEYS);
        float[] keys = new float[keysArpg.size() - keysArpg.size() % 2];

        for (int i = 0; i < keys.length; i++)
            keys[i] = keysArpg.get(i).getAsFloat();

        return keys;
    }

    private float[] sortKeys(float[] keys) {

        for (int a = 0; a < keys.length / 2; a++)
            for (int b = a + 1; b < keys.length / 2; b++)
                if (keys[b * 2] < keys[a * 2]) {
                    float place = keys[a * 2];
                    float share = keys[a * 2 + 1];
                    keys[a * 2] = keys[b * 2];
                    keys[a * 2 + 1] = keys[b * 2 + 1];
                    keys[b * 2] = place;
                    keys[b * 2 + 1] = share;
                }

        return keys;
    }

    private void writeKeys(ArpgObjectStruct drive, float[] keys) {

        ArpgArrayStruct keysArpg = new ArpgArrayStruct();

        for (float key : keys)
            keysArpg.add(CharacterViewUtility.round(key));

        drive.add(EditorSetting.CHARACTER_CONTROL_FIELD_KEYS, keysArpg);
    }

    // Files \\

    // The rig's controls written with the character; true when there is nothing to write
    boolean save() {

        String rigName = characterEditorManager.getActiveRigName();

        if (rigName == null || !infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName))
            return true;

        return infoManager.saveDocument(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName);
    }

    void revert() {

        String rigName = characterEditorManager.getActiveRigName();

        pickedSliders.clear();

        if (rigName != null && infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName))
            infoManager.revertDocument(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName);
    }

    boolean isDirty() {

        String rigName = characterEditorManager.getActiveRigName();

        return rigName != null && infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, rigName);
    }
}
