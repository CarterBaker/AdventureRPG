package editor.runtime.charactereditor.mode;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterRegionStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorModeSystem extends SystemPackage {

    /*
     * Keeps the layer a Character Editor window works on: the region of the
     * character it frames, isolates and holds every edit to, and the mode it
     * works in there, Appearance on the whole character when it opens. The
     * Layers tab lists them as the Character group holding the look modes,
     * then the Body holding the modes that take tools and the regions within
     * it, each region a group holding the modes it takes and the regions
     * listed under it, the regions being the parts the race's rig names; a
     * layer's key is its region's key and its mode's, a group's its region's
     * alone, so picking a group keeps the mode where the region takes it. The
     * window keeps its region by key, found again among the rig's regions
     * whenever they are made again, the Body once the rig no longer names
     * it. While the window is the active window, and
     * whenever a layer is picked for it, its region and mode are handed to
     * the editor, so the Character Outliner and the Properties tab follow
     * the window worked in last. The window draws its preview in its own
     * mode whatever the editor's is.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private CharacterEditorManager characterEditorManager;

    // Layer
    private String regionKey;
    private CharacterRegionStruct region;
    private int regionsRevision;
    private CharacterEditorMode mode;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layer
        this.region = CharacterRegionStruct.WHOLE;
        this.regionKey = region.getKey();
        this.regionsRevision = EngineSetting.INDEX_NOT_FOUND;
        this.mode = CharacterEditorMode.APPEARANCE;
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        if (activeWindowManager.isActive(context.getWindow()))
            adopt();
    }

    // This window's region and mode handed to the editor, the mode only when it moved, so the editor's state holds
    public void adopt() {

        characterEditorManager.setRegion(getRegion());

        if (characterEditorManager.getMode() != mode)
            characterEditorManager.setMode(mode);
    }

    // Layers \\

    // A layer by its key: a mode in a region, the look group keeping a look mode, or a region keeping a mode it holds
    public void select(String layerKey) {

        int separator = layerKey.indexOf(EditorSetting.CHARACTER_LAYER_KEY_SEPARATOR);

        if (separator != EngineSetting.INDEX_NOT_FOUND)
            pick(
                    characterEditorManager.findRegion(layerKey.substring(0, separator)),
                    ArpgUtility.toEnum(
                            layerKey.substring(separator + EditorSetting.CHARACTER_LAYER_KEY_SEPARATOR.length()),
                            CharacterEditorMode.class));
        else if (layerKey.equals(EditorSetting.CHARACTER_LAYER_LOOK_KEY))
            pick(CharacterRegionStruct.WHOLE, isLook(mode) ? mode : CharacterEditorMode.APPEARANCE);
        else
            pickRegion(characterEditorManager.findRegion(layerKey));
    }

    private void pickRegion(CharacterRegionStruct picked) {
        pick(picked, mode.isHeldBy(picked) ? mode : CharacterEditorMode.MESH);
    }

    private void pick(CharacterRegionStruct pickedRegion, CharacterEditorMode pickedMode) {

        if (pickedRegion == getRegion() && pickedMode == mode)
            return;

        this.region = pickedRegion;
        this.regionKey = pickedRegion.getKey();
        this.mode = pickedMode;
        revision++;
        adopt();
    }

    // The Character group of look modes, then the Body and every region within it
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        LayerNodeStruct look = new LayerNodeStruct(
                EditorSetting.CHARACTER_LAYER_LOOK_KEY, EditorSetting.CHARACTER_LAYER_LOOK_LABEL, isLook(mode), true);

        for (CharacterEditorMode listed : CharacterEditorMode.VALUES)
            if (isLook(listed))
                look.addChild(buildLayer(CharacterRegionStruct.WHOLE, listed));

        roots.add(look);
        roots.add(buildRegion(CharacterRegionStruct.WHOLE));
    }

    // A region's group: the modes it takes, then the regions listed under it, open by default while it holds regions
    private LayerNodeStruct buildRegion(CharacterRegionStruct listed) {

        ObjectArrayList<CharacterRegionStruct> regions = characterEditorManager.getRegions();
        boolean nests = false;

        for (int i = 0; i < regions.size(); i++)
            nests |= regions.get(i).getParent() == listed;

        LayerNodeStruct group = new LayerNodeStruct(listed.getKey(), listed.getLabel(), holdsLayer(listed), nests);

        for (CharacterEditorMode held : CharacterEditorMode.VALUES)
            if (held.isHeldBy(listed))
                group.addChild(buildLayer(listed, held));

        for (int i = 0; i < regions.size(); i++)
            if (regions.get(i).getParent() == listed)
                group.addChild(buildRegion(regions.get(i)));

        return group;
    }

    private LayerNodeStruct buildLayer(CharacterRegionStruct listedRegion, CharacterEditorMode listedMode) {
        return new LayerNodeStruct(
                listedRegion.getKey() + EditorSetting.CHARACTER_LAYER_KEY_SEPARATOR
                        + ArpgUtility.toEnumName(listedMode),
                listedMode.getLabel(),
                listedRegion == getRegion() && listedMode == mode,
                false);
    }

    // Whether a region's group holds the layer worked on: one of its modes, or one of a region listed under it
    private boolean holdsLayer(CharacterRegionStruct listed) {

        if (isLook(mode))
            return false;

        for (CharacterRegionStruct held = getRegion(); held != null; held = held.getParent())
            if (held == listed)
                return true;

        return false;
    }

    // A mode of the whole character's look, listed in the Character group
    private boolean isLook(CharacterEditorMode listed) {
        return !listed.isModeling();
    }

    // Accessible \\

    public CharacterEditorMode getMode() {
        return mode;
    }

    // The window's region among the rig's regions as they stand, found again by its key once they are made again
    public CharacterRegionStruct getRegion() {

        int regionsRevision = characterEditorManager.getRegionsRevision();

        if (regionsRevision != this.regionsRevision) {
            this.regionsRevision = regionsRevision;
            this.region = characterEditorManager.findRegion(regionKey);
            revision++;
        }

        return region;
    }

    // Moves whenever the layer picked or the layers listed change
    public int getRevision() {
        getRegion();
        return revision;
    }
}
