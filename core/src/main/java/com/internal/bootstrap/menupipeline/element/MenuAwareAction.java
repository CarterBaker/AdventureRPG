package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.menupipeline.menu.MenuInstance;

@FunctionalInterface
public interface MenuAwareAction {
    void execute(MenuInstance parent);
}