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
