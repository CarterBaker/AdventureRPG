package editor.profiler;

public class ProfilerSetting {

    /*
     * Constants used only by ProfilerContext — its menu and templates, entry
     * points, the frame time graph, how many rows it shows and how often it
     * refreshes them, and the column titles and number formats of each view.
     */

    // Menus
    public static final String MENU_PROFILER = "editor/Profiler/Profiler";
    public static final String MENU_ROW = "editor/Profiler/profiler_row";
    public static final String MENU_BAR = "editor/Profiler/profiler_bar";

    // Elements
    public static final String ELEMENT_COLUMN_NAME = "profiler_column_name";
    public static final String ELEMENT_COLUMN_A = "profiler_column_a";
    public static final String ELEMENT_COLUMN_B = "profiler_column_b";
    public static final String ELEMENT_COLUMN_C = "profiler_column_c";
    public static final String ELEMENT_COLUMN_D = "profiler_column_d";
    public static final String ELEMENT_BAR_FILL = "profiler_bar_fill";

    // Entry Points
    public static final int ENTRY_GRAPH = 0;
    public static final int ENTRY_ROWS = 1;
    public static final int ENTRY_HEADER = 2;
    public static final int ENTRY_STATUS = 3;
    public static final int ENTRY_PAUSE_LABEL = 4;
    public static final int ENTRY_GRAPH_LABEL = 5;

    // Graph
    public static final int GRAPH_BAR_COUNT = 120;
    public static final float GRAPH_SCALE_BUDGETS = 2f;
    public static final float GRAPH_BAR_WIDTH_PERCENT = 70f;
    public static final int GRAPH_HEIGHT_STEPS = 100;

    // Rows
    public static final int ROW_COUNT = 60;
    public static final float REFRESH_SECONDS = 0.25f;

    // Text
    public static final String TEXT_EMPTY = "";
    public static final String TEXT_PAUSE = "Pause";
    public static final String TEXT_RESUME = "Resume";
    public static final String TEXT_STATUS_LIVE = "Live  |  %s";
    public static final String TEXT_STATUS_PAUSED = "Paused  |  %s";
    public static final String TEXT_STATUS_CAPTURING = "Capturing  |  %d frames left";
    public static final String TEXT_STATUS_REPORT = "Last report: ";
    public static final String TEXT_STATUS_NO_REPORT = "Capture writes a report to the Profiler folder";
    public static final String TEXT_GRAPH = "Frame time  |  scale %.1f ms  |  budget %.1f ms";

    // Formats
    public static final String FORMAT_MILLIS = "%.3f";
    public static final String FORMAT_PERCENT = "%.1f%%";
    public static final String FORMAT_COUNT = "%.0f";
    public static final String FORMAT_DECIMAL = "%.2f";
    public static final String FORMAT_FPS = "%.1f";

    // Overview
    public static final String OVERVIEW_FPS = "Frames per second";
    public static final String OVERVIEW_FRAME = "Frame time (ms)";
    public static final String OVERVIEW_PHASE_PREFIX = "  ";
    public static final String OVERVIEW_PHASE_SUFFIX = " (ms)";
    public static final String OVERVIEW_CPU_SECTION = "CPU phases";
    public static final String OVERVIEW_GPU_SECTION = "GPU";
    public static final String OVERVIEW_MEMORY_SECTION = "Memory";
    public static final String OVERVIEW_COUNTER_SECTION = "Counters";

    // Columns — name, a, b, c, d per view
    public static final String[] COLUMNS_OVERVIEW = { "Metric", "Average", "P95", "Peak", "Last" };
    public static final String[] COLUMNS_SYSTEMS = { "System (self time)", "Avg ms", "Peak ms", "Frame", "Group" };
    public static final String[] COLUMNS_GROUPS = { "Pipeline / Context", "Avg ms", "Peak ms", "Frame", "" };
    public static final String[] COLUMNS_GPU = { "GPU scope", "Avg ms", "Peak ms", "Draws", "Triangles" };
    public static final String[] COLUMNS_THREADS = { "Thread pool", "Busy ms", "Load", "Tasks", "In flight" };
}
