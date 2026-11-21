package application.runtime.menueventsmanager.menus;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.photofiltermanager.PhotoFilterManager;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.photo.ScreenshotSystem;
import application.runtime.postprocessing.PostEffectsSystem;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PhotoModeBranch extends BranchPackage {

    /*
     * Runs photo mode for this context's window, opened from the pause menu
     * and leaving back to it on Pause or Leave. The world stays paused behind
     * it as the pause menu left it. The camera leaves the player where they
     * stand and flies free near them (PlayerManager), the HUD gives way, and
     * the panel at the side steps the look laid over the scene (a photo
     * filter, exposure, contrast, saturation, vignette and grain, through
     * PostEffectsSystem), the field of view, the pose the player holds —
     * every pose made for their own rig, so each race and species holds its
     * own — and whether the player is in the picture. The Photo Panel binding
     * puts the panel away to fly the camera and brings it back; Take Photo or
     * the Screenshot binding takes the shot (ScreenshotSystem), never with the
     * panel in it.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private PlayerManager playerManager;
    private PhotoFilterManager photoFilterManager;
    private AnimationManager animationManager;
    private SettingsSystem settingsSystem;
    private PostEffectsSystem postEffectsSystem;
    private ScreenshotSystem screenshotSystem;
    private PauseMenuBranch pauseMenuBranch;

    // State
    private boolean active;
    private boolean closeArmed;
    private MenuInstance panel;

    // Look
    private short photoFilterID;
    private int poseIndex;
    private boolean playerHidden;
    private float[] values;
    private ObjectArrayList<AnimationClipHandle> poses;

    // Base \\

    @Override
    protected void create() {

        // Look
        this.values = new float[PhotoModeOption.VALUES.length];
        this.poses = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.playerManager = get(PlayerManager.class);
        this.photoFilterManager = get(PhotoFilterManager.class);
        this.animationManager = get(AnimationManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.postEffectsSystem = get(PostEffectsSystem.class);
        this.screenshotSystem = get(ScreenshotSystem.class);
        this.pauseMenuBranch = get(PauseMenuBranch.class);
    }

    @Override
    protected void update() {

        if (!active)
            return;

        WindowInstance window = context.getWindow();

        if (inputManager.bindingClicked(KeyBindings.PAUSE, window) && closeArmed) {
            leave(window);
            return;
        }

        if (inputManager.bindingClicked(KeyBindings.PHOTO_PANEL, window))
            togglePanel(window);

        this.closeArmed = true;
    }

    @Override
    protected void dispose() {
        if (active)
            playerManager.endPhotoMode(context.getWindow().getWindowID());
    }

    // Open / Close \\

    // From the pause menu: the pause menu gives way, the world staying paused, and photo mode opens on a plain look
    public void enter(WindowInstance window) {

        int windowID = window.getWindowID();

        if (active || !playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        pauseMenuBranch.handOver();
        playerManager.beginPhotoMode(windowID);
        animationManager.collectPoseClips(player.getAnimationStateHandle().getRigHandle(), poses);

        for (PhotoModeOption option : PhotoModeOption.VALUES)
            values[option.ordinal()] = option.getNeutral();

        values[PhotoModeOption.FIELD_OF_VIEW.ordinal()] = window.getActiveCamera().getFOV();
        this.photoFilterID = photoFilterManager.getPlainPhotoFilterID();
        this.poseIndex = EngineSetting.INDEX_NOT_FOUND;
        this.playerHidden = false;
        this.active = true;
        this.closeArmed = false;

        openPanel(window);
        applyLook(window);
    }

    // Back to the pause menu, everything photo mode laid over the view taken away
    public void leave(WindowInstance window) {

        if (!active)
            return;

        this.panel = menuManager.closeMenu(panel);
        this.active = false;

        playerManager.endPhotoMode(window.getWindowID());
        postEffectsSystem.clearPhotoLook();
        settingsSystem.applyFieldOfView();
        pauseMenuBranch.reopen(window);
    }

    public boolean isActive() {
        return active;
    }

    // The panel put away so the camera flies, or brought back
    private void togglePanel(WindowInstance window) {

        if (panel != null) {
            this.panel = menuManager.closeMenu(panel);
            return;
        }

        openPanel(window);
    }

    private void openPanel(WindowInstance window) {

        this.panel = menuManager.openMenu(RuntimeSetting.MENU_PHOTO_MODE, window);
        panel.getEntryPoint(RuntimeSetting.ENTRY_PHOTO_HINT).setFontText(String.format(
                RuntimeSetting.PHOTO_FORMAT_HINT,
                InputNameUtility.getName(KeyBindings.PHOTO_PANEL),
                InputNameUtility.getName(KeyBindings.SCREENSHOT),
                InputNameUtility.getName(KeyBindings.PAUSE)));
        refreshRows();
    }

    // Photo \\

    public void takePhoto() {
        screenshotSystem.request();
    }

    // Rows \\

    public void previousOption(String optionName, WindowInstance window) {
        stepOption(window, PhotoModeOption.valueOf(optionName), -1);
    }

    public void nextOption(String optionName, WindowInstance window) {
        stepOption(window, PhotoModeOption.valueOf(optionName), 1);
    }

    private void stepOption(WindowInstance window, PhotoModeOption option, int step) {

        if (!active)
            return;

        switch (option) {
            case FILTER -> this.photoFilterID = photoFilterManager.stepPhotoFilterID(photoFilterID, step);
            case POSE -> stepPose(window, step);
            case HIDE_PLAYER -> {
                this.playerHidden = !playerHidden;
                playerManager.setPhotoPlayerHidden(window.getWindowID(), playerHidden);
            }
            default -> values[option.ordinal()] = option.step(values[option.ordinal()], step);
        }

        applyLook(window);
        refreshRows();
    }

    // The pose after or before the one held, round through the stance the player was caught in
    private void stepPose(WindowInstance window, int step) {

        this.poseIndex = Math.floorMod(poseIndex + 1 + step, poses.size() + 1) - 1;
        playerManager.setPhotoPose(window.getWindowID(), poseIndex >= 0 ? poses.get(poseIndex) : null);
    }

    private void refreshRows() {

        if (panel == null)
            return;

        menuManager.ejectAll(panel, RuntimeSetting.ENTRY_PHOTO_ROWS);

        for (PhotoModeOption option : PhotoModeOption.VALUES)
            menuManager.inject(panel, RuntimeSetting.ENTRY_PHOTO_ROWS, RuntimeSetting.MENU_PHOTO_ROW, row -> {
                row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(option.getLabel());
                row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_VALUE).setFontText(formatOption(option));
                row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_PREVIOUS).setActionArgOverride(option.name());
                row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_NEXT).setActionArgOverride(option.name());
            });
    }

    private String formatOption(PhotoModeOption option) {
        return switch (option) {
            case FILTER -> photoFilterManager.getPhotoFilterHandleFromPhotoFilterID(photoFilterID).getDisplayName();
            case POSE -> poseIndex >= 0 ? formatPose(poses.get(poseIndex)) : RuntimeSetting.PHOTO_VALUE_NO_POSE;
            case HIDE_PLAYER -> playerHidden ? RuntimeSetting.SETTINGS_VALUE_ON : RuntimeSetting.SETTINGS_VALUE_OFF;
            case FIELD_OF_VIEW -> String.format(RuntimeSetting.SETTINGS_FORMAT_FIELD_OF_VIEW,
                    values[option.ordinal()]);
            default -> String.format(RuntimeSetting.SETTINGS_FORMAT_PERCENT,
                    values[option.ordinal()] * RuntimeSetting.SETTINGS_SLIDER_PERCENT_SCALE);
        };
    }

    // A pose clip's name as words: the last part of its path, split where its PascalCase starts a word
    private String formatPose(AnimationClipHandle pose) {

        String name = pose.getClipName();
        String last = name.substring(name.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);

        return last.replaceAll(RuntimeSetting.PHOTO_POSE_WORD_SPLIT, RuntimeSetting.PHOTO_POSE_WORD_SPACE);
    }

    // Look \\

    private void applyLook(WindowInstance window) {

        postEffectsSystem.setPhotoLook(
                photoFilterManager.getPhotoFilterHandleFromPhotoFilterID(photoFilterID),
                values[PhotoModeOption.EXPOSURE.ordinal()],
                values[PhotoModeOption.CONTRAST.ordinal()],
                values[PhotoModeOption.SATURATION.ordinal()],
                values[PhotoModeOption.VIGNETTE.ordinal()],
                values[PhotoModeOption.GRAIN.ordinal()]);
        window.getActiveCamera().setFOV(values[PhotoModeOption.FIELD_OF_VIEW.ordinal()]);
    }
}
