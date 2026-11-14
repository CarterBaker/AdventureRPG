package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.racemanager.RaceManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;

class CharacterPreviewBranch extends BranchPackage {

    /*
     * Builds the character editor's preview of a race from its ARPG as it
     * stands, saved or not, through the same builder the game loads races
     * with, without registering it. A race the builder refuses keeps its last
     * good preview and says why, in the editor's status and the log.
     */

    // Internal
    private RaceManager raceManager;

    // Base \\

    @Override
    protected void get() {
        this.raceManager = get(RaceManager.class);
    }

    // Preview \\

    void rebuild(CharacterDocumentInstance document, ArpgObjectStruct raceArpg) {

        try {
            document.setPreview(raceManager.buildPreviewRace(document.getRaceName(), raceArpg));
        } catch (InternalException e) {
            errorLog(e.getMessage());
            document.refusePreview(e.getMessage());
        }
    }
}