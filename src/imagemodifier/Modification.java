package imagemodifier;

import java.awt.image.BufferedImage;

/*
* Modifications that still need to be added:
* Sobel (color the edges depending on where they face) (computerphile video has more info)
* */

public interface Modification {
    void updatePixel(BufferedImage reference, BufferedImage image, int x, int y);
    boolean isDefault();
}
