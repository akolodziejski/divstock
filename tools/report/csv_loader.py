import re
from pathlib import Path

import pandas as pd


def _detect_separator(filepath: Path) -> str:
    with open(filepath, encoding="ISO-8859-1") as f:
        first_line = f.readline().lstrip("\ufeff")
    return ";" if first_line.count(";") > first_line.count(",") else ","


def _find_header_row(filepath: Path, sep: str) -> int:
    """Return 0-based row index with the most identifier-like cell values (the header row).

    Scans up to 500 rows. Rows with fewer than 5 columns are skipped (IB metadata
    key-value rows). The row whose cells are most densely populated with single-word
    alphabetic strings (column name identifiers) is returned as the header row.
    """
    with open(filepath, encoding="ISO-8859-1") as f:
        raw_lines = [f.readline() for _ in range(500)]
    # Strip BOM from first line only
    lines = (
        [raw_lines[0].strip().lstrip("\ufeff")] + [l.strip() for l in raw_lines[1:]]
        if raw_lines
        else []
    )

    def _is_identifier(val: str) -> bool:
        val = val.strip()
        # Single-word alphabetic strings only (column names don't have spaces)
        return bool(val) and val.isalpha() and len(val) > 1

    best_row, best_score = 0, 0
    for i, line in enumerate(lines):
        if not line:
            continue
        parts = line.split(sep)
        # Fewer than 5 columns = IB metadata key-value row, skip
        if len(parts) < 5:
            continue
        score = sum(1 for p in parts if _is_identifier(p))
        if score > best_score:
            best_score = score
            best_row = i
    return best_row


def _stem_to_varname(stem: str) -> str:
    return "df_" + re.sub(r"[^a-zA-Z0-9]", "_", stem)


def load_csvs(folder: Path) -> dict[str, pd.DataFrame]:
    folder = Path(folder)
    result = {}
    for csv_file in sorted(folder.glob("*.csv")):
        sep = _detect_separator(csv_file)
        header_row = _find_header_row(csv_file, sep)
        df = pd.read_csv(
            csv_file,
            header=header_row,
            sep=sep,
            encoding="ISO-8859-1",
            on_bad_lines="skip",
        )
        varname = _stem_to_varname(csv_file.stem)
        result[varname] = df
    return result
