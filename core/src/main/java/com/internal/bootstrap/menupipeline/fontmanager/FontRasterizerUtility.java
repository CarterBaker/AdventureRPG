package com.internal.bootstrap.menupipeline.fontmanager;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import com.internal.bootstrap.menupipeline.fonts.FontTileData;
import com.internal.core.engine.UtilityPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Rasterizes a TTF/OTF font file into per-glyph BufferedImages using Java AWT.
 * This is the only class in the font pipeline that touches java.awt.Font.
 * All output is plain BufferedImage data feeding into the standard atlas
 * pipeline — no LibGDX, no external font libraries.
 *
 * Each glyph is rendered onto a tight-fitting ARGB canvas with sub-pixel
 * antialiasing. The canvas is sized to the glyph's actual ink bounds so
 * the atlas packer can work with accurate dimensions per character.
 */
class FontRasterizerUtility extends UtilityPackage {

    static ObjectArrayList<FontTileData> rasterize(
            File fontFile, int size, String charset, InternalBuilder builder) {

        Font awtFont = loadFont(fontFile, size);

        // Scratch canvas for metric measurement
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D scratchG = scratch.createGraphics();
        scratchG.setFont(awtFont);
        FontMetrics metrics = scratchG.getFontMetrics();
        scratchG.dispose();

        ObjectArrayList<FontTileData> tiles = new ObjectArrayList<>();

        for (int i = 0; i < charset.length(); i++) {
            char c = charset.charAt(i);
            int cp = (int) c;
            int advance = metrics.charWidth(c);
            int bearingY = metrics.getAscent();

            // Glyph bounding box from per-char string bounds
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