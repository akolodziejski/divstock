# PDF Splitter — Design Spec

**Date:** 2026-05-05

## Overview

A Python CLI script that splits a PDF file into multiple output PDFs, either one per page or by explicit page ranges. Lives in `tools/pdf_splitter/` following the existing `tools/report/` module pattern.

## CLI Interface

```
python -m tools.pdf_splitter \
  --input path/to/file.pdf \
  --output path/to/output/folder \
  [--ranges "1-3,5,7-10"]
```

| Argument | Required | Description |
|----------|----------|-------------|
| `--input` | yes | Path to source PDF file |
| `--output` | yes | Path to output folder (created if missing) |
| `--ranges` | no | Comma-separated page ranges; omit for single-page mode |

## Splitting Modes

### Single-page mode (default — no `--ranges`)

Splits every page of the PDF into its own file.

- Output files: `page_001.pdf`, `page_002.pdf`, ...
- Zero-padding width matches total page count (e.g., 100 pages → 3-digit padding)

### Range mode (`--ranges` provided)

Splits only the specified segments. Pages outside specified ranges are ignored.

Each segment in the comma-separated string is either:
- A single page: `5` → `pages_005.pdf`
- A range: `1-3` → `pages_001-003.pdf`

Example: `--ranges "10-20"` on a 100-page PDF produces exactly one file: `pages_010-020.pdf`.

Page numbers are **1-based**.

## Module Structure

```
tools/pdf_splitter/
  __init__.py
  __main__.py      # CLI entry point: argparse, input validation, orchestration
  splitter.py      # Core logic: parse ranges, split pages, write output PDFs
  requirements.txt # pypdf>=4.0.0
```

## Data Flow

```
__main__.py
  → parse & validate CLI args
  → resolve input path, create output folder
  → call splitter.parse_ranges(ranges_str, total_pages)  [range mode only]
  → call splitter.split_by_pages(reader, output_dir, total_pages)
    OR splitter.split_by_ranges(reader, output_dir, segments)
  → print summary: N files written to <output>
```

## Error Handling

All errors print a descriptive message to stderr and exit with code 1:

| Condition | Message |
|-----------|---------|
| Input file not found | `Error: File not found: <path>` |
| Input is not a `.pdf` | `Error: Input must be a .pdf file` |
| Output folder cannot be created | `Error: Cannot create output folder: <path>` |
| `--ranges` string malformed | `Error: Invalid range format: '<segment>'` |
| Page number out of bounds | `Error: Page <n> is out of range (PDF has <total> pages)` |
| Range end < start | `Error: Invalid range '<start>-<end>': end must be >= start` |
| PDF is encrypted/unreadable | `Error: Cannot read PDF: <reason>` |

## Dependencies

- `pypdf>=4.0.0` — pure Python PDF reading/writing, no system dependencies

## Output Naming Examples

| Mode | Input pages | `--ranges` | Output files |
|------|-------------|------------|--------------|
| single-page | 5 pages | — | `page_1.pdf` … `page_5.pdf` (1-digit, matches total) |
| single-page | 100 pages | — | `page_001.pdf` … `page_100.pdf` |
| range | 100 pages | `10-20` | `pages_010-020.pdf` |
| range | 100 pages | `1-3,5,7-10` | `pages_001-003.pdf`, `pages_005.pdf`, `pages_007-010.pdf` |
