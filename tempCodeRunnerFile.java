import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class VQReportGenerator {
    private static final String HTML_REPORT_DIR = "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\html_report";
    private static final int CODEBOOK_SIZE = 256;
    private static final int BLOCK_SIZE = 2;

    /**
     * Generates an HTML report with visualizations for Vector Quantization results
     * 
     * @param numTrainImages      Number of training images used
     * @param numTestImages       Number of test images used
     * @param psnrValues          PSNR values for each test image
     * @param ssimValues          SSIM values for each test image
     * @param compressionRatios   Compression ratios for each test image
     * @param avgPsnr             Average PSNR across all test images
     * @param avgSsim             Average SSIM across all test images
     * @param avgCompressionRatio Average compression ratio across all test images
     * @param outputDir           Directory containing the reconstructed images
     * @throws IOException If an I/O error occurs
     */
    public static void generateHtmlReport(int numTrainImages, int numTestImages,
            List<Double> psnrValues, List<Double> ssimValues,
            List<Double> compressionRatios,
            double avgPsnr, double avgSsim, double avgCompressionRatio,
            String outputDir) throws IOException {

        // Create HTML report directory
        Files.createDirectories(Paths.get(HTML_REPORT_DIR));

        // Copy all reconstructed images to the HTML report directory
        File reconstructedDir = new File(outputDir);
        File[] reconstructedFiles = reconstructedDir.listFiles();
        if (reconstructedFiles != null) {
            for (File file : reconstructedFiles) {
                if (file.isFile() && file.getName().endsWith(".jpg")) {
                    Files.copy(file.toPath(),
                            Paths.get(HTML_REPORT_DIR, file.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        // Generate HTML content
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Vector Quantization Compression Report</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; line-height: 1.6; }\n");
        html.append("        h1, h2 { color: #333; }\n");
        html.append("        .container { max-width: 1200px; margin: 0 auto; }\n");
        html.append(
                "        .summary { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        .metrics { display: flex; justify-content: space-between; flex-wrap: wrap; }\n");
        html.append(
                "        .metric-card { background-color: #fff; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin-bottom: 15px; width: 30%; }\n");
        html.append("        .metric-value { font-size: 24px; font-weight: bold; color: #0066cc; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("        th { background-color: #f2f2f2; }\n");
        html.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("        .image-container { margin: 20px 0; }\n");
        html.append("        .image-pair { margin-bottom: 30px; }\n");
        html.append("        .image-pair img { max-width: 100%; border: 1px solid #ddd; }\n");
        html.append("        .chart-container { width: 100%; height: 400px; margin: 20px 0; }\n");
        html.append("    </style>\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <h1>Vector Quantization Compression Report</h1>\n");

        // Summary section
        html.append("        <div class=\"summary\">\n");
        html.append("            <h2>Summary</h2>\n");
        html.append(
                "            <p>This report presents the results of image compression using Vector Quantization.</p>\n");
        html.append("            <p>Number of training images: ").append(numTrainImages).append("</p>\n");
        html.append("            <p>Number of test images: ").append(numTestImages).append("</p>\n");
        html.append("            <p>Codebook size: ").append(CODEBOOK_SIZE).append(" vectors (").append(BLOCK_SIZE)
                .append("x").append(BLOCK_SIZE).append(" pixels each)</p>\n");
        html.append("        </div>\n");

        // Metrics section
        html.append("        <h2>Compression Results</h2>\n");
        html.append("        <div class=\"metrics\">\n");
        html.append("            <div class=\"metric-card\">\n");
        html.append("                <h3>Average PSNR</h3>\n");
        html.append("                <div class=\"metric-value\">").append(String.format("%.2f", avgPsnr))
                .append(" dB</div>\n");
        html.append(
                "                <p>Peak Signal-to-Noise Ratio measures the quality of the compressed images.</p>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card\">\n");
        html.append("                <h3>Average SSIM</h3>\n");
        html.append("                <div class=\"metric-value\">").append(String.format("%.4f", avgSsim))
                .append("</div>\n");
        html.append("                <p>Structural Similarity Index measures the perceived quality of images.</p>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card\">\n");
        html.append("                <h3>Average Compression Ratio</h3>\n");
        html.append("                <div class=\"metric-value\">").append(String.format("%.2f", avgCompressionRatio))
                .append(":1</div>\n");
        html.append("                <p>Ratio of original file size to compressed file size.</p>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Chart section
        html.append("        <h2>Performance Visualization</h2>\n");
        html.append("        <div class=\"chart-container\">\n");
        html.append("            <canvas id=\"metricsChart\"></canvas>\n");
        html.append("        </div>\n");

        // Image comparison section
        html.append("        <h2>Image Comparisons</h2>\n");
        html.append("        <div class=\"image-container\">\n");

        // Add the first 3 comparison images
        for (int i = 0; i < Math.min(3, numTestImages); i++) {
            html.append("            <div class=\"image-pair\">\n");
            html.append("                <h3>Image ").append(i + 1).append("</h3>\n");
            html.append("                <img src=\"comparison_").append(i).append(".jpg\" alt=\"Comparison ")
                    .append(i + 1).append("\">\n");
            html.append("                <p>PSNR: ").append(String.format("%.2f", psnrValues.get(i)))
                    .append(" dB, SSIM: ").append(String.format("%.4f", ssimValues.get(i)))
                    .append(", Compression Ratio: ").append(String.format("%.2f", compressionRatios.get(i)))
                    .append(":1</p>\n");
            html.append("            </div>\n");
        }

        html.append("        </div>\n");

        // Detailed results table
        html.append("        <h2>Detailed Results</h2>\n");
        html.append("        <table>\n");
        html.append("            <tr>\n");
        html.append("                <th>Image</th>\n");
        html.append("                <th>PSNR (dB)</th>\n");
        html.append("                <th>SSIM</th>\n");
        html.append("                <th>Compression Ratio</th>\n");
        html.append("                <th>Reconstructed Image</th>\n");
        html.append("            </tr>\n");

        for (int i = 0; i < numTestImages; i++) {
            html.append("            <tr>\n");
            html.append("                <td>Image ").append(i + 1).append("</td>\n");
            html.append("                <td>").append(String.format("%.2f", psnrValues.get(i))).append("</td>\n");
            html.append("                <td>").append(String.format("%.4f", ssimValues.get(i))).append("</td>\n");
            html.append("                <td>").append(String.format("%.2f", compressionRatios.get(i)))
                    .append(":1</td>\n");
            html.append("                <td><a href=\"reconstructed_").append(i)
                    .append(".jpg\" target=\"_blank\">View</a></td>\n");
            html.append("            </tr>\n");
        }

        html.append("        </table>\n");

        // JavaScript for charts
        html.append("        <script>\n");
        html.append("            // Data for charts\n");
        html.append("            const imageLabels = [");
        for (int i = 0; i < numTestImages; i++) {
            if (i > 0)
                html.append(", ");
            html.append("'Image ").append(i + 1).append("'");
        }
        html.append("];\n");

        html.append("            const psnrData = [");
        for (int i = 0; i < psnrValues.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append(String.format("%.2f", psnrValues.get(i)));
        }
        html.append("];\n");

        html.append("            const ssimData = [");
        for (int i = 0; i < ssimValues.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append(String.format("%.4f", ssimValues.get(i)));
        }
        html.append("];\n");

        html.append("            const compressionData = [");
        for (int i = 0; i < compressionRatios.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append(String.format("%.2f", compressionRatios.get(i)));
        }
        html.append("];\n");

        html.append("            // Create chart\n");
        html.append("            const ctx = document.getElementById('metricsChart').getContext('2d');\n");
        html.append("            const metricsChart = new Chart(ctx, {\n");
        html.append("                type: 'bar',\n");
        html.append("                data: {\n");
        html.append("                    labels: imageLabels,\n");
        html.append("                    datasets: [\n");
        html.append("                        {\n");
        html.append("                            label: 'PSNR (dB)',\n");
        html.append("                            data: psnrData,\n");
        html.append("                            backgroundColor: 'rgba(54, 162, 235, 0.5)',\n");
        html.append("                            borderColor: 'rgba(54, 162, 235, 1)',\n");
        html.append("                            borderWidth: 1\n");
        html.append("                        },\n");
        html.append("                        {\n");
        html.append("                            label: 'SSIM (x100)',\n");
        html.append("                            data: ssimData.map(val => val * 100),\n");
        html.append("                            backgroundColor: 'rgba(75, 192, 192, 0.5)',\n");
        html.append("                            borderColor: 'rgba(75, 192, 192, 1)',\n");
        html.append("                            borderWidth: 1\n");
        html.append("                        },\n");
        html.append("                        {\n");
        html.append("                            label: 'Compression Ratio',\n");
        html.append("                            data: compressionData,\n");
        html.append("                            backgroundColor: 'rgba(255, 99, 132, 0.5)',\n");
        html.append("                            borderColor: 'rgba(255, 99, 132, 1)',\n");
        html.append("                            borderWidth: 1\n");
        html.append("                        }\n");
        html.append("                    ]\n");
        html.append("                },\n");
        html.append("                options: {\n");
        html.append("                    responsive: true,\n");
        html.append("                    scales: {\n");
        html.append("                        y: {\n");
        html.append("                            beginAtZero: true\n");
        html.append("                        }\n");
        html.append("                    }\n");
        html.append("                }\n");
        html.append("            });\n");
        html.append("        </script>\n");

        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        // Write HTML to file
        try (PrintWriter writer = new PrintWriter(new FileWriter(HTML_REPORT_DIR + "\\index.html"))) {
            writer.write(html.toString());
        }

        System.out.println("HTML report generated at: " + HTML_REPORT_DIR + "\\index.html");
    }
}