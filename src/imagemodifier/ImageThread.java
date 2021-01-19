package imagemodifier;

import java.awt.image.BufferedImage;

public class ImageThread implements Runnable {
    private int id; // id of thread
    private BufferedImage reference; // image used as reference (never modified)
    private BufferedImage image; // image being modified
    private int start;  // starting pixel
    private int end; // ending pixel
    private QuadConsumer<BufferedImage, BufferedImage, Integer, Integer> algorithm; // algorithm to perform on the image

    public ImageThread(int id, BufferedImage reference, BufferedImage image, int start, int end, QuadConsumer<BufferedImage, BufferedImage, Integer, Integer> algorithm) {
        this.id = id;
        this.reference = reference;
        this.image = image;
        this.start = start;
        this.end = end;
        this.algorithm = algorithm;
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        // do algorithm on every pixel from start to end
        for (int pixel = start; pixel < end; pixel++) {
            int x = pixel % image.getWidth();
            int y = pixel / image.getWidth();

            algorithm.accept(reference, image, x, y);
        }
    }

    @FunctionalInterface
    public interface QuadConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);
    }
}
