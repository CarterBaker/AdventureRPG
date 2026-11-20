package editor.bootstrap.materialspipeline.materialsmanager;

import java.io.File;

import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

class MaterialDocumentBranch extends BranchPackage {

    /*
     * The materials of the workspace, as a texturing tool keeps its
     * materials apart from the engine that imports their maps: each one file
     * in the workspace's Materials folder naming the texture it is made from,
     * the texture any of its maps is taken from instead, keyed by the map's
     * alias, and its parameters by key. A key the file leaves out keeps its
     * default, so a material written before a parameter existed still opens.
     */

    // Directory
    private File materialRoot;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.materialRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.MATERIALS_WORKSPACE_DIRECTORY);
    }

    // Files \\

    File getFolder() {
        return materialRoot;
    }

    // The material a file in the Materials folder holds, by name; null for a file outside it
    String toMaterialName(File file) {
        return WorkspaceUtility.toName(materialRoot, file);
    }

    boolean isNameAvailable(String materialName) {
        return FileUtility.isValidFileName(materialName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !resolveFile(materialName).exists();
    }

    private File resolveFile(String materialName) {
        return ArpgUtility.resolveFile(materialRoot, materialName);
    }

    // Load \\

    // The material its file holds; null when the file cannot be read or does not hold a material
    MaterialDocumentStruct read(String materialName) {

        ArpgObjectStruct materialArpg = ArpgUtility.tryLoadObject(resolveFile(materialName));

        if (materialArpg == null)
            return null;

        try {
            return toDocument(materialName, materialArpg);
        } catch (InternalException e) {
            return null;
        }
    }

    private MaterialDocumentStruct toDocument(String materialName, ArpgObjectStruct materialArpg) {

        MaterialDocumentStruct document = new MaterialDocumentStruct(
                materialName,
                materialArpg.get(EditorSetting.MATERIALS_FIELD_BASE).getAsString());
        ArpgObjectStruct mapsArpg = materialArpg.has(EditorSetting.MATERIALS_FIELD_MAPS)
                ? materialArpg.getAsObject(EditorSetting.MATERIALS_FIELD_MAPS)
                : new ArpgObjectStruct();
        ArpgObjectStruct parametersArpg = materialArpg.has(EditorSetting.MATERIALS_FIELD_PARAMETERS)
                ? materialArpg.getAsObject(EditorSetting.MATERIALS_FIELD_PARAMETERS)
                : new ArpgObjectStruct();

        for (int m = 0; m < EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length; m++)
            document.setMapTexture(
                    m,
                    ArpgUtility.getString(mapsArpg, EditorSetting.CHARACTER_PAINT_MAP_ALIASES[m], ""));

        for (int p = 0; p < EditorSetting.MATERIALS_PARAMETER_KEYS.length; p++)
            document.setParameter(p, ArpgUtility.getFloat(
                    parametersArpg,
                    EditorSetting.MATERIALS_PARAMETER_KEYS[p],
                    EditorSetting.MATERIALS_PARAMETER_DEFAULT[p]));

        document.markSaved();
        return document;
    }

    // Save \\

    boolean write(MaterialDocumentStruct document) {

        File file = resolveFile(document.getMaterialName());

        file.getParentFile().mkdirs();
        return ArpgUtility.tryWriteObject(file, toArpg(document));
    }

    // The base, then only the maps that take a texture of their own, then every parameter
    private ArpgObjectStruct toArpg(MaterialDocumentStruct document) {

        ArpgObjectStruct materialArpg = new ArpgObjectStruct();
        ArpgObjectStruct mapsArpg = new ArpgObjectStruct();
        ArpgObjectStruct parametersArpg = new ArpgObjectStruct();

        for (int m = 0; m < EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length; m++)
            if (!document.getMapTexture(m).isEmpty())
                mapsArpg.addProperty(EditorSetting.CHARACTER_PAINT_MAP_ALIASES[m], document.getMapTexture(m));

        for (int p = 0; p < EditorSetting.MATERIALS_PARAMETER_KEYS.length; p++)
            parametersArpg.addProperty(EditorSetting.MATERIALS_PARAMETER_KEYS[p], document.getParameter(p));

        materialArpg.addProperty(EditorSetting.MATERIALS_FIELD_BASE, document.getBaseTexture());
        materialArpg.add(EditorSetting.MATERIALS_FIELD_MAPS, mapsArpg);
        materialArpg.add(EditorSetting.MATERIALS_FIELD_PARAMETERS, parametersArpg);

        return materialArpg;
    }

    // Delete \\

    boolean delete(String materialName) {

        File file = resolveFile(materialName);
        return !file.isFile() || file.delete();
    }
}
