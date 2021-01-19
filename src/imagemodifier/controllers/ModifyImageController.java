package imagemodifier.controllers;

import imagemodifier.ImageThread;
import imagemodifier.Main;
import imagemodifier.Modification;
import imagemodifier.modifications.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ModifyImageController {

    @FXML
    private ImageView originalImageView;
    @FXML
    private ImageView previewImageView;

    @FXML
    private VBox modificationVBox;
    @FXML
    private ChoiceBox modifyChoiceBox;

    @FXML
    private Button resetButton;
    @FXML
    private Button generateButton;
    @FXML
    private ProgressBar generateProgressBar;
    @FXML
    private Label generatePercentLabel;

    private BufferedImage originalImage;
    private BufferedImage previewImage;

    private BufferedImage originalImageSmall;
    private BufferedImage previewImageSmall;

    private static LinkedHashMap<String, Modification> modifications;

    private File imageFile;


    public void initialize() {
        modifications = new LinkedHashMap<>();

        modifyChoiceBox.getItems().addAll("Brightness, Contrast, and Gamma", new Separator(),
                "ARGB", "HSV", "YUV", new Separator(),
                "Grayscale", "Threshold", "Bit Depth", new Separator(),
                "Kernel", "Sobel");

        modifyChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                if(newValue.intValue() != -1) {
                    handleModification((String) modifyChoiceBox.getItems().get(newValue.intValue()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Image selectImage = new Image("imagemodifier/resources/selectimage.png");
        originalImageView.setImage(selectImage);
    }

    public void openMainMenu(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(Main.class.getResource("views/MainMenu.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(scene);
        window.show();
    }

    public void openImage(MouseEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.wbmp"));
        String home = System.getProperty("user.home");
        File downloadsFolder = new File(home + "/Downloads");
        fileChooser.setInitialDirectory(downloadsFolder);

        imageFile = fileChooser.showOpenDialog(window);

        if(imageFile != null) {
            originalImage = ImageIO.read(imageFile);
            previewImage = copyImage(originalImage);

            Image image = SwingFXUtils.toFXImage(originalImage, null);

            // set image to the original image so the fit width and fit height of the imageviews can be found
            originalImageView.setImage(image);
            previewImageView.setImage(image);

            originalImageSmall = shrinkImage(originalImage, originalImageView);
            previewImageSmall = shrinkImage(previewImage, previewImageView);

            originalImageView.setImage(SwingFXUtils.toFXImage(originalImageSmall, null));
            previewImageView.setImage(SwingFXUtils.toFXImage(previewImageSmall, null));

            resetButton.setDisable(false);
            generateButton.setDisable(false);
            modifyChoiceBox.setDisable(false);

            resetModifications(event);
        }
    }

    public void generateImage(MouseEvent event) throws IOException {
        String extension = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1);

        FileChooser fileChooser = new FileChooser();
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase() + " File", "*." + extension));
        extension = extension.equals("jpg") ? "jpeg" : extension; // convert jpg to jpeg

        String home = System.getProperty("user.home");
        File downloadsFolder = new File(home + "/Downloads");
        fileChooser.setInitialDirectory(downloadsFolder);

        File saveFile = fileChooser.showSaveDialog(window);

        if(saveFile != null) {
            previewImage = copyImage(originalImage); // copy the original image
            createImage(previewImage); // do modifications on preview image

            // save the file
            ImageIO.write(copyImage(previewImage), extension, saveFile);
        }
    }

    // modifies the input image with the current modifications in the modifications map
    public void createImage(BufferedImage image) throws IOException {
        final int THREADS_COUNT = SettingsController.getThreadsCount();
        final int TOTAL_PIXELS = image.getHeight() * image.getWidth();
        final int PIXELS_PER_THREAD = TOTAL_PIXELS / THREADS_COUNT; // last thread will get the remaining pixels
        final double PROGRESS_PER_THREAD = 1.0 / modifications.size() / THREADS_COUNT;

        if(!modifications.isEmpty()) {
            // reset progress
            generateProgressBar.setProgress(0);
            generatePercentLabel.setText("0.00%");
        } else {
            // set progress to 100% (no modifications so instant completion)
            generateProgressBar.setProgress(1);
            generatePercentLabel.setText("100.00%");
        }

        // iterate through all modifications
        for(String key: modifications.keySet()) {
            Modification mod = modifications.get(key);

            // skip all modifications that don't update the image
            if(mod.isDefault()) {
                generateProgressBar.setProgress(generateProgressBar.getProgress() + PROGRESS_PER_THREAD * THREADS_COUNT);
                generatePercentLabel.setText(String.format("%.2f%%", generateProgressBar.getProgress() * 100));
                continue;
            }

            ExecutorService threads = Executors.newFixedThreadPool(THREADS_COUNT);
            List<Future<?>> futures = new LinkedList<>();

            ImageThread.QuadConsumer<BufferedImage, BufferedImage, Integer, Integer> algorithm = mod::updatePixel;
            BufferedImage reference = copyImage(image);

            // create threads
            for(int id = 0; id < THREADS_COUNT; id++) {
                int start = id * PIXELS_PER_THREAD;
                int end = id + 1 != THREADS_COUNT ? (id + 1) * PIXELS_PER_THREAD: TOTAL_PIXELS;

                ImageThread thread = new ImageThread(id, reference, image, start, end, algorithm);
                futures.add(threads.submit(thread));
            }

            threads.shutdown();

            int index = 0; // keeps track of current index in the futures list
            long start = System.currentTimeMillis(); // keeps track of starting time

            while(!futures.isEmpty()) {
                // remove future from list once done
                if(futures.get(index).isDone()) {
                    futures.remove(index);

                    // update progress bar and label
                    generateProgressBar.setProgress(generateProgressBar.getProgress() + PROGRESS_PER_THREAD);
                    generatePercentLabel.setText(String.format("%.2f%%", generateProgressBar.getProgress() * 100));
                }

                index++;
                index = index >= futures.size() - 1 ? 0 : index; // reset index once its at end of list

                // if the thread timeout time has passed, stop all threads (default timeout is 30 seconds)
                if(System.currentTimeMillis() - start >= SettingsController.getThreadsTimeout()) {
                    System.out.println("Shutting down threads that are still incomplete.");
                    threads.shutdownNow();
                    break;
                }
            }
        }
    }

    public BufferedImage shrinkImage(BufferedImage image, ImageView imageView) {
        double width = imageView.getBoundsInParent().getWidth();
        double height = imageView.getBoundsInParent().getHeight();

        BufferedImage newImage = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.drawImage(image.getScaledInstance((int)width, (int)height, BufferedImage.SCALE_SMOOTH), 0, 0, null);
        graphics2D.dispose();

        return newImage;
    }

    public static BufferedImage copyImage(BufferedImage source) {
        ColorModel colorModel = source.getColorModel();
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster raster = source.copyData(null);

        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    public void handleModification(String modification) {
        // remove all nodes from the modification vbox
        modificationVBox.getChildren().clear();

        switch(modification) {
            case "Brightness, Contrast, and Gamma":
                handleBrightnessContrastGamma();
                break;
            case "ARGB":
                handleAlphaRedGreenBlue();
                break;
            case "HSV":
                handleHueSaturationValue();
                break;
            case "YUV":
                handleYUV();
                break;
            case "Grayscale":
                handleGrayscale();
                break;
            case "Threshold":
                handleThreshold();
                break;
            case "Bit Depth":
                handleBitDepth();
                break;
            case "Kernel":
                handleKernel();
                break;
            case "Sobel":
                handleSobel();
                break;
        }
    }

    public void handleBrightnessContrastGamma() {
        final String KEY = "BCG";

        modifications.putIfAbsent(KEY, new BrightnessContrastGamma());
        BrightnessContrastGamma bcg = (BrightnessContrastGamma) modifications.get(KEY);

        Label brightnessLabel = new Label("Adjust the brightness of the image.");
        brightnessLabel.getStyleClass().add("description");
        Slider brightnessSlider = new Slider(-255, 255, 0);
        brightnessSlider.setValue(bcg.getBrightness());

        brightnessSlider.valueProperty().addListener(((observableValue, oldValue, newValue) -> {
            try {
                bcg.setBrightness(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }));

        Label contrastLabel = new Label("Adjust the contrast of the image.");
        contrastLabel.getStyleClass().add("description");
        Slider contrastSlider = new Slider(0, 5, 1);
        contrastSlider.setValue(bcg.getContrast());

        contrastSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                bcg.setContrast(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label gammaLabel = new Label("Adjust the gamma of the image.");
        gammaLabel.getStyleClass().add("description");
        Slider gammaSlider = new Slider(0, 5, 1);
        gammaSlider.setValue(bcg.getGamma());

        gammaSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                bcg.setGamma(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(brightnessLabel, brightnessSlider,
                contrastLabel, contrastSlider,
                gammaLabel, gammaSlider);
    }

    public void handleAlphaRedGreenBlue() {
        final String KEY = "ARGB";

        modifications.putIfAbsent(KEY, new AlphaRedGreenBlue());
        AlphaRedGreenBlue argb = (AlphaRedGreenBlue) modifications.get(KEY);

        Label alphaLabel = new Label("Adjust the alpha value.");
        alphaLabel.getStyleClass().add("description");
        Slider alphaSlider = new Slider(-255, 255, 0);
        alphaSlider.setValue(argb.getAlphaDelta());

        alphaSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                argb.setAlphaDelta(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label redLabel = new Label("Adjust the red value.");
        redLabel.getStyleClass().add("description");
        Slider redSlider = new Slider(-255, 255, 0);
        redSlider.setValue(argb.getRedDelta());

        redSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                argb.setRedDelta(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label greenLabel = new Label("Adjust the green value.");
        greenLabel.getStyleClass().add("description");
        Slider greenSlider = new Slider(-255, 255, 0);
        greenSlider.setValue(argb.getGreenDelta());

        greenSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                argb.setGreenDelta(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label blueLabel = new Label("Adjust the blue value.");
        blueLabel.getStyleClass().add("description");
        Slider blueSlider = new Slider(-255, 255, 0);
        blueSlider.setValue(argb.getBlueDelta());

        blueSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                argb.setBlueDelta(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(alphaLabel, alphaSlider,
                redLabel, redSlider,
                greenLabel, greenSlider,
                blueLabel, blueSlider);
    }

    public void handleHueSaturationValue() {
        final String KEY = "HSV";

        modifications.putIfAbsent(KEY, new HueSaturationValue());
        HueSaturationValue hsv = (HueSaturationValue) modifications.get(KEY);

        Label hueLabel = new Label("Adjust the hue value.");
        hueLabel.getStyleClass().add("description");
        Slider hueSlider = new Slider(0, 360, 0);
        hueSlider.setValue(hsv.getHueDelta());

        hueSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                hsv.setHueDelta(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label saturationLabel = new Label("Adjust the saturation value.");
        saturationLabel.getStyleClass().add("description");
        Slider saturationSlider = new Slider(-1, 1, 0);
        saturationSlider.setValue(hsv.getSaturationDelta());

        saturationSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                hsv.setSaturationDelta(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label valueLabel = new Label("Adjust the value value.");
        valueLabel.getStyleClass().add("description");
        Slider valueSlider = new Slider(-1, 1, 0);
        valueSlider.setValue(hsv.getValueDelta());

        valueSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                hsv.setValueDelta(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(hueLabel, hueSlider,
                saturationLabel, saturationSlider,
                valueLabel, valueSlider);
    }

    public void handleYUV() {
        final String KEY = "YUV";

        modifications.putIfAbsent(KEY, new YUV());
        YUV yuv = (YUV) modifications.get(KEY);

        Label yLabel = new Label("Adjust the Y value.");
        yLabel.getStyleClass().add("description");
        Slider ySlider = new Slider(-1, 1, 0);
        ySlider.setValue(yuv.getYDelta());

        ySlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                yuv.setYDelta(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label uLabel = new Label("Adjust the U value.");
        uLabel.getStyleClass().add("description");
        Slider uSlider = new Slider(-0.5, 0.5, 0);
        uSlider.setValue(yuv.getUDelta());

        uSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                yuv.setUDelta(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Label vLabel = new Label("Adjust the V value.");
        vLabel.getStyleClass().add("description");
        Slider vSlider = new Slider(-0.5, 0.5, 0);
        vSlider.setValue(yuv.getVDelta());

        vSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                yuv.setVDelta(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(yLabel, ySlider, uLabel, uSlider, vLabel, vSlider);
    }

    public void handleGrayscale() {
        final String KEY = "GS";

        modifications.putIfAbsent(KEY, new Grayscale());
        Grayscale grayscale = (Grayscale) modifications.get(KEY);

        Label grayscaleLabel = new Label("Select grayscale type.");
        grayscaleLabel.getStyleClass().add("description");
        ChoiceBox grayscaleTypes = new ChoiceBox();
        grayscaleTypes.getItems().addAll("None", "Luma (BT601)", "Luma (BT709)", "Luma (BT2000)",
                "Average", "Red Channel", "Green Channel", "Blue Channel", "Desaturated", "Maximum", "Minimum");

        grayscaleTypes.getSelectionModel().select(grayscale.getType());

        grayscaleTypes.getSelectionModel().selectedIndexProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                grayscale.setType(newValue.intValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(grayscaleLabel, grayscaleTypes);
    }

    public void handleThreshold() {
        final String KEY = "TH";

        modifications.putIfAbsent(KEY, new Threshold());
        Threshold threshold = (Threshold) modifications.get(KEY);

        Label thresholdCheckBoxLabel = new Label("Enable threshold.");
        thresholdCheckBoxLabel.getStyleClass().add("description");
        CheckBox thresholdCheckBox = new CheckBox();
        thresholdCheckBox.setSelected(threshold.getEnabled());

        Label thresholdSliderLabel = new Label("Set threshold level.");
        thresholdSliderLabel.getStyleClass().add("description");
        Slider thresholdSlider = new Slider(0, 1, 0.5);
        thresholdSlider.setValue(threshold.getThreshold());

        thresholdSliderLabel.setDisable(!thresholdCheckBox.isSelected());
        thresholdSlider.setDisable(!thresholdCheckBox.isSelected());

        thresholdCheckBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                threshold.setEnabled(newValue);
                thresholdSliderLabel.setDisable(!newValue);
                thresholdSlider.setDisable(!newValue);
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        thresholdSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                threshold.setThreshold(newValue.doubleValue());
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(thresholdCheckBoxLabel, thresholdCheckBox,
                thresholdSliderLabel, thresholdSlider);
    }

    public void handleBitDepth() {
        final String KEY = "BD";

        modifications.putIfAbsent(KEY, new BitDepth());
        BitDepth bitDepth = (BitDepth) modifications.get(KEY);

        Label bitDepthLabel = new Label("Adjust the amount of bits per color.");
        bitDepthLabel.getStyleClass().add("description");
        Slider bitDepthSlider = new Slider(1, 8, 8);
        bitDepthSlider.setValue(bitDepth.getBits());

        bitDepthSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                bitDepthSlider.setValue(newValue.intValue()); // snap to nearest bit value

                if(bitDepth.getBits() != newValue.intValue()) {
                    bitDepth.setBits(newValue.intValue());
                    BufferedImage image = copyImage(previewImageSmall);
                    createImage(image);
                    previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(bitDepthLabel, bitDepthSlider);
    }

    public void handleKernel() {
        final String KEY = "K";

        modifications.putIfAbsent(KEY, new Kernel());
        Kernel kernel = (Kernel) modifications.get(KEY);

        Label kernelLabel = new Label("Adjust the kernel.");
        kernelLabel.getStyleClass().add("description");

        HBox kernelMultiplierHBox = new HBox();
        kernelMultiplierHBox.setSpacing(10);
        kernelMultiplierHBox.setAlignment(Pos.CENTER_LEFT);

        TextField kernelMultiplier = new TextField();
        kernelMultiplier.setText(Double.toString(kernel.getMultiplier()));
        kernelMultiplier.setPrefWidth(50);

        kernelMultiplier.textProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                kernel.setMultiplier(Double.parseDouble(newValue));
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException ioe) {
                ioe.printStackTrace();
            } catch(NumberFormatException nfe) {
                // do nothing
            }
        });

        VBox kernelVBox = new VBox(); // stores the rows of the kernel

        HBox kernelSizeHBox = new HBox();
        kernelSizeHBox.setSpacing(10);
        Button kernelSizeAddButton = new Button("+");
        kernelSizeAddButton.getStyleClass().add("menu-button");
        Button kernelSizeSubtractButton = new Button("-");
        kernelSizeSubtractButton.getStyleClass().add("menu-button");

        kernelSizeAddButton.setOnMouseClicked(event -> {
            kernel.setKernelSize(kernel.getKernelSize() + 2);

            // remove all nodes from kernel VBox and add the updated nodes back
            kernelVBox.getChildren().clear();
            kernelVBox.getChildren().addAll(getKernelHBoxes(kernel));
        });

        kernelSizeSubtractButton.setOnMouseClicked(event -> {
            if(kernel.getKernelSize() > 3) {
                kernel.setKernelSize(kernel.getKernelSize() - 2);

                // remove all nodes from kernel VBox and add the updated nodes back
                kernelVBox.getChildren().clear();
                kernelVBox.getChildren().addAll(getKernelHBoxes(kernel));
            }
        });

        ChoiceBox kernelPresetChoiceBox = new ChoiceBox();
        kernelPresetChoiceBox.setValue("Choose Preset");

        kernelPresetChoiceBox.getItems().addAll("Identity",
                "Edge Detection 1", "Edge Detection 2", "Edge Detection 3",
                "Sharpen", "Box Blur", "Gausian Blur 3x3", "Gausian Blur 5x5", "Unsharp Masking 5x5",
                "Emboss", "Top Sobel", "Bottom Sobel", "Left Sobel", "Right Sobel");

        kernelPresetChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, oldValue, newValue) -> {
            int size = -1;
            double multiplier = 0;
            double[][] matrix = new double[1][1];

            switch(newValue.intValue()) {
                case 0: // Identity
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{0, 0, 0}, {0, 1, 0}, {0, 0, 0}};
                    break;
                case 1: // Edge Detection 1
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{1, 0, -1}, {0, 0, 0}, {-1, 0, 1}};
                    break;
                case 2: // Edge Detection 2
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{0, -1, 0}, {-1, 4, -1}, {0, -1, 0}};
                    break;
                case 3: // Edge Detection 3
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{-1, -1, -1}, {-1, 8, -1}, {-1, -1, -1}};
                    break;
                case 4: // Sharpen
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{0, -1, 0}, {-1, 5, -1}, {0, -1, 0}};
                    break;
                case 5: // Box Blur
                    size = 3;
                    multiplier = 1.0 / 9.0;
                    matrix = new double[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
                    break;
                case 6: // Gaussian Blur 3x3
                    size = 3;
                    multiplier = 1.0 / 16.0;
                    matrix = new double[][]{{1, 2, 1}, {2, 4, 2}, {1, 2, 1}};
                    break;
                case 7: // Gaussian Blur 5x5
                    size = 5;
                    multiplier = 1.0 / 256.0;
                    matrix = new double[][]{{1, 4, 6, 4, 1}, {4, 16, 24, 16, 4}, {6, 24, 36, 24, 6}, {4, 16, 24, 16, 4}, {1, 4, 6, 4, 1}};
                    break;
                case 8: // Unsharp Masking 5x5
                    size = 5;
                    multiplier = -1.0 / 256.0;
                    matrix = new double[][]{{1, 4, 6, 4, 1}, {4, 16, 24, 16, 4}, {6, 24, -476, 24, 6}, {4, 16, 24, 16, 4}, {1, 4, 6, 4, 1}};
                    break;
                case 9: // Emboss
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{-2, -1, 0}, {-1, 1, 1}, {0, 1, 2}};
                    break;
                case 10: // Top Sobel
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
                    break;
                case 11: // Bottom Sobel
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
                    break;
                case 12: // Left Sobel
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
                    break;
                case 13: // Right Sobel
                    size = 3;
                    multiplier = 1;
                    matrix = new double[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
                    break;
            }

            kernel.setKernelSize(size);
            kernel.setMultiplier(multiplier);
            kernel.setKernel(matrix);

            kernelVBox.getChildren().clear();
            kernelVBox.getChildren().addAll(getKernelHBoxes(kernel));

            try {
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        kernelSizeHBox.getChildren().addAll(kernelSizeAddButton, kernelSizeSubtractButton);
        kernelVBox.getChildren().addAll(getKernelHBoxes(kernel));
        kernelMultiplierHBox.getChildren().addAll(kernelMultiplier, kernelVBox);
        modificationVBox.getChildren().addAll(kernelLabel, kernelPresetChoiceBox, kernelMultiplierHBox, kernelSizeHBox);
    }

    public ArrayList<HBox> getKernelHBoxes(Kernel kernel) {
        ArrayList<HBox> kernelHBoxes = new ArrayList<>();
        double[][] matrix = kernel.getKernel();

        for(int row = 0; row < kernel.getKernelSize(); row++) {
            HBox kernelHBox = new HBox();

            for(int col = 0; col < kernel.getKernelSize(); col++) {
                TextField kernelTextField = new TextField();
                kernelTextField.setText(Double.toString(matrix[row][col]));
                kernelTextField.setPrefWidth(50);

                int finalRow = row;
                int finalCol = col;

                kernelTextField.textProperty().addListener((observableValue, oldValue, newValue) -> {
                    try {
                        matrix[finalRow][finalCol] = Double.parseDouble(newValue);
                        kernel.setKernel(matrix);

                        BufferedImage image = copyImage(previewImageSmall);
                        createImage(image);
                        previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
                    } catch(IOException ioe) {
                        ioe.printStackTrace();
                    } catch(NumberFormatException nfe) {
                        // do nothing
                    }
                });
                kernelHBox.getChildren().add(kernelTextField);
            }
            kernelHBoxes.add(kernelHBox);
        }

        return kernelHBoxes;
    }

    public void handleSobel() {
        final String KEY = "S";

        modifications.putIfAbsent(KEY, new Sobel());
        Sobel sobel = (Sobel) modifications.get(KEY);

        Label sobelEnableLabel = new Label("Enable Sobel.");
        sobelEnableLabel.getStyleClass().add("description");
        CheckBox sobelEnableCheckBox = new CheckBox();
        sobelEnableCheckBox.setSelected(sobel.getEnabled());

        Label sobelColorDirectionLabel = new Label("Color the edges based on direction.");
        sobelColorDirectionLabel.getStyleClass().add("description");
        sobelColorDirectionLabel.setDisable(!sobel.getEnabled());
        CheckBox sobelColorDirectionCheckBox = new CheckBox();
        sobelColorDirectionCheckBox.setSelected(sobel.getColorDirection());
        sobelColorDirectionCheckBox.setDisable(!sobel.getEnabled());

        sobelEnableCheckBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                sobel.setEnabled(newValue);
                sobelColorDirectionLabel.setDisable(!newValue);
                sobelColorDirectionCheckBox.setDisable(!newValue);
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        sobelColorDirectionCheckBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                sobel.setColorDirection(newValue);
                BufferedImage image = copyImage(previewImageSmall);
                createImage(image);
                previewImageView.setImage(SwingFXUtils.toFXImage(image, null));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        modificationVBox.getChildren().addAll(sobelEnableLabel, sobelEnableCheckBox, sobelColorDirectionLabel, sobelColorDirectionCheckBox);
    }

    public void resetModifications(MouseEvent event) {
        // clear modifications map and reset the modification choicebox
        modifications.clear();
        modifyChoiceBox.setValue("Select Modification");
        modificationVBox.getChildren().clear();

        // reset the small preview image
        previewImageSmall = copyImage(originalImageSmall);
        previewImageView.setImage(SwingFXUtils.toFXImage(previewImageSmall, null));

        // reset progress
        generateProgressBar.setProgress(0);
        generatePercentLabel.setText("0.00%");
    }
}
