package com.internal.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private CalendarManager calendarManager;
    private InternalBuilder internalBuilder;

    // Registry for on-demand loading
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
        }

        catch (IOException e) {
            throwException("CalendarLoader failed to walk directory: ", e);
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
            throwException("[CalendarLoader] On-demand load failed — not found in scan registry: \""
                    + calendarName + "\"");

        request(file);
    }
}