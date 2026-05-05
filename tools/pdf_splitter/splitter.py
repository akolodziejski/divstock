from pathlib import Path
from pypdf import PdfReader, PdfWriter


def split_by_pages(reader: PdfReader, output_dir: Path) -> list[Path]:
    raise NotImplementedError


def split_by_ranges(reader: PdfReader, ranges: list[tuple[int, int]], output_dir: Path) -> list[Path]:
    raise NotImplementedError


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
