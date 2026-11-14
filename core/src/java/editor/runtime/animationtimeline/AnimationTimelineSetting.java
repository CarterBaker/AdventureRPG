package editor.runtime.animationtimeline;

public class AnimationTimelineSetting {

    /*
     * Constants used only by AnimationTimelineContext — its menu and the
     * entry points of its time, its summary track's keys and playhead and
     * its lanes, the lane and key marker templates and their elements, and
     * the fixed steps along a track keys and the playhead are placed in, so
     * moving them never allocates.
     */

    // Menus
    public static final String MENU_TIMELINE = "editor/AnimationTimeline/Timeline";
    public static final int ENTRY_TIME = 0;
    public static final int ENTRY_KEYS = 1;
    public static final int ENTRY_PLAYHEAD = 2;
    public static final int ENTRY_LANES = 3;

    // Templates
    public static final String TEMPLATE_LANE = "editor/AnimationTimeline/animation_timeline_lane";
    public static final String TEMPLATE_LANE_SELECTED = "editor/AnimationTimeline/animation_timeline_lane_selected";
    public static final String TEMPLATE_KEY_MARKER = "editor/AnimationTimeline/animation_timeline_key_marker";
    public static final String TEMPLATE_OTHER_KEY_MARKER =
            "editor/AnimationTimeline/animation_timeline_other_key_marker";

    // Elements
    public static final String ELEMENT_LANE_NAME = "animation_timeline_lane_name";
    public static final String ELEMENT_LANE_LABEL = "animation_timeline_lane_label";
    public static final String ELEMENT_LANE_TRACK = "animation_timeline_lane_track";
    public static final String ELEMENT_LANE_KEYS = "animation_timeline_lane_keys";

    // Track — positions along a track, as a percentage of it
    public static final int TRACK_STEPS = 1000;
    public static final float TRACK_PERCENT = 100f;
}
