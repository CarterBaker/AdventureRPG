package application.bootstrap.settingspipeline;

import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import engine.root.AssemblyPackage;

public class SettingsPipeline extends AssemblyPackage {

    @Override
    public void create() {
        create(SettingsSystem.class);
    }
}