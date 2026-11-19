package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.io.File;
import java.util.Locale;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterWorkspaceBranch extends BranchPackage {

    /*
     * The character models of the workspace, as a modelling tool keeps its
     * skinned meshes apart from the engine that imports them: each a rigged
     * mesh file in the workspace's Characters folder, skinned to one of the
     * game's rigs the way a skeletal mesh is skinned to a shared skeleton. A
     * new model holds only the rig it stands on, blank until its first part
     * is placed. A model with a body is made known to the mesh loader under
     * a workspace name and previewed through a race built in memory alone,
     * its one gender, material and animation tree taken from a race already
     * on that rig and its look and anchors with them, or, on a rig no race
     * stands on (an item's), the game's first gender in the standalone
     * material moving by the rig's own animation tree, so every character
     * tool works on it as on any character; it is never written as a race.
     * Import brings the model into the game: its mesh written among the
     * game's meshes and worn by a gender of a race (one already on the rig,
     * or a new one made with that gender alone, looking like the race it was
     * previewed beside and not yet offered to the player), and an entity of
     * the race made when one is named. The model's file stays in the
     * workspace to be worked and imported again.
     */

    // Internal
    private CharacterMeshBranch characterMeshBranch;
    private InfoManager infoManager;
    private MeshManager meshManager;
    private RigManager rigManager;
    private GenderManager genderManager;

    // Directory
    private File modelRoot;

    // Status — why the last open or import could not be done
    private String refusal;

    // Scratch — the race and the name of the gender the last search found
    private ArpgObjectStruct foundRaceArpg;
    private String foundGenderName;

    // Base \\

    @Override
    protected void create() {
        this.modelRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.CHARACTER_WORKSPACE_DIRECTORY);
    }

    @Override
    protected void get() {
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.infoManager = get(InfoManager.class);
        this.meshManager = get(MeshManager.class);
        this.rigManager = get(RigManager.class);
        this.genderManager = get(GenderManager.class);
    }

    // Files \\

    File getFolder() {
        return modelRoot;
    }

    // The model a file in the Characters folder holds, by name; null for a file outside it
    String toModelName(File file) {
        return WorkspaceUtility.toName(modelRoot, file);
    }

    boolean isNameAvailable(String modelName) {
        return FileUtility.isValidFileName(modelName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !resolveFile(modelName).exists();
    }

    // Whether a rig of that name is one of the game's
    boolean isRig(String rigName) {
        return infoManager.hasDocument(EditorSetting.INFO_SCHEMA_RIGS, rigName);
    }

    private File resolveFile(String modelName) {
        return ArpgUtility.resolveFile(modelRoot, modelName);
    }

    // A model's mesh, and the race it is previewed through, go by its name under the workspace's prefix
    private static String toWorkspaceName(String modelName) {
        return EditorSetting.CHARACTER_WORKSPACE_PREFIX + modelName;
    }

    // New \\

    // A blank model written: the rig it stands on, nothing more; false when it could not be written
    boolean create(String modelName, String rigName) {

        ArpgObjectStruct modelArpg = new ArpgObjectStruct();

        modelArpg.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigName);
        resolveFile(modelName).getParentFile().mkdirs();

        return ArpgUtility.tryWriteObject(resolveFile(modelName), modelArpg);
    }

    // Open \\

    // The model's document, its body made known and its preview race built when it has one; null, with the refusal
    // kept, when the file or its rig cannot be read
    CharacterDocumentInstance open(String modelName) {

        ArpgObjectStruct modelArpg = ArpgUtility.tryLoadObject(resolveFile(modelName));

        if (modelArpg == null || !ArpgUtility.hasString(modelArpg, EditorSetting.CHARACTER_MODEL_FIELD_RIG)) {
            this.refusal = EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_READ + modelName;
            return null;
        }

        RigHandle rigHandle;

        try {
            rigHandle = rigManager.getRigHandleFromRigName(
                    modelArpg.get(EditorSetting.CHARACTER_MODEL_FIELD_RIG).getAsString());
        } catch (InternalException e) {
            this.refusal = e.getMessage();
            return null;
        }

        CharacterDocumentInstance document = create(CharacterDocumentInstance.class);
        document.constructModel(toWorkspaceName(modelName), modelName, rigHandle);

        if (ArpgUtility.hasObject(modelArpg, EditorSetting.CHARACTER_MODEL_FIELD_GEOMETRY) && !registerBody(document))
            return null;

        return document;
    }

    // Body \\

    // A blank model's first body written to its file and made known, so it opens as any model does; false, with the
    // refusal kept, when it could not be
    boolean writeBody(CharacterDocumentInstance document, RiggedMeshStruct mesh) {
        return writeModel(document.getModelName(), mesh) && registerBody(document);
    }

    // A model written whole with the mesh as its body, on the mesh's rig, over any model of that name; false, with
    // the refusal kept, when it could not be
    boolean writeModel(String modelName, RiggedMeshStruct mesh) {

        ArpgObjectStruct sourceArpg = new ArpgObjectStruct();
        ArpgArrayStruct layout = new ArpgArrayStruct();
        File file = resolveFile(modelName);

        layout.add(EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        layout.add(EngineSetting.RIGGED_MESH_UV_FLOATS);
        sourceArpg.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, mesh.getRigHandle().getRigName());
        sourceArpg.add(EditorSetting.CHARACTER_MODEL_FIELD_VAO, layout);
        file.getParentFile().mkdirs();

        if (!ArpgUtility.tryWriteObject(file, meshManager.toRiggedMeshArpg(mesh, sourceArpg))) {
            this.refusal = EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_WRITTEN + modelName;
            return false;
        }

        return true;
    }

    // The model's mesh made known to the mesh loader and the mesh tools under its workspace name, and its preview
    // race built from a race on its rig
    private boolean registerBody(CharacterDocumentInstance document) {

        String meshName = toWorkspaceName(document.getModelName());
        File file = resolveFile(document.getModelName());
        ArpgObjectStruct templateArpg = buildTemplate(document.getRigHandle().getRigName(), meshName);

        if (templateArpg == null) {
            this.refusal = EditorSetting.CHARACTER_MODEL_MESSAGE_NO_RACE + document.getRigHandle().getRigName();
            return false;
        }

        meshManager.addMeshFile(meshName, file);
        characterMeshBranch.addMeshFile(meshName, file);
        document.setTemplate(templateArpg);

        return true;
    }

    // Template \\

    // A race in memory alone with one gender wearing the mesh: the gender, material and animation tree of the first
    // race's gender on the rig; null when no race stands on it
    private ArpgObjectStruct buildTemplate(String rigName, String meshName) {
        return buildRace(rigName, meshName, null);
    }

    // A race standing on the rig with one gender wearing the mesh, the gender named (or the source's own when none is),
    // the anchors of the first race on the rig and the part of its look any body takes (its head bone, colors,
    // palettes and detail amounts, never the sliders, expressions or features that name a body's own morphs and
    // meshes), as a new race starts looking like the race it was made beside; null when no race stands on it
    private ArpgObjectStruct buildRace(String rigName, String meshName, String genderName) {

        ArpgObjectStruct gender = resolveGender(rigName);

        if (gender == null)
            return null;

        ArpgObjectStruct raceArpg = new ArpgObjectStruct();
        ArpgObjectStruct gendersArpg = new ArpgObjectStruct();

        gendersArpg.add(genderName != null ? genderName : foundGenderName, toGender(gender, meshName));
        raceArpg.add(EditorSetting.CHARACTER_MODEL_FIELD_GENDERS, gendersArpg);
        copyField(foundRaceArpg, raceArpg, EditorSetting.CHARACTER_MODEL_FIELD_EQUIPMENT);

        if (foundRaceArpg != null
                && ArpgUtility.hasObject(foundRaceArpg, EditorSetting.CHARACTER_MODEL_FIELD_APPEARANCE)) {

            ArpgObjectStruct sourceLook = foundRaceArpg.getAsObject(EditorSetting.CHARACTER_MODEL_FIELD_APPEARANCE);
            ArpgObjectStruct look = new ArpgObjectStruct();

            for (int i = 0; i < EditorSetting.CHARACTER_MODEL_LOOK_FIELDS.length; i++)
                copyField(sourceLook, look, EditorSetting.CHARACTER_MODEL_LOOK_FIELDS[i]);

            raceArpg.add(EditorSetting.CHARACTER_MODEL_FIELD_APPEARANCE, look);
        }

        return raceArpg;
    }

    private static void copyField(ArpgObjectStruct source, ArpgObjectStruct target, String key) {

        if (source != null && source.has(key))
            target.add(key, source.get(key).deepCopy());
    }

    // The first gender of a race on the rig, its race and name kept as the ones found; one of the given race only when
    // a race is named; null for none
    private ArpgObjectStruct findGender(String rigName, String raceName) {

        ObjectArrayList<InfoEntryStruct> races = infoManager.getEntries(EditorSetting.INFO_SCHEMA_RACES);

        for (int r = 0; r < races.size(); r++) {

            InfoEntryStruct race = races.get(r);

            if (raceName != null && !race.getEntryName().equals(raceName)
                    || !ArpgUtility.hasObject(race.getArpg(), EditorSetting.CHARACTER_MODEL_FIELD_GENDERS))
                continue;

            ArpgObjectStruct gendersArpg = race.getArpg().getAsObject(EditorSetting.CHARACTER_MODEL_FIELD_GENDERS);

            for (String genderName : gendersArpg.keySet()) {

                ArpgObjectStruct gender = gendersArpg.getAsObject(genderName);

                if (!rigName.equals(ArpgUtility.getString(gender, EditorSetting.CHARACTER_MODEL_FIELD_RIG, "")))
                    continue;

                this.foundRaceArpg = race.getArpg();
                this.foundGenderName = genderName;
                return gender;
            }
        }

        return null;
    }

    // The gender a model on the rig is previewed as: the first gender of a race on the rig, or on a rig no race stands
    // on, the game's first gender in the standalone material moving by the rig's own animation tree, no race found;
    // null when the rig has neither
    private ArpgObjectStruct resolveGender(String rigName) {

        ArpgObjectStruct gender = findGender(rigName, null);

        return gender != null ? gender : buildStandaloneGender(rigName);
    }

    private ArpgObjectStruct buildStandaloneGender(String rigName) {

        String treeName = findAnimationTree(rigName);
        ObjectArrayList<InfoEntryStruct> genders = infoManager.getEntries(EditorSetting.INFO_SCHEMA_GENDERS);

        if (treeName == null || genders.isEmpty())
            return null;

        ArpgObjectStruct gender = new ArpgObjectStruct();

        gender.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigName);
        gender.addProperty(
                EditorSetting.CHARACTER_MODEL_FIELD_MATERIAL, EditorSetting.CHARACTER_MODEL_STANDALONE_MATERIAL);
        gender.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_ANIMATION_TREE, treeName);

        this.foundRaceArpg = null;
        this.foundGenderName = genders.get(0).getEntryName();
        return gender;
    }

    // The first animation tree moving the rig; null for none
    private String findAnimationTree(String rigName) {

        ObjectArrayList<InfoEntryStruct> trees = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ANIMATION_TREES);

        for (int i = 0; i < trees.size(); i++)
            if (rigName.equals(ArpgUtility.getString(
                    trees.get(i).getArpg(), EditorSetting.CHARACTER_MODEL_FIELD_RIG, "")))
                return trees.get(i).getEntryName();

        return null;
    }

    // A gender wearing the mesh on the source gender's rig, material and animation tree, nothing worn beside it
    private static ArpgObjectStruct toGender(ArpgObjectStruct source, String meshName) {

        ArpgObjectStruct gender = new ArpgObjectStruct();

        gender.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_MESH, meshName);

        for (int i = 0; i < EditorSetting.CHARACTER_MODEL_GENDER_FIELDS.length; i++) {

            String key = EditorSetting.CHARACTER_MODEL_GENDER_FIELDS[i];
            gender.addProperty(key, ArpgUtility.getString(source, key, ""));
        }

        return gender;
    }

    // Import \\

    // Whether a model may go to the race: a race of that name on the model's rig, or a name no race holds yet
    boolean isImportRace(CharacterDocumentInstance document, String raceName) {

        if (!FileUtility.isValidFileName(raceName, EditorSetting.NAME_INPUT_MAX_LENGTH))
            return false;

        return !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_RACES, raceName)
                || findGender(document.getRigHandle().getRigName(), raceName) != null;
    }

    boolean isImportGender(String genderName) {
        return genderManager.hasGender(genderName);
    }

    // Whether an entity may be made under the name: none at all, or a name no entity holds yet
    boolean isImportEntity(String entityName) {
        return entityName.isEmpty()
                || FileUtility.isValidFileName(entityName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName);
    }

    // The saved model brought into the game: its mesh written among the game's meshes, worn by the gender of the race
    // (made when it does not exist) and an entity of the race made when one is named; null when it was, else why not
    String importModel(CharacterDocumentInstance document, String raceName, String genderName, String entityName) {

        String meshName = EditorSetting.CHARACTER_MODEL_IMPORT_FOLDER + raceName.toLowerCase(Locale.ROOT)
                + EditorSetting.INFO_FOLDER_SEPARATOR + FileUtility.getFileName(document.getModelName());
        File meshFile = ArpgUtility.resolveFile(new File(EngineSetting.MESH_PATH), meshName);
        ArpgObjectStruct modelArpg = ArpgUtility.tryLoadObject(resolveFile(document.getModelName()));

        meshFile.getParentFile().mkdirs();

        if (modelArpg == null || !ArpgUtility.tryWriteObject(meshFile, modelArpg))
            return EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_WRITTEN + meshName;

        meshManager.addMeshFile(meshName, meshFile);

        ArpgObjectStruct gender = toGender(resolveGender(document.getRigHandle().getRigName()), meshName);

        if (infoManager.hasDocument(EditorSetting.INFO_SCHEMA_RACES, raceName))
            infoManager.editEntry(EditorSetting.INFO_SCHEMA_RACES, raceName, raceName, race -> wearGender(
                    CharacterViewUtility.requireObject(race, EditorSetting.CHARACTER_MODEL_FIELD_GENDERS),
                    genderName,
                    gender,
                    meshName));
        else
            infoManager.createDocument(EditorSetting.INFO_SCHEMA_RACES, raceName, race -> fillRace(
                    race,
                    buildRace(document.getRigHandle().getRigName(), meshName, genderName)));

        if (!infoManager.saveDocument(EditorSetting.INFO_SCHEMA_RACES, raceName))
            return infoManager.getRefusal();

        if (entityName.isEmpty())
            return null;

        infoManager.createDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName, entity -> entity.addProperty(
                EditorSetting.CHARACTER_EDITOR_FIELD_RACE, raceName));

        return infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName)
                ? null
                : infoManager.getRefusal();
    }

    // A new race's fields, every default replaced by the race built for it, not yet offered to the player
    private static void fillRace(ArpgObjectStruct race, ArpgObjectStruct raceArpg) {

        ObjectArrayList<String> defaultKeys = new ObjectArrayList<>(race.keySet());

        for (int i = 0; i < defaultKeys.size(); i++)
            race.remove(defaultKeys.get(i));

        for (String key : raceArpg.keySet())
            race.add(key, raceArpg.get(key));

        race.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_PLAYABLE, false);
    }

    // The gender wears the mesh: the race's own gender of that name keeps every other field, a gender it lacks is made
    private static void wearGender(
            ArpgObjectStruct gendersArpg,
            String genderName,
            ArpgObjectStruct gender,
            String meshName) {

        if (ArpgUtility.hasObject(gendersArpg, genderName))
            gendersArpg.getAsObject(genderName).addProperty(EditorSetting.CHARACTER_MODEL_FIELD_MESH, meshName);
        else
            gendersArpg.add(genderName, gender);
    }

    // Accessible \\

    String getRefusal() {
        return refusal;
    }
}
