package editor.runtime.charactereditor.region;

import application.bootstrap.entitypipeline.appearance.AppearanceIsolationStruct;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedMaskMode;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CharacterRegionIsolationSystem extends SystemPackage {

    /*
     * Shows a part editor's part alone. The whole character's tab draws
     * everything; any other draws the body only where it holds a part of the
     * rig, through a mask showing the bones the region holds — on the side
     * shown, where the part is sided — and of the worn parts only its own
     * feature, through the same mask where it is sided, so the Eye Editor
     * shows one socket with its eye, the Hand Editor one hand, the Mouth
     * Editor the jaw and lips with the teeth, and the Hair Editor the hair.
     * The isolation is built again only when the editor's state moves.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;

    // Bound
    private int builtRevision;
    private CharacterEditorRegion builtRegion;
    private RigHandle builtRig;

    // Base \\

    @Override
    protected void create() {

        // Bound
        this.builtRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        CharacterEditorRegion region = characterEditorModeSystem.getRegion();
        RigHandle rigHandle = characterViewPoseSystem.hasSubject()
                ? characterViewPoseSystem.getRaceData().getRigHandle()
                : null;

        if (characterEditorManager.getRevision() == builtRevision && region == builtRegion && rigHandle == builtRig)
            return;

        this.builtRevision = characterEditorManager.getRevision();
        this.builtRegion = region;
        this.builtRig = rigHandle;
        isolate(region, rigHandle, characterViewRenderSystem.getIsolation());
    }

    private void isolate(CharacterEditorRegion region, RigHandle rigHandle, AppearanceIsolationStruct isolation) {

        isolation.clear();

        if (region.isWhole() || rigHandle == null)
            return;

        boolean sided = characterEditorManager.isSided(region);

        isolation.setBodyShown(region.holdsBody());

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            isolation.setFeature(featureSlot, featureSlot == region.getFeatureSlot(), sided);

        if (!region.holdsBody())
            return;

        SkinnedBoneMaskStruct boneMask = isolation.getBoneMask();
        boneMask.setMode(SkinnedMaskMode.SHOW);

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            if (characterEditorManager.isBoneShown(region, rigHandle, i))
                boneMask.addBone(i);
    }
}
