package editor.bootstrap.structureeditorpipeline.structureeditormanager;

import java.util.Objects;

import application.bootstrap.contentpipeline.contentmanager.ContentManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.SubBlockShape;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorLayer;
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
     * one the Hierarchy and the Palette tab pick, its cells read from its
     * entry, the layer picked (Blocks, Coverage, Contents), the architecture
     * it is shown built in and what each layer paints with. The Palette tab
     * lists every structure, then for the Blocks layer the architectures, the
     * slots the one chosen fills, every block entry the structure lays and
     * every block, and for the Coverage and Contents layers every covering or
     * content after None. A press hands the tool's work to the cells: the
     * Brush lays the block chosen against the face struck, or paints every
     * cell within its Size that can take the covering or content chosen, the
     * covering at the level its Strength gives; Erase takes blocks away or
     * empties cells of their covering or content; Fill does the same to every
     * cell joined to the one struck that is laid alike; Pick takes up what the
     * cell struck holds. No cell ever lies below zero: a block laid past the
     * structure's low edge shifts every cell and the origin up together, so
     * every offset from the origin holds. A stroke is one step of the editor's own history and
     * is written into the entry through InfoManager.editEntry() when it ends,
     * so the file is saved and reverted as any content; any other change to
     * the entry reads the cells again.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private BlockManager blockManager;
    private CoveringManager coveringManager;
    private ContentManager contentManager;
    private SubVoxelManager subVoxelManager;
    private StructureDocumentBranch structureDocumentBranch;
    private StructurePaintBranch structurePaintBranch;

    // Document
    private String structureName;
    private Long2ObjectOpenHashMap<ArpgObjectStruct> cells;
    private int[] origin;
    private boolean committing;

    // Choice
    private StructureEditorLayer layer;
    private String architectureName;
    private Object2ObjectOpenHashMap<String, String> slot2Block;
    private ArpgObjectStruct chosenEntry;
    private String chosenCovering;
    private String chosenContent;

    // Palette — the structure's own block entries as last listed, which their rows name by index
    private ObjectArrayList<ArpgObjectStruct> listedEntries;

    // Stroke
    private boolean stroking;
    private boolean strokeChanged;
    private long lastStrokeCell;
    private LongArrayList fillQueue;
    private LongOpenHashSet fillSeen;

    // History — the cells and origin before each step, and the steps undone
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

        // Document
        this.cells = new Long2ObjectOpenHashMap<>();
        this.origin = new int[EngineSetting.AXIS_COUNT];

        // Choice
        this.layer = StructureEditorLayer.BLOCKS;
        this.architectureName = "";
        this.slot2Block = new Object2ObjectOpenHashMap<>();
        this.chosenCovering = "";
        this.chosenContent = "";

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
        readCells();
    }

    // The entry changed from anywhere but a stroke of this editor read again, its history let go
    private void applyChange(InfoEntryStruct entry) {

        if (committing || !entry.getEntryName().equals(structureName))
            return;

        undoSteps.clear();
        redoSteps.clear();
        readCells();
    }

    private void applyDelete(InfoEntryStruct entry) {

        if (!entry.getEntryName().equals(structureName))
            return;

        this.structureName = null;
        cells.clear();
        notifyChanged();
    }

    private void applyArchitectureChange(InfoEntryStruct entry) {

        if (entry.getEntryName().equals(architectureName))
            chooseArchitecture(architectureName);
    }

    private void readCells() {

        InfoEntryStruct entry = findEntry();

        if (entry != null)
            structureDocumentBranch.read(entry.getArpg(), cells, origin);
        else
            cells.clear();

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

    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        StructureEditorLayer[] layers = StructureEditorLayer.values();

        for (int i = 0; i < layers.length; i++)
            roots.add(new LayerNodeStruct(layers[i].name(), layers[i].label, layers[i] == layer, false));
    }

    public void selectLayer(String layerKey) {

        StructureEditorLayer selected = StructureEditorLayer.valueOf(layerKey);

        if (selected == layer)
            return;

        endStroke();
        this.layer = selected;
        notifyChanged();
    }

    // Palette \\

    // Every structure, then what the layer picked paints with
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.STRUCTURE_EDITOR_PALETTE_STRUCTURES));
        addEntryRows(rows, EditorSetting.INFO_SCHEMA_STRUCTURES, EditorSetting.STRUCTURE_EDITOR_ROW_STRUCTURE,
                structureName);

        if (structureName == null)
            return;

        if (layer == StructureEditorLayer.BLOCKS) {
            addBlockRows(rows);
            return;
        }

        String prefix = layer == StructureEditorLayer.COVERAGE
                ? EditorSetting.STRUCTURE_EDITOR_ROW_COVERING
                : EditorSetting.STRUCTURE_EDITOR_ROW_CONTENT;
        String chosen = layer == StructureEditorLayer.COVERAGE ? chosenCovering : chosenContent;

        rows.add(CharacterViewRowStruct.header(layer.label));
        rows.add(CharacterViewRowStruct.item(prefix, EditorSetting.STRUCTURE_EDITOR_LABEL_NONE, chosen.isEmpty()));
        addEntryRows(rows, layer.schemaName, prefix, chosen);
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

    // Every distinct block entry the structure lays, in the order first met
    private void gatherListedEntries() {

        listedEntries.clear();

        ObjectArrayList<String> texts = new ObjectArrayList<>();

        for (ArpgObjectStruct entry : cells.values()) {

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

    // A Palette row chosen: a structure to paint, an architecture to show it in, or what the layer paints with
    public void choosePaletteRow(String rowKey) {

        if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_STRUCTURE))
            selectStructure(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_STRUCTURE.length()));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_ARCHITECTURE))
            chooseArchitecture(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_ARCHITECTURE.length()));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_SLOT))
            chooseSlot(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_SLOT.length()));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_ENTRY))
            chooseEntry(listedEntries.get(
                    Integer.parseInt(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_ENTRY.length()))));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_BLOCK))
            chooseBlock(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_BLOCK.length()));
        else if (rowKey.startsWith(EditorSetting.STRUCTURE_EDITOR_ROW_COVERING))
            chooseCovering(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_COVERING.length()));
        else
            chooseContent(rowKey.substring(EditorSetting.STRUCTURE_EDITOR_ROW_CONTENT.length()));
    }

    // The architecture the structure is shown built in, empty to show it as authored
    private void chooseArchitecture(String chosenName) {

        this.architectureName = chosenName;
        slot2Block.clear();

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
        notifyChanged();
    }

    private void chooseCovering(String coveringName) {

        this.chosenCovering = coveringName;
        notifyChanged();
    }

    private void chooseContent(String contentName) {

        this.chosenContent = contentName;
        notifyChanged();
    }

    // Tools \\

    // A press on the cell struck, or on the ground below the structure where nothing is struck
    public void pressTool(int cellX, int cellY, int cellZ, Direction3Vector face) {

        EditorTool tool = toolManager.getTool(ToolCanvas.STRUCTURE);

        if (structureName == null || tool == EditorTool.HAND)
            return;

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

        boolean building = layer == StructureEditorLayer.BLOCKS
                && toolManager.getTool(ToolCanvas.STRUCTURE) == EditorTool.BRUSH;
        int centerX = building && face != null ? cellX + face.x : cellX;
        int centerY = building && face != null ? cellY + face.y : cellY;
        int centerZ = building && face != null ? cellZ + face.z : cellZ;
        long center = Coordinate3Long.pack(centerX, centerY, centerZ);

        if (center == lastStrokeCell)
            return;

        this.lastStrokeCell = center;
        stamp(centerX, centerY, centerZ);
    }

    public void releaseTool() {
        endStroke();
    }

    // Every cell within the Brush's Size of a centre given the tool's work, the structure shifted up first where
    // the Brush builds past its low edge
    private void stamp(int centerX, int centerY, int centerZ) {

        int reach = Math.max(0, Math.round(toolManager.getOption(ToolCanvas.STRUCTURE, ToolOption.SIZE)) - 1);

        if (layer == StructureEditorLayer.BLOCKS && toolManager.getTool(ToolCanvas.STRUCTURE) == EditorTool.BRUSH) {

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

    // Every cell and the origin moved up together, the stroke's last cell with them
    private void shift(int shiftX, int shiftY, int shiftZ) {

        if (shiftX == 0 && shiftY == 0 && shiftZ == 0)
            return;

        Long2ObjectOpenHashMap<ArpgObjectStruct> shifted = new Long2ObjectOpenHashMap<>(cells.size());

        for (Long2ObjectOpenHashMap.Entry<ArpgObjectStruct> cell : cells.long2ObjectEntrySet())
            shifted.put(Coordinate3Long.pack(
                    Coordinate3Long.unpackX(cell.getLongKey()) + shiftX,
                    Coordinate3Long.unpackY(cell.getLongKey()) + shiftY,
                    Coordinate3Long.unpackZ(cell.getLongKey()) + shiftZ), cell.getValue());

        cells.clear();
        cells.putAll(shifted);
        origin[0] += shiftX;
        origin[1] += shiftY;
        origin[2] += shiftZ;
        this.lastStrokeCell = Long.MIN_VALUE;
        this.strokeChanged = true;
    }

    // Every cell joined face to face to the one struck and laid alike given the tool's work
    private void fill(long start) {

        ArpgObjectStruct match = cells.get(start);

        if (match == null)
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

                long neighbour = Coordinate3Long.pack(
                        x + direction.x, y + direction.y, z + direction.z);

                if (cells.get(neighbour) == match && fillSeen.add(neighbour))
                    fillQueue.add(neighbour);
            }
        }

        for (long cell : fillSeen)
            apply(cell);
    }

    // One cell given the tool's work on the layer picked
    private void apply(long cell) {

        EditorTool tool = toolManager.getTool(ToolCanvas.STRUCTURE);
        ArpgObjectStruct entry = cells.get(cell);

        if (layer == StructureEditorLayer.BLOCKS) {
            applyBlock(cell, entry, tool);
            return;
        }

        if (entry == null)
            return;

        ArpgObjectStruct painted = tool == EditorTool.ERASE
                ? structurePaintBranch.clear(layer, entry)
                : structurePaintBranch.paint(layer, entry, resolveChoice(), resolveCoverageLevel());

        if (painted != entry)
            setCell(cell, painted);
    }

    // The Brush lays the block chosen in empty cells, Fill lays it over the cells filled, Erase takes blocks away
    private void applyBlock(long cell, ArpgObjectStruct entry, EditorTool tool) {

        if (tool == EditorTool.ERASE) {

            if (entry != null) {
                cells.remove(cell);
                this.strokeChanged = true;
                notifyChanged();
            }

            return;
        }

        if (chosenEntry == null) {
            setStatusMessage(EditorSetting.STRUCTURE_EDITOR_MESSAGE_NO_BLOCK);
            return;
        }

        if (tool == EditorTool.BRUSH && entry != null)
            return;

        if (cells.size() >= EngineSetting.STRUCTURE_MAX_BLOCK_COUNT && entry == null)
            return;

        setCell(cell, chosenEntry);
    }

    private void setCell(long cell, ArpgObjectStruct entry) {

        cells.put(cell, entry);
        this.strokeChanged = true;
        notifyChanged();
    }

    // What the cell struck holds taken up as the layer's choice
    private void pick(long cell) {

        ArpgObjectStruct entry = cells.get(cell);

        if (entry == null)
            return;

        if (layer == StructureEditorLayer.BLOCKS)
            chooseEntry(entry);
        else if (layer == StructureEditorLayer.COVERAGE)
            chooseCovering(ArpgUtility.getString(entry, layer.field, ""));
        else
            chooseContent(ArpgUtility.getString(entry, layer.field, ""));
    }

    private String resolveChoice() {
        return layer == StructureEditorLayer.COVERAGE ? chosenCovering : chosenContent;
    }

    // The covering's level the Brush's Strength gives, never under one
    private int resolveCoverageLevel() {
        return Math.max(1, Math.round(
                toolManager.getOption(ToolCanvas.STRUCTURE, ToolOption.STRENGTH) * CoverageUtility.LEVEL_MAX));
    }

    // History \\

    private void beginStep() {

        undoSteps.add(new StructureEditorStepStruct(cells, origin));
        redoSteps.clear();

        if (undoSteps.size() > EditorSetting.STRUCTURE_EDITOR_HISTORY_STEPS)
            undoSteps.remove(0);

        this.strokeChanged = false;
    }

    // The stroke ended and written into the entry, or forgotten when it changed nothing
    private void endStroke() {

        boolean ended = stroking || strokeChanged;

        this.stroking = false;

        if (!ended)
            return;

        if (!strokeChanged) {
            undoSteps.remove(undoSteps.size() - 1);
            return;
        }

        this.strokeChanged = false;
        commit();
    }

    // Moves with the revision, but only once a stroke has ended
    public int getListRevision() {
        return listRevision;
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

        to.add(new StructureEditorStepStruct(cells, origin));

        StructureEditorStepStruct step = from.remove(from.size() - 1);

        cells.clear();
        cells.putAll(step.getCells());
        System.arraycopy(step.getOrigin(), 0, origin, 0, EngineSetting.AXIS_COUNT);
        commit();
        setStatusMessage(message);
    }

    // The cells written into the structure's entry, the file marked changed and every window told
    private void commit() {

        if (findEntry() == null)
            return;

        this.committing = true;

        try {
            infoManager.editEntry(
                    EditorSetting.INFO_SCHEMA_STRUCTURES, structureName, structureName,
                    structureArpg -> {
                        if (!structureDocumentBranch.write(structureArpg, cells, origin))
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

    // The tile a cell is drawn with on the layer picked: its covering's or content's where it holds one, else its
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
        return blockManager.hasBlock(blockName) ? blockManager.getBlockHandleFromBlockName(blockName).getLocalName()
                : blockName;
    }

    // Status \\

    private void setStatusMessage(String message) {

        this.statusMessage = message;
        this.revision++;
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

    // Every cell laid, keyed by its position from the structure's corner through Coordinate3Long
    public Long2ObjectOpenHashMap<ArpgObjectStruct> getCells() {
        return cells;
    }

    public StructureEditorLayer getLayer() {
        return layer;
    }

    // The architecture it is shown built in, empty while shown as authored
    public String getArchitectureName() {
        return architectureName;
    }

    public boolean isStroking() {
        return stroking;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    // Moves whenever the cells, the layer, the architecture or a choice change, a stroke's stamps included
    public int getRevision() {
        return revision;
    }
}
