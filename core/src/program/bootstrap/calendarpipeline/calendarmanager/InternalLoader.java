package program.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import program.bootstrap.calendarpipeline.calendar.CalendarHandle;
import program.core.engine.LoaderPackage;
import program.core.settings.EngineSetting;
import program.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the calendar JSON directory and loads all calendar definitions into
     * CalendarManager. Supports on-demand loading for calendars not yet in the
     * palette at runtime.
     */

    // Internal
    private File root;
    private CalendarManager calendarManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> calendarName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.CALENDAR_JSON_PATH);
        this.calendarName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Calendar directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.getExtension(f).equals("json"))
                    .forEach(file -> {
                        String calendarName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        calendarName2File.put(calendarName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk calendar directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
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