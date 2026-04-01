from datetime import datetime
from pathlib import Path
from typing import Union

import pandas as pd


def write_report(
    result: pd.DataFrame,
    description: str,
    folder: Union[str, Path],
    output_path: Path,
) -> None:
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    table = result.to_markdown(index=False)
    if table is None:
        raise RuntimeError(
            "tabulate is required for markdown output; run: pip install tabulate"
        )
    content = (
        f"# {description}\n\n"
        f"{table}\n\n"
        f"_Source: {str(folder)} | Generated: {timestamp}_\n"
    )
    output_path.write_text(content, encoding="utf-8")
