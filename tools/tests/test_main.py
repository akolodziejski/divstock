import os
import sys
import pytest
import pandas as pd
from pathlib import Path
from unittest.mock import patch, MagicMock


def _run_main(argv: list[str], env: dict | None = None):
    """Helper: run main() with patched sys.argv and additional environment vars."""
    env = env or {}
    with patch("sys.argv", argv):
        with patch.dict(os.environ, env):
            from tools.report.__main__ import main
            main()


def test_main_missing_folder_exits_1(tmp_path, capsys):
    with pytest.raises(SystemExit) as exc:
        _run_main(
            ["report", "--description", "test", "--folder", str(tmp_path / "nonexistent")],
            {"ANTHROPIC_API_KEY": "key"},
        )
    assert exc.value.code == 1
    assert "Folder not found" in capsys.readouterr().err


def test_main_no_csv_files_exits_1(tmp_path, capsys):
    with pytest.raises(SystemExit) as exc:
        _run_main(
            ["report", "--description", "test", "--folder", str(tmp_path)],
            {"ANTHROPIC_API_KEY": "key"},
        )
    assert exc.value.code == 1
    assert "No .csv files found" in capsys.readouterr().err


def test_main_missing_api_key_exits_1(tmp_path, capsys):
    (tmp_path / "test.csv").write_text("a,b,c,d,e\n1,2,3,4,5\n")
    env_without_key = {k: v for k, v in os.environ.items() if k != "ANTHROPIC_API_KEY"}
    with patch("sys.argv", ["report", "--description", "test", "--folder", str(tmp_path)]):
        with patch.dict(os.environ, env_without_key, clear=True):
            with pytest.raises(SystemExit) as exc:
                from tools.report.__main__ import main
                main()
    assert exc.value.code == 1
    assert "ANTHROPIC_API_KEY" in capsys.readouterr().err


def test_main_writes_output_file(tmp_path, capsys):
    (tmp_path / "divs.csv").write_text(
        "Symbol,Gross\nBNS,9.0\nAAPL,12.5\n", encoding="ISO-8859-1"
    )
    output = tmp_path / "out.md"
    mock_result = pd.DataFrame({"Symbol": ["BNS", "AAPL"], "Total": [9.0, 12.5]})

    with patch("tools.report.__main__.execute", return_value=mock_result):
        with patch("sys.argv", [
            "report",
            "--description", "Test report",
            "--folder", str(tmp_path),
            "--output", str(output),
        ]):
            with patch.dict(os.environ, {"ANTHROPIC_API_KEY": "key"}):
                from tools.report.__main__ import main
                main()

    assert output.exists()
    content = output.read_text(encoding="utf-8")
    assert "# Test report" in content
    assert "BNS" in content
