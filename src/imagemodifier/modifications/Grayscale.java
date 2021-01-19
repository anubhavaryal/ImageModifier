package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class Grayscale implements Modification {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_LIGHT_BT601 = 1;
    public static final int TYPE_LIGHT_BT709 = 2;
    public static final int TYPE_LIGHT_BT2000 = 3;
    public static final int TYPE_AVERAGE = 4;
    public static final int TYPE_RED_CHANNEL = 5;
    public static final int TYPE_GREEN_CHANNEL = 6;
    public static final int TYPE_BLUE_CHANNEL = 7;
    public static final int TYPE_DESATURATED = 8;
    public static final int TYPE_MIN = 9;
    public static final int TYPE_MAX = 10;

    private int type; // type of grayscale

    public Grayscale() {
        type = Grayscale.TYPE_NONE;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        int red = 0xff & (argb >> 16);
        int green = 0xff & (argb >> 8);
        int blue = 0xff & (argb >> 0);

        double luminance = 0;

        switch(type) {
            case Grayscale.TYPE_LIGHT_BT601:
                luminance = 0.299 * red + 0.587 * green + 0.114 * blue;
                break;
            case Grayscale.TYPE_LIGHT_BT709:
                luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
                break;
            case Grayscale.TYPE_LIGHT_BT2000:
                luminance = 0.2627 * red + 0.6780 * green + 0.0593 * blue;
                break;
            case Grayscale.TYPE_AVERAGE:
                luminance = (red + green + blue) / 3.0;
                break;
            case Grayscale.TYPE_RED_CHANNEL:
                luminance = red;
                break;
            case Grayscale.TYPE_GREEN_CHANNEL:
                luminance = green;
                break;
            case Grayscale.TYPE_BLUE_CHANNEL:
                luminance = blue;
                break;
            case Grayscale.TYPE_DESATURATED:
                luminance = (Math.max(red, Math.max(green, blue)) + Math.min(red, Math.min(green, blue))) / 2.0;
                break;
            case Grayscale.TYPE_MAX:
                luminance = Math.max(red, Math.max(green, blue));
                break;
            case Grayscale.TYPE_MIN:
                luminance = Math.min(red, Math.min(green, blue));
                break;
        }

        red = (int) luminance;
        green = (int) luminance;
        blue = (int) luminance;

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return type == Grayscale.TYPE_NONE;
    }
}
