package imagemodifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static final File settingsFile = new File("settings.cfg");

    public static void main(String[] args) throws IOException {
        // create the settings file if it doesn't exist
        if(!settingsFile.exists()) {
            FileOutputStream fileOutput = new FileOutputStream(settingsFile);
            Properties properties = new Properties();

            // thread count defaults to the number of available processors
            properties.setProperty("thread-count", Integer.toString(Runtime.getRuntime().availableProcessors()));

            // thread timeout defaults to 30 seconds (30000 milliseconds)
            properties.setProperty("thread-timeout", "30000");

            // save the properties to the settings file
            properties.store(fileOutput, null);
        }

        MainMenu.main(args); // run the javafx program
    }
}
