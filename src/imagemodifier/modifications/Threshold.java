package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class Threshold implements Modification {
    private boolean enabled;
    private double threshold;

    public Threshold() {
        enabled = false;
        threshold = 0.5;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public double getThreshold() {
        return threshold;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        int red = 0xff & (argb >> 16);
        int green = 0xff & (argb >> 8);
        int blue = 0xff & (argb >> 0);

        // calculate luminance (uses BT.601 to preserve luminosity)
        double luminance = 0.299 * red + 0.587 * green + 0.114 * blue;

        // check if luminance is above the threshold
        if(luminance > threshold * 255) {
            // set color to black
            red = 0;
            green = 0;
            blue = 0;
        } else {
            // set color to white
            red = 255;
            green = 255;
            blue = 255;
        }

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return !enabled;
    }
}
