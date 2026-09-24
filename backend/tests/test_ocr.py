"""Unit tests for OCR preprocessor and text cleaning."""

import pytest
from PIL import Image
from app.ocr.preprocessor import validate_and_load_image, preprocess_for_ocr
from app.ocr.engine import clean_ocr_text
from app.core.exceptions import InvalidInputException


def test_clean_ocr_text():
    raw_ocr = "BREAKING  NEWS\nGovern-\nment approves  ₹50,000 scholarship | ` ~"
    cleaned = clean_ocr_text(raw_ocr)
    assert "Government approves" in cleaned
    assert "₹50,000 scholarship" in cleaned
    assert "~" not in cleaned
    assert "`" not in cleaned


def test_validate_and_load_image_success(sample_test_image_bytes):
    img = validate_and_load_image(sample_test_image_bytes, max_size_mb=5)
    assert img.width == 300
    assert img.height == 150


def test_validate_empty_image_raises():
    with pytest.raises(InvalidInputException):
        validate_and_load_image(b"", max_size_mb=5)


def test_validate_corrupted_image_raises():
    with pytest.raises(InvalidInputException):
        validate_and_load_image(b"not-an-image-data-string", max_size_mb=5)


def test_preprocess_scaling():
    large_img = Image.new("RGB", (3000, 2000), color=(255, 255, 255))
    arr, resized = preprocess_for_ocr(large_img)
    assert max(resized.width, resized.height) <= 2400
