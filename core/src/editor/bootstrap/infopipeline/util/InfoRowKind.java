package editor.bootstrap.infopipeline.util;

public enum InfoRowKind {

    /*
     * How one row of the info panel is drawn: a VALUE edited in place, a
     * GROUP that expands into its children, or a MISSING optional field that
     * can be added.
     */

    VALUE,
    GROUP,
    MISSING
}
