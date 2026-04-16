package application.bootstrap.menupipeline.fontmanager;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import application.bootstrap.menupipeline.font.FontTileData;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class FontRasterizerUtility extends EngineUtility {

    /*
     * Rasterizes a TTF/OTF font file into a list of per-glyph FontTileData
     * instances using AWT. Each tile holds the rendered glyph image and the
     * metrics needed to build the GlyphMetricStruct table after atlas packing.
     * Package-private — only InternalBuilder may call these.
     */

    static ObjectArrayList<FontTileData> rasterize(
            File fontFile,
            int size,
            String charset,
            InternalBuilder builder) {

        Font awtFont = loadFont(fontFile, size);
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D scratchG = scratch.createGraphics();
        scratchG.setFont(awtFont);

        FontMetrics metrics = scratchG.getFontMetrics();
        ObjectArrayList<FontTileData> tiles = new ObjectArrayList<>();

        for (int i = 0; i < charset.length(); i++) {

            char c = charset.charAt(i);
            int cp = (int) c;
            int advance = metrics.charWidth(c);
            int bearingY = metrics.getAscent();
            java.awt.geom.Rectangle2D bounds = metrics.getStringBounds(String.valueOf(c), scratchG);
            int gw = Math.max(1, (int) Math.ceil(bounds.getWidth()));
            int gh = Math.max(1, metrics.getAscent() + metrics.getDescent());

            BufferedImage glyphImage = new BufferedImage(gw, gh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = glyphImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(awtFont);
            g.setColor(java.awt.Color.WHITE);
            g.drawString(String.valueOf(c), 0, metrics.getAscent());
            g.dispose();

            FontTileData tile = builder.createFontTile();
            tile.constructor(cp, glyphImage, 0, bearingY, advance);
            tiles.add(tile);
        }

        scratchG.dispose();

        return tiles;
    }

    private static Font loadFont(File fontFile, int size) {

        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            return base.deriveFont(Font.PLAIN, (float) size);
        } catch (Exception e) {
            throwException("Failed to load font file: " + fontFile.getAbsolutePath(), e);
            return null;
        }
    }
}