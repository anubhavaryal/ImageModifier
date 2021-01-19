package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class AlphaRedGreenBlue implements Modification {
    private int alphaDelta; // change in alpha
    private int redDelta; // change in red
    private int greenDelta; // change in green
    private int blueDelta; // change in blue

    public AlphaRedGreenBlue() {
        alphaDelta = 0;
        redDelta = 0;
        greenDelta = 0;
        blueDelta = 0;
    }

    public void setAlphaDelta(int alphaDelta) {
        this.alphaDelta = alphaDelta;
    }

    public void setRedDelta(int redDelta) {
        this.redDelta = redDelta;
    }

    public void setGreenDelta(int greenDelta) {
        this.greenDelta = greenDelta;
    }

    public void setBlueDelta(int blueDelta) {
        this.blueDelta = blueDelta;
    }

    public int getAlphaDelta() {
        return alphaDelta;
    }

    public int getRedDelta() {
        return redDelta;
    }

    public int getGreenDelta() {
        return greenDelta;
    }

    public int getBlueDelta() {
        return blueDelta;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        int red = 0xff & (argb >> 16);
        int green = 0xff & (argb >> 8);
        int blue = 0xff & (argb >> 0);

        alpha += alphaDelta;
        red += redDelta;
        green += greenDelta;
        blue += blueDelta;

        alpha = Math.max(0, Math.min(255, alpha));
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return alphaDelta == 0 && redDelta == 0 && greenDelta == 0 && blueDelta == 0;
    }
}
