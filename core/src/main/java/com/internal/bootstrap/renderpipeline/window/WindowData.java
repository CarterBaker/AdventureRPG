package com.internal.bootstrap.renderpipeline.window;

import com.internal.core.engine.DataPackage;

public class WindowData extends DataPackage {

        /*
         * Raw window state. Holds the window ID assigned at creation, current
         * dimensions, and display title. Created with new — owned by WindowInstance.
         */

        // Identity
        private final int windowID;
        private final String title;

        // Dimensions
        private int width;
        private int height;

        // Constructor \\

        public WindowData(
                        int windowID,
                        int width,
                        int height,
                        String title) {

                // Identity
                this.windowID = windowID;
                this.title = title;

                // Dimensions
                this.width = width;
                this.height = height;
        }

        // Accessible \\

        public int getWindowID() {
                return windowID;
        }

        public String getTitle() {
                return title;
        }

        public int getWidth() {
                return width;
        }

        public void setWidth(int width) {
                this.width = width;
        }

        public int getHeight() {
                return height;
        }

        public void setHeight(int height) {
                this.height = height;
        }
}