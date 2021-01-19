# Image Modifier
A simple program that allows you to perform basic modifications on an image and save it.

## How to Use
1. Download `ImageModifier.jar` (requires Java 15) or the source code and run it.
2. Navigate to **Settings** and ensure that the program is using the correct values
    - The thread count should default to the number of available processors on your computer.
    - The thread timeout time should default to 30 seconds (30000 milliseconds).
3. Go back to the main menu and open the **Modify Image** tab.
4. Click on the *Select Image* box to select an image.
    - To select another image, click on the leftmost (original) image.
5. Select a modification using the drop-down list and adjust any settings associated with that modification.
6. The image can be reset back to its original state by pressing the *Reset* button at the bottom-left of the screen.
7. Once you are done adding modifications, you can generate the image and save it to your computer by pressing the 
*Generate* button at the bottom-left of the screen.
8. Below the *Generate* button, there is a progress bar that indicates how close the generated image is to completion.
    - To the right of the *Generate* button, there is a label that shows the percentage the progress bar is at.
    - The progress bar updates only when a thread has completed its task. If only 1 thread was specified
     in **Settings**, the progress bar will instantly jump from 0% to 100% once the thread has completed 
     its task.
