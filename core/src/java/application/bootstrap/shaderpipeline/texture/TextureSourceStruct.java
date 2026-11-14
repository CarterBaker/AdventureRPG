package application.bootstrap.shaderpipeline.texture;

import java.io.File;

import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureSourceStruct extends StructPackage {

    /*
     * The source images of one tile under textures/: its full name (pack and
     * tile), and each image file with the alias its suffix selects. The bake
     * reads it to build the tile's baked files.
     */

    // Identity
    private final String tileName;

    // Sources
    private final ObjectArrayList<File> files;
    private final ObjectArrayList<AliasStruct> aliases;

    // Constructor \\

    public TextureSourceStruct(String tileName) {
        this.tileName = tileName;
        this.files = new ObjectArrayList<>();
        this.aliases = new ObjectArrayList<>();
    }

    // Management \\

    public void addSource(File file, AliasStruct alias) {
        files.add(file);
        aliases.add(alias);
    }

    // Accessible \\

    public String getTileName() {
        return tileName;
    }

    public int getSourceCount() {
        return files.size();
    }

    public File getFile(int index) {
        return files.get(index);
    }

    public AliasStruct getAlias(int index) {
        return aliases.get(index);
    }
}
