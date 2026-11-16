package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceSectionStruct extends StructPackage {

    /*
     * One section of a creator tab: the name its header shows and the options
     * it lists, in order. A section with nothing to offer the character being
     * made is left out of the creator, header and all.
     */

    // Identity
    private final String sectionName;

    // Options
    private final AppearanceOptionStruct[] options;

    // Constructor \\

    public AppearanceSectionStruct(String sectionName, AppearanceOptionStruct[] options) {

        // Identity
        this.sectionName = sectionName;

        // Options
        this.options = options;
    }

    // Accessible \\

    public String getSectionName() {
        return sectionName;
    }

    public int getOptionCount() {
        return options.length;
    }

    public AppearanceOptionStruct getOption(int index) {
        return options[index];
    }
}
