package application.bootstrap.settingspipeline;

import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import engine.root.PipelinePackage;

public class SettingsPipeline extends PipelinePackage {

    /*
     * Registers SettingsSystem, which applies and persists user settings.
     */

    @Override
    protected void create() {
        create(SettingsSystem.class);
    }
}