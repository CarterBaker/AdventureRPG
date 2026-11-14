package editor.runtime.properties.panel;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;

public interface PropertiesView {

    /*
     * One kind of content the Properties tab shows in its rows. Refreshing
     * fills the rows when what the view shows has moved since, or whenever
     * forced, as when it has just taken the rows over, and says whether it
     * filled them; the title is read only after a fill. Clearing takes every
     * row the view put there away, so the next view starts from nothing.
     */

    boolean refresh(PropertiesProvider provider, boolean force);

    String getTitle(PropertiesProvider provider);

    void clear();
}
