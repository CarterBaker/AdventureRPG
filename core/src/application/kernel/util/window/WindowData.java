package application.kernel.util.window;

import engine.root.DataPackage;

public class WindowData extends DataPackage {

        /*
         * Raw data payload for a WindowInstance. Stores identity and dimensions.
         * Width and height are mutable — updated by WindowInstance on resize.
         * Window ID is assigned at creation time and never changes.
         */

        // Identity
        private final int windowID;
        private final String title;

        // Dimensions
        private int width;
        private int height;

        // Internal \\

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