package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;

class CharacterPreviewBranch extends BranchPackage {

    /*
     * Builds the character editor's preview of a template from its ARPG as
     * it stands, saved or not, through the same builder the game loads
     * templates with, without registering it. A template the builder refuses,
     * or one with no character model to show, keeps its last good preview
     * and says why, in the editor's status and the log.
     */

    // Internal
    private EntityManager entityManager;

    // Base \\

    @Override
    protected void get() {
        this.entityManager = get(EntityManager.class);
    }

    // Preview \\

    void rebuild(CharacterDocumentInstance document, ArpgObjectStruct templateArpg) {

        try {
            EntityHandle previewHandle = entityManager.buildPreviewTemplate(document.getTemplateName(), templateArpg);

            if (previewHandle.getEntityData().hasCharacterModel())
                document.setPreview(previewHandle);
            else
                document.refusePreview(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_MODEL);
        } catch (InternalException e) {
            errorLog(e.getMessage());
            document.refusePreview(e.getMessage());
        }
    }
}
