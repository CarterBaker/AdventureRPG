package editor.bootstrap.infopipeline.util;

public enum InfoLocalStep {

    /*
     * One step of a local reference's path through its entry. FIELD reads a
     * key of an object; ANY_ELEMENT reads a key holding an array and goes on
     * into every element of it; SAME_ELEMENT goes on only into the element
     * the referring value itself sits in; MAP_KEYS ends the path on the keys
     * of a map.
     */

    FIELD,
    ANY_ELEMENT,
    SAME_ELEMENT,
    MAP_KEYS
}
