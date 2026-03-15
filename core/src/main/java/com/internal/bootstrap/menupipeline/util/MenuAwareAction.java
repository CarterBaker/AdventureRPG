package com.internal.bootstrap.menupipeline.util;

import com.internal.bootstrap.menupipeline.menu.MenuInstance;

@FunctionalInterface
public interface MenuAwareAction {

    /*
     * Click action variant that receives the owning MenuInstance so the
     * action can close or manipulate its parent menu at runtime.
     */

    void execute(MenuInstance parent);
}