package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.function.Consumer;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CharacterCommandBranch extends BranchPackage {

    /*
     * Runs the command console's character commands on the character open in
     * the character tabs, the entity or race picked last. Every command is
     * mapped to its action once. Each Mirror command symmetrizes one mesh the
     * character wears, its body or its hair, from the side it names onto the
     * other through the same Symmetrize the Mesh tab runs, as one edit undone
     * and saved like any other, and reports what it did to the log as the
     * status line shows it.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterSymmetryBranch characterSymmetryBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, Consumer<CommandStruct>> commandName2Action;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.commandName2Action = new Object2ObjectOpenHashMap<>();
        registerActions();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterSymmetryBranch = get(CharacterSymmetryBranch.class);
    }

    private void registerActions() {
        commandName2Action.put(EditorSetting.COMMAND_MIRROR_BODY_LEFT,
                command -> mirror(EditorSetting.CHARACTER_EDITOR_PART_BODY, true));
        commandName2Action.put(EditorSetting.COMMAND_MIRROR_BODY_RIGHT,
                command -> mirror(EditorSetting.CHARACTER_EDITOR_PART_BODY, false));
        commandName2Action.put(EditorSetting.COMMAND_MIRROR_HAIR_LEFT,
                command -> mirror(ArpgUtility.toEnumName(FeatureSlot.HAIR), true));
        commandName2Action.put(EditorSetting.COMMAND_MIRROR_HAIR_RIGHT,
                command -> mirror(ArpgUtility.toEnumName(FeatureSlot.HAIR), false));
    }

    // Commands \\

    void executeCommand(CommandStruct command) {

        Consumer<CommandStruct> action = commandName2Action.get(command.getCommandName());

        if (action == null) {
            errorLog(EditorSetting.COMMAND_MESSAGE_NO_CHARACTER_ACTION + command.getCommandName());
            return;
        }

        action.accept(command);
    }

    // The part's mesh symmetrized, left (+x) onto right or right onto left
    private void mirror(String partKey, boolean leftToRight) {

        CharacterMeshDocumentInstance document = characterEditorManager.getPartMeshDocument(partKey);

        if (document == null) {
            errorLog(EditorSetting.COMMAND_MESSAGE_NO_PART_MESH + partKey);
            return;
        }

        characterSymmetryBranch.symmetrize(document, leftToRight);
        log(characterEditorManager.getStatusMessage());
    }
}
