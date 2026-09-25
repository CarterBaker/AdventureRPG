package editor.bootstrap.infopipeline.infomanager;

import java.io.File;
import java.util.List;

import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;

class InfoSchemaLoader extends LoaderPackage {

    /*
     * Scans the schema directory and builds every info schema into
     * InfoManager. The manager requests them all at once before its tabs are
     * registered, so the hierarchy always lists every schema.
     */

    // Internal
    private File root;
    private InfoManager infoManager;
    private InfoSchemaBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EditorSetting.INFO_SCHEMA_PATH);

        FileUtility.verifyDirectory(root, "Info schema directory not found: " + root.getAbsolutePath());

        List<File> schemaFiles = FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS);

        for (int i = 0; i < schemaFiles.size(); i++)
            fileQueue.offer(schemaFiles.get(i));
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InfoSchemaBuilder.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String schemaName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        InfoSchemaHandle schemaHandle = internalBuilder.build(file, schemaName);

        infoManager.addSchema(schemaHandle);
    }

    // On-Demand \\

    void requestAll() {

        File[] pendingFiles = fileQueue.toArray(new File[0]);

        for (int i = 0; i < pendingFiles.length; i++)
            request(pendingFiles[i]);
    }
}
