"""Image preprocessing pipeline for OCR accuracy optimization."""

import io
from typing import Tuple
from PIL import Image, ImageEnhance, ImageFilter
from app.core.exceptions import InvalidInputException, OCRException
from app.core.logging import logger

try:
    import cv2
    import numpy as np
    HAVE_CV2 = True
except ImportError:
    HAVE_CV2 = False


def validate_and_load_image(image_bytes: bytes, max_size_mb: int = 10) -> Image.Image:
    """
    Validates uploaded image byte stream, dimensions, and corruptions.
    """
    if not image_bytes:
        raise InvalidInputException("Uploaded image payload is completely empty.")

    size_mb = len(image_bytes) / (1024 * 1024)
    if size_mb > max_size_mb:
        raise InvalidInputException(
            f"Image file size ({size_mb:.2f} MB) exceeds maximum permissible threshold ({max_size_mb} MB)."
        )

    try:
        image = Image.open(io.BytesIO(image_bytes))
        image.verify()  # Verifies file integrity without decoding
        # Re-open for actual processing because verify() corrupts descriptor
        image = Image.open(io.BytesIO(image_bytes))
    except Exception as e:
        raise InvalidInputException(f"Invalid or corrupted image format: {str(e)}")

    if image.width < 50 or image.height < 50:
        raise InvalidInputException(f"Image dimensions ({image.width}x{image.height}) are too small for OCR analysis.")

    return image


def preprocess_for_ocr(image: Image.Image) -> Tuple[np.ndarray, Image.Image]:
    """
    Enhances screenshot text contrast, removes noise, and creates optimized OCR array.
    """
    # Convert RGBA or CMYK to standard RGB
    if image.mode != "RGB":
        image = image.convert("RGB")

    # Resize if extremely large to prevent OOM / keep processing under 2 seconds
    max_dimension = 2400
    if max(image.width, image.height) > max_dimension:
        scale = max_dimension / float(max(image.width, image.height))
        new_size = (int(image.width * scale), int(image.height * scale))
        image = image.resize(new_size, Image.Resampling.LANCZOS)

    if HAVE_CV2:
        try:
            # Convert PIL RGB to OpenCV BGR
            open_cv_image = np.array(image)
            open_cv_image = open_cv_image[:, :, ::-1].copy()

            # 1. Grayscale
            gray = cv2.cvtColor(open_cv_image, cv2.COLOR_BGR2GRAY)

            # 2. Contrast limited adaptive histogram equalization (CLAHE)
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
            contrast_enhanced = clahe.apply(gray)

            # 3. Denoising
            denoised = cv2.bilateralFilter(contrast_enhanced, 9, 75, 75)

            # 4. Otsu Adaptive Thresholding for crisp foreground text
            _, binary = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

            return binary, image
        except Exception as e:
            logger.warning(f"OpenCV enhancement failed: {e}. Falling back to Pillow pipeline.")

    # Pillow fallback enhancement
    enhanced_pil = image.convert("L")  # Grayscale
    enhancer = ImageEnhance.Contrast(enhanced_pil)
    enhanced_pil = enhancer.enhance(2.0)
    enhanced_pil = enhanced_pil.filter(ImageFilter.SHARPEN)
    
    # Return as numpy array
    import numpy as np
    return np.array(enhanced_pil), image
