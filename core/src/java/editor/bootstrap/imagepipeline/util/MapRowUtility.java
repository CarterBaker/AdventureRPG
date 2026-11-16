package editor.bootstrap.imagepipeline.util;

import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MapRowUtility extends EngineUtility {

    /*
     * Lays out the Maps tab the one way every editor's maps are listed, as
     * the Tools tab lists its tools above their options: an entry per map
     * the image takes, the one picked selected and those generated marked,
     * then the picked map's settings below, each a slider across its range,
     * a switch across its two ends, and the Generate and Clear buttons that
     * make the map from the image's settings or erase it. The albedo is the
     * image's own layers, so it lists no settings.
     */

    // Rows \\

    public static void addMapItems(
            ObjectArrayList<CharacterViewRowStruct> rows,
            String itemKeyName,
            ImageMap[] maps,
            ImageMap selected,
            long generatedMask) {

        for (int i = 0; i < maps.length; i++) {

            ImageMap map = maps[i];
            boolean generated = (generatedMask & toBit(map)) != 0L;

            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(itemKeyName, i),
                    generated ? map.getLabel() + EditorSetting.MAPS_GENERATED_SUFFIX : map.getLabel(),
                    map == selected));
        }
    }

    public static void addSettingRows(
            ObjectArrayList<CharacterViewRowStruct> rows,
            ImageMap map,
            float[] settings,
            String settingKeyName,
            String generateKey,
            String clearKey) {

        rows.add(CharacterViewRowStruct.header(map.getLabel() + EditorSetting.MAPS_HEADER_SETTINGS_SUFFIX));

        if (!map.isGenerated()) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.MAPS_HEADER_ALBEDO));
            return;
        }

        for (MapGenerateOption option : map.getOptions()) {

            float value = option.get(settings);

            rows.add(CharacterViewRowStruct.ranged(
                    CharacterViewUtility.toKey(settingKeyName, option.ordinal()),
                    option.getLabel(),
                    option.formatValue(value),
                    value,
                    option.getMin(),
                    option.getMax(),
                    option.getStep()));
        }

        rows.add(CharacterViewRowStruct.action(generateKey, EditorSetting.MAPS_LABEL_GENERATE));
        rows.add(CharacterViewRowStruct.action(clearKey, EditorSetting.MAPS_LABEL_CLEAR));
    }

    // Utility \\

    // A map's bit in a mask of the maps generated, by its ordinal
    public static long toBit(ImageMap map) {
        return 1L << map.ordinal();
    }
}
