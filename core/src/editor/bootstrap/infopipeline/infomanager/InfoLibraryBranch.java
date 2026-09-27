package editor.bootstrap.infopipeline.infomanager;

import java.io.File;

import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InfoLibraryBranch extends BranchPackage {

    /*
     * Reads and writes info content on disk. A schema's files are every ARPG
     * file under its directory, named by their path relative to it — the same
     * names the game loads them under. A file that is not an ARPG object is
     * reported and left out rather than opened.
     */

    // Scan \\

    ObjectArrayList<String> scanDefinitionNames(InfoSchemaHandle schema) {

        File root = new File(schema.getDirectory());
        ObjectArrayList<String> definitionNames = new ObjectArrayList<>();

        if (!root.isDirectory())
            return definitionNames;

        ObjectArrayList<File> files = FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS);

        for (int i = 0; i < files.size(); i++)
            definitionNames.add(FileUtility.getPathWithFileNameWithoutExtension(root, files.get(i)));

        definitionNames.sort(String.CASE_INSENSITIVE_ORDER);
        return definitionNames;
    }

    // Load \\

    ArpgObjectStruct load(InfoSchemaHandle schema, String definitionName) {

        File file = getFile(schema, definitionName);
        ArpgObjectStruct root = ArpgUtility.tryLoadObject(file);

        if (root == null)
            errorLog("Info schema '" + schema.getSchemaName() + "' skipped a file that is not an ARPG object: "
                    + file.getAbsolutePath());

        return root;
    }

    // Save \\

    void save(InfoDocumentInstance document) {
        ArpgUtility.writeObject(
                getFile(document.getSchema(), document.getDefinitionName()),
                document.getRoot());
    }

    // Delete \\

    void delete(InfoDocumentInstance document) {

        File file = getFile(document.getSchema(), document.getDefinitionName());

        if (file.isFile() && !file.delete())
            throwException("Failed to delete info file: " + file.getAbsolutePath());
    }

    // Utility \\

    boolean exists(InfoSchemaHandle schema, String definitionName) {
        return getFile(schema, definitionName).exists();
    }

    private File getFile(InfoSchemaHandle schema, String definitionName) {
        return ArpgUtility.resolveFile(new File(schema.getDirectory()), definitionName);
    }
}
