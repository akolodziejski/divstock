# PDF Splitter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Python CLI tool that splits a PDF into one-file-per-page or by explicit page ranges.

**Architecture:** A single `splitter.py` module holds all logic (range parsing + file writing); `__main__.py` handles CLI arg parsing, validation, and calls into `splitter.py`. Tests live in `tools/tests/` alongside existing tests and use in-memory PDF fixtures built with `pypdf`.

**Tech Stack:** Python 3.10+, `pypdf>=4.0.0`, `pytest`

---

## File Map

| Path | Action | Purpose |
|------|--------|---------|
| `tools/pdf_splitter/__init__.py` | Create | Empty package marker |
| `tools/pdf_splitter/requirements.txt` | Create | `pypdf>=4.0.0` |
| `tools/pdf_splitter/splitter.py` | Create | `parse_ranges`, `split_by_pages`, `split_by_ranges` |
| `tools/pdf_splitter/__main__.py` | Create | CLI entry point |
| `tools/tests/test_splitter.py` | Create | Unit tests for `splitter.py` |
| `tools/tests/test_pdf_splitter_main.py` | Create | Integration tests for `__main__.py` |

---

## Task 1: Scaffold the module

**Files:**
- Create: `tools/pdf_splitter/__init__.py`
- Create: `tools/pdf_splitter/requirements.txt`

- [ ] **Step 1: Create the package directory and empty `__init__.py`**

```bash
mkdir tools/pdf_splitter
touch tools/pdf_splitter/__init__.py
```

- [ ] **Step 2: Create `requirements.txt`**

`tools/pdf_splitter/requirements.txt`:
```
pypdf>=4.0.0
pytest>=8.0.0
```

- [ ] **Step 3: Install the dependency into the existing venv**

```bash
cd tools
pip install pypdf>=4.0.0
```

Expected: `Successfully installed pypdf-...` (or already satisfied)

- [ ] **Step 4: Commit**

```bash
git add tools/pdf_splitter/
git commit -m "feat: scaffold tools/pdf_splitter module"
```

---

## Task 2: Implement `parse_ranges`

Parses a string like `"1-3,5,7-10"` into a list of `(start, end)` tuples (1-based, inclusive). Raises `ValueError` on any invalid input.

**Files:**
- Create: `tools/pdf_splitter/splitter.py`
- Create: `tools/tests/test_splitter.py`

- [ ] **Step 1: Write the failing tests**

`tools/tests/test_splitter.py`:
```python
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
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd tools
python -m pytest tests/test_splitter.py -v -k "parse_ranges"
```

Expected: `ImportError` or `ModuleNotFoundError` — `splitter` not yet defined.

- [ ] **Step 3: Create `splitter.py` with `parse_ranges`**

`tools/pdf_splitter/splitter.py`:
```python
from pathlib import Path
from pypdf import PdfReader, PdfWriter


def parse_ranges(ranges_str: str, total_pages: int) -> list[tuple[int, int]]:
    segments = []
    for part in ranges_str.split(","):
        part = part.strip()
        if not part:
            raise ValueError(f"Invalid range format: '{part}'")
        if "-" in part:
            halves = part.split("-", 1)
            start_s, end_s = halves[0].strip(), halves[1].strip()
            if not start_s.isdigit() or not end_s.isdigit():
                raise ValueError(f"Invalid range format: '{part}'")
            start, end = int(start_s), int(end_s)
            if end < start:
                raise ValueError(f"Invalid range '{start}-{end}': end must be >= start")
        else:
            if not part.isdigit():
                raise ValueError(f"Invalid range format: '{part}'")
            start = end = int(part)
        if start < 1 or end > total_pages:
            bad = start if start < 1 else end
            raise ValueError(f"Page {bad} is out of range (PDF has {total_pages} pages)")
        segments.append((start, end))
    return segments
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd tools
python -m pytest tests/test_splitter.py -v -k "parse_ranges"
```

Expected: all `parse_ranges` tests PASS.

- [ ] **Step 5: Commit**

```bash
git add tools/pdf_splitter/splitter.py tools/tests/test_splitter.py
git commit -m "feat: implement parse_ranges in pdf_splitter"
```

---

## Task 3: Implement `split_by_pages`

Splits every page of a PDF into its own file. Output filenames are zero-padded to match the width of the total page count.

