import io
import pytest
from pathlib import Path
from pypdf import PdfWriter, PdfReader

from tools.pdf_splitter.splitter import parse_ranges, split_by_pages, split_by_ranges


def make_reader(num_pages: int) -> PdfReader:
    writer = PdfWriter()
    for _ in range(num_pages):
        writer.add_blank_page(width=72, height=72)
    buf = io.BytesIO()
    writer.write(buf)
    buf.seek(0)
    return PdfReader(buf)


# --- parse_ranges ---

def test_parse_ranges_single_page():
    assert parse_ranges("5", 10) == [(5, 5)]


def test_parse_ranges_range():
    assert parse_ranges("1-3", 10) == [(1, 3)]


def test_parse_ranges_multiple_segments():
    assert parse_ranges("1-3,5,7-10", 10) == [(1, 3), (5, 5), (7, 10)]


def test_parse_ranges_out_of_bounds_high():
    with pytest.raises(ValueError, match="out of range"):
        parse_ranges("5", 3)


def test_parse_ranges_out_of_bounds_low():
    with pytest.raises(ValueError, match="out of range"):
        parse_ranges("0", 10)


def test_parse_ranges_end_less_than_start():
    with pytest.raises(ValueError, match="end must be >= start"):
        parse_ranges("5-3", 10)


def test_parse_ranges_malformed_non_numeric():
    with pytest.raises(ValueError, match="Invalid range format"):
        parse_ranges("abc", 10)


def test_parse_ranges_malformed_extra_dash():
    with pytest.raises(ValueError, match="Invalid range format"):
        parse_ranges("1-2-3", 10)
