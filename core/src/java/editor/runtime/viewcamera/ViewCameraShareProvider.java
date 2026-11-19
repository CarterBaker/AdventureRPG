package editor.runtime.viewcamera;

public interface ViewCameraShareProvider {

    /*
     * Names the kind of view a context's camera shares with the tabs stacked
     * with it (EditorSetting.VIEW_CAMERA_SHARE_*), none to keep its view its
     * own, and supplies that view. Only stacked tabs naming the same kind
     * share: tabs in separate docks always look independently.
     */

    String getViewCameraShare();

    ViewCameraStateStruct getViewCameraState();
}
