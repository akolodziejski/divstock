import io
import sys
import pytest
from pathlib import Path
from pypdf import PdfWriter, PdfReader


def make_pdf_file(tmp_path: Path, num_pages: int, name: str = "source.pdf") -> Path:
    writer = PdfWriter()
    for _ in range(num_pages):
        writer.add_blank_page(width=72, height=72)
    pdf_path = tmp_path / name
    with open(pdf_path, "wb") as f:
        writer.write(f)
    return pdf_path


# --- single-page mode ---

def test_main_single_page_creates_files(tmp_path):
    pdf = make_pdf_file(tmp_path, 3)
    out_dir = tmp_path / "out"
    sys.argv = ["pdf_splitter", "--input", str(pdf), "--output", str(out_dir)]
    from tools.pdf_splitter.__main__ import main
    main()
    files = sorted(out_dir.glob("*.pdf"))
    assert len(files) == 3
    assert files[0].name == "page_1.pdf"
    assert files[2].name == "page_3.pdf"


def test_main_creates_output_folder_if_missing(tmp_path):
    pdf = make_pdf_file(tmp_path, 2)
    out_dir = tmp_path / "nested" / "output"
    sys.argv = ["pdf_splitter", "--input", str(pdf), "--output", str(out_dir)]
    from tools.pdf_splitter.__main__ import main
    main()
    assert out_dir.is_dir()
    assert len(list(out_dir.glob("*.pdf"))) == 2


# --- range mode ---

def test_main_range_mode_produces_one_file(tmp_path):
    pdf = make_pdf_file(tmp_path, 10)
    out_dir = tmp_path / "out"
    sys.argv = ["pdf_splitter", "--input", str(pdf), "--output", str(out_dir), "--ranges", "2-4"]
    from tools.pdf_splitter.__main__ import main
    main()
    files = list(out_dir.glob("*.pdf"))
    assert len(files) == 1
    assert files[0].name == "pages_02-04.pdf"


def test_main_range_mode_respects_page_count(tmp_path):
    pdf = make_pdf_file(tmp_path, 10)
    out_dir = tmp_path / "out"
    sys.argv = ["pdf_splitter", "--input", str(pdf), "--output", str(out_dir), "--ranges", "2-4"]
    from tools.pdf_splitter.__main__ import main
    main()
    out_file = next(out_dir.glob("*.pdf"))
    r = PdfReader(str(out_file))
    assert len(r.pages) == 3


# --- error cases ---

def test_main_exits_1_on_missing_input(tmp_path):
    out_dir = tmp_path / "out"
    sys.argv = ["pdf_splitter", "--input", str(tmp_path / "nope.pdf"), "--output", str(out_dir)]
    from tools.pdf_splitter.__main__ import main
    with pytest.raises(SystemExit) as exc:
        main()
    assert exc.value.code == 1


def test_main_exits_1_on_non_pdf_extension(tmp_path):
    txt_file = tmp_path / "file.txt"
    txt_file.write_text("hello")
    out_dir = tmp_path / "out"
    sys.argv = ["pdf_splitter", "--input", str(txt_file), "--output", str(out_dir)]
    from tools.pdf_splitter.__main__ import main
    with pytest.raises(SystemExit) as exc:
        main()
    assert exc.value.code == 1


def test_main_exits_1_on_bad_ranges(tmp_path):
    pdf = make_pdf_file(tmp_path, 5)
    out_dir = tmp_path / "out"
    sys.argv = ["pdf_splitter", "--input", str(pdf), "--output", str(out_dir), "--ranges", "99"]
    from tools.pdf_splitter.__main__ import main
    with pytest.raises(SystemExit) as exc:
        main()
    assert exc.value.code == 1
