# Information-Theory-Project-YUV-Compression-Vector-Quantization-

# YUV Vector Quantization Image Compression

This project demonstrates image compression using **Vector Quantization (VQ)** in the **YUV color space**, with subsampling of chrominance channels and a fixed codebook for quantization.

---

---

## 🔍 Methodology

### 1. **Color Space Conversion**
- Convert RGB images to **YUV**.
- Subsample **U and V channels** to 50% width and height to exploit chrominance sensitivity.

### 2. **Vector Quantization**
- Divide Y, U, and V channels into **2x2 blocks**.
- Flatten each block into a vector and use **K-Means clustering** to generate a **codebook** of 256 vectors.
- Replace blocks with the nearest codebook index.

### 3. **Compression**
- Store the indices of vectors instead of full image data.
- Store the subsampled U and V channels at lower resolution.

### 4. **Decompression**
- Reconstruct each image from the quantized indices and codebook.
- Upsample U and V channels.
- Convert back to RGB for display.

---

## 📈 Results Summary

### 🟪 YUV Vector Quantization (with U/V subsampling)

- **Average PSNR**: 23.74 dB  
- **Average SSIM**: 0.9784  
- **Average Compression Ratio**: 8.09:1

### 🟨 Vector Quantization (RGB, no subsampling)

- **Average PSNR**: 26.65 dB  
- **Average SSIM**: 0.9799  
- **Average Compression Ratio**: 4.03:1

> Subsampling improves compression ratio significantly, at the cost of a slight drop in image quality.

---

## 📊 Sample Evaluation Metrics

| Image    | PSNR (dB) | SSIM   | Compression Ratio |
|----------|-----------|--------|-------------------|
| Image 1  | 22.90     | 0.9829 | 8.09:1            |
| Image 4  | 30.14     | 0.9948 | 8.07:1            |
| Image 13 | 29.82     | 0.9871 | 8.00:1            |
| Image 2  | 20.19     | 0.9669 | 8.08:1            |

---

## 🛠️ How to Run

### Prerequisites
- Python 3.x
- NumPy, OpenCV, scikit-learn, scikit-image, Matplotlib

### Step-by-Step

```bash
# 1. Generate the codebook using training images
python codebook_generator.py

# 2. Compress the test images
python compressor.py

# 3. Decompress the images for evaluation
python decompressor.py

# 4. Evaluate compression quality
python evaluation.py


