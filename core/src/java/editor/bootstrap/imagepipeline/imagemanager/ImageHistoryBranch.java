package editor.bootstrap.imagepipeline.imagemanager;

import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagehistory.ImageHistoryStruct;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class ImageHistoryBranch extends BranchPackage {

    /*
     * Records undo steps. While a step is open each pixel keeps the color it
     * had before its first change; committing turns them into one step with
     * the colors they ended on, clears the redo history, and drops the oldest
     * steps once the history holds too many or too many pixels. Undo and redo
     * write one side of a step back and move it to the other stack.
     */

    // Step \\

    void beginStep(ImageDocumentInstance document) {
        document.getStepBeforeColors().clear();
        document.getStepRegion().clear();
    }

    void record(ImageDocumentInstance document, int pixelIndex, int beforeColor) {

        Int2IntOpenHashMap stepBeforeColors = document.getStepBeforeColors();

        if (stepBeforeColors.containsKey(pixelIndex))
            return;

        stepBeforeColors.put(pixelIndex, beforeColor);

        int width = document.getWidth();
        document.getStepRegion().include(pixelIndex % width, pixelIndex / width);
    }

    void commitStep(ImageDocumentInstance document) {

        int count = document.getStepBeforeColors().size();

        if (count == 0)
            return;

        int[] pixelIndices = new int[count];
        int[] beforeColors = new int[count];
        int[] afterColors = new int[count];
        int width = document.getWidth();
        Pixmap pixmap = document.getPixmap();
        ObjectIterator<Int2IntMap.Entry> iterator = document.getStepBeforeColors().int2IntEntrySet().fastIterator();

        for (int i = 0; iterator.hasNext(); i++) {

            Int2IntMap.Entry entry = iterator.next();
            int pixelIndex = entry.getIntKey();

            pixelIndices[i] = pixelIndex;
            beforeColors[i] = entry.getIntValue();
            afterColors[i] = pixmap.getPixel(pixelIndex % width, pixelIndex / width);
        }

        clearSteps(document, document.getRedoSteps());
        pushStep(document, document.getUndoSteps(),
                new ImageHistoryStruct(pixelIndices, beforeColors, afterColors, document.getStepRegion()));
        trim(document);
        beginStep(document);
    }

    // Undo \\

    ImageRegionStruct undo(ImageDocumentInstance document) {
        return moveStep(document, document.getUndoSteps(), document.getRedoSteps(), true);
    }

    ImageRegionStruct redo(ImageDocumentInstance document) {
        return moveStep(document, document.getRedoSteps(), document.getUndoSteps(), false);
    }

    private ImageRegionStruct moveStep(
            ImageDocumentInstance document,
            ObjectArrayList<ImageHistoryStruct> from,
            ObjectArrayList<ImageHistoryStruct> to,
            boolean before) {

        if (from.isEmpty())
            return null;

        ImageHistoryStruct step = from.pop();
        int width = document.getWidth();
        Pixmap pixmap = document.getPixmap();

        for (int i = 0; i < step.getPixelCount(); i++) {

            int pixelIndex = step.getPixelIndex(i);

            pixmap.drawPixel(
                    pixelIndex % width,
                    pixelIndex / width,
                    before ? step.getBeforeColor(i) : step.getAfterColor(i));
        }

        to.push(step);
        return step.getRegion();
    }

    // Management \\

    void clear(ImageDocumentInstance document) {
        clearSteps(document, document.getUndoSteps());
        clearSteps(document, document.getRedoSteps());
        beginStep(document);
    }

    private void pushStep(
            ImageDocumentInstance document,
            ObjectArrayList<ImageHistoryStruct> steps,
            ImageHistoryStruct step) {

        steps.push(step);
        document.adjustHistoryPixels(step.getPixelCount());
    }

    private void clearSteps(ImageDocumentInstance document, ObjectArrayList<ImageHistoryStruct> steps) {

        for (int i = 0; i < steps.size(); i++)
            document.adjustHistoryPixels(-steps.get(i).getPixelCount());

        steps.clear();
    }

    // The newest step always stays, however large, so the last change can be undone
    private void trim(ImageDocumentInstance document) {

        ObjectArrayList<ImageHistoryStruct> undoSteps = document.getUndoSteps();

        while (undoSteps.size() > 1
                && (undoSteps.size() > EditorSetting.IMAGE_HISTORY_MAX_STEPS
                        || document.getHistoryPixels() > EditorSetting.IMAGE_HISTORY_MAX_PIXELS))
            document.adjustHistoryPixels(-undoSteps.remove(0).getPixelCount());
    }
}
