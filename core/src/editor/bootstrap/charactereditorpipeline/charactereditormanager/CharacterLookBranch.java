package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.util.mathematics.vectors.Vector3;

class CharacterLookBranch extends BranchPackage {

    /*
     * Puts the character editor's preview look on an appearance: the gender
     * it is made as, with the body and features that gender wears, the tone
     * worn of every color, every slider, the blink, the build and the head
     * shape. Every viewport that shows the active character — the Character
     * Editor's and the Face Editor's — takes its look from here, so both show
     * one face. A neutral look holds every slider at nothing and the eyes
     * open, as the mesh is authored; a still face never blinks on its own.
     * The sliders are staged and taken up in the one proportions pass the
     * build sets off.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    // Look \\

    void apply(AppearanceHandle appearance, AppearanceData appearanceData, boolean neutral, boolean stillFace) {

        Vector3 headProportion = characterEditorManager.getHeadProportion();
        short genderID = characterEditorManager.getPreviewGenderID();
        float blink = characterEditorManager.getBlink();

        if (appearance.getGenderID() != genderID)
            appearance.setGender(genderID);

        for (CharacterEditorColor color : CharacterEditorColor.VALUES) {

            Color tone = resolveTone(
                    appearanceData.getPalette(color.getSkinnedColor()),
                    appearanceData.getColor(color.getSkinnedColor()),
                    characterEditorManager.getToneIndex(color));

            appearance.setColor(color.getSkinnedColor(), tone.r, tone.g, tone.b);
        }

        for (int i = 0; i < appearanceData.getShapeCount(); i++)
            appearance.stageShapeValue(i, neutral ? 0f : characterEditorManager.getShapeValue(i));

        appearance.setFaceAnimated(!neutral && !stillFace && blink <= 0f);
        appearance.setBlink(neutral ? 0f : blink);
        appearance.setWeightRatio(characterEditorManager.getWeightRatio());
        appearance.setHeadProportion(headProportion.x, headProportion.y, headProportion.z);
    }

    // The tone worn, or the default color while none is or the palette has not caught up with the edit
    private Color resolveTone(Color[] palette, Color defaultColor, int toneIndex) {
        return toneIndex >= 0 && toneIndex < palette.length ? palette[toneIndex] : defaultColor;
    }
}
