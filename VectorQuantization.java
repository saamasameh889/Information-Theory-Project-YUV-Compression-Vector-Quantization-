import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class VectorQuantization {
    // Paths to image folders
    private static final String ANIMAL_PATH = "/Users/samasameh/Desktop/Final_Project_IT/Final_Project_IT/animal_images";
    private static final String FACES_PATH = "/Users/samasameh/Desktop/Final_Project_IT/Final_Project_IT/faces_images";
    private static final String NATURE_PATH = "/Users/samasameh/Desktop/Final_Project_IT/Final_Project_IT/nature_images";
    private static final String OUTPUT_DIR = "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\reconstructed_images";
    private static final int CODEBOOK_SIZE = 256;
    private static final int BLOCK_SIZE = 2;

    public static void main(String[] args) throws IOException {
        System.out.println("Loading images...");

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

        // Extract blocks from all training images for each color channel
        System.out.println("Extracting blocks from training images...");
        List<double[]> rBlocks = new ArrayList<>();
        List<double[]> gBlocks = new ArrayList<>();
        List<double[]> bBlocks = new ArrayList<>();

        for (BufferedImage img : trainImages) {
            // Extract blocks from each channel
            extractBlocksFromImage(img, rBlocks, gBlocks, bBlocks);
            System.out.print(".");
        }
        System.out.println();

        System.out.println("Extracted " + rBlocks.size() + " blocks for each channel");

        // Create codebooks for each channel
        System.out.println("Creating codebooks...");
        double[][] rCodebook = createCodebook(rBlocks, CODEBOOK_SIZE);
        double[][] gCodebook = createCodebook(gBlocks, CODEBOOK_SIZE);
        double[][] bCodebook = createCodebook(bBlocks, CODEBOOK_SIZE);

        // Save codebooks
        saveCodebook(rCodebook, "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\r_codebook.dat");
        saveCodebook(gCodebook, "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\g_codebook.dat");
        saveCodebook(bCodebook, "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\b_codebook.dat");

        System.out.println("Codebooks created and saved");

        // Create output directory for reconstructed images
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        // Compress and decompress test images
        System.out.println("Compressing and decompressing test images...");
        List<Double> psnrValues = new ArrayList<>();
        List<Double> ssimValues = new ArrayList<>();
        List<Double> compressionRatios = new ArrayList<>();

        for (int idx = 0; idx < testImages.size(); idx++) {
            BufferedImage img = testImages.get(idx);

            // Split into RGB channels
            int[][][] channels = splitChannels(img);
            int[][] rChannel = channels[0];
            int[][] gChannel = channels[1];
            int[][] bChannel = channels[2];

            // Compress each channel
            CompressionResult rCompressed = compressChannel(rChannel, rCodebook);
            CompressionResult gCompressed = compressChannel(gChannel, gCodebook);
            CompressionResult bCompressed = compressChannel(bChannel, bCodebook);

            // Calculate compression ratio
            int originalSize = img.getWidth() * img.getHeight() * 3 * 8; // 8 bits per channel
            int compressedSize = (rCompressed.indices.length * rCompressed.indices[0].length +
                    gCompressed.indices.length * gCompressed.indices[0].length +
                    bCompressed.indices.length * bCompressed.indices[0].length) * 8; // 8 bits per index
            double compressionRatio = (double) originalSize / compressedSize;
            compressionRatios.add(compressionRatio);

            // Decompress each channel
            int[][] rReconstructed = decompressChannel(rCompressed.indices, rCodebook, rChannel.length,
                    rChannel[0].length);
            int[][] gReconstructed = decompressChannel(gCompressed.indices, gCodebook, gChannel.length,
                    gChannel[0].length);
            int[][] bReconstructed = decompressChannel(bCompressed.indices, bCodebook, bChannel.length,
                    bChannel[0].length);

            // Merge channels
            BufferedImage reconstructed = mergeChannels(rReconstructed, gReconstructed, bReconstructed);

            // Save reconstructed image
            ImageIO.write(reconstructed, "jpg", new File(OUTPUT_DIR + "\\reconstructed_" + idx + ".jpg"));

            // Calculate PSNR and SSIM
            double imgPsnr = calculatePSNR(img, reconstructed);
            double imgSsim = calculateSSIM(img, reconstructed);

            psnrValues.add(imgPsnr);
            ssimValues.add(imgSsim);

            System.out.println("Image " + (idx + 1) + ": PSNR = " + String.format("%.2f", imgPsnr) +
                    " dB, SSIM = " + String.format("%.4f", imgSsim) +
                    ", Compression Ratio = " + String.format("%.2f", compressionRatio) + ":1");

            // Save comparison image for first 3 examples
            if (idx < 3) {
                saveComparisonImage(img, reconstructed, imgPsnr, imgSsim, idx);
            }
        }

        // Calculate average metrics
        double avgPsnr = psnrValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgSsim = ssimValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgCompressionRatio = compressionRatios.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        System.out.println("Average PSNR: " + String.format("%.2f", avgPsnr) + " dB");
        System.out.println("Average SSIM: " + String.format("%.4f", avgSsim));
        System.out.println("Average Compression Ratio: " + String.format("%.2f", avgCompressionRatio) + ":1");

        // Generate a report
        generateReport(trainImages.size(), testImages.size(), psnrValues, ssimValues, compressionRatios, avgPsnr,
                avgSsim, avgCompressionRatio);

        // Generate HTML report with visualizations
        VQReportGenerator.generateHtmlReport(trainImages.size(), testImages.size(), psnrValues, ssimValues,
                compressionRatios, avgPsnr, avgSsim, avgCompressionRatio, OUTPUT_DIR);

        System.out.println("Reports generated. Process complete!");
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

    // Extract 2x2 blocks from an image
    private static void extractBlocksFromImage(BufferedImage image, List<double[]> rBlocks, List<double[]> gBlocks,
            List<double[]> bBlocks) {
        int height = image.getHeight();
        int width = image.getWidth();

        // Ensure dimensions are divisible by block size
        int hPad = height - (height % BLOCK_SIZE);
        int wPad = width - (width % BLOCK_SIZE);

        for (int i = 0; i < hPad; i += BLOCK_SIZE) {
            for (int j = 0; j < wPad; j += BLOCK_SIZE) {
                double[] rBlock = new double[BLOCK_SIZE * BLOCK_SIZE];
                double[] gBlock = new double[BLOCK_SIZE * BLOCK_SIZE];
                double[] bBlock = new double[BLOCK_SIZE * BLOCK_SIZE];

                int idx = 0;
                for (int bi = 0; bi < BLOCK_SIZE; bi++) {
                    for (int bj = 0; bj < BLOCK_SIZE; bj++) {
                        Color color = new Color(image.getRGB(j + bj, i + bi));
                        rBlock[idx] = color.getRed();
                        gBlock[idx] = color.getGreen();
                        bBlock[idx] = color.getBlue();
                        idx++;
                    }
                }

                rBlocks.add(rBlock);
                gBlocks.add(gBlock);
                bBlocks.add(bBlock);
            }
        }
    }

    // Create codebook using K-means clustering
    private static double[][] createCodebook(List<double[]> blocks, int codebookSize) {
        System.out.println("Creating codebook with " + blocks.size() + " blocks...");

        // Initialize codebook with random blocks
        Random random = new Random(0);
        Set<Integer> selectedIndices = new HashSet<>();
        double[][] codebook = new double[codebookSize][];

        for (int i = 0; i < codebookSize; i++) {
            int idx;
            if (selectedIndices.size() >= blocks.size()) {
                // If we've used all available blocks, just pick randomly
                idx = random.nextInt(blocks.size());
            } else {
                // Otherwise try to get unique blocks
                do {
                    idx = random.nextInt(blocks.size());
                } while (selectedIndices.contains(idx));
                selectedIndices.add(idx);
            }

            codebook[i] = blocks.get(idx).clone();
        }

        // K-means clustering
        boolean changed = true;
        int maxIterations = 100;
        int iteration = 0;

        while (changed && iteration < maxIterations) {
            changed = false;
            iteration++;

            // Assign blocks to nearest codebook entry
            int[] assignments = new int[blocks.size()];
            for (int i = 0; i < blocks.size(); i++) {
                double[] block = blocks.get(i);
                int bestIdx = 0;
                double bestDistance = Double.MAX_VALUE;

                for (int j = 0; j < codebookSize; j++) {
                    double distance = calculateDistance(block, codebook[j]);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestIdx = j;
                    }
                }

                assignments[i] = bestIdx;
            }

            // Update codebook entries
            double[][] newCodebook = new double[codebookSize][];
            int[] counts = new int[codebookSize];

            for (int i = 0; i < codebookSize; i++) {
                newCodebook[i] = new double[BLOCK_SIZE * BLOCK_SIZE];
            }

            for (int i = 0; i < blocks.size(); i++) {
                int assignment = assignments[i];
                double[] block = blocks.get(i);
                counts[assignment]++;

                for (int j = 0; j < block.length; j++) {
                    newCodebook[assignment][j] += block[j];
                }
            }

            // Calculate means
            for (int i = 0; i < codebookSize; i++) {
                if (counts[i] > 0) {
                    for (int j = 0; j < newCodebook[i].length; j++) {
                        newCodebook[i][j] /= counts[i];
                    }
                } else {
                    // If a cluster is empty, assign a random block
                    int randomIdx = random.nextInt(blocks.size());
                    newCodebook[i] = blocks.get(randomIdx).clone();
                    changed = true;
                }
            }

            // Check if codebook has changed
            for (int i = 0; i < codebookSize; i++) {
                if (!Arrays.equals(codebook[i], newCodebook[i])) {
                    changed = true;
                    break;
                }
            }

            codebook = newCodebook;

            if (iteration % 10 == 0) {
                System.out.println("K-means iteration " + iteration);
            }
        }

        System.out.println("K-means completed in " + iteration + " iterations");
        return codebook;
    }

    // Calculate Euclidean distance between two vectors
    private static double calculateDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // Save codebook to file
    private static void saveCodebook(double[][] codebook, String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(codebook);
        }
    }

    // Split image into RGB channels
    private static int[][][] splitChannels(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();

        int[][] rChannel = new int[height][width];
        int[][] gChannel = new int[height][width];
        int[][] bChannel = new int[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color color = new Color(image.getRGB(j, i));
                rChannel[i][j] = color.getRed();
                gChannel[i][j] = color.getGreen();
                bChannel[i][j] = color.getBlue();
            }
        }

        return new int[][][] { rChannel, gChannel, bChannel };
    }

    // Compress a channel using a codebook
    private static CompressionResult compressChannel(int[][] channel, double[][] codebook) {
        int height = channel.length;
        int width = channel[0].length;

        // Ensure dimensions are divisible by block size
        int hPad = height - (height % BLOCK_SIZE);
        int wPad = width - (width % BLOCK_SIZE);

        int[][] indices = new int[hPad / BLOCK_SIZE][wPad / BLOCK_SIZE];

        for (int i = 0; i < hPad; i += BLOCK_SIZE) {
            for (int j = 0; j < wPad; j += BLOCK_SIZE) {
                // Extract block
                double[] block = new double[BLOCK_SIZE * BLOCK_SIZE];
                int idx = 0;
                for (int bi = 0; bi < BLOCK_SIZE; bi++) {
                    for (int bj = 0; bj < BLOCK_SIZE; bj++) {
                        block[idx++] = channel[i + bi][j + bj];
                    }
                }

                // Find nearest codebook entry
                int bestIdx = 0;
                double bestDistance = Double.MAX_VALUE;

                for (int k = 0; k < codebook.length; k++) {
                    double distance = calculateDistance(block, codebook[k]);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestIdx = k;
                    }
                }

                indices[i / BLOCK_SIZE][j / BLOCK_SIZE] = bestIdx;
            }
        }

        return new CompressionResult(indices, height, width);
    }

    // Decompress a channel using a codebook
    private static int[][] decompressChannel(int[][] indices, double[][] codebook, int originalHeight,
            int originalWidth) {
        // Ensure dimensions are divisible by block size
        int hPad = originalHeight - (originalHeight % BLOCK_SIZE);
        int wPad = originalWidth - (originalWidth % BLOCK_SIZE);

        int[][] reconstructed = new int[originalHeight][originalWidth];

        for (int i = 0; i < indices.length; i++) {
            for (int j = 0; j < indices[0].length; j++) {
                int codeIdx = indices[i][j];
                double[] codeVector = codebook[codeIdx];

                // Place the block in the reconstructed image
                int idx = 0;
                for (int bi = 0; bi < BLOCK_SIZE; bi++) {
                    for (int bj = 0; bj < BLOCK_SIZE; bj++) {
                        if (i * BLOCK_SIZE + bi < hPad && j * BLOCK_SIZE + bj < wPad) {
                            reconstructed[i * BLOCK_SIZE + bi][j * BLOCK_SIZE + bj] = (int) Math.round(codeVector[idx]);
                        }
                        idx++;
                    }
                }
            }
        }

        return reconstructed;
    }

    // Merge RGB channels into a single image
    private static BufferedImage mergeChannels(int[][] rChannel, int[][] gChannel, int[][] bChannel) {
        int height = rChannel.length;
        int width = rChannel[0].length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int r = Math.min(255, Math.max(0, rChannel[i][j]));
                int g = Math.min(255, Math.max(0, gChannel[i][j]));
                int b = Math.min(255, Math.max(0, bChannel[i][j]));

                Color color = new Color(r, g, b);
                image.setRGB(j, i, color.getRGB());
            }
        }

        return image;
    }

    // Calculate PSNR between two images
    static double calculatePSNR(BufferedImage original, BufferedImage compressed) {
        int width = original.getWidth();
        int height = original.getHeight();

        double mse = 0.0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color origColor = new Color(original.getRGB(j, i));
                Color compColor = new Color(compressed.getRGB(j, i));

                double diffR = origColor.getRed() - compColor.getRed();
                double diffG = origColor.getGreen() - compColor.getGreen();
                double diffB = origColor.getBlue() - compColor.getBlue();

                mse += (diffR * diffR + diffG * diffG + diffB * diffB) / 3.0;
            }
        }

        mse /= (width * height);

        if (mse == 0) {
            return 100.0;
        }

        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }

    // Calculate SSIM between two images (simplified version)
    static double calculateSSIM(BufferedImage original, BufferedImage compressed) {
        int width = original.getWidth();
        int height = original.getHeight();

        double meanOrig = 0.0;
        double meanComp = 0.0;

        // Calculate means
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color origColor = new Color(original.getRGB(j, i));
                Color compColor = new Color(compressed.getRGB(j, i));

                double origIntensity = (origColor.getRed() + origColor.getGreen() + origColor.getBlue()) / 3.0;
                double compIntensity = (compColor.getRed() + compColor.getGreen() + compColor.getBlue()) / 3.0;

                meanOrig += origIntensity;
                meanComp += compIntensity;
            }
        }

        meanOrig /= (width * height);
        meanComp /= (width * height);

        // Calculate variances and covariance
        double varOrig = 0.0;
        double varComp = 0.0;
        double covar = 0.0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color origColor = new Color(original.getRGB(j, i));
                Color compColor = new Color(compressed.getRGB(j, i));

                double origIntensity = (origColor.getRed() + origColor.getGreen() + origColor.getBlue()) / 3.0;
                double compIntensity = (compColor.getRed() + compColor.getGreen() + compColor.getBlue()) / 3.0;

                varOrig += Math.pow(origIntensity - meanOrig, 2);
                varComp += Math.pow(compIntensity - meanComp, 2);
                covar += (origIntensity - meanOrig) * (compIntensity - meanComp);
            }
        }

        varOrig /= (width * height - 1);
        varComp /= (width * height - 1);
        covar /= (width * height - 1);

        // Constants for stability
        double C1 = Math.pow(0.01 * 255, 2);
        double C2 = Math.pow(0.03 * 255, 2);

        // Calculate SSIM
        double numerator = (2 * meanOrig * meanComp + C1) * (2 * covar + C2);
        double denominator = (meanOrig * meanOrig + meanComp * meanComp + C1) * (varOrig + varComp + C2);

        return numerator / denominator;
    }

    // Save comparison image
    private static void saveComparisonImage(BufferedImage original, BufferedImage reconstructed,
            double psnr, double ssim, int idx) throws IOException {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage comparison = new BufferedImage(width * 2, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = comparison.createGraphics();

        // Draw original image
        g2d.drawImage(original, 0, 0, null);

        // Draw reconstructed image
        g2d.drawImage(reconstructed, width, 0, null);

        // Add text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Original", 10, 20);
        g2d.drawString("Reconstructed", width + 10, 20);
        g2d.drawString(String.format("PSNR: %.2f dB, SSIM: %.4f", psnr, ssim), width + 10, 40);

        g2d.dispose();

        // Save comparison image
        ImageIO.write(comparison, "jpg", new File(OUTPUT_DIR + "\\comparison_" + idx + ".jpg"));
    }

    // Generate report
    private static void generateReport(int numTrainImages, int numTestImages,
            List<Double> psnrValues, List<Double> ssimValues,
            List<Double> compressionRatios,
            double avgPsnr, double avgSsim, double avgCompressionRatio) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter("c:\\Users\\DELL\\Desktop\\Final_Project_IT\\vq_report.txt"))) {
            writer.println("Vector Quantization Compression Report");
            writer.println("=====================================\n");
            writer.println("Number of training images: " + numTrainImages);
            writer.println("Number of test images: " + numTestImages);
            writer.println("Codebook size: " + CODEBOOK_SIZE + " vectors (" + BLOCK_SIZE + "x" + BLOCK_SIZE
                    + " pixels each)\n");

            writer.println("Compression Results:");
            writer.println("Average PSNR: " + String.format("%.2f", avgPsnr) + " dB");
            writer.println("Average SSIM: " + String.format("%.4f", avgSsim));
            writer.println("Average Compression Ratio: " + String.format("%.2f", avgCompressionRatio) + ":1\n");

            writer.println("Individual Image Results:");
            for (int i = 0; i < psnrValues.size(); i++) {
                writer.println("Image " + (i + 1) + ": PSNR = " + String.format("%.2f", psnrValues.get(i)) +
                        " dB, SSIM = " + String.format("%.4f", ssimValues.get(i)) +
                        ", Compression Ratio = " + String.format("%.2f", compressionRatios.get(i)) + ":1");
            }
        }
    }

    // Class to hold compression result
    private static class CompressionResult {
        int[][] indices;
        int originalHeight;
        int originalWidth;

        public CompressionResult(int[][] indices, int originalHeight, int originalWidth) {
            this.indices = indices;
            this.originalHeight = originalHeight;
            this.originalWidth = originalWidth;
        }
    }
}