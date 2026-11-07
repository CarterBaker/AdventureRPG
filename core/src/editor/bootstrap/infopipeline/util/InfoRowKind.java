package editor.bootstrap.infopipeline.util;

public enum InfoRowKind {

    /*
     * How one row of the info panel is drawn: a VALUE typed in place, a
     * NUMBER on a slider with step buttons and a typed value, a CHOICE stepped
     * through every value the field may take (an enum's values, true and
     * false, or the entries and files a reference may name) and typed too, a
     * GROUP that expands into its children, or a MISSING optional field that
     * can be added.
     */

    VALUE,
    NUMBER,
    CHOICE,
    GROUP,
    MISSING
}
