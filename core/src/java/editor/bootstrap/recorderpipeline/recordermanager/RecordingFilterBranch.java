package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.renderpipeline.photofilter.PhotoFilterHandle;
import application.bootstrap.renderpipeline.photofiltermanager.PhotoFilterManager;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingFilterBranch extends BranchPackage {

    /*
     * The photo filters a clip may hold: every look photo mode steps through
     * but the plain one, which lays nothing, in palette order, listed once
     * the first time they are asked for. A clip's filter is stepped through
     * none and then each filter in turn, or typed by its display or file name,
     * and a filter carried over the lanes is laid on the clip under the
     * cursor that shows a picture. The look a clip's filter lays is the
     * photo filter of its name.
     */

    // Internal
    private PhotoFilterManager photoFilterManager;

    // Filters
    private ObjectArrayList<PhotoFilterHandle> filters;

    // Base \\

    @Override
    protected void create() {

        // Filters
        this.filters = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.photoFilterManager = get(PhotoFilterManager.class);
    }

    // Filters \\

    // Every filter a clip may hold, in palette order
    ObjectArrayList<PhotoFilterHandle> getFilters() {

        if (!filters.isEmpty())
            return filters;

        short plainID = photoFilterManager.getPlainPhotoFilterID();

        for (int id = 1; id <= photoFilterManager.getPhotoFilterCount(); id++)
            if (id != plainID)
                filters.add(photoFilterManager.getPhotoFilterHandleFromPhotoFilterID((short) id));

        return filters;
    }

    // True when a clip may hold the filter of the name
    boolean isFilter(String filterName) {
        return findFilter(filterName) != EngineSetting.INDEX_NOT_FOUND;
    }

    // The look of the filter of the name, null for none or a name no clip may hold
    PhotoFilterHandle resolveHandle(String filterName) {

        int index = findFilter(filterName);

        return index != EngineSetting.INDEX_NOT_FOUND ? getFilters().get(index) : null;
    }

    // The filter's name as a row shows it: its display name, or none
    String describe(String filterName) {

        PhotoFilterHandle handle = resolveHandle(filterName);

        return handle != null ? handle.getDisplayName() : EditorSetting.RECORDER_FILTER_NONE;
    }

    // The filter a number of steps from the one given, through none and then every filter, round from the last
    String step(String filterName, int direction) {

        int choices = getFilters().size() + 1;
        int index = Math.floorMod(findFilter(filterName) + 1 + direction, choices);

        return index == 0 ? null : getFilters().get(index - 1).getPhotoFilterName();
    }

    // True when the text names none or a filter, by its display or its file name, ignoring case
    boolean isTyped(String text) {
        return isNoneTyped(text) || findTyped(text) != EngineSetting.INDEX_NOT_FOUND;
    }

    // The filter the text names, null for none; call only after isTyped
    String resolveTyped(String text) {
        return isNoneTyped(text) ? null : getFilters().get(findTyped(text)).getPhotoFilterName();
    }

    // The clip under the cursor a filter carried there would be laid on, one that shows a picture; -1 for none
    int findDropTarget(RecordingInstance recording, float time, int lane) {

        int clipIndex = recording.findClipAt(time, lane);

        if (clipIndex == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);

        return clip.showsPicture() ? clipIndex : EngineSetting.INDEX_NOT_FOUND;
    }

    // Utility \\

    private int findFilter(String filterName) {

        if (filterName == null)
            return EngineSetting.INDEX_NOT_FOUND;

        ObjectArrayList<PhotoFilterHandle> listed = getFilters();

        for (int i = 0; i < listed.size(); i++)
            if (listed.get(i).getPhotoFilterName().equals(filterName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private int findTyped(String text) {

        String typed = text.trim();
        ObjectArrayList<PhotoFilterHandle> listed = getFilters();

        for (int i = 0; i < listed.size(); i++)
            if (listed.get(i).getDisplayName().equalsIgnoreCase(typed)
                    || listed.get(i).getPhotoFilterName().equalsIgnoreCase(typed))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean isNoneTyped(String text) {
        return text.trim().equalsIgnoreCase(EditorSetting.RECORDER_FILTER_NONE);
    }
}
