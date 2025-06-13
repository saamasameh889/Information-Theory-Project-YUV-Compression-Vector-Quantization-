import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunYUVCompression {
    // Paths to image folders
    private static final String ANIMAL_PATH = "/Users/samasameh/Desktop/Final_Project_IT/Final_Project_IT/animal_images";
    private static final String FACES_PATH = "/Users/samasameh/Desktop/Final_Project_IT/Final_Project_IT/faces_images";
    private static final String NATURE_PATH = "/Users/samasameh/Desktop/Final_Project_IT/Final_Project_IT/nature_images";

    public static void main(String[] args) throws IOException {
        System.out.println("YUV Compression - Loading images...");

        // Load images from each category
        List<BufferedImage> animalImages = loadImagesFromFolder(ANIMAL_PATH, 15);
        List<BufferedImage> facesImages = loadImagesFromFolder(FACES_PATH, 15);
        List<BufferedImage> natureImages = loadImagesFromFolder(NATURE_PATH, 15);

        // Split into training and test sets, handling cases with fewer than 15 images
        List<BufferedImage> trainImages = new ArrayList<>();
        List<BufferedImage> testImages = new ArrayList<>();

        // For animal images
        int animalTrainCount = Math.min(10, animalImages.size());
        trainImages.addAll(animalImages.subList(0, animalTrainCount));
        if (animalImages.size() > animalTrainCount) {
            testImages.addAll(animalImages.subList(animalTrainCount, animalImages.size()));
        }

        // For faces images
        int facesTrainCount = Math.min(10, facesImages.size());
        trainImages.addAll(facesImages.subList(0, facesTrainCount));
        if (facesImages.size() > facesTrainCount) {
            testImages.addAll(facesImages.subList(facesTrainCount, facesImages.size()));
        }

        // For nature images
        int natureTrainCount = Math.min(10, natureImages.size());
        trainImages.addAll(natureImages.subList(0, natureTrainCount));
        if (natureImages.size() > natureTrainCount) {
            testImages.addAll(natureImages.subList(natureTrainCount, natureImages.size()));
        }

        System.out
                .println("Loaded " + trainImages.size() + " training images and " + testImages.size() + " test images");

        // Check if we have enough images to proceed
        if (trainImages.isEmpty() || testImages.isEmpty()) {
            System.out.println("Error: Not enough images found. Please ensure your image folders contain images.");
            return;
        }

        // Run YUV compression
        YUVCompression.runYUVCompression(trainImages, testImages);

        System.out.println("YUV Compression complete!");
    }

    // Load images from a directory
    private static List<BufferedImage> loadImagesFromFolder(String folderPath, int numImages) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Warning: Folder does not exist: " + folderPath);
            return images;
        }

        File[] files = folder.listFiles();

        if (files != null) {
            Arrays.sort(files);
            int count = 0;

            for (File file : files) {
                if (count >= numImages)
                    break;

                if (file.isFile() && !file.isHidden()) {
                    try {
                        BufferedImage img = ImageIO.read(file);
                        if (img != null) {
                            images.add(img);
                            count++;
                        }
                    } catch (IOException e) {
                        System.out.println("Could not read file: " + file.getName());
                    }
                }
            }
        }

        if (images.size() < numImages) {
            System.out.println("Warning: Only found " + images.size() + " images in " + folderPath);
        }

        return images;
    }
}