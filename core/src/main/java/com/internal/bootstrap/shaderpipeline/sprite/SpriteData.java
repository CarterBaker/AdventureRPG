package com.internal.bootstrap.shaderpipeline.sprite;

import java.awt.image.BufferedImage;
import com.internal.core.engine.DataPackage;

public class SpriteData extends DataPackage {

    private String name;
    private BufferedImage image;
    private int width;
    private int height;

    public void constructor(String name, BufferedImage image) {
        this.name = name;
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public String getName() {
        return name;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}