package editor.profiler.panel;

import java.util.Comparator;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.profilerpipeline.profiler.ProfilerGpuStruct;
import application.kernel.profilerpipeline.profiler.ProfilerPhase;
import application.kernel.profilerpipeline.profiler.ProfilerSampleStruct;
import application.kernel.profilerpipeline.profiler.ProfilerStatisticsUtility;
import application.kernel.profilerpipeline.profiler.ProfilerSystemStruct;
import application.kernel.profilerpipeline.profiler.ProfilerThreadStruct;
import application.kernel.profilerpipeline.profilermanager.ProfilerManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.profiler.ProfilerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ProfilerPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the engine profiler.
     * Holds profiler sampling open for as long as the tab lives. A few times a
     * second it redraws the frame time graph, one bar per recent frame scaled
     * so the frame budget sits at mid height, and refills a fixed pool of rows
     * with the current view sorted heaviest first, rewriting only text that
     * changed. Pausing freezes the panel while the profiler keeps measuring.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ProfilerManager profilerManager;

    // Menus
    private MenuInstance profilerMenu;
    private ElementInstance statusLabel;
    private ElementInstance pauseLabel;
    private ElementInstance[] headerColumns;

    // Graph
    private ElementInstance[] barFills;
    private int[] barSteps;
    private boolean[] barsOverBudget;
    private DimensionVector2Struct[] barSizes;
    private MenuColorStruct withinBudgetColor;
    private MenuColorStruct overBudgetColor;
    private float budgetMillis;
    private float scaleMillis;

    // Rows
    private ElementInstance[][] rowColumns;
    private int rowCursor;

    // State
    private ProfilerView view;
    private boolean paused;
    private float refreshElapsed;

    // Scratch
    private float[] percentileScratch;
    private ObjectArrayList<ProfilerSystemStruct> systemScratch;
    private ObjectArrayList<ProfilerSampleStruct> sampleScratch;
    private ObjectArrayList<ProfilerGpuStruct> gpuScratch;

    // Sorting
    private static final Comparator<ProfilerSystemStruct> SYSTEM_ORDER = Comparator.comparingDouble(
            (ProfilerSystemStruct system) -> system.getTotal().getAverage()).reversed();
    private static final Comparator<ProfilerSampleStruct> SAMPLE_ORDER = Comparator.comparingDouble(
            ProfilerSampleStruct::getAverage).reversed();
    private static final Comparator<ProfilerGpuStruct> GPU_ORDER = Comparator.comparingDouble(
            (ProfilerGpuStruct record) -> record.getMillis().getAverage()).reversed();

    // Base \\

    @Override
    protected void create() {

        // Graph
        this.barFills = new ElementInstance[ProfilerSetting.GRAPH_BAR_COUNT];
        this.barSteps = new int[ProfilerSetting.GRAPH_BAR_COUNT];
        this.barsOverBudget = new boolean[ProfilerSetting.GRAPH_BAR_COUNT];
        this.barSizes = new DimensionVector2Struct[ProfilerSetting.GRAPH_HEIGHT_STEPS + 1];

        for (int step = 0; step <= ProfilerSetting.GRAPH_HEIGHT_STEPS; step++)
            this.barSizes[step] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(ProfilerSetting.GRAPH_BAR_WIDTH_PERCENT),
                    DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX * step
                            / ProfilerSetting.GRAPH_HEIGHT_STEPS));

        this.withinBudgetColor = new MenuColorStruct(ThemeColor.ACCENT, EngineSetting.COLOR_CHANNEL_MAX);
        this.overBudgetColor = new MenuColorStruct(ThemeColor.DANGER, EngineSetting.COLOR_CHANNEL_MAX);
        this.budgetMillis = EngineSetting.MILLIS_PER_SECOND_FLOAT / EngineSetting.TARGET_FRAME_RATE;
        this.scaleMillis = budgetMillis * ProfilerSetting.GRAPH_SCALE_BUDGETS;

        // Rows
        this.headerColumns = new ElementInstance[ProfilerSetting.COLUMNS_OVERVIEW.length];
        this.rowColumns = new ElementInstance[ProfilerSetting.ROW_COUNT][ProfilerSetting.COLUMNS_OVERVIEW.length];

        // State
        this.view = ProfilerView.OVERVIEW;
        this.refreshElapsed = ProfilerSetting.REFRESH_SECONDS;

        // Scratch
        this.percentileScratch = new float[EngineSetting.PROFILER_HISTORY_FRAMES];
        this.systemScratch = new ObjectArrayList<>();
        this.sampleScratch = new ObjectArrayList<>();
        this.gpuScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.profilerManager = get(ProfilerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.profilerMenu = menuManager.openMenu(ProfilerSetting.MENU_PROFILER, window);

        this.statusLabel = requireEntryPoint(ProfilerSetting.ENTRY_STATUS);
        this.pauseLabel = requireEntryPoint(ProfilerSetting.ENTRY_PAUSE_LABEL);

        resolveColumns(requireEntryPoint(ProfilerSetting.ENTRY_HEADER), headerColumns);
        requireEntryPoint(ProfilerSetting.ENTRY_GRAPH_LABEL).setFontText(String.format(
                ProfilerSetting.TEXT_GRAPH, scaleMillis, budgetMillis));

        buildGraph();
        buildRows();
        applyHeader();

        profilerManager.acquireSampling();
    }

    @Override
    protected void dispose() {
        profilerManager.releaseSampling();
    }

    // Build \\

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = profilerMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Profiler menu '" + ProfilerSetting.MENU_PROFILER + "' has no entry point " + entryPoint + ".");

        return element;
    }

    private void buildGraph() {

        requireEntryPoint(ProfilerSetting.ENTRY_GRAPH);

        for (int i = 0; i < ProfilerSetting.GRAPH_BAR_COUNT; i++) {

            ElementInstance bar = menuManager.inject(profilerMenu, ProfilerSetting.ENTRY_GRAPH, ProfilerSetting.MENU_BAR);

            barFills[i] = bar.findChildById(ProfilerSetting.ELEMENT_BAR_FILL);
            barFills[i].setSizeOverride(barSizes[0]);
            barFills[i].setColorOverride(withinBudgetColor);
        }
    }

    private void buildRows() {

        requireEntryPoint(ProfilerSetting.ENTRY_ROWS);

        for (int i = 0; i < ProfilerSetting.ROW_COUNT; i++)
            resolveColumns(
                    menuManager.inject(profilerMenu, ProfilerSetting.ENTRY_ROWS, ProfilerSetting.MENU_ROW),
                    rowColumns[i]);
    }

    private void resolveColumns(ElementInstance row, ElementInstance[] columns) {
        columns[0] = row.findChildById(ProfilerSetting.ELEMENT_COLUMN_NAME);
        columns[1] = row.findChildById(ProfilerSetting.ELEMENT_COLUMN_A);
        columns[2] = row.findChildById(ProfilerSetting.ELEMENT_COLUMN_B);
        columns[3] = row.findChildById(ProfilerSetting.ELEMENT_COLUMN_C);
        columns[4] = row.findChildById(ProfilerSetting.ELEMENT_COLUMN_D);
    }

    // Management \\

    public void showView(ProfilerView view) {
        this.view = view;
        applyHeader();
        refresh();
    }

    public void togglePause() {
        this.paused = !paused;
        writeText(pauseLabel, paused ? ProfilerSetting.TEXT_RESUME : ProfilerSetting.TEXT_PAUSE);
        refreshStatus();
    }

    public void capture() {
        profilerManager.beginCapture();
        refreshStatus();
    }

    private void applyHeader() {
        for (int i = 0; i < headerColumns.length; i++)
            writeText(headerColumns[i], view.columns[i]);
    }

    // Update \\

    @Override
    protected void update() {

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed < ProfilerSetting.REFRESH_SECONDS)
            return;

        this.refreshElapsed = 0f;
        refreshStatus();

        if (!paused)
            refresh();
    }

    private void refresh() {

        refreshGraph();

        this.rowCursor = 0;

        switch (view) {
            case OVERVIEW -> fillOverview();
            case SYSTEMS -> fillSystems();
            case GROUPS -> fillGroups();
            case GPU -> fillGpu();
            case THREADS -> fillThreads();
        }

        while (rowCursor < ProfilerSetting.ROW_COUNT)
            writeRow(ProfilerSetting.TEXT_EMPTY, ProfilerSetting.TEXT_EMPTY, ProfilerSetting.TEXT_EMPTY,
                    ProfilerSetting.TEXT_EMPTY, ProfilerSetting.TEXT_EMPTY);
    }

    // Status \\

    private void refreshStatus() {

        if (profilerManager.isCapturing()) {
            writeText(statusLabel, String.format(
                    ProfilerSetting.TEXT_STATUS_CAPTURING, profilerManager.getCaptureFramesRemaining()));
            return;
        }

        String report = profilerManager.getLastReportFile() != null
                ? ProfilerSetting.TEXT_STATUS_REPORT + profilerManager.getLastReportFile().getAbsolutePath()
                : ProfilerSetting.TEXT_STATUS_NO_REPORT;

        writeText(statusLabel, String.format(
                paused ? ProfilerSetting.TEXT_STATUS_PAUSED : ProfilerSetting.TEXT_STATUS_LIVE, report));
    }

    // Graph \\

    private void refreshGraph() {

        ProfilerSampleStruct frame = profilerManager.getFrameSample();

        for (int i = 0; i < ProfilerSetting.GRAPH_BAR_COUNT; i++) {

            float millis = frame.getHistory(ProfilerSetting.GRAPH_BAR_COUNT - 1 - i);
            int step = Math.round(Math.min(millis / scaleMillis, 1f)
                    * ProfilerSetting.GRAPH_HEIGHT_STEPS);
            boolean overBudget = millis > budgetMillis;

            if (step != barSteps[i]) {
                barFills[i].setSizeOverride(barSizes[step]);
                barSteps[i] = step;
            }

            if (overBudget != barsOverBudget[i]) {
                barFills[i].setColorOverride(overBudget ? overBudgetColor : withinBudgetColor);
                barsOverBudget[i] = overBudget;
            }
        }
    }

    // Views \\

    private void fillOverview() {

        ProfilerSampleStruct frame = profilerManager.getFrameSample();
        float frameAverage = frame.getAverage();

        writeRow(
                ProfilerSetting.OVERVIEW_FPS,
                formatFps(frameAverage),
                formatFps(percentile(frame)),
                ProfilerSetting.TEXT_EMPTY,
                formatFps(frame.getLast()));
        writeSampleRow(ProfilerSetting.OVERVIEW_FRAME, frame);

        writeSection(ProfilerSetting.OVERVIEW_CPU_SECTION);

        for (ProfilerPhase phase : ProfilerPhase.VALUES)
            writeSampleRow(
                    ProfilerSetting.OVERVIEW_PHASE_PREFIX + phase.displayName + ProfilerSetting.OVERVIEW_PHASE_SUFFIX,
                    profilerManager.getPhaseSample(phase));

        writeSection(ProfilerSetting.OVERVIEW_GPU_SECTION);

        for (ProfilerGpuStruct record : profilerManager.getGpuRecords())
            if (record.getLabel().equals(EngineSetting.PROFILER_GPU_FRAME_LABEL))
                writeSampleRow(ProfilerSetting.OVERVIEW_PHASE_PREFIX + record.getLabel(), record.getMillis());

        writeSection(ProfilerSetting.OVERVIEW_MEMORY_SECTION);
        writeSampleRow(ProfilerSetting.OVERVIEW_PHASE_PREFIX + EngineSetting.PROFILER_MEMORY_HEAP_USED,
                profilerManager.getHeapUsed());
        writeSampleRow(ProfilerSetting.OVERVIEW_PHASE_PREFIX + EngineSetting.PROFILER_MEMORY_HEAP_COMMITTED,
                profilerManager.getHeapCommitted());
        writeSampleRow(ProfilerSetting.OVERVIEW_PHASE_PREFIX + EngineSetting.PROFILER_MEMORY_GC_COLLECTIONS,
                profilerManager.getCollections());
        writeSampleRow(ProfilerSetting.OVERVIEW_PHASE_PREFIX + EngineSetting.PROFILER_MEMORY_GC_MILLIS,
                profilerManager.getCollectionMillis());

        writeSection(ProfilerSetting.OVERVIEW_COUNTER_SECTION);

        for (ProfilerSampleStruct counter : profilerManager.getCounters())
            writeSampleRow(ProfilerSetting.OVERVIEW_PHASE_PREFIX + counter.getName(), counter);
    }

    private void fillSystems() {

        float frameAverage = profilerManager.getFrameSample().getAverage();

        systemScratch.clear();
        systemScratch.addAll(profilerManager.getSystems());
        systemScratch.sort(SYSTEM_ORDER);

        for (int i = 0; i < systemScratch.size() && rowCursor < ProfilerSetting.ROW_COUNT; i++) {

            ProfilerSystemStruct system = systemScratch.get(i);
            ProfilerSampleStruct total = system.getTotal();

            if (total.getAverage() <= 0f)
                break;

            writeRow(
                    system.getName(),
                    formatMillis(total.getAverage()),
                    formatMillis(total.getPeak()),
                    formatShare(total.getAverage(), frameAverage),
                    system.getGroupName());
        }
    }

    private void fillGroups() {

        float frameAverage = profilerManager.getFrameSample().getAverage();

        sampleScratch.clear();
        sampleScratch.addAll(profilerManager.getGroups());
        sampleScratch.sort(SAMPLE_ORDER);

        for (int i = 0; i < sampleScratch.size() && rowCursor < ProfilerSetting.ROW_COUNT; i++) {

            ProfilerSampleStruct group = sampleScratch.get(i);

            writeRow(
                    group.getName(),
                    formatMillis(group.getAverage()),
                    formatMillis(group.getPeak()),
                    formatShare(group.getAverage(), frameAverage),
                    ProfilerSetting.TEXT_EMPTY);
        }
    }

    private void fillGpu() {

        gpuScratch.clear();
        gpuScratch.addAll(profilerManager.getGpuRecords());
        gpuScratch.sort(GPU_ORDER);

        for (int i = 0; i < gpuScratch.size() && rowCursor < ProfilerSetting.ROW_COUNT; i++) {

            ProfilerGpuStruct record = gpuScratch.get(i);

            writeRow(
                    record.getLabel(),
                    formatMillis(record.getMillis().getAverage()),
                    formatMillis(record.getMillis().getPeak()),
                    formatCount(record.getDrawCalls().getAverage()),
                    formatCount(record.getTriangles().getAverage()));
        }
    }

    private void fillThreads() {

        for (ProfilerThreadStruct thread : profilerManager.getThreads()) {

            if (rowCursor >= ProfilerSetting.ROW_COUNT)
                return;

            writeRow(
                    thread.getName(),
                    formatMillis(thread.getBusyMillis().getAverage()),
                    formatShare(thread.getUtilization().getAverage(), 1f),
                    formatDecimal(thread.getCompletedTasks().getAverage()),
                    formatDecimal(thread.getInFlight().getAverage()));
        }
    }

    // Rows \\

    private void writeSection(String title) {
        writeRow(title, ProfilerSetting.TEXT_EMPTY, ProfilerSetting.TEXT_EMPTY, ProfilerSetting.TEXT_EMPTY,
                ProfilerSetting.TEXT_EMPTY);
    }

    private void writeSampleRow(String name, ProfilerSampleStruct sample) {
        writeRow(
                name,
                formatDecimal(sample.getAverage()),
                formatDecimal(percentile(sample)),
                formatDecimal(sample.getPeak()),
                formatDecimal(sample.getLast()));
    }

    private void writeRow(String name, String a, String b, String c, String d) {

        if (rowCursor >= ProfilerSetting.ROW_COUNT)
            return;

        ElementInstance[] columns = rowColumns[rowCursor++];

        writeText(columns[0], name);
        writeText(columns[1], a);
        writeText(columns[2], b);
        writeText(columns[3], c);
        writeText(columns[4], d);
    }

    private void writeText(ElementInstance label, String text) {
        if (!text.equals(label.getText()))
            label.setFontText(text);
    }

    // Utility \\

    private float percentile(ProfilerSampleStruct sample) {
        return ProfilerStatisticsUtility.percentile(
                sample, EngineSetting.PROFILER_PERCENTILES[1], percentileScratch);
    }

    private String formatMillis(float millis) {
        return String.format(ProfilerSetting.FORMAT_MILLIS, millis);
    }

    private String formatDecimal(float value) {
        return String.format(ProfilerSetting.FORMAT_DECIMAL, value);
    }

    private String formatCount(float value) {
        return String.format(ProfilerSetting.FORMAT_COUNT, value);
    }

    private String formatFps(float frameMillis) {
        return String.format(
                ProfilerSetting.FORMAT_FPS,
                frameMillis > 0f ? EngineSetting.MILLIS_PER_SECOND_FLOAT / frameMillis : 0f);
    }

    private String formatShare(float part, float whole) {
        return String.format(ProfilerSetting.FORMAT_PERCENT, whole > 0f ? part / whole * EngineSetting.PERCENT_MAX : 0f);
    }
}
