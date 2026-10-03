package editor.bootstrap.imagepipeline.imagemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.runtime.EditorSetting;
import engine.assets.image.PixmapUtility;
import engine.root.BranchPackage;

class ImageLibraryBranch extends BranchPackage {

    /*
     * Reads and writes images on disk. Saving writes a temporary file beside
     * the image and moves it into place, so a failed save never leaves a
     * broken image; reloading reads the file back into the document's own
     * pixels, which only works while the file keeps the image's size. Both
     * report failure instead of stopping the editor.
     */

    // Save \\

    boolean save(ImageDocumentInstance document) {

        File file = document.getFile();
        File temporary = new File(file.getPath() + EditorSetting.IMAGE_TEMP_SUFFIX);

        try {

            if (!ImageIO.write(
                    PixmapUtility.toBufferedImage(document.getPixmap()), EditorSetting.IMAGE_FORMAT, temporary)) {
                errorLog("No image writer for format '" + EditorSetting.IMAGE_FORMAT + "': " + file.getAbsolutePath());
                return false;
            }

            Files.move(
                    temporary.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            errorLog("Failed to save image: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    // Reload \\

    boolean reload(ImageDocumentInstance document) {

        File file = document.getFile();
        BufferedImage image;

        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            errorLog("Failed to read image: " + file.getAbsolutePath(), e);
            return false;
        }

        if (image == null || image.getWidth() != document.getWidth() || image.getHeight() != document.getHeight()) {
            errorLog("Image no longer matches the open image's size: " + file.getAbsolutePath());
            return false;
        }

        PixmapUtility.copyPixels(PixmapUtility.fromBufferedImage(image, false), document.getPixmap());
        return true;
    }
}