**Files:**
- Modify: `tools/pdf_splitter/splitter.py`
- Modify: `tools/tests/test_splitter.py`

- [ ] **Step 1: Add the failing tests** (append to `tools/tests/test_splitter.py`)

```python
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
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd tools
python -m pytest tests/test_splitter.py -v -k "split_by_pages"
```

Expected: `ImportError` — `split_by_pages` not yet defined.

- [ ] **Step 3: Add `split_by_pages` to `splitter.py`** (append after `parse_ranges`)

```python
def _page_filename(page_num: int, pad: int) -> str:
    return f"page_{page_num:0{pad}d}.pdf"


def _range_filename(start: int, end: int, pad: int) -> str:
    if start == end:
        return f"pages_{start:0{pad}d}.pdf"
    return f"pages_{start:0{pad}d}-{end:0{pad}d}.pdf"


def split_by_pages(reader: PdfReader, output_dir: Path, total_pages: int) -> int:
    pad = len(str(total_pages))
    for i in range(total_pages):
        writer = PdfWriter()
        writer.add_page(reader.pages[i])
        out_path = output_dir / _page_filename(i + 1, pad)
        with open(out_path, "wb") as f:
            writer.write(f)
    return total_pages
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd tools
python -m pytest tests/test_splitter.py -v -k "split_by_pages"
```

Expected: all `split_by_pages` tests PASS.

- [ ] **Step 5: Commit**

```bash
git add tools/pdf_splitter/splitter.py tools/tests/test_splitter.py
git commit -m "feat: implement split_by_pages in pdf_splitter"
```

---

## Task 4: Implement `split_by_ranges`

Splits a PDF by explicit page segments. Only pages in the specified ranges appear in output; all others are skipped.

**Files:**
- Modify: `tools/pdf_splitter/splitter.py`
- Modify: `tools/tests/test_splitter.py`

- [ ] **Step 1: Add the failing tests** (append to `tools/tests/test_splitter.py`)

```python
# --- split_by_ranges ---

def test_split_by_ranges_single_range_one_file(tmp_path):
    reader = make_reader(100)
    count = split_by_ranges(reader, tmp_path, [(10, 20)], 100)
    assert count == 1
    files = list(tmp_path.glob("*.pdf"))
    assert len(files) == 1
    assert files[0].name == "pages_010-020.pdf"


def test_split_by_ranges_single_page_segment(tmp_path):
    reader = make_reader(10)
    split_by_ranges(reader, tmp_path, [(5, 5)], 10)
    files = list(tmp_path.glob("*.pdf"))
    assert len(files) == 1
    assert files[0].name == "pages_05.pdf"  # pad=2 because total_pages=10


def test_split_by_ranges_multiple_segments(tmp_path):
    reader = make_reader(10)
    count = split_by_ranges(reader, tmp_path, [(1, 3), (5, 5), (7, 10)], 10)
    assert count == 3
    names = sorted(p.name for p in tmp_path.glob("*.pdf"))
    assert names == ["pages_01-03.pdf", "pages_05.pdf", "pages_07-10.pdf"]


def test_split_by_ranges_correct_page_count_in_output(tmp_path):
    reader = make_reader(10)
    split_by_ranges(reader, tmp_path, [(2, 4)], 10)
    out_file = next(tmp_path.glob("*.pdf"))
    r = PdfReader(str(out_file))
    assert len(r.pages) == 3
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd tools
python -m pytest tests/test_splitter.py -v -k "split_by_ranges"
```

Expected: `ImportError` — `split_by_ranges` not yet defined.

- [ ] **Step 3: Add `split_by_ranges` to `splitter.py`** (append after `split_by_pages`)

```python
def split_by_ranges(
    reader: PdfReader,
    output_dir: Path,
    segments: list[tuple[int, int]],
    total_pages: int,
) -> int:
    pad = len(str(total_pages))
    for start, end in segments:
        writer = PdfWriter()
        for page_num in range(start, end + 1):
            writer.add_page(reader.pages[page_num - 1])
        out_path = output_dir / _range_filename(start, end, pad)
        with open(out_path, "wb") as f:
            writer.write(f)
    return len(segments)
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd tools
python -m pytest tests/test_splitter.py -v -k "split_by_ranges"
```

Expected: all `split_by_ranges` tests PASS.

- [ ] **Step 5: Run the full splitter test suite**

```bash
cd tools
python -m pytest tests/test_splitter.py -v
```

