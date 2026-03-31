package com.internal.platform.files;

import java.io.File;

public class FileHandle {
    private final File file;

    public FileHandle(String path) { this.file = new File(path); }
    public FileHandle(File file) { this.file = file; }

    public File file() { return file; }
    public String path() { return file.getPath(); }
}
