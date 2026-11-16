package editor.runtime.stamps.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.bootstrap.stamppipeline.stampmanager.StampManager;
import editor.runtime.EditorSetting;
import editor.runtime.label.LabelUtility;
import editor.runtime.stamps.StampsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StampsMenuSystem extends SystemPackage {

    /*
     * Opens the Stamps menu: the stamps as tiles, a row of them injected at a
     * time with every tile of the row filled from it, each with the emblem of
     * the part and its name, the one in use tinted and every tile carrying its
     * stamp's name; then the details
     * of the stamp in use — its kind, its bones and faces, the loop it joins
     * a host by or that it is a root part, and its sockets. Rebuilt only when
     * the stamps move.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private StampManager stampManager;

    // Menus
    private MenuInstance stampsMenu;

    // Tints
    private MenuColorStruct selectedTint;
    private MenuColorStruct emptyTint;

    // Rows
    private ObjectArrayList<ElementInstance> tileRows;
    private ObjectArrayList<ElementInstance> detailLines;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Tints
        this.selectedTint = new MenuColorStruct(ThemeColor.ACCENT, StampsSetting.TILE_SELECTED_ALPHA);
        this.emptyTint = new MenuColorStruct(ThemeColor.TEXT, StampsSetting.TILE_EMPTY_ALPHA);

        // Rows
        this.tileRows = new ObjectArrayList<>();
        this.detailLines = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.stampManager = get(StampManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.stampsMenu = menuManager.openMenu(StampsSetting.MENU_STAMPS, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (stampManager.getRevision() == shownRevision)
            return;

        shownRevision = stampManager.getRevision();
        refreshTiles();
        refreshDetails();
    }

    // Tiles \\

    // The stamps a row at a time, each row's tiles filled in turn and any left over in the last row emptied
    private void refreshTiles() {

        clear(tileRows, StampsSetting.ENTRY_TILES);

        ObjectArrayList<StampHandle> stamps = stampManager.getStamps();

        for (int first = 0; first < stamps.size(); first += EditorSetting.STAMP_TILES_PER_ROW) {

            int rowFirst = first;

            tileRows.add(menuManager.inject(
                    stampsMenu, StampsSetting.ENTRY_TILES, StampsSetting.TEMPLATE_TILE_ROW,
                    row -> fillRow(row, stamps, rowFirst)));
        }
    }

    private void fillRow(ElementInstance row, ObjectArrayList<StampHandle> stamps, int first) {

        for (int slot = 0; slot < EditorSetting.STAMP_TILES_PER_ROW; slot++) {

            int index = first + slot;

            fillTile(row, slot, index < stamps.size() ? stamps.get(index) : null);
        }
    }

    // A tile showing a stamp's emblem and name, tinted while it is the stamp in use; an empty one shows nothing and
    // picks nothing
    private void fillTile(ElementInstance row, int slot, StampHandle stamp) {

        ElementInstance tile = row.findChildById(StampsSetting.ELEMENT_TILE + slot);
        ElementInstance thumbnail = row.findChildById(StampsSetting.ELEMENT_TILE_THUMBNAIL + slot);

        if (tile == null || thumbnail == null)
            return;

        tile.setActionArgOverride(stamp != null ? stamp.getStampName() : "");
        setChildText(row, StampsSetting.ELEMENT_TILE_LABEL + slot, stamp != null ? stamp.getDisplayName() : "");

        if (stamp == stampManager.getActiveStamp() && stamp != null)
            tile.setColorOverride(selectedTint);

        if (stamp == null) {
            thumbnail.setColorOverride(emptyTint);
            return;
        }

        if (thumbnail.hasSprite())
            thumbnail.getSpriteInstance().setTexture(stampManager.getThumbnail(stamp));
    }

    // Details \\

    private void refreshDetails() {

        clear(detailLines, StampsSetting.ENTRY_DETAILS);

        StampHandle stamp = stampManager.getActiveStamp();

        if (stamp == null) {
            injectDetail(EditorSetting.STAMP_DETAIL_NONE);
            return;
        }

        injectDetail(EditorSetting.STAMP_DETAIL_CATEGORY
                + LabelUtility.toWords(ArpgUtility.toEnumName(stamp.getCategory())));
        injectDetail(EditorSetting.STAMP_DETAIL_BONES + (stamp.getRigHandle().getBoneCount() - 1));
        injectDetail(EditorSetting.STAMP_DETAIL_FACES + stamp.getMesh().getFaceCount());
        injectDetail(stamp.hasPlug()
                ? EditorSetting.STAMP_DETAIL_PLUG + resolveTopBones(stamp)
                : EditorSetting.STAMP_DETAIL_ROOT);
        injectDetail(EditorSetting.STAMP_DETAIL_SOCKETS + stamp.getSocketCount());

        for (int i = 0; i < stamp.getSocketCount(); i++)
            injectDetail(EditorSetting.STAMP_DETAIL_SOCKET_PREFIX + LabelUtility.toWords(
                    stamp.getSocket(i).getSocketName()));
    }

    // The bones that hang from the host, the ones joining it, by name
    private String resolveTopBones(StampHandle stamp) {

        StringBuilder names = new StringBuilder();

        for (int b = 1; b < stamp.getRigHandle().getBoneCount(); b++) {

            if (stamp.getRigHandle().getBone(b).getParentIndex() != 0)
                continue;

            if (!names.isEmpty())
                names.append(EditorSetting.STAMP_DETAIL_SEPARATOR);

            names.append(LabelUtility.toWords(stamp.getRigHandle().getBone(b).getName()));
        }

        return names.toString();
    }

    private void injectDetail(String text) {
        detailLines.add(menuManager.inject(
                stampsMenu, StampsSetting.ENTRY_DETAILS, StampsSetting.TEMPLATE_DETAIL_LINE,
                element -> setChildText(element, StampsSetting.ELEMENT_DETAIL_LABEL, text)));
    }

    // Utility \\

    private void clear(ObjectArrayList<ElementInstance> elements, int entry) {

        for (int i = 0; i < elements.size(); i++)
            menuManager.eject(stampsMenu, entry, elements.get(i));

        elements.clear();
    }

    private void setChildText(ElementInstance element, String childID, String text) {

        ElementInstance child = element.findChildById(childID);

        if (child != null)
            child.setFontText(text);
    }
}
