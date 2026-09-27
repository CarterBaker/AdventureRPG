package editor.profiler.panel;

import editor.profiler.ProfilerSetting;

public enum ProfilerView {

    /*
     * The tables the profiler panel can show, each with its column titles:
     * the frame overview, every system's own time, each pipeline or context
     * rolled up, every GPU scope, and every thread pool's load.
     */

    OVERVIEW(ProfilerSetting.COLUMNS_OVERVIEW),
    SYSTEMS(ProfilerSetting.COLUMNS_SYSTEMS),
    GROUPS(ProfilerSetting.COLUMNS_GROUPS),
    GPU(ProfilerSetting.COLUMNS_GPU),
    THREADS(ProfilerSetting.COLUMNS_THREADS);

    public final String[] columns;

    ProfilerView(String[] columns) {
        this.columns = columns;
    }
}
