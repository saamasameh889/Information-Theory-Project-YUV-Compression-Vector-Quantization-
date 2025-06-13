import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class YUVCompression {
    private static final String OUTPUT_DIR = "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\yuv_reconstructed_images";
    private static final int CODEBOOK_SIZE = 256;
    private static final int BLOCK_SIZE = 2;

    public static void runYUVCompression(List<BufferedImage> trainImages, List<BufferedImage> testImages)
            throws IOException {
        // Create output directory for YUV reconstructed images
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        System.out.println("\n\n========== YUV COMPRESSION ==========");
        System.out.println("Converting images to YUV color space...");

        // Extract blocks from all training images for YUV channels
        System.out.println("Extracting blocks from training images (YUV)...");
        List<double[]> yBlocks = new ArrayList<>();
        List<double[]> uBlocks = new ArrayList<>();
        List<double[]> vBlocks = new ArrayList<>();

        for (BufferedImage img : trainImages) {
            // Convert to YUV
            double[][][] yuvImage = convertRGBtoYUV(img);

            // Apply subsampling to U and V channels (50% width and height)
            double[][] uSubsampled = subsampleChannel(yuvImage[1]);
            double[][] vSubsampled = subsampleChannel(yuvImage[2]);

            // Extract blocks from each channel
            extractYUVBlocksFromImage(yuvImage[0], uSubsampled, vSubsampled, yBlocks, uBlocks, vBlocks);
            System.out.print(".");
        }
        System.out.println();

        System.out.println("Extracted " + yBlocks.size() + " blocks for Y channel");
        System.out.println("Extracted " + uBlocks.size() + " blocks for U channel");
        System.out.println("Extracted " + vBlocks.size() + " blocks for V channel");

        // Create codebooks for each channel
        System.out.println("Creating YUV codebooks...");
        double[][] yCodebook = createCodebook(yBlocks, CODEBOOK_SIZE);
        double[][] uCodebook = createCodebook(uBlocks, CODEBOOK_SIZE);
        double[][] vCodebook = createCodebook(vBlocks, CODEBOOK_SIZE);

        // Save codebooks
        saveCodebook(yCodebook, "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\y_codebook.dat");
        saveCodebook(uCodebook, "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\u_codebook.dat");
        saveCodebook(vCodebook, "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\v_codebook.dat");

        System.out.println("YUV Codebooks created and saved");

        // Compress and decompress test images using YUV
        System.out.println("Compressing and decompressing test images using YUV...");
        List<Double> yuvPsnrValues = new ArrayList<>();
        List<Double> yuvSsimValues = new ArrayList<>();
        List<Double> yuvCompressionRatios = new ArrayList<>();

        for (int idx = 0; idx < testImages.size(); idx++) {
            BufferedImage img = testImages.get(idx);

            // Convert to YUV
            double[][][] yuvImage = convertRGBtoYUV(img);

            // Apply subsampling to U and V channels
            double[][] uSubsampled = subsampleChannel(yuvImage[1]);
            double[][] vSubsampled = subsampleChannel(yuvImage[2]);

            // Compress each channel
            YUVCompressionResult yCompressed = compressYUVChannel(yuvImage[0], yCodebook);
            YUVCompressionResult uCompressed = compressYUVChannel(uSubsampled, uCodebook);
            YUVCompressionResult vCompressed = compressYUVChannel(vSubsampled, vCodebook);

            // Calculate compression ratio
            int originalSize = img.getWidth() * img.getHeight() * 3 * 8; // 8 bits per channel
            int compressedSize = (yCompressed.indices.length * yCompressed.indices[0].length +
                    uCompressed.indices.length * uCompressed.indices[0].length +
                    vCompressed.indices.length * vCompressed.indices[0].length) * 8; // 8 bits per index
            double compressionRatio = (double) originalSize / compressedSize;
            yuvCompressionRatios.add(compressionRatio);

            // Decompress each channel
            double[][] yReconstructed = decompressYUVChannel(yCompressed.indices, yCodebook, yuvImage[0].length,
                    yuvImage[0][0].length);
            double[][] uReconstructed = decompressYUVChannel(uCompressed.indices, uCodebook, uSubsampled.length,
                    uSubsampled[0].length);
            double[][] vReconstructed = decompressYUVChannel(vCompressed.indices, vCodebook, vSubsampled.length,
                    vSubsampled[0].length);

            // Upsample U and V channels back to original size
            double[][] uUpsampled = upsampleChannel(uReconstructed, yuvImage[0].length, yuvImage[0][0].length);
            double[][] vUpsampled = upsampleChannel(vReconstructed, yuvImage[0].length, yuvImage[0][0].length);

            // Convert back to RGB
            BufferedImage reconstructed = convertYUVtoRGB(yReconstructed, uUpsampled, vUpsampled);

            // Save reconstructed image
            ImageIO.write(reconstructed, "jpg", new File(OUTPUT_DIR + "\\reconstructed_yuv_" + idx + ".jpg"));

            // Calculate PSNR and SSIM
            double imgPsnr = VectorQuantization.calculatePSNR(img, reconstructed);
            double imgSsim = VectorQuantization.calculateSSIM(img, reconstructed);

            yuvPsnrValues.add(imgPsnr);
            yuvSsimValues.add(imgSsim);

            System.out.println("YUV Image " + (idx + 1) + ": PSNR = " + String.format("%.2f", imgPsnr) +
                    " dB, SSIM = " + String.format("%.4f", imgSsim) +
                    ", Compression Ratio = " + String.format("%.2f", compressionRatio) + ":1");

            // Save comparison image for first 3 examples
            if (idx < 3) {
                saveComparisonImage(img, reconstructed, imgPsnr, imgSsim, idx);
            }
        }

        // Calculate average metrics
        double avgYuvPsnr = yuvPsnrValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgYuvSsim = yuvSsimValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgYuvCompressionRatio = yuvCompressionRatios.stream().mapToDouble(Double::doubleValue).average()
                .orElse(0);

        System.out.println("YUV Average PSNR: " + String.format("%.2f", avgYuvPsnr) + " dB");
        System.out.println("YUV Average SSIM: " + String.format("%.4f", avgYuvSsim));
        System.out.println("YUV Average Compression Ratio: " + String.format("%.2f", avgYuvCompressionRatio) + ":1");

        // Generate YUV report
        generateYUVReport(trainImages.size(), testImages.size(), yuvPsnrValues, yuvSsimValues, yuvCompressionRatios,
                avgYuvPsnr, avgYuvSsim, avgYuvCompressionRatio);

        // Generate HTML report with visualizations
        YUVReportGenerator.generateHtmlReport(trainImages.size(), testImages.size(), yuvPsnrValues, yuvSsimValues,
                yuvCompressionRatios, avgYuvPsnr, avgYuvSsim, avgYuvCompressionRatio, OUTPUT_DIR);
    }

    // Convert RGB image to YUV color space
    private static double[][][] convertRGBtoYUV(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();

        double[][] yChannel = new double[height][width];
        double[][] uChannel = new double[height][width];
        double[][] vChannel = new double[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color color = new Color(image.getRGB(j, i));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                // RGB to YUV conversion
                yChannel[i][j] = 0.299 * r + 0.587 * g + 0.114 * b;
                uChannel[i][j] = -0.14713 * r - 0.28886 * g + 0.436 * b + 128;
                vChannel[i][j] = 0.615 * r - 0.51499 * g - 0.10001 * b + 128;
            }
        }

        return new double[][][] { yChannel, uChannel, vChannel };
    }

    // Convert YUV back to RGB
    private static BufferedImage convertYUVtoRGB(double[][] yChannel, double[][] uChannel, double[][] vChannel) {
        int height = yChannel.length;
        int width = yChannel[0].length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double y = yChannel[i][j];
                double u = uChannel[i][j] - 128;
                double v = vChannel[i][j] - 128;

                // YUV to RGB conversion
                int r = (int) Math.round(y + 1.13983 * v);
                int g = (int) Math.round(y - 0.39465 * u - 0.58060 * v);
                int b = (int) Math.round(y + 2.03211 * u);

                // Clamp values to valid range
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                Color color = new Color(r, g, b);
                image.setRGB(j, i, color.getRGB());
            }
        }

        return image;
    }

    // Subsample a channel to 50% width and height
    private static double[][] subsampleChannel(double[][] channel) {
        int originalHeight = channel.length;
        int originalWidth = channel[0].length;
        int newHeight = originalHeight / 2;
        int newWidth = originalWidth / 2;

        double[][] subsampled = new double[newHeight][newWidth];

        for (int i = 0; i < newHeight; i++) {
            for (int j = 0; j < newWidth; j++) {
                // Average 2x2 block
                subsampled[i][j] = (channel[i * 2][j * 2] +
                        (i * 2 + 1 < originalHeight ? channel[i * 2 + 1][j * 2] : channel[i * 2][j * 2]) +
                        (j * 2 + 1 < originalWidth ? channel[i * 2][j * 2 + 1] : channel[i * 2][j * 2]) +
                        (i * 2 + 1 < originalHeight && j * 2 + 1 < originalWidth ? channel[i * 2 + 1][j * 2 + 1]
                                : channel[i * 2][j * 2]))
                        / 4.0;
            }
        }

        return subsampled;
    }

    // Upsample a channel back to original size
    private static double[][] upsampleChannel(double[][] channel, int targetHeight, int targetWidth) {
        int originalHeight = channel.length;
        int originalWidth = channel[0].length;

        double[][] upsampled = new double[targetHeight][targetWidth];

        for (int i = 0; i < targetHeight; i++) {
            for (int j = 0; j < targetWidth; j++) {
                // Nearest neighbor interpolation
                upsampled[i][j] = channel[Math.min(i / 2, originalHeight - 1)][Math.min(j / 2, originalWidth - 1)];
            }
        }

        return upsampled;
    }

    // Extract blocks from YUV channels
    private static void extractYUVBlocksFromImage(double[][] yChannel, double[][] uChannel, double[][] vChannel,
            List<double[]> yBlocks, List<double[]> uBlocks, List<double[]> vBlocks) {
        // Extract Y blocks
        int yHeight = yChannel.length;
        int yWidth = yChannel[0].length;
        int yHPad = yHeight - (yHeight % BLOCK_SIZE);
        int yWPad = yWidth - (yWidth % BLOCK_SIZE);

        for (int i = 0; i < yHPad; i += BLOCK_SIZE) {
            for (int j = 0; j < yWPad; j += BLOCK_SIZE) {
                double[] yBlock = new double[BLOCK_SIZE * BLOCK_SIZE];
                int idx = 0;
                for (int bi = 0; bi < BLOCK_SIZE; bi++) {
                    for (int bj = 0; bj < BLOCK_SIZE; bj++) {
                        yBlock[idx++] = yChannel[i + bi][j + bj];
                    }
                }
                yBlocks.add(yBlock);
            }
        }

        // Extract U and V blocks
        int uvHeight = uChannel.length;
        int uvWidth = uChannel[0].length;
        int uvHPad = uvHeight - (uvHeight % BLOCK_SIZE);
        int uvWPad = uvWidth - (uvWidth % BLOCK_SIZE);

        for (int i = 0; i < uvHPad; i += BLOCK_SIZE) {
            for (int j = 0; j < uvWPad; j += BLOCK_SIZE) {
                double[] uBlock = new double[BLOCK_SIZE * BLOCK_SIZE];
                double[] vBlock = new double[BLOCK_SIZE * BLOCK_SIZE];
                int idx = 0;
                for (int bi = 0; bi < BLOCK_SIZE; bi++) {
                    for (int bj = 0; bj < BLOCK_SIZE; bj++) {
                        if (i + bi < uvHeight && j + bj < uvWidth) {
                            uBlock[idx] = uChannel[i + bi][j + bj];
                            vBlock[idx] = vChannel[i + bi][j + bj];
                        }
                        idx++;
                    }
                }
                uBlocks.add(uBlock);
                vBlocks.add(vBlock);
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

    // Compress a YUV channel
    private static YUVCompressionResult compressYUVChannel(double[][] channel, double[][] codebook) {
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
                        if (i + bi < height && j + bj < width) {
                            block[idx] = channel[i + bi][j + bj];
                        }
                        idx++;
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

        return new YUVCompressionResult(indices, height, width);
    }

    // Decompress a YUV channel
    private static double[][] decompressYUVChannel(int[][] indices, double[][] codebook, int originalHeight,
            int originalWidth) {
        // Ensure dimensions are divisible by block size
        int hPad = originalHeight - (originalHeight % BLOCK_SIZE);
        int wPad = originalWidth - (originalWidth % BLOCK_SIZE);

        double[][] reconstructed = new double[originalHeight][originalWidth];

        for (int i = 0; i < indices.length; i++) {
            for (int j = 0; j < indices[0].length; j++) {
                int codeIdx = indices[i][j];
                double[] codeVector = codebook[codeIdx];

                // Place the block in the reconstructed image
                int idx = 0;
                for (int bi = 0; bi < BLOCK_SIZE; bi++) {
                    for (int bj = 0; bj < BLOCK_SIZE; bj++) {
                        if (i * BLOCK_SIZE + bi < hPad && j * BLOCK_SIZE + bj < wPad) {
                            reconstructed[i * BLOCK_SIZE + bi][j * BLOCK_SIZE + bj] = codeVector[idx];
                        }
                        idx++;
                    }
                }
            }
        }

        return reconstructed;
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
        g2d.drawString("YUV Reconstructed", width + 10, 20);
        g2d.drawString(String.format("PSNR: %.2f dB, SSIM: %.4f", psnr, ssim), width + 10, 40);

        g2d.dispose();

        // Save comparison image
        ImageIO.write(comparison, "jpg", new File(OUTPUT_DIR + "\\yuv_comparison_" + idx + ".jpg"));
    }

    // Generate YUV report
    private static void generateYUVReport(int numTrainImages, int numTestImages,
            List<Double> psnrValues, List<Double> ssimValues,
            List<Double> compressionRatios,
            double avgPsnr, double avgSsim, double avgCompressionRatio) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter("c:\\Users\\DELL\\Desktop\\Final_Project_IT\\yuv_report.txt"))) {
            writer.println("YUV Vector Quantization Compression Report");
            writer.println("=========================================\n");
            writer.println("Number of training images: " + numTrainImages);
            writer.println("Number of test images: " + numTestImages);
            writer.println("Codebook size: " + CODEBOOK_SIZE + " vectors (" + BLOCK_SIZE + "x" + BLOCK_SIZE
                    + " pixels each)");
            writer.println("U and V channels subsampled to 50% width and 50% height\n");

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

    // Class to hold YUV compression result
    private static class YUVCompressionResult {
        int[][] indices;
        int originalHeight;
        int originalWidth;

        public YUVCompressionResult(int[][] indices, int originalHeight, int originalWidth) {
            this.indices = indices;
            this.originalHeight = originalHeight;
            this.originalWidth = originalWidth;
        }
    }
}