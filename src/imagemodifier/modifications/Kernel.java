package imagemodifier.modifications;

import imagemodifier.Modification;

import java.awt.image.BufferedImage;

public class Kernel implements Modification {
    private int size;
    
    private double[][] kernel;
    private double multiplier;

    public Kernel() {
        size = 3;
        
        kernel = new double[size][size];
        multiplier = 1;
        
        resetKernel();
    }

    public void setKernelSize(int size) {
        this.size = size;
        kernel = new double[size][size];

        resetKernel();
    }

    public void setKernel(double[][] kernel) {
        this.kernel = kernel.clone();
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public int getKernelSize() {
        return size;
    }

    public double[][] getKernel() {
        return kernel.clone();
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void resetKernel() {
        // populate kernel with 0s
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                kernel[row][col] = 0;
            }
        }

        // set middle value to 1
        kernel[size / 2][size / 2] = 1;
    }

    @Override
    public void updatePixel(BufferedImage reference, BufferedImage image, int x, int y) {
        int[][] redValues = new int[size][size];
        int[][] blueValues = new int[size][size];
        int[][] greenValues = new int[size][size];

        final int DISTANCE = (size - 1) / 2; // max distance away from the center pixel for each side
        final int WIDTH = reference.getWidth();
        final int HEIGHT = reference.getHeight();

        for (int i = x - DISTANCE; i < x + DISTANCE + 1; i++) {
            for (int j = y - DISTANCE; j < y + DISTANCE + 1; j++) {
                int col = i + DISTANCE - x;
                int row = j + DISTANCE - y;

                int argb = 0;

                if (i >= 0 && i < WIDTH && j >= 0 && j < HEIGHT) {
                    // if both i and j are valid
                    argb = reference.getRGB(i, j);

                } else if (i >= 0 && i < WIDTH) {
                    // if i is valid but j is not
                    if (j < 0) {
                        // if j is above the top row, set y to 0
                        argb = reference.getRGB(i, 0);
                    } else if (j >= HEIGHT) {
                        // else if j is below the bottom row, set y to image height
                        argb = reference.getRGB(i, HEIGHT - 1);
                    }
                } else if (j >= 0 && j < HEIGHT) {
                    // if j is valid but i is not
                    if (i < 0) {
                        // if i is left of the left column, set x to 0
                        argb = reference.getRGB(0, j);
                    } else if (i >= WIDTH) {
                        // if i is right of the right column, set x to width
                        argb = reference.getRGB(WIDTH - 1, j);
                    }
                } else {
                    // if neither i or j is valid (can only occur at corners)
                    if (i < 0 && j < 0) {
                        // top-left corner, set x and y to 0
                        argb = reference.getRGB(0, 0);
                    } else if (i >= WIDTH && j < 0) {
                        // top-right corner, set x to width and y to 0
                        argb = reference.getRGB(WIDTH - 1, 0);
                    } else if (i < 0 && j >= HEIGHT) {
                        // bottom-left corner, set x to 0 and y to height
                        argb = reference.getRGB(0, HEIGHT - 1);
                    } else if (i >= WIDTH && j >= HEIGHT) {
                        // bottom-right corner, set x to width and y to height
                        argb = reference.getRGB(WIDTH - 1, HEIGHT - 1);
                    }
                }

                int red = 0xff & (argb >> 16);
                int green = 0xff & (argb >> 8);
                int blue = 0xff & (argb >> 0);

                redValues[row][col] = red;
                greenValues[row][col] = green;
                blueValues[row][col] = blue;
            }
        }

        int argb = reference.getRGB(x, y);
        int alpha = 0xff | (argb >> 24);

        // convolve
        double redSum = 0;
        double greenSum = 0;
        double blueSum = 0;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                double kernelVal = kernel[row][col] * multiplier;

                redSum += redValues[row][col] * kernelVal;
                greenSum += greenValues[row][col] * kernelVal;
                blueSum += blueValues[row][col] * kernelVal;
            }
        }

        int red = Math.max(0, Math.min(255, (int)redSum));
        int green = Math.max(0, Math.min(255, (int)greenSum));
        int blue = Math.max(0, Math.min(255, (int)blueSum));

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        image.setRGB(x, y, color);
    }

    @Override
    public boolean isDefault() {
        if(kernel[size / 2][size / 2] != 1) {
            return false;
        }

        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                if(kernel[row][col] != 0 && (row != size / 2 ||  col != size / 2)) {
                    return false;
                }
            }
        }

        return true;
    }
}
