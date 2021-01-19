package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class HueSaturationValue implements Modification {
    private int hueDelta; // change in hue
    private double saturationDelta; // change in saturation
    private double valueDelta; // change in value

    public HueSaturationValue() {
        hueDelta = 0;
        saturationDelta = 0;
        valueDelta = 0;
    }

    public void setHueDelta(int hueDelta) {
        this.hueDelta = hueDelta;
    }

    public void setSaturationDelta(double saturationDelta) {
        this.saturationDelta = saturationDelta;
    }

    public void setValueDelta(double valueDelta) {
        this.valueDelta = valueDelta;
    }

    public int getHueDelta() {
        return hueDelta;
    }

    public double getSaturationDelta() {
        return saturationDelta;
    }

    public double getValueDelta() {
        return valueDelta;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        double red = 0xff & (argb >> 16);
        double green = 0xff & (argb >> 8);
        double blue = 0xff & (argb >> 0);

        // convert rgb to hsv
        red /= 255;
        green /= 255;
        blue /= 255;

        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double range = max - min;

        double hue = Double.NaN;

        if(range == 0) {
            hue = 0;
        } else if(max == red) {
            hue = ((green - blue) / range) % 6;
        } else if(max == green) {
            hue = ((blue - red) / range) + 2;
        } else if(max == blue) {
            hue = ((red - green) / range) + 4;
        }

        hue *= 60;

        double saturation = max == 0 ? 0 : range / max;
        double value = max;

        hue += hueDelta;
        saturation += saturationDelta;
        value += valueDelta;

        hue %= 360;
        saturation = Math.max(0, Math.min(1, saturation));
        value = Math.max(0, Math.min(1, value));

        // convert hsv to rgb
        double chroma = saturation * value;

        hue /= 60;
        double xComponent = chroma * (1 - Math.abs(hue % 2 - 1));

        double redComponent = 0;
        double greenComponent = 0;
        double blueComponent = 0;

        if(hue >= 0 && hue <= 1) {
            redComponent = chroma;
            greenComponent = xComponent;
        } else if(hue > 1 && hue <= 2) {
            redComponent = xComponent;
            greenComponent = chroma;
        } else if(hue > 2 && hue <= 3) {
            greenComponent = chroma;
            blueComponent = xComponent;
        } else if(hue > 3 && hue <= 4) {
            greenComponent = xComponent;
            blueComponent = chroma;
        } else if(hue > 4 && hue <= 5) {
            redComponent = xComponent;
            blueComponent = chroma;
        } else if(hue > 5 && hue <= 6) {
            redComponent = chroma;
            blueComponent = xComponent;
        }

        double add = value - chroma;

        red = redComponent + add;
        green = greenComponent + add;
        blue = blueComponent + add;

        red *= 255;
        green *= 255;
        blue *= 255;

        int color = (alpha << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return hueDelta == 0 && saturationDelta == 0 && valueDelta == 0;
    }
}
