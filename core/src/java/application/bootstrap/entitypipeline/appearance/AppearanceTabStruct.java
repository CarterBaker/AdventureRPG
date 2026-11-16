package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceTabStruct extends StructPackage {

    /*
     * One tab of a race's character creator: the name its button shows and
     * its sections, in the order the race lists them.
     */

    // Identity
    private final String tabName;

    // Sections
    private final AppearanceSectionStruct[] sections;

    // Constructor \\

    public AppearanceTabStruct(String tabName, AppearanceSectionStruct[] sections) {

        // Identity
        this.tabName = tabName;

        // Sections
        this.sections = sections;
    }

    // Accessible \\

    public String getTabName() {
        return tabName;
    }

    public int getSectionCount() {
        return sections.length;
    }

    public AppearanceSectionStruct getSection(int index) {
        return sections[index];
    }
}