Expected: all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add tools/pdf_splitter/splitter.py tools/tests/test_splitter.py
git commit -m "feat: implement split_by_ranges in pdf_splitter"
```

---

## Task 5: Implement CLI `__main__.py`

Wires up argparse with `splitter.py` and handles all validation errors.

**Files:**
- Create: `tools/pdf_splitter/__main__.py`
- Create: `tools/tests/test_pdf_splitter_main.py`

- [ ] **Step 1: Write the failing tests**

`tools/tests/test_pdf_splitter_main.py`:
```python
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


def run_main(args: list[str]) -> tuple[int, str, str]:
    """Run main() and return (exit_code, stdout, stderr)."""
    from io import StringIO
    import tools.pdf_splitter.__main__ as m

    stdout_cap, stderr_cap = StringIO(), StringIO()
    exit_code = 0
    old_argv = sys.argv
    try:
        sys.argv = ["pdf_splitter"] + args
        try:
            m.main()
        except SystemExit as e:
            exit_code = e.code if isinstance(e.code, int) else 1
    finally:
        sys.argv = old_argv
    return exit_code, stdout_cap.getvalue(), stderr_cap.getvalue()


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
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd tools
python -m pytest tests/test_pdf_splitter_main.py -v
```

Expected: `ModuleNotFoundError` — `__main__.py` not yet created.

- [ ] **Step 3: Create `tools/pdf_splitter/__main__.py`**

```python
import argparse
import sys
from pathlib import Path

from pypdf import PdfReader

from tools.pdf_splitter.splitter import parse_ranges, split_by_pages, split_by_ranges


def main() -> None:
    parser = argparse.ArgumentParser(description="Split a PDF file by pages or page ranges.")
    parser.add_argument("--input", required=True, help="Path to source PDF file")
    parser.add_argument("--output", required=True, help="Path to output folder (created if missing)")
    parser.add_argument("--ranges", default=None, help='Page ranges, e.g. "1-3,5,7-10". Omit for single-page mode.')
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        print(f"Error: File not found: {input_path}", file=sys.stderr)
        sys.exit(1)
    if input_path.suffix.lower() != ".pdf":
        print("Error: Input must be a .pdf file", file=sys.stderr)
        sys.exit(1)

    output_dir = Path(args.output)
    try:
        output_dir.mkdir(parents=True, exist_ok=True)
    except OSError as e:
        print(f"Error: Cannot create output folder: {output_dir} ({e})", file=sys.stderr)
        sys.exit(1)

    try:
        reader = PdfReader(str(input_path))
    except Exception as e:
        print(f"Error: Cannot read PDF: {e}", file=sys.stderr)
        sys.exit(1)

    total_pages = len(reader.pages)

    if args.ranges:
        try:
            segments = parse_ranges(args.ranges, total_pages)
        except ValueError as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        count = split_by_ranges(reader, output_dir, segments, total_pages)
    else:
        count = split_by_pages(reader, output_dir, total_pages)

    print(f"{count} file(s) written to {output_dir}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd tools
python -m pytest tests/test_pdf_splitter_main.py -v
```

Expected: all tests PASS.

- [ ] **Step 5: Run the complete test suite to check for regressions**

```bash
cd tools
python -m pytest tests/ -v
```

Expected: all tests PASS (including existing `test_csv_loader.py`, `test_main.py`, etc.).

- [ ] **Step 6: Smoke test the CLI manually**

```bash
cd tools
# create a 5-page test PDF
python -c "
from pypdf import PdfWriter
w = PdfWriter()
for _ in range(5): w.add_blank_page(72, 72)
with open('test_input.pdf', 'wb') as f: w.write(f)
"

python -m tools.pdf_splitter --input test_input.pdf --output test_out/
ls test_out/
# Expected: page_1.pdf  page_2.pdf  page_3.pdf  page_4.pdf  page_5.pdf

python -m tools.pdf_splitter --input test_input.pdf --output test_out_ranges/ --ranges "2-4"
ls test_out_ranges/
# Expected: pages_2-4.pdf

rm -rf test_input.pdf test_out/ test_out_ranges/
```

- [ ] **Step 7: Commit**

```bash
git add tools/pdf_splitter/__main__.py tools/tests/test_pdf_splitter_main.py
git commit -m "feat: add pdf_splitter CLI with single-page and range modes"
```
