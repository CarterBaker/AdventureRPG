package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.io.File;
import java.util.Locale;

import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.animationtreemanager.AnimationTreeManager;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
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
     * the race made when one is named, or, named an item worn on the body,
     * worn as that item on the model's rig: its mesh written among the
     * game's worn meshes and listed in the item's worn models for the gender
     * named or every gender, in place of any it held for the same rig and
     * gender. The model's file stays in the workspace to be worked and
     * imported again.
     */

    // Internal
    private CharacterMeshBranch characterMeshBranch;
    private InfoManager infoManager;
    private MeshManager meshManager;
    private RigManager rigManager;
    private AnimationManager animationManager;
    private AnimationTreeManager animationTreeManager;
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
        this.animationManager = get(AnimationManager.class);
        this.animationTreeManager = get(AnimationTreeManager.class);
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

    // The mesh a model is worked as, by its name and its rig under the workspace's prefix, so a model moved onto a rig
    // of its own is loaded afresh on it
    private static String toWorkspaceMeshName(String modelName, String rigName) {
        return EditorSetting.CHARACTER_WORKSPACE_PREFIX + modelName + EditorSetting.INFO_FOLDER_SEPARATOR + rigName;
    }

    // The race a model is previewed through goes by its name under the workspace's prefix
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

        String meshName = toWorkspaceMeshName(document.getModelName(), document.getRigHandle().getRigName());
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

    // Rig \\

    // The model moved onto a rig of its own: a copy of the one it stands on, named after it, with a rest pose and an
    // animation tree playing it to preview it by, written among the game's rigs and clips and made known at once;
    // the model's file then names it. Null when it was, else why not
    String moveToNewRig(CharacterDocumentInstance document) {

        String sourceRig = document.getRigHandle().getRigName();
        InfoEntryStruct source = infoManager.findEntry(EditorSetting.INFO_SCHEMA_RIGS, sourceRig, sourceRig);
        ArpgObjectStruct modelArpg = ArpgUtility.tryLoadObject(resolveFile(document.getModelName()));

        if (source == null || modelArpg == null)
            return EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_READ + document.getModelName();

        String rigName = resolveNewRigName(FileUtility.getFileName(document.getModelName()));
        String folder = rigName.toLowerCase(Locale.ROOT) + EditorSetting.INFO_FOLDER_SEPARATOR;
        String clipName = folder + rigName + EditorSetting.CHARACTER_RIG_REST_CLIP_SUFFIX;
        String treeName = folder + rigName + EditorSetting.CHARACTER_RIG_REST_TREE_SUFFIX;
        ArpgObjectStruct rigArpg = source.getArpg().deepCopy().getAsObject();
        String rootBone = rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES).get(0).getAsObject()
                .get(EditorSetting.CHARACTER_RIG_FIELD_NAME).getAsString();

        String refusal = writeContent(EditorSetting.INFO_SCHEMA_RIGS, rigName, rigArpg);

        if (refusal == null)
            refusal = writeContent(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName, buildRestClip(rigName, rootBone));

        if (refusal == null)
            refusal = writeContent(
                    EditorSetting.INFO_SCHEMA_ANIMATION_TREES, treeName, buildRestTree(rigName, clipName));

        if (refusal != null)
            return refusal;

        rigManager.addRigFile(rigName, ArpgUtility.resolveFile(new File(EngineSetting.RIG_PATH), rigName));
        animationManager.addAnimationFile(
                clipName, ArpgUtility.resolveFile(new File(EngineSetting.ANIMATION_PATH), clipName));
        animationTreeManager.addAnimationTreeFile(
                treeName, ArpgUtility.resolveFile(new File(EngineSetting.ANIMATION_TREE_PATH), treeName));

        modelArpg.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigName);

        return ArpgUtility.tryWriteObject(resolveFile(document.getModelName()), modelArpg)
                ? null
                : EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_WRITTEN + document.getModelName();
    }

    // The model's name and Rig, numbered past every rig of the game already holding it
    private String resolveNewRigName(String stem) {

        String rigName = stem + EditorSetting.CHARACTER_RIG_NEW_SUFFIX;
        int number = 2;

        while (infoManager.hasDocument(EditorSetting.INFO_SCHEMA_RIGS, rigName))
            rigName = stem + EditorSetting.CHARACTER_RIG_NEW_SUFFIX + number++;

        return rigName;
    }

    // A file of the schema made with exactly the fields given and saved; null when it was, else why not
    private String writeContent(String schemaName, String definitionName, ArpgObjectStruct contentArpg) {

        infoManager.createDocument(schemaName, definitionName, content -> replaceFields(content, contentArpg));

        return infoManager.saveDocument(schemaName, definitionName) ? null : infoManager.getRefusal();
    }

    // A looping clip holding the rig at rest, keyed on its root at its start and end
    private static ArpgObjectStruct buildRestClip(String rigName, String rootBone) {

        ArpgObjectStruct clip = new ArpgObjectStruct();
        ArpgObjectStruct tracks = new ArpgObjectStruct();
        ArpgArrayStruct keys = new ArpgArrayStruct();
        float[] times = EditorSetting.CHARACTER_RIG_REST_KEY_TIMES;

        for (int i = 0; i < times.length; i++) {

            ArpgObjectStruct key = new ArpgObjectStruct();
            key.addProperty(EditorSetting.CHARACTER_RIG_FIELD_TIME, times[i]);
            keys.add(key);
        }

        tracks.add(rootBone, keys);
        clip.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigName);
        clip.addProperty(EditorSetting.CHARACTER_RIG_FIELD_LOOP, true);
        clip.add(EditorSetting.CHARACTER_RIG_FIELD_TRACKS, tracks);
        return clip;
    }

    // A tree of one layer whose one node plays the rest clip while idle
    private static ArpgObjectStruct buildRestTree(String rigName, String clipName) {

        ArpgObjectStruct tree = new ArpgObjectStruct();
        ArpgArrayStruct layers = new ArpgArrayStruct();
        ArpgObjectStruct layer = new ArpgObjectStruct();
        ArpgObjectStruct nodes = new ArpgObjectStruct();
        ArpgObjectStruct node = new ArpgObjectStruct();
        ArpgObjectStruct states = new ArpgObjectStruct();

        node.addProperty(EditorSetting.CHARACTER_RIG_FIELD_CLIP, clipName);
        nodes.add(EditorSetting.CHARACTER_RIG_REST_NODE, node);
        states.addProperty(EditorSetting.CHARACTER_RIG_REST_STATE, EditorSetting.CHARACTER_RIG_REST_NODE);
        layer.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, EditorSetting.CHARACTER_RIG_REST_LAYER);
        layer.add(EditorSetting.CHARACTER_RIG_FIELD_NODES, nodes);
        layer.add(EditorSetting.CHARACTER_RIG_FIELD_STATES, states);
        layers.add(layer);
        tree.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_RIG, rigName);
        tree.add(EditorSetting.CHARACTER_RIG_FIELD_LAYERS, layers);
        return tree;
    }

    // Item Import \\

    // The full name of the first item whose sub-voxels are the mesh; empty for none
    String findItemByMesh(String meshName) {

        ObjectArrayList<InfoEntryStruct> items = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ITEMS);

        for (int i = 0; i < items.size(); i++)
            if (meshName.equals(ArpgUtility.getString(
                    items.get(i).getArpg(), EditorSetting.INFO_ITEM_MESH_FIELD, "")))
                return ItemRegistryUtility.toItemName(items.get(i).getDefinitionName(), items.get(i).getEntryName());

        return "";
    }

    boolean isImportItem(String itemName) {
        return findItem(itemName) != null;
    }

    // The item model brought into the game as the mesh the item is drawn with: written among the game's painted item
    // meshes and named by the item's painted field, its sub-voxels kept for its shape; null when it was, else why not
    String importItem(CharacterDocumentInstance document, String itemName) {

        InfoEntryStruct item = findItem(itemName);
        String meshName = EditorSetting.CHARACTER_ITEM_IMPORT_FOLDER + FileUtility.getFileName(document.getModelName());
        File meshFile = ArpgUtility.resolveFile(new File(EngineSetting.MESH_PATH), meshName);
        ArpgObjectStruct modelArpg = ArpgUtility.tryLoadObject(resolveFile(document.getModelName()));

        meshFile.getParentFile().mkdirs();

        if (item == null || modelArpg == null || !ArpgUtility.tryWriteObject(meshFile, modelArpg))
            return EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_WRITTEN + meshName;

        meshManager.addMeshFile(meshName, meshFile);
        infoManager.notifyResourcesChanged();
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ITEMS,
                item.getDefinitionName(),
                item.getEntryName(),
                itemArpg -> itemArpg.addProperty(EngineSetting.ITEM_PAINTED_FIELD, meshName));

        return infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, item.getDefinitionName())
                ? null
                : infoManager.getRefusal();
    }

    // Worn Import \\

    // Whether a model may be worn as the item: one of that full name worn on the body, not only held
    boolean isImportWorn(String itemName) {

        InfoEntryStruct item = findItem(itemName);

        return item != null && ArpgUtility.getEnum(
                item.getArpg(), EditorSetting.INFO_ITEM_EQUIP_FIELD, EquipmentType.class, EquipmentType.NONE)
                .isWornOnBody();
    }

    // The saved model brought into the game as the model the item is worn as on the model's rig: written among the
    // game's worn meshes and listed in the item's worn models for the gender named, empty for every gender; null when
    // it was, else why not
    String importWorn(CharacterDocumentInstance document, String itemName, String genderName) {

        InfoEntryStruct item = findItem(itemName);
        String meshName = EditorSetting.CHARACTER_WORN_IMPORT_FOLDER + FileUtility.getFileName(document.getModelName());
        File meshFile = ArpgUtility.resolveFile(new File(EngineSetting.MESH_PATH), meshName);
        ArpgObjectStruct modelArpg = ArpgUtility.tryLoadObject(resolveFile(document.getModelName()));

        meshFile.getParentFile().mkdirs();

        if (item == null || modelArpg == null || !ArpgUtility.tryWriteObject(meshFile, modelArpg))
            return EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_WRITTEN + meshName;

        meshManager.addMeshFile(meshName, meshFile);
        infoManager.notifyResourcesChanged();
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ITEMS,
                item.getDefinitionName(),
                item.getEntryName(),
                itemArpg -> wearModel(itemArpg, meshName, genderName, document.getRigHandle().getRigName()));

        return infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, item.getDefinitionName())
                ? null
                : infoManager.getRefusal();
    }

    // Lists the mesh among the item's worn models for the gender, dropping the one it held for the same rig and gender
    private void wearModel(ArpgObjectStruct itemArpg, String meshName, String genderName, String rigName) {

        ArpgArrayStruct worn = ArpgUtility.hasArray(itemArpg, EngineSetting.ITEM_WORN_FIELD)
                ? itemArpg.getAsArray(EngineSetting.ITEM_WORN_FIELD)
                : new ArpgArrayStruct();

        for (int i = worn.size() - 1; i >= 0; i--) {

            ArpgObjectStruct wornArpg = worn.get(i).getAsObject();

            if (genderName.equals(ArpgUtility.getString(
                    wornArpg, EditorSetting.INFO_ITEM_WORN_GENDER_FIELD, EngineSetting.ITEM_WORN_GENDER_NONE))
                    && isOnRig(ArpgUtility.getString(wornArpg, EditorSetting.INFO_ITEM_WORN_MESH_FIELD, ""),
                            meshName, rigName))
                worn.remove(i);
        }

        ArpgObjectStruct wornArpg = new ArpgObjectStruct();
        wornArpg.addProperty(EditorSetting.INFO_ITEM_WORN_MESH_FIELD, meshName);

        if (!genderName.isEmpty())
            wornArpg.addProperty(EditorSetting.INFO_ITEM_WORN_GENDER_FIELD, genderName);

        worn.add(wornArpg);
        itemArpg.add(EngineSetting.ITEM_WORN_FIELD, worn);
    }

    // Whether a worn model's mesh is the one imported or another skinned to the same rig
    private boolean isOnRig(String wornMeshName, String meshName, String rigName) {
        return wornMeshName.equals(meshName)
                || rigName.equals(meshManager.getMeshHandleFromMeshName(wornMeshName).getRigHandle().getRigName());
    }

    private InfoEntryStruct findItem(String itemName) {

        int separator = itemName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);

        return separator > 0
                ? infoManager.findEntry(
                        EditorSetting.INFO_SCHEMA_ITEMS,
                        ItemRegistryUtility.toDefinitionName(itemName),
                        itemName.substring(separator + 1))
                : null;
    }

    // A new race's fields, every default replaced by the race built for it, not yet offered to the player
    private static void fillRace(ArpgObjectStruct race, ArpgObjectStruct raceArpg) {
        replaceFields(race, raceArpg);
        race.addProperty(EditorSetting.CHARACTER_MODEL_FIELD_PLAYABLE, false);
    }

    // A new document's fields, every default replaced by the ones given
    private static void replaceFields(ArpgObjectStruct target, ArpgObjectStruct source) {

        ObjectArrayList<String> defaultKeys = new ObjectArrayList<>(target.keySet());

        for (int i = 0; i < defaultKeys.size(); i++)
            target.remove(defaultKeys.get(i));

        for (String key : source.keySet())
            target.add(key, source.get(key));
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
