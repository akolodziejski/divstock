from pathlib import Path
from pypdf import PdfReader, PdfWriter


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
        with out_path.open("wb") as f:
            writer.write(f)
    return total_pages


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
        with out_path.open("wb") as f:
            writer.write(f)
    return len(segments)


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
            bad = start if start < 1 or start > total_pages else end
            raise ValueError(f"Page {bad} is out of range (PDF has {total_pages} pages)")
        segments.append((start, end))
    return segments
