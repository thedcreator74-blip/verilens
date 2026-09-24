"""EasyOCR text extraction engine with post-processing."""

import re
import time
from typing import Tuple, List, Optional
from PIL import Image
from app.config.settings import settings
from app.core.logging import logger
from app.core.exceptions import OCRException
from app.ocr.preprocessor import validate_and_load_image, preprocess_for_ocr
from app.utils.text_cleaner import normalize_whitespace

# Global singleton reader reference
_easyocr_reader = None


def get_ocr_reader():
    """Initializes and caches EasyOCR reader instance."""
    global _easyocr_reader
    if _easyocr_reader is None:
        try:
            import easyocr
            languages = ["en"]
            if settings.ENABLE_TAMIL_OCR:
                languages.append("ta")
            logger.info(f"Initializing EasyOCR Reader with languages: {languages}")
            _easyocr_reader = easyocr.Reader(languages, gpu=False)
            logger.info("EasyOCR Reader initialized successfully.")
        except Exception as e:
            logger.warning(f"EasyOCR could not be initialized directly: {e}. Fallback reader will be used.")
            _easyocr_reader = None
    return _easyocr_reader


def clean_ocr_text(raw_text: str) -> str:
    """
    Stitches broken lines, removes OCR noise symbols, and normalizes spacing.
    """
    if not raw_text:
        return ""

    # Replace weird OCR pipe or tilde artifacts
    cleaned = raw_text.replace("|", "I").replace("`", "'").replace("~", "")
    
    # Remove isolated non-alphanumeric junk characters (e.g. solitary ^, _, §, ¢)
    cleaned = re.sub(r"(?:^|\s)[^\w\s.,!?'\"₹$€£%@#&()-](?=\s|$)", " ", cleaned)

    # Stitch hyphenated word breaks at line ends (e.g. "govern-\nment" -> "government")
    cleaned = re.sub(r"(\w+)-\s*\n\s*(\w+)", r"\1\2", cleaned)

    # Replace multiple linebreaks with single newline
    cleaned = re.sub(r"\n{2,}", "\n", cleaned)

    # Normalize whitespace per line
    lines = [normalize_whitespace(line) for line in cleaned.split("\n")]
    stitched = " ".join([line for line in lines if line])

    return normalize_whitespace(stitched)


class OCREngine:
    """Executes OCR extraction on raw image bytes."""

    @staticmethod
    def extract_text_from_bytes(image_bytes: bytes) -> Tuple[str, float, float]:
        """
        Extracts text from raw uploaded image bytes.
        Returns: (extracted_text, average_confidence, processing_time_ms)
        """
        start_time = time.perf_counter()

        image = validate_and_load_image(image_bytes, max_size_mb=settings.MAX_IMAGE_SIZE_MB)
        preprocessed_array, _ = preprocess_for_ocr(image)

        reader = get_ocr_reader()

        if reader is not None:
            try:
                # detail=1 returns [([box], text, confidence), ...]
                results = reader.readtext(preprocessed_array, detail=1)
                
                if not results:
                    # Retry with original image array
                    import numpy as np
                    results = reader.readtext(np.array(image), detail=1)

                if not results:
                    elapsed_ms = (time.perf_counter() - start_time) * 1000
                    return "", 0.0, elapsed_ms

                text_fragments = []
                confidences = []

                for item in results:
                    if len(item) >= 3:
                        _, text, conf = item[:3]
                        if conf > 0.2 and text.strip():
                            text_fragments.append(text.strip())
                            confidences.append(float(conf))

                raw_combined = " ".join(text_fragments)
                cleaned_text = clean_ocr_text(raw_combined)
                avg_confidence = float(sum(confidences) / len(confidences)) if confidences else 0.0
                elapsed_ms = (time.perf_counter() - start_time) * 1000

                logger.info(f"OCR extracted {len(cleaned_text)} chars in {elapsed_ms:.2f}ms (conf: {avg_confidence:.2f})")
                return cleaned_text, round(avg_confidence, 2), round(elapsed_ms, 2)

            except Exception as e:
                logger.error(f"EasyOCR extraction failed: {e}", exc_info=True)
                # Fallback to empty text
                elapsed_ms = (time.perf_counter() - start_time) * 1000
                return "", 0.0, elapsed_ms

        # In testing or minimal environment where EasyOCR weights aren't downloaded
        elapsed_ms = (time.perf_counter() - start_time) * 1000
        logger.info(f"OCR simulated/fallback processed in {elapsed_ms:.2f}ms")
        return "", 0.0, elapsed_ms
