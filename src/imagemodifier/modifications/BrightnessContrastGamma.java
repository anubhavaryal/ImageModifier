package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class BrightnessContrastGamma implements Modification {
    private double contrast;
    private int brightness;
    private double gamma;

    public BrightnessContrastGamma() {
        contrast = 1.0;
        brightness = 0;
        gamma = 1.0;
    }

    public void setContrast(double contrast) {
        this.contrast = contrast;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getContrast() {
        return contrast;
    }

    public int getBrightness() {
        return brightness;
    }

    public double getGamma() {
        return gamma;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        int red = 0xff & (argb >> 16);
        int green = 0xff & (argb >> 8);
        int blue = 0xff & (argb >> 0);

        red = (int) (contrast * (red - 128) + 128 + brightness);
        green = (int) (contrast * (green - 128) + 128 + brightness);
        blue = (int) (contrast * (blue - 128) + 128 + brightness);

        red = (int) (Math.pow(red / 255.0, gamma) * 255.0);
        green = (int) (Math.pow(green / 255.0, gamma) * 255.0);
        blue = (int) (Math.pow(blue / 255.0, gamma) * 255.0);

        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return contrast == 1 && brightness == 0 && gamma == 1;
    }
}
