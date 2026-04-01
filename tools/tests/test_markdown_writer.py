import pandas as pd
from pathlib import Path
from tools.report.markdown_writer import write_report


def test_write_report_creates_file(tmp_path):
    df = pd.DataFrame({"Symbol": ["BNS", "AAPL"], "Total": [9.0, 12.5]})
    output = tmp_path / "report.md"
    write_report(df, "Test report", "./statements", output)
    assert output.exists()


def test_write_report_title_is_description(tmp_path):
    df = pd.DataFrame({"Symbol": ["BNS"], "Total": [9.0]})
    output = tmp_path / "report.md"
    write_report(df, "Dividends by ticker", "./statements", output)
    content = output.read_text(encoding="utf-8")
    assert "# Dividends by ticker" in content


def test_write_report_contains_markdown_table(tmp_path):
    df = pd.DataFrame({"Symbol": ["BNS", "AAPL"], "Total": [9.0, 12.5]})
    output = tmp_path / "report.md"
    write_report(df, "Test", "./statements", output)
    content = output.read_text(encoding="utf-8")
    assert "BNS" in content
    assert "AAPL" in content
    assert "|" in content


def test_write_report_footer_contains_folder_and_timestamp(tmp_path):
    df = pd.DataFrame({"Symbol": ["BNS"], "Total": [9.0]})
    output = tmp_path / "report.md"
    write_report(df, "Test", "./statements/ib/div", output)
    content = output.read_text(encoding="utf-8")
    assert "./statements/ib/div" in content
    assert "Generated:" in content
