package editor.runtime.charactereditor.mode;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
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
     * listed under it; a layer's key is its region's name and its mode's,
     * a group's its region's alone, so picking a group keeps the mode where
     * the region takes it. While the window is the active window, and
     * whenever a layer is picked for it, its region and mode are handed to
     * the editor, so the Character Outliner and the Properties tab follow
     * the window worked in last. The window draws its preview in its own
     * mode whatever the editor's is.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private CharacterEditorManager characterEditorManager;

    // Layer
    private CharacterEditorRegion region;
    private CharacterEditorMode mode;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layer
        this.region = CharacterEditorRegion.WHOLE;
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

        characterEditorManager.setRegion(region);

        if (characterEditorManager.getMode() != mode)
            characterEditorManager.setMode(mode);
    }

    // Layers \\

    // A layer by its key: a mode in a region, the look group keeping a look mode, or a region keeping a mode it holds
    public void select(String layerKey) {

        int separator = layerKey.indexOf(EditorSetting.CHARACTER_LAYER_KEY_SEPARATOR);

        if (separator != EngineSetting.INDEX_NOT_FOUND)
            pick(
                    ArpgUtility.toEnum(layerKey.substring(0, separator), CharacterEditorRegion.class),
                    ArpgUtility.toEnum(
                            layerKey.substring(separator + EditorSetting.CHARACTER_LAYER_KEY_SEPARATOR.length()),
                            CharacterEditorMode.class));
        else if (layerKey.equals(EditorSetting.CHARACTER_LAYER_LOOK_KEY))
            pick(CharacterEditorRegion.WHOLE, isLook(mode) ? mode : CharacterEditorMode.APPEARANCE);
        else
            pickRegion(ArpgUtility.toEnum(layerKey, CharacterEditorRegion.class));
    }

    private void pickRegion(CharacterEditorRegion picked) {
        pick(picked, mode.isHeldBy(picked) ? mode : CharacterEditorMode.MESH);
    }

    private void pick(CharacterEditorRegion pickedRegion, CharacterEditorMode pickedMode) {

        if (pickedRegion == region && pickedMode == mode)
            return;

        this.region = pickedRegion;
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
                look.addChild(buildLayer(CharacterEditorRegion.WHOLE, listed));

        roots.add(look);
        roots.add(buildRegion(CharacterEditorRegion.WHOLE));
    }

    // A region's group: the modes it takes, then the regions listed under it, open by default while it holds regions
    private LayerNodeStruct buildRegion(CharacterEditorRegion listed) {

        boolean nests = false;

        for (CharacterEditorRegion child : CharacterEditorRegion.VALUES)
            nests |= child.getParent() == listed;

        LayerNodeStruct group = new LayerNodeStruct(
                ArpgUtility.toEnumName(listed), listed.getLabel(), holdsLayer(listed), nests);

        for (CharacterEditorMode held : CharacterEditorMode.VALUES)
            if (held.isHeldBy(listed))
                group.addChild(buildLayer(listed, held));

        for (CharacterEditorRegion child : CharacterEditorRegion.VALUES)
            if (child.getParent() == listed)
                group.addChild(buildRegion(child));

        return group;
    }

    private LayerNodeStruct buildLayer(CharacterEditorRegion listedRegion, CharacterEditorMode listedMode) {
        return new LayerNodeStruct(
                ArpgUtility.toEnumName(listedRegion) + EditorSetting.CHARACTER_LAYER_KEY_SEPARATOR
                        + ArpgUtility.toEnumName(listedMode),
                listedMode.getLabel(),
                listedRegion == region && listedMode == mode,
                false);
    }

    // Whether a region's group holds the layer worked on: one of its modes, or one of a region listed under it
    private boolean holdsLayer(CharacterEditorRegion listed) {

        if (isLook(mode))
            return false;

        for (CharacterEditorRegion held = region; held != null; held = held.getParent())
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

    public CharacterEditorRegion getRegion() {
        return region;
    }

    public int getRevision() {
        return revision;
    }
}
