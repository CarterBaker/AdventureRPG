package application.kernel.windowpipeline.window;

import engine.root.DataPackage;

public class WindowData extends DataPackage {

        /*
         * Raw data payload for a WindowInstance. Stores identity, dimensions, and
         * whether a native OS window should be created for this instance.
         * createOSWindow defaults to true for all normal windows. Logical windows
         * (tabs) pass false — they are virtual windows that composite onto an OS window
         * and do not need their own native handle.
         */

        // Identity
        private final int windowID;
        private final String title;

        // Dimensions
        private int width;
        private int height;

        // Platform
        private final boolean createOSWindow;

        // Constructors \\

        public WindowData(int windowID, int width, int height, String title) {
                this.windowID = windowID;
                this.title = title;
                this.width = width;
                this.height = height;
                this.createOSWindow = true;
        }

        public WindowData(int windowID, int width, int height, String title, boolean createOSWindow) {
                this.windowID = windowID;
                this.title = title;
                this.width = width;
                this.height = height;
                this.createOSWindow = createOSWindow;
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

        public boolean shouldCreateOSWindow() {
                return createOSWindow;
        }
}