import argparse
import os
import sys
from datetime import datetime
from pathlib import Path

from tools.report.csv_loader import load_csvs
from tools.report.llm_executor import execute
from tools.report.markdown_writer import write_report


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate a markdown stock report from CSV files using AI."
    )
    parser.add_argument("--description", required=True, help="Natural language description of the report")
    parser.add_argument("--folder", required=True, help="Path to folder containing CSV files")
    parser.add_argument("--output", default=None, help="Output markdown file path")
    args = parser.parse_args()

    folder = Path(args.folder)
    if not folder.exists():
        print(f"Error: Folder not found: {folder}", file=sys.stderr)
        sys.exit(1)

    csv_files = list(folder.glob("*.csv"))
    if not csv_files:
        print(f"Error: No .csv files found in {folder}", file=sys.stderr)
        sys.exit(1)

    if "ANTHROPIC_API_KEY" not in os.environ:
        print("Error: Set ANTHROPIC_API_KEY environment variable", file=sys.stderr)
        sys.exit(1)

    output_path = (
        Path(args.output)
        if args.output
        else Path(f"report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
    )

    dataframes = load_csvs(folder)
    result = execute(args.description, dataframes)
    write_report(result, args.description, args.folder, output_path)
    print(f"Report written to {output_path}")


if __name__ == "__main__":
    main()
