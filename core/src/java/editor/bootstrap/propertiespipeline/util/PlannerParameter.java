package editor.bootstrap.propertiespipeline.util;

public interface PlannerParameter {

    /*
     * One number or choice a planner edits, one row of its node's Properties:
     * where it lies in the content's file — its group, empty for the file's
     * own fields, its key, and for one end of a range which end — the name it
     * is shown by, how far one step moves it, the bounds it is held within,
     * whether it is whole, the value it reads as while the file leaves it
     * out, and for a choice the enum whose names it cycles through. Every
     * implementation is an enum, the other end of a range among its own
     * constants, so PlannerFieldUtility finds it.
     */

    String name();

    String getGroup();

    String getKey();

    int getEnd();

    String getLabel();

    float getStep();

    float getMin();

    float getMax();

    boolean isWhole();

    float getFallback();

    Class<? extends Enum<?>> getChoices();

    boolean isRangeEnd();
}
