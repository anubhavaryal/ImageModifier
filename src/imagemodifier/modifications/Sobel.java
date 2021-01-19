package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class Sobel implements Modification {
    private boolean enabled;
    private boolean colorDirection;

    public Sobel() {
        enabled = false;
        colorDirection = false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setColorDirection(boolean colorDirection) {
        this.colorDirection = colorDirection;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean getColorDirection() {
        return colorDirection;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        try {
            int[][] horizontal = new int[][]{{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
            int[][] vertical = new int[][]{{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};

            int[][] pixelValues = new int[3][3];

            final int WIDTH = reference.getWidth();
            final int HEIGHT = reference.getHeight();

            for (int i = x - 1; i < x + 1 + 1; i++) {
                for (int j = y - 1; j < y + 1 + 1; j++) {
                    int col = i + 1 - x;
                    int row = j + 1 - y;

                    int argb = 0;

                    if (i >= 0 && i < WIDTH && j >= 0 && j < HEIGHT) {
                        argb = reference.getRGB(i, j);
                    } else if (i >= 0 && i < WIDTH) {
                        if (j < 0) {
                            argb = reference.getRGB(i, 0);
                        } else if (j >= HEIGHT) {
                            argb = reference.getRGB(i, HEIGHT - 1);
                        }
                    } else if (j >= 0 && j < HEIGHT) {
                        if (i < 0) {
                            argb = reference.getRGB(0, j);
                        } else if (i >= WIDTH) {
                            argb = reference.getRGB(WIDTH - 1, j);
                        }
                    } else {
                        if (i < 0 && j < 0) {
                            argb = reference.getRGB(0, 0);
                        } else if (i >= WIDTH && j < 0) {
                            argb = reference.getRGB(WIDTH - 1, 0);
                        } else if (i < 0 && j >= HEIGHT) {
                            argb = reference.getRGB(0, HEIGHT - 1);
                        } else if (i >= WIDTH && j >= HEIGHT) {
                            argb = reference.getRGB(WIDTH - 1, HEIGHT - 1);
                        }
                    }

                    int red = 0xff & (argb >> 16);
                    int green = 0xff & (argb >> 8);
                    int blue = 0xff & (argb >> 0);

                    // convert to grayscale
                    pixelValues[row][col] = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                }
            }

            double horizontalGradient = 0; // horizontal gradient
            double verticalGradient = 0; // vertical gradient


            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    horizontalGradient += pixelValues[row][col] * horizontal[row][col];
                    verticalGradient += pixelValues[row][col] * vertical[row][col];
                }
            }

            double gradientMagnitude = Math.sqrt(horizontalGradient * horizontalGradient + verticalGradient * verticalGradient);
            gradientMagnitude = Math.max(0, Math.min(255, gradientMagnitude));

            int alpha = 0xff & (image.getRGB(x, y) >> 24);
            int color;

            if (colorDirection && gradientMagnitude != 0) {
                double gradientDirection = -(Math.toDegrees(Math.atan2(horizontalGradient, verticalGradient)) - 180);
                gradientDirection %= 360;

                // find rgb values using gradientDirection as the hue, gradientMagnitude as value, and max saturation
                double chroma = gradientMagnitude / 255;

                double hue = gradientDirection / 60;
                double xComponent = chroma * (1 - Math.abs(hue % 2 - 1));

                double redComponent = 0;
                double greenComponent = 0;
                double blueComponent = 0;

                if (hue >= 0 && hue <= 1) {
                    redComponent = chroma;
                    greenComponent = xComponent;
                } else if (hue > 1 && hue <= 2) {
                    redComponent = xComponent;
                    greenComponent = chroma;
                } else if (hue > 2 && hue <= 3) {
                    greenComponent = chroma;
                    blueComponent = xComponent;
                } else if (hue > 3 && hue <= 4) {
                    greenComponent = xComponent;
                    blueComponent = chroma;
                } else if (hue > 4 && hue <= 5) {
                    redComponent = xComponent;
                    blueComponent = chroma;
                } else if (hue > 5 && hue <= 6) {
                    redComponent = chroma;
                    blueComponent = xComponent;
                }

                double red = redComponent * 255;
                double green = greenComponent * 255;
                double blue = blueComponent * 255;

                red = Math.max(0, Math.min(255, red));
                green = Math.max(0, Math.min(255, green));
                blue = Math.max(0, Math.min(255, blue));

                color = (alpha << 24) | ((int) red << 16) | ((int) green << 8) | (int) blue;
            } else {
                color = (alpha << 24) | ((int) gradientMagnitude << 16) | ((int) gradientMagnitude << 8) | (int) gradientMagnitude;
            }

            image.setRGB(x, y, color);
        } catch(Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public boolean isDefault() {
        return !enabled;
    }
}
