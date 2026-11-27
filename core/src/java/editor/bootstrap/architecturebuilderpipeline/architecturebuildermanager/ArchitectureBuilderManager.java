package editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager;

import java.util.Objects;

import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import editor.bootstrap.architecturebuilderpipeline.architecturebuilder.ArchitectureSlotKind;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.propertiespipeline.util.PaletteRowUtility;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.label.LabelUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArchitectureBuilderManager extends ManagerPackage {

    /*
     * Owns the Architecture Builder's shared state: the architecture worked
     * on, the one the Hierarchy and the Palette tab pick, and the slot picked
     * in the Layers tab, every block slot grouped as its structures name it
     * (Siding, Roof, Floor...) and every furnishing table grouped by its
     * folder (Beds: Large, Small, Parents, Kids Bunk...). A slot is a layer;
     * its group and its kind are layers holding it. The Palette tab lists
     * every architecture, then what the picked slot may be filled with: every
     * whole block that breaks into a piece for a block slot, every piece of
     * furniture for a furniture slot, each after the choice to leave the slot
     * as structures were authored. Choosing one fills the slot in the
     * architecture's entry through ArchitectureSlotBranch, so the file is
     * changed, saved and reverted as any content and every builder tab shows
     * the new look at once. The catalogue of slots is gathered again whenever
     * the architecture or the slot picked changes or the entry is edited.
     */

    // Internal
    private InfoManager infoManager;
    private ArchitectureManager architectureManager;
    private FurnishingManager furnishingManager;
    private BlockManager blockManager;
    private ItemDefinitionManager itemDefinitionManager;
    private ArchitectureSlotBranch architectureSlotBranch;

    // Choice
    private String architectureName;
    private String layerKey;

    // Catalogue — every slot of each kind, by name in order, and what slots may be filled with
    private ObjectArrayList<String> blockSlotNames;
    private ObjectArrayList<String> furnitureSlotNames;
    private ObjectArrayList<BlockHandle> blockCandidates;
    private ObjectArrayList<ItemDefinitionHandle> furnitureCandidates;
    private ObjectArrayList<String> scratchNames;
    private boolean candidatesGathered;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.architectureSlotBranch = create(ArchitectureSlotBranch.class);

        // Choice
        this.layerKey = ArchitectureSlotKind.BLOCKS.name();

        // Catalogue
        this.blockSlotNames = new ObjectArrayList<>();
        this.furnitureSlotNames = new ObjectArrayList<>();
        this.blockCandidates = new ObjectArrayList<>();
        this.furnitureCandidates = new ObjectArrayList<>();
        this.scratchNames = new ObjectArrayList<>();

        // Status
        this.statusMessage = "";
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.architectureManager = get(ArchitectureManager.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.blockManager = get(BlockManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    @Override
    protected void awake() {

        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_ARCHITECTURES, entry -> applySelection(entry.getEntryName()));
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ARCHITECTURES, this::applyChange);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ARCHITECTURES, this::applyDelete);
        gatherSlots();
    }

    // Selection \\

    // An architecture picked in the Palette tab, opened in the Hierarchy and the Info Panel alike
    public void selectArchitecture(String selectedName) {

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_ARCHITECTURES, selectedName, selectedName);
        applySelection(selectedName);
    }

    private void applySelection(String selectedName) {

        if (Objects.equals(selectedName, architectureName))
            return;

        this.architectureName = selectedName;
        notifyChanged();
    }

    private void applyChange(InfoEntryStruct entry) {

        if (entry.getEntryName().equals(architectureName))
            notifyChanged();
    }

    private void applyDelete(InfoEntryStruct entry) {

        if (!entry.getEntryName().equals(architectureName))
            return;

        this.architectureName = null;
        notifyChanged();
    }

    // The slot, group or kind picked in the Layers tab
    public void selectLayer(String selectedKey) {

        if (selectedKey.equals(layerKey))
            return;

        this.layerKey = selectedKey;
        this.statusMessage = "";
        notifyChanged();
    }

    // The architecture opened in the Hierarchy so the Info Panel's own New and Duplicate act on it — false with none
    public boolean focusArchitecture() {

        if (architectureName == null) {
            setStatusMessage(EditorSetting.ARCHITECTURE_BUILDER_MESSAGE_NONE);
            return false;
        }

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName, architectureName);
        return true;
    }

    // Palette \\

    // Every architecture, then what the slot picked may be filled with
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ARCHITECTURE_BUILDER_PALETTE_ARCHITECTURES));

        PaletteRowUtility.addEntryRows(
                infoManager, rows, EditorSetting.INFO_SCHEMA_ARCHITECTURES,
                EditorSetting.ARCHITECTURE_BUILDER_ROW_ARCHITECTURE, architectureName);

        if (architectureName == null || !isSlot(layerKey))
            return;

        gatherCandidates();

        ArchitectureSlotKind kind = toKind(layerKey);
        String slotName = toSlotName(layerKey);
        String filled = architectureSlotBranch.read(architectureName, kind, slotName);

        rows.add(CharacterViewRowStruct.header(toSlotLabel(kind, slotName)));
        rows.add(CharacterViewRowStruct.item(
                EditorSetting.ARCHITECTURE_BUILDER_ROW_EMPTY, toEmptyLabel(kind), filled == null));

        if (kind.nested)
            addBlockRows(rows, filled);
        else
            addFurnitureRows(rows, filled);
    }

    private void addBlockRows(ObjectArrayList<CharacterViewRowStruct> rows, String filled) {

        for (int i = 0; i < blockCandidates.size(); i++) {

            String name = blockCandidates.get(i).getBlockName();

            rows.add(CharacterViewRowStruct.item(
                    EditorSetting.ARCHITECTURE_BUILDER_ROW_FILL + name,
                    blockCandidates.get(i).getLocalName(),
                    name.equals(filled)));
        }
    }

    private void addFurnitureRows(ObjectArrayList<CharacterViewRowStruct> rows, String filled) {

        for (int i = 0; i < furnitureCandidates.size(); i++) {

            String name = furnitureCandidates.get(i).getItemName();

            rows.add(CharacterViewRowStruct.item(
                    EditorSetting.ARCHITECTURE_BUILDER_ROW_FILL + name,
                    furnitureCandidates.get(i).getDisplayName(),
                    name.equals(filled)));
        }
    }

    // A Palette row chosen: an architecture to work on, or what the slot picked is filled with or left as
    public void choosePaletteRow(String rowKey) {

        if (rowKey.startsWith(EditorSetting.ARCHITECTURE_BUILDER_ROW_ARCHITECTURE)) {
            selectArchitecture(rowKey.substring(EditorSetting.ARCHITECTURE_BUILDER_ROW_ARCHITECTURE.length()));
            return;
        }

        if (architectureName == null || !isSlot(layerKey))
            return;

        ArchitectureSlotKind kind = toKind(layerKey);
        String slotName = toSlotName(layerKey);

        if (rowKey.startsWith(EditorSetting.ARCHITECTURE_BUILDER_ROW_FILL))
            architectureSlotBranch.fill(architectureName, kind, slotName,
                    rowKey.substring(EditorSetting.ARCHITECTURE_BUILDER_ROW_FILL.length()));
        else
            architectureSlotBranch.empty(architectureName, kind, slotName);
    }

    // Every whole block that breaks into a piece and every piece of furniture, gathered the first time one is listed
    private void gatherCandidates() {

        if (candidatesGathered)
            return;

        this.candidatesGathered = true;

        int blockIDCount = blockManager.getBlockIDCount();

        for (int blockID = EngineSetting.REGISTRY_RESERVED_ID + 1; blockID < blockIDCount; blockID++) {

            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockID);

            if (blockHandle.getGeometry() == DynamicGeometryType.FULL && blockHandle.hasPiece())
                blockCandidates.add(blockHandle);
        }

        ObjectArrayList<ItemDefinitionHandle> items = itemDefinitionManager.getItemHandles();

        for (int i = 0; i < items.size(); i++)
            if (items.get(i).getCategory() == ItemCategory.FURNITURE)
                furnitureCandidates.add(items.get(i));
    }

    // Layers \\

    // Blocks over Furniture, each holding its groups and each group its slots, labelled with what fills them
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        roots.add(buildKindLayer(ArchitectureSlotKind.BLOCKS, blockSlotNames));
        roots.add(buildKindLayer(ArchitectureSlotKind.FURNITURE, furnitureSlotNames));
    }

    private LayerNodeStruct buildKindLayer(ArchitectureSlotKind kind, ObjectArrayList<String> slotNames) {

        String kindKey = kind.name();
        LayerNodeStruct kindLayer = new LayerNodeStruct(kindKey, kind.label, holds(kindKey), true);
        LayerNodeStruct groupLayer = null;
        String group = null;

        markChosen(kindLayer, kindKey);

        for (int i = 0; i < slotNames.size(); i++) {

            String slotName = slotNames.get(i);
            String slotGroup = toGroup(slotName);

            if (!slotGroup.equals(group)) {

                group = slotGroup;
                String groupKey = toLayerKey(kind, group);
                groupLayer = new LayerNodeStruct(groupKey, LabelUtility.toWords(group), holds(groupKey), true);
                markChosen(groupLayer, groupKey);
                kindLayer.addChild(groupLayer);
            }

            String slotKey = toLayerKey(kind, slotName);

            groupLayer.addChild(new LayerNodeStruct(slotKey, toLayerLabel(kind, slotName), holds(slotKey), false));
        }

        return kindLayer;
    }

    private void markChosen(LayerNodeStruct layer, String key) {

        if (key.equals(layerKey))
            layer.markChosen();
    }

    // Whether a layer is the one picked or holds it
    private boolean holds(String key) {
        return layerKey.equals(key)
                || layerKey.startsWith(key + EditorSetting.ARCHITECTURE_BUILDER_LAYER_SEPARATOR)
                || layerKey.startsWith(key + EngineSetting.ARCHITECTURE_SLOT_SEPARATOR);
    }

    // A slot's name in the Layers tab, then what the architecture fills it with
    private String toLayerLabel(ArchitectureSlotKind kind, String slotName) {
        return toPlaceLabel(kind, slotName) + EditorSetting.ARCHITECTURE_BUILDER_LABEL_SEPARATOR
                + toFillLabel(kind, slotName);
    }

    // What the architecture fills a slot with, read as its name, or that it leaves the slot as authored
    private String toFillLabel(ArchitectureSlotKind kind, String slotName) {

        String filled = architectureSlotBranch.read(architectureName, kind, slotName);

        return filled == null ? toEmptyLabel(kind) : toValueLabel(kind, filled);
    }

    // Catalogue \\

    // Every slot of each kind, those structures name and those the architecture fills, sorted so groups lie together
    private void gatherSlots() {

        scratchNames.clear();

        ObjectArrayList<String> registered = architectureManager.getBlockSlotNames();

        for (int i = EngineSetting.REGISTRY_RESERVED_ID + 1; i < registered.size(); i++)
            scratchNames.add(registered.get(i));

        architectureSlotBranch.collect(architectureName, ArchitectureSlotKind.BLOCKS, scratchNames::add);
        fillSorted(blockSlotNames);

        furnishingManager.collectFurnishingNames("", scratchNames);
        architectureSlotBranch.collect(architectureName, ArchitectureSlotKind.FURNITURE, scratchNames::add);
        fillSorted(furnitureSlotNames);
    }

    private void fillSorted(ObjectArrayList<String> slotNames) {

        slotNames.clear();
        scratchNames.sort(null);

        for (int i = 0; i < scratchNames.size(); i++)
            if (slotNames.isEmpty() || !slotNames.get(slotNames.size() - 1).equals(scratchNames.get(i)))
                slotNames.add(scratchNames.get(i));

        scratchNames.clear();
    }

    // Preview \\

    // Every slot the layer picked takes in: the slot itself, or every slot of its group or its kind
    public void collectPreviewSlots(ObjectArrayList<String> out) {

        ArchitectureSlotKind kind = toKind(layerKey);
        String path = toSlotName(layerKey);
        ObjectArrayList<String> slotNames = kind.nested ? blockSlotNames : furnitureSlotNames;

        for (int i = 0; i < slotNames.size(); i++) {

            String slotName = slotNames.get(i);

            if (path.isEmpty() || slotName.equals(path)
                    || slotName.startsWith(path + EngineSetting.ARCHITECTURE_SLOT_SEPARATOR))
                out.add(toLayerKey(kind, slotName));
        }
    }

    // What a slot shows as: the piece of the block filling it, or the furniture filling it or else first drawn from
    // its table; null for a block slot left as authored or a name that is not content yet
    public ItemDefinitionHandle resolvePreviewItem(String slotKey) {

        ArchitectureSlotKind kind = toKind(slotKey);
        String slotName = toSlotName(slotKey);
        String filled = architectureSlotBranch.read(architectureName, kind, slotName);

        if (kind.nested)
            return resolveBlockPiece(filled);

        if (filled != null && itemDefinitionManager.hasItem(filled))
            return itemDefinitionManager.getItemHandleFromItemName(filled);

        return furnishingManager.hasFurnishing(slotName)
                ? furnishingManager.getFurnishingHandleFromFurnishingName(slotName).getItems()[0]
                : null;
    }

    private ItemDefinitionHandle resolveBlockPiece(String blockName) {

        if (blockName == null || !blockManager.hasBlock(blockName))
            return null;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);

        return blockHandle.hasPiece() ? itemDefinitionManager.getBlockPieceHandle(blockHandle.getBlockID()) : null;
    }

    public boolean isBlockSlot(String slotKey) {
        return toKind(slotKey).nested;
    }

    // File \\

    public void save() {

        if (!focusArchitecture())
            return;

        String message = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName)
                ? EditorSetting.ARCHITECTURE_BUILDER_MESSAGE_SAVED
                : EditorSetting.ARCHITECTURE_BUILDER_MESSAGE_SAVE_FAILED;

        setStatusMessage(message + architectureName);
    }

    public void revert() {

        if (!focusArchitecture())
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName);
        setStatusMessage(EditorSetting.ARCHITECTURE_BUILDER_MESSAGE_REVERTED + architectureName);
    }

    // Keys \\

    private ArchitectureSlotKind toKind(String key) {

        int separator = key.indexOf(EditorSetting.ARCHITECTURE_BUILDER_LAYER_SEPARATOR);

        return ArchitectureSlotKind.valueOf(separator < 0 ? key : key.substring(0, separator));
    }

    // The slot or group a layer key names, empty for a kind
    private String toSlotName(String key) {

        int separator = key.indexOf(EditorSetting.ARCHITECTURE_BUILDER_LAYER_SEPARATOR);

        return separator < 0 ? "" : key.substring(separator + 1);
    }

    private boolean isSlot(String key) {
        return toSlotName(key).indexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR) > 0;
    }

    private String toLayerKey(ArchitectureSlotKind kind, String path) {
        return kind.name() + EditorSetting.ARCHITECTURE_BUILDER_LAYER_SEPARATOR + path;
    }

    private String toGroup(String slotName) {
        return slotName.substring(0, slotName.lastIndexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR));
    }

    // Labels \\

    // A slot named as people say it: "siding/wall" is Siding Wall, "beds/KidsBunk" is Beds Kids Bunk
    private String toSlotLabel(ArchitectureSlotKind kind, String slotName) {
        return LabelUtility.toWords(toGroup(slotName)) + EditorSetting.INFO_LABEL_WORD_SEPARATOR
                + toPlaceLabel(kind, slotName);
    }

    private String toPlaceLabel(ArchitectureSlotKind kind, String slotName) {
        if (!kind.nested)
            return CharacterViewUtility.toResourceLabel(slotName);

        return LabelUtility.toWords(
                slotName.substring(slotName.lastIndexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR) + 1));
    }

    private String toValueLabel(ArchitectureSlotKind kind, String filled) {

        if (kind.nested)
            return blockManager.hasBlock(filled)
                    ? blockManager.getBlockHandleFromBlockName(filled).getLocalName()
                    : filled;

        return itemDefinitionManager.hasItem(filled)
                ? itemDefinitionManager.getItemHandleFromItemName(filled).getDisplayName()
                : filled;
    }

    private String toEmptyLabel(ArchitectureSlotKind kind) {
        return kind.nested
                ? EditorSetting.ARCHITECTURE_BUILDER_EMPTY_BLOCK
                : EditorSetting.ARCHITECTURE_BUILDER_EMPTY_FURNITURE;
    }

    // Status \\

    private void setStatusMessage(String message) {

        this.statusMessage = message;
        this.revision++;
    }

    private void notifyChanged() {

        gatherSlots();
        this.revision++;
    }

    // Accessible \\

    // The architecture worked on, null before one is chosen
    public String getArchitectureName() {
        return architectureName;
    }

    public String getLayerKey() {
        return layerKey;
    }

    // The layer picked read as words: a slot with what fills it, or the group or kind it is
    public String describeLayer() {

        ArchitectureSlotKind kind = toKind(layerKey);
        String path = toSlotName(layerKey);

        if (path.isEmpty())
            return kind.label;

        if (!isSlot(layerKey))
            return LabelUtility.toWords(path);

        return toSlotLabel(kind, path) + EditorSetting.ARCHITECTURE_BUILDER_LABEL_SEPARATOR + toFillLabel(kind, path);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getRevision() {
        return revision;
    }
}
