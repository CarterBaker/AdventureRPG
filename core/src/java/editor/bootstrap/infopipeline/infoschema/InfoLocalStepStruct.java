package editor.bootstrap.infopipeline.infoschema;

import editor.bootstrap.infopipeline.util.InfoLocalStep;
import engine.root.StructPackage;

public class InfoLocalStepStruct extends StructPackage {

    /*
     * One step of a local reference's path: the object key it reads and how
     * it goes on from the value found there.
     */

    // Identity
    private final String key;
    private final InfoLocalStep step;

    // Constructor \\

    public InfoLocalStepStruct(String key, InfoLocalStep step) {

        // Identity
        this.key = key;
        this.step = step;
    }

    // Accessible \\

    public String getKey() {
        return key;
    }

    public InfoLocalStep getStep() {
        return step;
    }
}
