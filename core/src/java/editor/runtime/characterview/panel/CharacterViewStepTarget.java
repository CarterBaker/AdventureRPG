package editor.runtime.characterview.panel;

public interface CharacterViewStepTarget {

    /*
     * Takes the steps a number row's slider asks for: the value under a key
     * moved by a whole number of the tool's own steps, the same path its step
     * buttons take, so a slider drags any value the tool can step, held to
     * whatever range the tool keeps it in.
     */

    void stepRow(String key, int steps);
}
