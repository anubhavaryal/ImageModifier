package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class BitDepth implements Modification {
    private int bits;
    private int[] values;

    public BitDepth() {
        bits = 8;
    }

    public void setBits(int bits) {
        this.bits = bits;

        // populate the array of possible values
        int totalValues = (int)Math.pow(2, bits);
        values = new int[totalValues];

        for(int i = 0; i < totalValues; i++) {
            values[i] = (i * 255 / (totalValues - 1));
        }
    }

    public int getBits() {
        return bits;
    }

    public int getClosest(int num) {
        int index = 0;
        int closest = num;

        for(int i = 0; i < values.length; i++) {
            int distance = Math.abs(num - values[i]);

            if(distance < closest) {
                index = i;
                closest = distance;
            }
        }

        return values[index];
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int argb = reference.getRGB(x, y);

        int alpha = 0xff & (argb >> 24);
        int red = 0xff & (argb >> 16);
        int green = 0xff & (argb >> 8);
        int blue = 0xff & (argb >> 0);

        alpha = getClosest(alpha);
        red = getClosest(red);
        green = getClosest(green);
        blue = getClosest(blue);

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        return bits == 8;
    }
}
