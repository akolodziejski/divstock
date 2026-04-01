from datetime import datetime
from pathlib import Path

import pandas as pd


def write_report(result: pd.DataFrame, description: str, folder: str, output_path: Path) -> None:
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    table = result.to_markdown(index=False)
    content = (
        f"# {description}\n\n"
        f"{table}\n\n"
        f"_Source: {folder} | Generated: {timestamp}_\n"
    )
    output_path.write_text(content, encoding="utf-8")
