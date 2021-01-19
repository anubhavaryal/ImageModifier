package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class YUV implements Modification {
    private double yDelta;
    private double uDelta;
    private double vDelta;

    public YUV() {
        yDelta = 0;
        uDelta = 0;
        vDelta = 0;
    }

    public void setYDelta(double yDelta) {
        this.yDelta = yDelta;
    }

    public void setUDelta(double uDelta) {
        this.uDelta = uDelta;
    }

    public void setVDelta(double vDelta) {
        this.vDelta = vDelta;
    }

    public double getYDelta() {
        return yDelta;
    }

    public double getUDelta() {
        return uDelta;
    }

    public double getVDelta() {
        return vDelta;
    }


    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        double red = 0xff & (argb >> 16);
        double green = 0xff & (argb >> 8);
        double blue = 0xff & (argb >> 0);

        // convert rgb to yuv
        final double wr = 0.299;
        final double wb = 0.114;
        final double wg = 1 - wr - wb;
        final double uMax = 0.436;
        final double vMax = 0.615;

        double yPrime = wr * red + wg * green + wb * blue;
        double u = uMax * ((blue - yPrime) / (1 - wb));
        double v = vMax * ((red - yPrime) / (1 - wr));

        yPrime += yDelta * 255;
        u += uDelta * 255;
        v += vDelta * 255;

        // convert yuv to rgb
        red = (yPrime + v * ((1 - wr) / vMax));
        green = (yPrime - u * ((wb * (1 - wb)) / (uMax * wg)) - v * ((wr * (1 - wr)) / (vMax * wg)));
        blue = (yPrime + u * ((1 - wb) / uMax));

        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        int color = (alpha << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return yDelta == 0 && uDelta == 0 && vDelta == 0;
    }
}
