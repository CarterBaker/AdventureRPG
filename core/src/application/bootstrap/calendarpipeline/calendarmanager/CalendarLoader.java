package application.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CalendarLoader extends LoaderPackage {

    /*
     * Scans the calendar JSON directory and loads all calendar definitions into
     * CalendarManager. Supports on-demand loading for calendars not yet in the
     * palette at runtime.
     */

    // Internal
    private File root;
    private CalendarManager calendarManager;
    private CalendarBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> calendarName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.CALENDAR_JSON_PATH);
        this.calendarName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Calendar directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String calendarName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            calendarName2File.put(calendarName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(CalendarBuilder.class);
    }

    @Override
    protected void get() {
        this.calendarManager = get(CalendarManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String calendarName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        CalendarHandle calendarHandle = internalBuilder.build(file, calendarName);

        if (calendarHandle == null)
            throwException("Failed to build calendar from: " + file.getAbsolutePath());

        calendarManager.addCalendarHandle(calendarHandle);
    }

    // On-Demand \\

    void request(String calendarName) {

        File file = calendarName2File.get(calendarName);

        if (file == null)
            throwException("On-demand calendar load failed — not found in scan registry: \"" + calendarName + "\"");

        request(file);
    }
}