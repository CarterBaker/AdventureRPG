package editor.bootstrap.structureeditorpipeline.structureeditormanager;

import java.util.Objects;

import application.bootstrap.contentpipeline.contentmanager.ContentManager;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.SubBlockShape;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorLayer;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorPartStruct;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorStepStruct;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.label.LabelUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureEditorManager extends ManagerPackage {

    /*
     * Owns the Structure Editor's shared state: the structure painted, the
     * one the Hierarchy and the Palette tab pick, read from its entry as a
     * stack of layers (a floor, a roof, any alteration), each laying its own
     * cells, furniture and NPCs, the layer picked and, within it, the job
     * picked: Blocks, Coverage, Contents, Furniture or Entities, fixed in
     * every layer. Layers are added, removed, moved up and down, renamed and
     * shown or hidden as a painting tool's are; a higher layer wins a cell a
     * lower one also lays, and only shown layers are drawn and struck. The
     * Palette tab lists every structure, then what the job paints with: the
     * architectures the structure may be shown built in, the slots the one
     * chosen fills, the structure's own block entries and every block; every
     * covering or content after None; every furnishing table, the loot its
     * containers are filled with and the turn it stands at; every NPC. The
     * Brush lays the block chosen against the face struck, or paints every
     * cell of the layer within its Size that can take the covering or content
     * chosen, the covering at its Strength's level, or stands the furniture
     * or NPC chosen in the cell against the face; Erase takes away what the
     * job lays; Fill works every cell of the layer joined to the one struck
     * and laid alike; Pick takes up what the cell holds. No cell lies below
     * zero: building past the low edge shifts every layer and the origin up
     * together. Every stroke and layer change is one step of the editor's own
     * history, written into the entry through InfoManager.editEntry() when it
     * ends, so the file is saved and reverted as any content; any other
     * change to the entry reads the layers again.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private BlockManager blockManager;
    private CoveringManager coveringManager;
    private ContentManager contentManager;
    private FurnishingManager furnishingManager;
    private ItemDefinitionManager itemDefinitionManager;
    private SubVoxelManager subVoxelManager;
    private StructureDocumentBranch structureDocumentBranch;
    private StructurePaintBranch structurePaintBranch;
    private StructurePlaceBranch structurePlaceBranch;

    // Document — every layer, lowest first, the one picked, and the origin
    private String structureName;
    private ObjectArrayList<StructureEditorPartStruct> parts;
    private int partIndex;
    private int[] origin;
    private boolean committing;

    // Shown — every shown layer's cells, a higher layer's winning, made again whenever the revision moves
    private Long2ObjectOpenHashMap<ArpgObjectStruct> shownCells;
    private int shownRevision;

    // Choice
    private StructureEditorLayer layer;
    private String architectureName;
    private Object2ObjectOpenHashMap<String, String> slot2Block;
    private Object2ObjectOpenHashMap<String, String> furnishing2Item;
    private ArpgObjectStruct chosenEntry;
    private String chosenCovering;
    private String chosenContent;
    private String chosenFurnishing;
    private String chosenLoot;
    private int chosenSpin;
    private String chosenNpc;

    // Palette — the structure's own block entries as last listed, which their rows name by index
    private ObjectArrayList<ArpgObjectStruct> listedEntries;

    // Stroke
    private boolean stroking;
    private boolean strokeChanged;
    private long lastStrokeCell;
    private LongArrayList fillQueue;
    private LongOpenHashSet fillSeen;

    // History — everything before each step, and the steps undone
    private ObjectArrayList<StructureEditorStepStruct> undoSteps;
    private ObjectArrayList<StructureEditorStepStruct> redoSteps;

    // Status
    private String statusMessage;
    private int revision;
    private int listRevision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.structureDocumentBranch = create(StructureDocumentBranch.class);
        this.structurePaintBranch = create(StructurePaintBranch.class);
        this.structurePlaceBranch = create(StructurePlaceBranch.class);

        // Document
        this.parts = new ObjectArrayList<>();
        this.origin = new int[EngineSetting.AXIS_COUNT];

        // Shown
        this.shownCells = new Long2ObjectOpenHashMap<>();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        // Choice
        this.layer = StructureEditorLayer.BLOCKS;
        this.architectureName = "";
        this.slot2Block = new Object2ObjectOpenHashMap<>();
        this.furnishing2Item = new Object2ObjectOpenHashMap<>();
        this.chosenCovering = "";
        this.chosenContent = "";
        this.chosenFurnishing = "";
        this.chosenLoot = "";
        this.chosenNpc = "";

        // Palette
        this.listedEntries = new ObjectArrayList<>();

        // Stroke
        this.fillQueue = new LongArrayList();
        this.fillSeen = new LongOpenHashSet();

        // History
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();

        // Status
        this.statusMessage = "";
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.toolManager = get(ToolManager.class);
        this.blockManager = get(BlockManager.class);
        this.coveringManager = get(CoveringManager.class);
        this.contentManager = get(ContentManager.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    @Override
    protected void awake() {

        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_STRUCTURES, entry -> applySelection(entry.getEntryName()));
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_STRUCTURES, this::applyChange);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_STRUCTURES, this::applyDelete);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ARCHITECTURES, this::applyArchitectureChange);
    }

    // Selection \\

    // A structure picked in the Palette tab, opened in the Hierarchy and the Info Panel alike
    public void selectStructure(String selectedName) {

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_STRUCTURES, selectedName, selectedName);
        applySelection(selectedName);
    }

    private void applySelection(String selectedName) {

        if (Objects.equals(selectedName, structureName))
            return;

        endStroke();
        this.structureName = selectedName;
        undoSteps.clear();
        redoSteps.clear();
        readParts();
    }

    // The entry changed from anywhere but this editor read again, its history let go
    private void applyChange(InfoEntryStruct entry) {

        if (committing || !entry.getEntryName().equals(structureName))
            return;

        undoSteps.clear();
        redoSteps.clear();
        readParts();
    }

    private void applyDelete(InfoEntryStruct entry) {

        if (!entry.getEntryName().equals(structureName))
            return;

        this.structureName = null;
        parts.clear();
        notifyChanged();
    }

    private void applyArchitectureChange(InfoEntryStruct entry) {

        if (entry.getEntryName().equals(architectureName))
            chooseArchitecture(architectureName);
    }

    // Every layer read from the entry, the same one picked by name where it still stands, the top one otherwise
    private void readParts() {

        String pickedName = parts.isEmpty() ? null : part().getName();
        InfoEntryStruct entry = findEntry();

        parts.clear();

        if (entry != null)
            structureDocumentBranch.read(entry.getArpg(), parts, origin);

        this.partIndex = Math.max(0, parts.size() - 1);

        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).getName().equals(pickedName))
                this.partIndex = i;

        notifyChanged();
    }

    // The structure opened in the Hierarchy so the Info Panel's own New and Duplicate act on it — false with none
    public boolean focusStructure() {

        if (structureName == null) {
            setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_NONE);
            return false;
        }

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_STRUCTURES, structureName, structureName);
        return true;
    }

    // Layers \\

    // Every layer, top first, each a group holding its fixed jobs, its eye showing or hiding all it lays
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        StructureEditorLayer[] jobs = StructureEditorLayer.values();

        for (int i = parts.size() - 1; i >= 0; i--) {

            StructureEditorPartStruct part = parts.get(i);
            LayerNodeStruct group = new LayerNodeStruct(
                    part.getName(), part.getName(), i == partIndex, true, part.isShown(), i == partIndex);

            for (int j = 0; j < jobs.length; j++)
                group.addChild(new LayerNodeStruct(
                        toLayerKey(part, jobs[j]), jobs[j].label, i == partIndex && jobs[j] == layer, false));

            roots.add(group);
        }
    }

    // A layer picked keeps the job picked; a job picked is picked in its layer
    public void selectLayer(String layerKey) {

        int separator = layerKey.lastIndexOf(EditorSetting.STRUCTURE_EDITOR_LAYER_SEPARATOR);
        int picked = findPart(separator < 0 ? layerKey : layerKey.substring(0, separator));

        if (picked == EngineSetting.INDEX_NOT_FOUND)
            return;

        endStroke();
        this.partIndex = picked;

        if (separator >= 0)
            this.layer = StructureEditorLayer.valueOf(layerKey.substring(separator + 1));

        notifyChanged();
    }

    // A layer's eye: shown or hidden in the editor only, never in the game
    public void toggleLayerShown(String layerKey) {

        int picked = findPart(layerKey);

        if (picked == EngineSetting.INDEX_NOT_FOUND)
            return;

        parts.get(picked).setShown(!parts.get(picked).isShown());
        notifyChanged();
    }

    // A new empty layer standing just above the one picked, and picked
    public void addLayer() {

        if (structureName == null)
            return;

        beginStep();
        parts.add(partIndex + 1, new StructureEditorPartStruct(resolveFreeName()));
        this.partIndex++;
        this.strokeChanged = true;
        endStroke();
    }

    // The layer picked taken away with all it lays, the last layer always kept
    public void removeLayer() {

        if (structureName == null || parts.size() <= 1) {
            setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_LAST_LAYER);
            return;
        }

        beginStep();
        parts.remove(partIndex);
        this.partIndex = Math.min(partIndex, parts.size() - 1);
        this.strokeChanged = true;
        endStroke();
    }

    // The layer picked moved one place up or down the stack, staying picked
    public void moveLayer(int direction) {

        int target = partIndex + direction;

        if (structureName == null || target < 0 || target >= parts.size())
            return;

        beginStep();
        parts.add(target, parts.remove(partIndex));
        this.partIndex = target;
        this.strokeChanged = true;
        endStroke();
    }

    // Whether a name may be given to the layer picked: not empty, not another layer's, never holding the separator
    public boolean isLayerNameAvailable(String name) {
        return !name.isBlank() && name.indexOf(EditorSetting.STRUCTURE_EDITOR_LAYER_SEPARATOR) < 0
                && (findPart(name) == EngineSetting.INDEX_NOT_FOUND || findPart(name) == partIndex);
    }

    public void renameLayer(String name) {

        if (structureName == null || parts.isEmpty() || !isLayerNameAvailable(name))
            return;

        beginStep();
        part().setName(name.strip());
        this.strokeChanged = true;
        endStroke();
    }

    public String getLayerName() {
        return parts.isEmpty() ? "" : part().getName();
    }

    private String resolveFreeName() {

        int number = parts.size() + 1;

        while (findPart(EditorSetting.STRUCTURE_EDITOR_LAYER_NAME + EditorSetting.INFO_LABEL_WORD_SEPARATOR
                + number) != EngineSetting.INDEX_NOT_FOUND)
            number++;

        return EditorSetting.STRUCTURE_EDITOR_LAYER_NAME + EditorSetting.INFO_LABEL_WORD_SEPARATOR + number;
    }

    private int findPart(String name) {

        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).getName().equals(name))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private String toLayerKey(StructureEditorPartStruct part, StructureEditorLayer job) {
        return part.getName() + EditorSetting.STRUCTURE_EDITOR_LAYER_SEPARATOR + job.name();
    }

    private StructureEditorPartStruct part() {
        return parts.get(partIndex);
    }

    // Palette \\

    // Every structure, then what the job picked paints with
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_STRUCTURES));
        addEntryRows(rows, EditorSetting.INFO_SCHEMA_STRUCTURES, EditorSetting.STRUCTURE_EDITOR_ROW_STRUCTURE,
                structureName);

        if (structureName == null)
            return;

        switch (layer) {
            case BLOCKS -> addBlockRows(rows);
            case COVERAGE -> addChoiceRows(rows, EditorSetting.STRUCTURE_EDITOR_ROW_COVERING, chosenCovering);
            case CONTENTS -> addChoiceRows(rows, EditorSetting.STRUCTURE_EDITOR_ROW_CONTENT, chosenContent);
            case FURNITURE -> addFurnitureRows(rows);
            case ENTITIES -> addChoiceRows(rows, EditorSetting.STRUCTURE_EDITOR_ROW_NPC, chosenNpc);
        }
    }

    // None, then every entry of the job's schema
    private void addChoiceRows(ObjectArrayList<CharacterViewRowStruct> rows, String prefix, String chosen) {

        rows.add(CharacterViewRowStruct.header(layer.label));
        rows.add(CharacterViewRowStruct.item(prefix, EditorSetting.STRUCTURE_EDITOR_LABEL_NONE, chosen.isEmpty()));
        addEntryRows(rows, layer.schemaName, prefix, chosen);
    }

    // Every furnishing table, then the loot its containers are filled with, then the turn it stands at
    private void addFurnitureRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(layer.label));
        addEntryRows(rows, layer.schemaName, EditorSetting.STRUCTURE_EDITOR_ROW_FURNISHING, chosenFurnishing);

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_LOOT));
        rows.add(CharacterViewRowStruct.item(
                EditorSetting.STRUCTURE_EDITOR_ROW_LOOT, EditorSetting.STRUCTURE_EDITOR_LABEL_NONE,
                chosenLoot.isEmpty()));
        addEntryRows(rows, EditorSetting.INFO_SCHEMA_LOOT_TABLES, EditorSetting.STRUCTURE_EDITOR_ROW_LOOT, chosenLoot);

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_TURN));

        for (int spin = 0; spin < EngineSetting.ENCODED_FACE_SPIN_COUNT; spin++)
            rows.add(CharacterViewRowStruct.item(
                    EditorSetting.STRUCTURE_EDITOR_ROW_TURN + spin,
                    EditorSetting.STRUCTURE_EDITOR_TURN_LABELS[spin],
                    spin == chosenSpin));
    }

    // Every entry of a schema as a row naming it behind a prefix, the chosen one marked
    private void addEntryRows(
            ObjectArrayList<CharacterViewRowStruct> rows,
            String schemaName,
            String prefix,
            String chosen) {

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(schemaName);

        for (int i = 0; i < entries.size(); i++) {

            String name = entries.get(i).getEntryName();
            rows.add(CharacterViewRowStruct.item(prefix + name, name, name.equals(chosen)));
        }
    }

    // The architectures it may be shown built in, the slots the one chosen fills, its own entries and every block
    private void addBlockRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_ARCHITECTURES));
        rows.add(CharacterViewRowStruct.item(
                EditorSetting.STRUCTURE_EDITOR_ROW_ARCHITECTURE, EditorSetting.STRUCTURE_EDITOR_LABEL_AS_AUTHORED,
                architectureName.isEmpty()));
        addEntryRows(rows, EditorSetting.INFO_SCHEMA_ARCHITECTURES, EditorSetting.STRUCTURE_EDITOR_ROW_ARCHITECTURE,
                architectureName);

        if (!slot2Block.isEmpty()) {

            rows.add(CharacterViewRowStruct.header(architectureName + EditorSetting.STRUCTURE_EDITOR_PALETTE_SLOTS));

            ObjectArrayList<String> slots = new ObjectArrayList<>(slot2Block.keySet());
            slots.sort(null);

            for (int i = 0; i < slots.size(); i++)
                rows.add(CharacterViewRowStruct.item(
                        EditorSetting.STRUCTURE_EDITOR_ROW_SLOT + slots.get(i),
                        LabelUtility.toWords(slots.get(i).replace(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR,
                                EditorSetting.INFO_KEY_WORD_SEPARATOR))
                                + EditorSetting.STRUCTURE_EDITOR_LABEL_SEPARATOR
                                + toBlockLabel(slot2Block.get(slots.get(i))),
                        chosenEntry != null && slots.get(i).equals(ArpgUtility.getString(chosenEntry, "slot", ""))));
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_ENTRIES));
        gatherListedEntries();

        for (int i = 0; i < listedEntries.size(); i++)
            rows.add(CharacterViewRowStruct.item(
                    EditorSetting.STRUCTURE_EDITOR_ROW_ENTRY + i,
                    describeEntry(listedEntries.get(i)),
                    isChosen(listedEntries.get(i))));

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_BLOCKS));

        int blockIDCount = blockManager.getBlockIDCount();

        for (int blockID = EngineSetting.REGISTRY_RESERVED_ID + 1; blockID < blockIDCount; blockID++) {

            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockID);
            String name = blockHandle.getBlockName();

            rows.add(CharacterViewRowStruct.item(
                    EditorSetting.STRUCTURE_EDITOR_ROW_BLOCK + name, blockHandle.getLocalName(),
                    chosenEntry != null && chosenEntry.size() == 1
                            && name.equals(ArpgUtility.getString(chosenEntry, "block", ""))));
        }
    }

    // Every distinct block entry any layer lays, in the order first met
    private void gatherListedEntries() {

        listedEntries.clear();

        ObjectArrayList<String> texts = new ObjectArrayList<>();

        for (int i = 0; i < parts.size(); i++)
            for (ArpgObjectStruct entry : parts.get(i).getCells().values()) {

                String text = ArpgUtility.formatCompactText(entry);

                if (!texts.contains(text)) {
                    texts.add(text);
                    listedEntries.add(entry);
                }
            }
    }

    private boolean isChosen(ArpgObjectStruct entry) {
        return chosenEntry != null
                && ArpgUtility.formatCompactText(chosenEntry).equals(ArpgUtility.formatCompactText(entry));
    }

    // A Palette row chosen: a structure, an architecture to show it in, or what the job paints with
    public void choosePaletteRow(String rowKey) {

        if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_STRUCTURE))
            selectStructure(after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_STRUCTURE));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_ARCHITECTURE))
            chooseArchitecture(after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_ARCHITECTURE));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_SLOT))
            chooseSlot(after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_SLOT));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_ENTRY))
            chooseEntry(listedEntries.get(Integer.parseInt(after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_ENTRY))));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_BLOCK))
            chooseBlock(after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_BLOCK));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_COVERING))
            this.chosenCovering = after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_COVERING);
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_CONTENT))
            this.chosenContent = after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_CONTENT);
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_FURNISHING))
            this.chosenFurnishing = after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_FURNISHING);
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_LOOT))
            this.chosenLoot = after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_LOOT);
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_TURN))
            this.chosenSpin = Integer.parseInt(after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_TURN));
        else
            this.chosenNpc = after(rowKey, EditorSetting.STRUCTURE_EDITOR_ROW_NPC);

        notifyChanged();
    }

    private String after(String rowKey, String prefix) {
        return rowKey.substring(prefix.length());
    }

    // The architecture the structure is shown built in, empty to show it as authored: its block and furniture slots
    private void chooseArchitecture(String chosenName) {

        this.architectureName = chosenName;
        slot2Block.clear();
        furnishing2Item.clear();

        InfoEntryStruct entry = chosenName.isEmpty()
                ? null
                : infoManager.findEntry(EditorSetting.INFO_SCHEMA_ARCHITECTURES, chosenName, chosenName);

        if (entry != null && ArpgUtility.hasObject(entry.getArpg(), "blocks")) {

            ArpgObjectStruct blocksArpg = entry.getArpg().getAsObject("blocks");

            for (String group : blocksArpg.keySet())
                if (ArpgUtility.hasObject(blocksArpg, group))
                    for (String place : blocksArpg.getAsObject(group).keySet())
                        slot2Block.put(
                                group + EngineSetting.ARCHITECTURE_SLOT_SEPARATOR + place,
                                blocksArpg.getAsObject(group).get(place).getAsString());
        }

        if (entry != null && ArpgUtility.hasObject(entry.getArpg(), "furniture")) {

            ArpgObjectStruct furnitureArpg = entry.getArpg().getAsObject("furniture");

            for (String table : furnitureArpg.keySet())
                furnishing2Item.put(table, furnitureArpg.get(table).getAsString());
        }

        notifyChanged();
    }

    // A slot laid with the block the architecture fills it with, so the structure is authored in that look
    private void chooseSlot(String slotName) {

        ArpgObjectStruct entry = new ArpgObjectStruct();
        entry.addProperty("block", slot2Block.get(slotName));
        entry.addProperty("slot", slotName);
        chooseEntry(entry);
    }

    private void chooseBlock(String blockName) {

        ArpgObjectStruct entry = new ArpgObjectStruct();
        entry.addProperty("block", blockName);
        chooseEntry(entry);
    }

    private void chooseEntry(ArpgObjectStruct entry) {
        this.chosenEntry = entry;
    }

    // Tools \\

    // A press on the cell struck, or on the ground below the structure where nothing is struck
    public void pressTool(int cellX, int cellY, int cellZ, Direction3Vector face) {

        EditorTool tool = toolManager.getTool(ToolCanvas.STRUCTURE);

        if (structureName == null || parts.isEmpty() || tool == EditorTool.HAND)
            return;

        if (!part().isShown()) {
            setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_HIDDEN);
            return;
        }

        long cell = cellX >= 0 && cellY >= 0 && cellZ >= 0
                ? Coordinate3Long.pack(cellX, cellY, cellZ)
                : Long.MIN_VALUE;

        if (tool == EditorTool.PICK) {
            pick(cell);
            return;
        }

        beginStep();

        if (tool == EditorTool.FILL) {
            fill(cell);
            endStroke();
            return;
        }

        this.stroking = true;
        this.lastStrokeCell = Long.MIN_VALUE;
        dragTool(cellX, cellY, cellZ, face);
    }

    // The stroke carried on to the cell the pointer now strikes, each cell stamped once as the pointer reaches it
    public void dragTool(int cellX, int cellY, int cellZ, Direction3Vector face) {

        if (!stroking)
            return;

        boolean against = isBuilding() && face != null;
        int centerX = against ? cellX + face.x : cellX;
        int centerY = against ? cellY + face.y : cellY;
        int centerZ = against ? cellZ + face.z : cellZ;
        long center = Coordinate3Long.pack(Math.max(0, centerX), Math.max(0, centerY), Math.max(0, centerZ));

        if (center == lastStrokeCell)
            return;

        this.lastStrokeCell = center;
        stamp(centerX, centerY, centerZ);
    }

    public void releaseTool() {
        endStroke();
    }

    // Whether the tool puts something new against the face struck: the Brush laying blocks, furniture or NPCs
    private boolean isBuilding() {
        return toolManager.getTool(ToolCanvas.STRUCTURE) == EditorTool.BRUSH
                && (layer == StructureEditorLayer.BLOCKS || layer == StructureEditorLayer.FURNITURE
                        || layer == StructureEditorLayer.ENTITIES);
    }

    // Every cell within the Brush's Size of a centre given the tool's work, furniture and NPCs one cell alone; the
    // structure shifted up first where the tool builds past its low edge
    private void stamp(int centerX, int centerY, int centerZ) {

        boolean single = layer == StructureEditorLayer.FURNITURE || layer == StructureEditorLayer.ENTITIES;
        int reach = single
                ? 0
                : Math.max(0, Math.round(toolManager.getOption(ToolCanvas.STRUCTURE, ToolOption.SIZE)) - 1);

        if (isBuilding()) {

            int shiftX = Math.max(0, reach - centerX);
            int shiftY = Math.max(0, reach - centerY);
            int shiftZ = Math.max(0, reach - centerZ);

            shift(shiftX, shiftY, shiftZ);
            centerX += shiftX;
            centerY += shiftY;
            centerZ += shiftZ;
        }

        for (int y = centerY - reach; y <= centerY + reach; y++)
            for (int z = centerZ - reach; z <= centerZ + reach; z++)
                for (int x = centerX - reach; x <= centerX + reach; x++)
                    if (x >= 0 && y >= 0 && z >= 0)
                        apply(Coordinate3Long.pack(x, y, z));
    }

    // Every layer's cells and the origin moved up together, so furniture and NPCs, placed from the origin, hold
    private void shift(int shiftX, int shiftY, int shiftZ) {

        if (shiftX == 0 && shiftY == 0 && shiftZ == 0)
            return;

        for (int i = 0; i < parts.size(); i++) {

            Long2ObjectOpenHashMap<ArpgObjectStruct> cells = parts.get(i).getCells();
            Long2ObjectOpenHashMap<ArpgObjectStruct> shifted = new Long2ObjectOpenHashMap<>(cells.size());

            for (Long2ObjectOpenHashMap.Entry<ArpgObjectStruct> cell : cells.long2ObjectEntrySet())
                shifted.put(Coordinate3Long.pack(
                        Coordinate3Long.unpackX(cell.getLongKey()) + shiftX,
                        Coordinate3Long.unpackY(cell.getLongKey()) + shiftY,
                        Coordinate3Long.unpackZ(cell.getLongKey()) + shiftZ), cell.getValue());

            cells.clear();
            cells.putAll(shifted);
        }

        origin[0] += shiftX;
        origin[1] += shiftY;
        origin[2] += shiftZ;
        this.lastStrokeCell = Long.MIN_VALUE;
        this.strokeChanged = true;
    }

    // Every cell of the layer joined face to face to the one struck and laid alike given the tool's work
    private void fill(long start) {

        Long2ObjectOpenHashMap<ArpgObjectStruct> cells = part().getCells();
        ArpgObjectStruct match = cells.get(start);

        if (match == null || layer == StructureEditorLayer.FURNITURE || layer == StructureEditorLayer.ENTITIES)
            return;

        fillQueue.clear();
        fillSeen.clear();
        fillQueue.add(start);
        fillSeen.add(start);

        while (!fillQueue.isEmpty() && fillSeen.size() <= EngineSetting.STRUCTURE_MAX_BLOCK_COUNT) {

            long cell = fillQueue.removeLong(fillQueue.size() - 1);
            int x = Coordinate3Long.unpackX(cell);
            int y = Coordinate3Long.unpackY(cell);
            int z = Coordinate3Long.unpackZ(cell);

            for (int i = 0; i < Direction3Vector.VALUES.length; i++) {

                Direction3Vector direction = Direction3Vector.VALUES[i];

                if (x + direction.x < 0 || y + direction.y < 0 || z + direction.z < 0)
                    continue;

                long neighbour = Coordinate3Long.pack(x + direction.x, y + direction.y, z + direction.z);

                if (cells.get(neighbour) == match && fillSeen.add(neighbour))
                    fillQueue.add(neighbour);
            }
        }

        for (long cell : fillSeen)
            apply(cell);
    }

    // One cell given the tool's work on the job picked, in the layer picked
    private void apply(long cell) {

        EditorTool tool = toolManager.getTool(ToolCanvas.STRUCTURE);

        switch (layer) {
            case BLOCKS -> applyBlock(cell, tool);
            case COVERAGE, CONTENTS -> applyPaint(cell, tool);
            case FURNITURE -> applyFurniture(cell, tool);
            case ENTITIES -> applySpawn(cell, tool);
        }
    }

    // The Brush lays the block chosen in empty cells, Fill lays it over the cells filled, Erase takes blocks away
    private void applyBlock(long cell, EditorTool tool) {

        Long2ObjectOpenHashMap<ArpgObjectStruct> cells = part().getCells();
        ArpgObjectStruct entry = cells.get(cell);

        if (tool == EditorTool.ERASE) {

            if (entry != null) {
                cells.remove(cell);
                markChanged();
            }

            return;
        }

        if (chosenEntry == null) {
            setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_NO_CHOICE);
            return;
        }

        if ((tool == EditorTool.BRUSH && entry != null)
                || (entry == null && countCells() >= EngineSetting.STRUCTURE_MAX_BLOCK_COUNT))
            return;

        cells.put(cell, chosenEntry);
        markChanged();
    }

    // A covering or a content painted on, or taken from, a cell the layer lays
    private void applyPaint(long cell, EditorTool tool) {

        Long2ObjectOpenHashMap<ArpgObjectStruct> cells = part().getCells();
        ArpgObjectStruct entry = cells.get(cell);

        if (entry == null)
            return;

        ArpgObjectStruct painted = tool == EditorTool.ERASE
                ? structurePaintBranch.clear(layer, entry)
                : structurePaintBranch.paint(
                        layer, entry, layer == StructureEditorLayer.COVERAGE ? chosenCovering : chosenContent,
                        resolveCoverageLevel());

        if (painted == entry)
            return;

        cells.put(cell, painted);
        markChanged();
    }

    // The furniture chosen stood in the cell, any standing there before replaced; Erase takes it away
    private void applyFurniture(long cell, EditorTool tool) {

        ObjectArrayList<ArpgObjectStruct> furnishings = part().getFurnishings();
        boolean removed = structurePlaceBranch.remove(furnishings, cell, origin);

        if (tool == EditorTool.BRUSH) {

            if (chosenFurnishing.isEmpty()) {
                setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_NO_CHOICE);
                return;
            }

            furnishings.add(structurePlaceBranch.createFurnishing(
                    chosenFurnishing, chosenLoot, chosenSpin, cell, origin));
            removed = true;
        }

        if (removed)
            markChanged();
    }

    // The NPC chosen placed in the cell, any living there before replaced; Erase takes them away
    private void applySpawn(long cell, EditorTool tool) {

        ObjectArrayList<ArpgObjectStruct> spawns = part().getSpawns();
        boolean removed = structurePlaceBranch.remove(spawns, cell, origin);

        if (tool == EditorTool.BRUSH) {

            if (chosenNpc.isEmpty()) {
                setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_NO_CHOICE);
                return;
            }

            spawns.add(structurePlaceBranch.createSpawn(chosenNpc, cell, origin));
            removed = true;
        }

        if (removed)
            markChanged();
    }

    // What the cell struck holds in the layer taken up as the job's choice
    private void pick(long cell) {

        StructureEditorPartStruct part = part();

        switch (layer) {
            case BLOCKS -> pickEntry(part.getCells().get(cell));
            case COVERAGE -> pickValue(part.getCells().get(cell), true);
            case CONTENTS -> pickValue(part.getCells().get(cell), false);
            case FURNITURE -> pickFurnishing(structurePlaceBranch.find(part.getFurnishings(), cell, origin));
            case ENTITIES -> pickNpc(structurePlaceBranch.find(part.getSpawns(), cell, origin));
        }

        notifyChanged();
    }

    private void pickEntry(ArpgObjectStruct entry) {

        if (entry != null)
            chooseEntry(entry);
    }

    private void pickValue(ArpgObjectStruct entry, boolean covering) {

        if (entry == null)
            return;

        if (covering)
            this.chosenCovering = ArpgUtility.getString(entry, layer.field, "");
        else
            this.chosenContent = ArpgUtility.getString(entry, layer.field, "");
    }

    private void pickFurnishing(ArpgObjectStruct entry) {

        if (entry == null)
            return;

        this.chosenFurnishing = ArpgUtility.getString(entry, "furnishing", "");
        this.chosenLoot = ArpgUtility.getString(entry, "loot", "");
        this.chosenSpin = ArpgUtility.getInt(entry, "spin", 0);
    }

    private void pickNpc(ArpgObjectStruct entry) {

        if (entry != null)
            this.chosenNpc = ArpgUtility.getString(entry, "npc", "");
    }

    // The covering's level the Brush's Strength gives, never under one
    private int resolveCoverageLevel() {
        return Math.max(1, Math.round(
                toolManager.getOption(ToolCanvas.STRUCTURE, ToolOption.STRENGTH) * CoverageUtility.LEVEL_MAX));
    }

    private int countCells() {

        int count = 0;

        for (int i = 0; i < parts.size(); i++)
            count += parts.get(i).getCells().size();

        return count;
    }

    private void markChanged() {

        this.strokeChanged = true;
        notifyChanged();
    }

    // History \\

    private void beginStep() {

        undoSteps.add(new StructureEditorStepStruct(parts, origin, partIndex));
        redoSteps.clear();

        if (undoSteps.size() > EditorSetting.STRUCTURE_EDITOR_HISTORY_STEPS)
            undoSteps.remove(0);

        this.strokeChanged = false;
    }

    // The step ended and written into the entry, or forgotten when it changed nothing
    private void endStroke() {

        boolean ended = stroking || strokeChanged;

        this.stroking = false;

        if (!ended)
            return;

        if (!strokeChanged) {
            undoSteps.remove(undoSteps.size() - 1);
            notifyChanged();
            return;
        }

        this.strokeChanged = false;
        commit();
    }

    public void undo() {
        step(undoSteps, redoSteps, EditorSetting.STRUCTURE_EDITOR_MESSAGE_UNDONE);
    }

    public void redo() {
        step(redoSteps, undoSteps, EditorSetting.STRUCTURE_EDITOR_MESSAGE_REDONE);
    }

    private void step(
            ObjectArrayList<StructureEditorStepStruct> from,
            ObjectArrayList<StructureEditorStepStruct> to,
            String message) {

        endStroke();

        if (structureName == null || from.isEmpty())
            return;

        to.add(new StructureEditorStepStruct(parts, origin, partIndex));

        StructureEditorStepStruct step = from.remove(from.size() - 1);

        parts.clear();

        for (int i = 0; i < step.getParts().size(); i++)
            parts.add(new StructureEditorPartStruct(step.getParts().get(i)));

        System.arraycopy(step.getOrigin(), 0, origin, 0, EngineSetting.AXIS_COUNT);
        this.partIndex = Math.min(step.getPartIndex(), parts.size() - 1);
        commit();
        setStatusMessage(message);
    }

    // Every layer written into the structure's entry, the file marked changed and every window told
    private void commit() {

        if (findEntry() == null)
            return;

        this.committing = true;

        try {
            infoManager.editEntry(
                    EditorSetting.INFO_SCHEMA_STRUCTURES, structureName, structureName,
                    structureArpg -> {
                        if (!structureDocumentBranch.write(structureArpg, parts, origin))
                            setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_TOO_MANY);
                    });
        } finally {
            this.committing = false;
        }

        notifyChanged();
    }

    // File \\

    public void save() {

        if (!focusStructure())
            return;

        endStroke();

        String message = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_STRUCTURES, structureName)
                ? EditorSetting.STRUCTURE_EDITOR_MESSAGE_SAVED
                : EditorSetting.STRUCTURE_EDITOR_MESSAGE_SAVE_FAILED;

        setStatusMessage(message + structureName);
    }

    public void revert() {

        if (!focusStructure())
            return;

        endStroke();
        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_STRUCTURES, structureName);
        setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_REVERTED + structureName);
    }

    private InfoEntryStruct findEntry() {

        if (structureName == null || !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_STRUCTURES, structureName))
            return null;

        return infoManager.findEntry(EditorSetting.INFO_SCHEMA_STRUCTURES, structureName, structureName);
    }

    // Shown \\

    // Every shown layer's cells, a higher layer's winning a cell a lower one lays
    public Long2ObjectOpenHashMap<ArpgObjectStruct> getShownCells() {

        if (shownRevision == revision)
            return shownCells;

        this.shownRevision = revision;
        shownCells.clear();

        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).isShown())
                shownCells.putAll(parts.get(i).getCells());

        return shownCells;
    }

    // Whether the pointer's ray stops at a cell: a block of any shown layer, or furniture or an NPC the layer picked
    // places there while its job is picked
    public boolean strikes(long cell) {

        if (getShownCells().containsKey(cell))
            return true;

        if (parts.isEmpty() || !part().isShown())
            return false;

        return (layer == StructureEditorLayer.FURNITURE
                && structurePlaceBranch.find(part().getFurnishings(), cell, origin) != null)
                || (layer == StructureEditorLayer.ENTITIES
                        && structurePlaceBranch.find(part().getSpawns(), cell, origin) != null);
    }

    // Every shown layer, lowest first, for the furniture and NPCs it places
    public ObjectArrayList<StructureEditorPartStruct> getParts() {
        return parts;
    }

    // The block an entry stands in from the corner, Long.MIN_VALUE when it stands below it
    public long resolvePlacedCell(ArpgObjectStruct entry) {
        return structurePlaceBranch.resolveCell(entry, origin);
    }

    // The furniture standing in a furnished place: the architecture's own piece for its table, else the first its
    // table draws; null for a table that is not content yet
    public ItemDefinitionHandle resolveFurnitureItem(ArpgObjectStruct entry) {

        String table = ArpgUtility.getString(entry, "furnishing", "");
        String itemName = furnishing2Item.get(table);

        if (itemName != null && itemDefinitionManager.hasItem(itemName))
            return itemDefinitionManager.getItemHandleFromItemName(itemName);

        return furnishingManager.hasFurnishing(table)
                ? furnishingManager.getFurnishingHandleFromFurnishingName(table).getItems()[0]
                : null;
    }

    // The tile a cell is drawn with on the job picked: its covering's or content's where it holds one, else its
    // block's, laid as the architecture chosen fills its slot; INDEX_NOT_FOUND for a cell that draws nothing
    public int resolveCellTile(ArpgObjectStruct entry) {

        if (layer == StructureEditorLayer.COVERAGE && entry.has("covering")
                && coveringManager.hasCovering(entry.get("covering").getAsString()))
            return coveringManager.getCoveringHandleFromCoveringName(entry.get("covering").getAsString())
                    .getTopTileID();

        if (layer == StructureEditorLayer.CONTENTS && entry.has("content")
                && contentManager.hasContent(entry.get("content").getAsString()))
            return contentManager.getContentHandleFromContentName(entry.get("content").getAsString()).getTileID();

        String blockName = entry.has("slot") && slot2Block.containsKey(entry.get("slot").getAsString())
                ? slot2Block.get(entry.get("slot").getAsString())
                : ArpgUtility.getString(entry, "block", "");

        if (!blockManager.hasBlock(blockName))
            return EngineSetting.INDEX_NOT_FOUND;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);

        if (blockHandle.getGeometry() == DynamicGeometryType.NONE)
            return EngineSetting.INDEX_NOT_FOUND;

        return blockHandle.getItemTextureName() != null
                ? subVoxelManager.resolveTile(blockHandle.getItemTextureName())
                : blockHandle.getTextureForFace(Direction3Vector.NORTH);
    }

    // The tile an NPC's marker is drawn with
    public int resolveSpawnTile() {
        return subVoxelManager.resolveTile(EditorSetting.STRUCTURE_EDITOR_SPAWN_MARKER_TEXTURE);
    }

    // The octants a cell fills as authored
    public int resolveCellMask(ArpgObjectStruct entry) {
        return structurePaintBranch.resolveMask(entry);
    }

    // Labels \\

    // A block entry read as words: its block, then its shape, slot, covering and content where it names them
    private String describeEntry(ArpgObjectStruct entry) {

        StringBuilder label = new StringBuilder(toBlockLabel(ArpgUtility.getString(entry, "block", "")));
        String shape = ArpgUtility.getString(entry, "shape", SubBlockShape.FULL.name());

        if (!shape.equalsIgnoreCase(SubBlockShape.FULL.name()))
            appendLabel(label, LabelUtility.toWords(shape.toLowerCase()));

        appendLabel(label, ArpgUtility.getString(entry, "slot", ""));
        appendLabel(label, ArpgUtility.getString(entry, "covering", ""));
        appendLabel(label, ArpgUtility.getString(entry, "content", ""));

        return label.toString();
    }

    private void appendLabel(StringBuilder label, String part) {

        if (!part.isEmpty())
            label.append(EditorSetting.STRUCTURE_EDITOR_LABEL_SEPARATOR).append(part);
    }

    private String toBlockLabel(String blockName) {
        return blockManager.hasBlock(blockName)
                ? blockManager.getBlockHandleFromBlockName(blockName).getLocalName()
                : blockName;
    }

    // Status \\

    private void setStatusMessage(String message) {

        this.statusMessage = message;
        notifyChanged();
    }

    // The revision moved, and the lists' with it unless a stroke is under way, so the Palette and Layers tabs are
    // laid out again only once a stroke ends
    private void notifyChanged() {

        this.revision++;

        if (!stroking)
            this.listRevision = revision;
    }

    // Accessible \\

    // The structure painted, null before one is chosen
    public String getStructureName() {
        return structureName;
    }

    public StructureEditorLayer getLayer() {
        return layer;
    }

    // The architecture it is shown built in, empty while shown as authored
    public String getArchitectureName() {
        return architectureName;
    }

    // The origin on an axis, in blocks from the structure's corner
    public int getOrigin(int axis) {
        return origin[axis];
    }

    public int getCellCount() {
        return countCells();
    }

    public boolean isStroking() {
        return stroking;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    // Moves whenever the layers, a job, the architecture or a choice change, a stroke's stamps included
    public int getRevision() {
        return revision;
    }

    // Moves with the revision, but only once a stroke has ended
    public int getListRevision() {
        return listRevision;
    }
}
