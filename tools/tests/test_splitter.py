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


def test_parse_ranges_range_start_out_of_bounds():
    with pytest.raises(ValueError, match="out of range"):
        parse_ranges("11-12", 10)


# --- split_by_pages ---

def test_split_by_pages_creates_correct_number_of_files(tmp_path):
    reader = make_reader(3)
    count = split_by_pages(reader, tmp_path, 3)
    assert count == 3
    assert len(list(tmp_path.glob("*.pdf"))) == 3


def test_split_by_pages_single_digit_padding(tmp_path):
    reader = make_reader(5)
    split_by_pages(reader, tmp_path, 5)
    names = sorted(p.name for p in tmp_path.glob("*.pdf"))
    assert names == ["page_1.pdf", "page_2.pdf", "page_3.pdf", "page_4.pdf", "page_5.pdf"]


def test_split_by_pages_three_digit_padding(tmp_path):
    reader = make_reader(100)
    split_by_pages(reader, tmp_path, 100)
    names = sorted(p.name for p in tmp_path.glob("*.pdf"))
    assert names[0] == "page_001.pdf"
    assert names[-1] == "page_100.pdf"
    assert len(names) == 100


def test_split_by_pages_each_file_has_one_page(tmp_path):
    reader = make_reader(3)
    split_by_pages(reader, tmp_path, 3)
    for pdf_file in tmp_path.glob("*.pdf"):
        r = PdfReader(str(pdf_file))
        assert len(r.pages) == 1
