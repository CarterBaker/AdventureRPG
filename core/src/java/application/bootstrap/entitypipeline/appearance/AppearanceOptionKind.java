package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import engine.root.EngineSetting;

public enum AppearanceOptionKind {

    /*
     * What one entry of a race's creator layout offers the player: the race,
     * picked among the playable races, the gender, the build (height and
     * weight), the head's shape, one color with its tones, one detail amount,
     * one overlay in the style it is painted in with how far it shows, one
     * feature with its variant, or every slider of one group. A kind that
     * names one of a set names it by the member's lower-case enum name, is
     * offered only while the race's "player" block holds it so (true unless
     * it says otherwise), and has a section of its own the layout gains when
     * it leaves an offered member out; the sliders name their group and keep
     * their own "player".
     */

    RACE(null, null, null),
    GENDER(null, null, null),
    BUILD(null, null, null),
    HEAD(null, null, null),
    COLOR(SkinnedColor.VALUES, EngineSetting.APPEARANCE_PLAYER_COLORS, EngineSetting.APPEARANCE_CREATOR_SECTION_COLORS),
    DETAIL(SkinnedDetail.VALUES, EngineSetting.APPEARANCE_PLAYER_DETAILS,
            EngineSetting.APPEARANCE_CREATOR_SECTION_DETAILS),
    OVERLAY(SkinnedOverlay.VALUES, EngineSetting.APPEARANCE_PLAYER_OVERLAYS,
            EngineSetting.APPEARANCE_CREATOR_SECTION_OVERLAYS),
    FEATURE(FeatureSlot.VALUES, EngineSetting.APPEARANCE_PLAYER_FEATURES,
            EngineSetting.APPEARANCE_CREATOR_SECTION_FEATURES),
    SLIDERS(null, null, null);

    // Values
    public static final AppearanceOptionKind[] VALUES = values();

    // Internal
    private final Enum<?>[] members;
    private final String flagBlock;
    private final String leftoverSection;

    // Constructor \\

    AppearanceOptionKind(Enum<?>[] members, String flagBlock, String leftoverSection) {
        this.members = members;
        this.flagBlock = flagBlock;
        this.leftoverSection = leftoverSection;
    }

    // Accessible \\

    // Whether the kind names one member of a set, flagged in the race's "player" block
    public boolean isFlagged() {
        return members != null;
    }

    public Enum<?>[] getMembers() {
        return members;
    }

    public String getFlagBlock() {
        return flagBlock;
    }

    public String getLeftoverSection() {
        return leftoverSection;
    }
}
