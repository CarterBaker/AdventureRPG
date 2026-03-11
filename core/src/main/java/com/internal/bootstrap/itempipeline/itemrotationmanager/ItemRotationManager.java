package com.internal.bootstrap.itempipeline.itemrotationmanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

public class ItemRotationManager extends ManagerPackage {

    private UBOManager uboManager;
    private UBOHandle rotationUBOHandle;

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.rotationUBOHandle = uboManager.getUBOHandleFromUBOName(EngineSetting.ITEM_ROTATION_UBO);
    }

    public UBOHandle getRotationUBOHandle() {
        return rotationUBOHandle;
    }
}