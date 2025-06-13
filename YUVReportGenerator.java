import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class YUVReportGenerator {
    private static final String HTML_REPORT_DIR = "c:\\Users\\DELL\\Desktop\\Final_Project_IT\\yuv_html_report";
    private static final int CODEBOOK_SIZE = 256;
    private static final int BLOCK_SIZE = 2;

    /**
     * Generates an HTML report with visualizations for YUV Vector Quantization
     * results
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
        html.append("    <title>YUV Vector Quantization Compression Report</title>\n");
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
        html.append("        <h1>YUV Vector Quantization Compression Report</h1>\n");

        // Summary section
        html.append("        <div class=\"summary\">\n");
        html.append("            <h2>Summary</h2>\n");
        html.append(
                "            <p>This report presents the results of YUV Vector Quantization compression applied to a set of test images.</p>\n");
        html.append("            <p>Number of training images: " + numTrainImages + "</p>\n");
        html.append("            <p>Number of test images: " + numTestImages + "</p>\n");
        html.append("            <p>Codebook size: " + CODEBOOK_SIZE + " vectors (" + BLOCK_SIZE + "x" + BLOCK_SIZE
                + " pixels each)</p>\n");
        html.append("            <p>U and V channels subsampled to 50% width and 50% height</p>\n");
        html.append("        </div>\n");

        // Metrics section
        html.append("        <h2>Compression Metrics</h2>\n");
        html.append("        <div class=\"metrics\">\n");
        html.append("            <div class=\"metric-card\">\n");
        html.append("                <h3>Average PSNR</h3>\n");
        html.append("                <div class=\"metric-value\">" + String.format("%.2f", avgPsnr) + " dB</div>\n");
        html.append("                <p>Peak Signal-to-Noise Ratio</p>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card\">\n");
        html.append("                <h3>Average SSIM</h3>\n");
        html.append("                <div class=\"metric-value\">" + String.format("%.4f", avgSsim) + "</div>\n");
        html.append("                <p>Structural Similarity Index</p>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card\">\n");
        html.append("                <h3>Average Compression Ratio</h3>\n");
        html.append("                <div class=\"metric-value\">" + String.format("%.2f", avgCompressionRatio)
                + ":1</div>\n");
        html.append("                <p>Original size : Compressed size</p>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Charts section
        html.append("        <h2>Performance Charts</h2>\n");

        // PSNR Chart
        html.append("        <div class=\"chart-container\">\n");
        html.append("            <canvas id=\"psnrChart\"></canvas>\n");
        html.append("        </div>\n");

        // SSIM Chart
        html.append("        <div class=\"chart-container\">\n");
        html.append("            <canvas id=\"ssimChart\"></canvas>\n");
        html.append("        </div>\n");

        // Compression Ratio Chart
        html.append("        <div class=\"chart-container\">\n");
        html.append("            <canvas id=\"compressionChart\"></canvas>\n");
        html.append("        </div>\n");

        // Detailed results table
        html.append("        <h2>Detailed Results</h2>\n");
        html.append("        <table>\n");
        html.append("            <tr>\n");
        html.append("                <th>Image</th>\n");
        html.append("                <th>PSNR (dB)</th>\n");
        html.append("                <th>SSIM</th>\n");
        html.append("                <th>Compression Ratio</th>\n");
        html.append("            </tr>\n");

        for (int i = 0; i < psnrValues.size(); i++) {
            html.append("            <tr>\n");
            html.append("                <td>Image " + (i + 1) + "</td>\n");
            html.append("                <td>" + String.format("%.2f", psnrValues.get(i)) + "</td>\n");
            html.append("                <td>" + String.format("%.4f", ssimValues.get(i)) + "</td>\n");
            html.append("                <td>" + String.format("%.2f", compressionRatios.get(i)) + ":1</td>\n");
            html.append("            </tr>\n");
        }

        html.append("        </table>\n");

        // Image comparison section
        html.append("        <h2>Image Comparisons</h2>\n");
        html.append("        <div class=\"image-container\">\n");

        // Add the first 3 comparison images (or fewer if less are available)
        for (int i = 0; i < Math.min(3, psnrValues.size()); i++) {
            html.append("            <div class=\"image-pair\">\n");
            html.append("                <h3>Image " + (i + 1) + "</h3>\n");
            html.append("                <p>PSNR: " + String.format("%.2f", psnrValues.get(i)) +
                    " dB, SSIM: " + String.format("%.4f", ssimValues.get(i)) +
                    ", Compression Ratio: " + String.format("%.2f", compressionRatios.get(i)) + ":1</p>\n");
            html.append("                <img src=\"yuv_comparison_" + i
                    + ".jpg\" alt=\"Comparison of original and reconstructed image " + (i + 1) + "\">\n");
            html.append("            </div>\n");
        }

        html.append("        </div>\n");

        // JavaScript for charts
        html.append("        <script>\n");

        // Labels for charts (Image 1, Image 2, etc.)
        html.append("            const labels = [");
        for (int i = 0; i < psnrValues.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append("'Image " + (i + 1) + "'");
        }
        html.append("];\n");

        // PSNR Chart
        html.append("            const psnrCtx = document.getElementById('psnrChart').getContext('2d');\n");
        html.append("            new Chart(psnrCtx, {\n");
        html.append("                type: 'bar',\n");
        html.append("                data: {\n");
        html.append("                    labels: labels,\n");
        html.append("                    datasets: [{\n");
        html.append("                        label: 'PSNR (dB)',\n");
        html.append("                        data: [");
        for (int i = 0; i < psnrValues.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append(String.format("%.2f", psnrValues.get(i)));
        }
        html.append("],\n");
        html.append("                        backgroundColor: 'rgba(54, 162, 235, 0.5)',\n");
        html.append("                        borderColor: 'rgba(54, 162, 235, 1)',\n");
        html.append("                        borderWidth: 1\n");
        html.append("                    }]\n");
        html.append("                },\n");
        html.append("                options: {\n");
        html.append("                    responsive: true,\n");
        html.append("                    plugins: {\n");
        html.append("                        title: {\n");
        html.append("                            display: true,\n");
        html.append("                            text: 'PSNR Values for Each Image'\n");
        html.append("                        },\n");
        html.append("                    },\n");
        html.append("                    scales: {\n");
        html.append("                        y: {\n");
        html.append("                            beginAtZero: false,\n");
        html.append("                            title: {\n");
        html.append("                                display: true,\n");
        html.append("                                text: 'PSNR (dB)'\n");
        html.append("                            }\n");
        html.append("                        }\n");
        html.append("                    }\n");
        html.append("                }\n");
        html.append("            });\n");

        // SSIM Chart
        html.append("            const ssimCtx = document.getElementById('ssimChart').getContext('2d');\n");
        html.append("            new Chart(ssimCtx, {\n");
        html.append("                type: 'bar',\n");
        html.append("                data: {\n");
        html.append("                    labels: labels,\n");
        html.append("                    datasets: [{\n");
        html.append("                        label: 'SSIM',\n");
        html.append("                        data: [");
        for (int i = 0; i < ssimValues.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append(String.format("%.4f", ssimValues.get(i)));
        }
        html.append("],\n");
        html.append("                        backgroundColor: 'rgba(75, 192, 192, 0.5)',\n");
        html.append("                        borderColor: 'rgba(75, 192, 192, 1)',\n");
        html.append("                        borderWidth: 1\n");
        html.append("                    }]\n");
        html.append("                },\n");
        html.append("                options: {\n");
        html.append("                    responsive: true,\n");
        html.append("                    plugins: {\n");
        html.append("                        title: {\n");
        html.append("                            display: true,\n");
        html.append("                            text: 'SSIM Values for Each Image'\n");
        html.append("                        },\n");
        html.append("                    },\n");
        html.append("                    scales: {\n");
        html.append("                        y: {\n");
        html.append("                            beginAtZero: false,\n");
        html.append("                            title: {\n");
        html.append("                                display: true,\n");
        html.append("                                text: 'SSIM'\n");
        html.append("                            }\n");
        html.append("                        }\n");
        html.append("                    }\n");
        html.append("                }\n");
        html.append("            });\n");

        // Compression Ratio Chart
        html.append(
                "            const compressionCtx = document.getElementById('compressionChart').getContext('2d');\n");
        html.append("            new Chart(compressionCtx, {\n");
        html.append("                type: 'bar',\n");
        html.append("                data: {\n");
        html.append("                    labels: labels,\n");
        html.append("                    datasets: [{\n");
        html.append("                        label: 'Compression Ratio',\n");
        html.append("                        data: [");
        for (int i = 0; i < compressionRatios.size(); i++) {
            if (i > 0)
                html.append(", ");
            html.append(String.format("%.2f", compressionRatios.get(i)));
        }
        html.append("],\n");
        html.append("                        backgroundColor: 'rgba(255, 159, 64, 0.5)',\n");
        html.append("                        borderColor: 'rgba(255, 159, 64, 1)',\n");
        html.append("                        borderWidth: 1\n");
        html.append("                    }]\n");
        html.append("                },\n");
        html.append("                options: {\n");
        html.append("                    responsive: true,\n");
        html.append("                    plugins: {\n");
        html.append("                        title: {\n");
        html.append("                            display: true,\n");
        html.append("                            text: 'Compression Ratios for Each Image'\n");
        html.append("                        },\n");
        html.append("                    },\n");
        html.append("                    scales: {\n");
        html.append("                        y: {\n");
        html.append("                            beginAtZero: false,\n");
        html.append("                            title: {\n");
        html.append("                                display: true,\n");
        html.append("                                text: 'Compression Ratio (x:1)'\n");
        html.append("                            }\n");
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

        System.out.println("YUV HTML report generated at: " + HTML_REPORT_DIR + "\\index.html");
    }
}