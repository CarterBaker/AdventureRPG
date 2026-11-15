package editor.bootstrap.charactereditorpipeline.util;

import java.nio.ByteBuffer;

import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;

public class GltfDocumentStruct extends StructPackage {

    /*
     * One .glb file opened: the tree its JSON chunk parsed into and its
     * binary chunk, little-endian, which every accessor reads from in place.
     */

    // Internal
    private final ArpgObjectStruct json;
    private final ByteBuffer binary;

    // Constructor \\

    GltfDocumentStruct(ArpgObjectStruct json, ByteBuffer binary) {

        // Internal
        this.json = json;
        this.binary = binary;
    }

    // Accessible \\

    public ArpgObjectStruct getJson() {
        return json;
    }

    ByteBuffer getBinary() {
        return binary;
    }
}
