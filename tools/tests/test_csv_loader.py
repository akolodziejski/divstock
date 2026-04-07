import pandas as pd
from pathlib import Path
from tools.report.csv_loader import load_csvs, _stem_to_varname, _find_header_row, _detect_separator


def test_stem_to_varname_dots_replaced():
    assert _stem_to_varname("U15541068.2025.dividends") == "df_U15541068_2025_dividends"


def test_stem_to_varname_simple():
    assert _stem_to_varname("Account") == "df_Account"


def test_detect_separator_comma(tmp_path):
    f = tmp_path / "test.csv"
    f.write_text("a,b,c,d,e\n1,2,3,4,5\n", encoding="ISO-8859-1")
    assert _detect_separator(f) == ","


def test_detect_separator_semicolon(tmp_path):
    f = tmp_path / "test.csv"
    f.write_text("a;b;c;d;e\n1;2;3;4;5\n", encoding="ISO-8859-1")
    assert _detect_separator(f) == ";"


def test_find_header_row_first_row(tmp_path):
    f = tmp_path / "degiro.csv"
    f.write_text(
        "Symbol,Currency,Gross,Withhold,ReportDate\n"
        "BNS,CAD,9.0,-1.35,20210428\n",
        encoding="ISO-8859-1",
    )
    assert _find_header_row(f, ",") == 0


def test_find_header_row_skips_metadata(tmp_path):
    f = tmp_path / "ib.csv"
    f.write_text(
        "Statement,Data,BrokerName\n"                                                         # row 0: 3 cols, skip
        "Account,Data,U15541068\n"                                                            # row 1: 3 cols, skip
        "DividendDetail,Header,DataDiscriminator,Currency,Symbol,Conid,Country,ReportDate\n"  # row 2: real IB header
        "DividendDetail,Data,Summary,CAD,BNS,4457153,CA,20250130,\n",                        # row 3: data with trailing comma
        encoding="ISO-8859-1",
    )
    assert _find_header_row(f, ",") == 2


def test_find_header_row_beyond_30_rows(tmp_path):
    """Header at row 50 (beyond old 30-row limit) should be found."""
    f = tmp_path / "ib_large.csv"
    # Write 48 metadata rows (3 cols each), then the real header at row 48
    metadata = "Statement,Data,BrokerName\n" * 48
    header = "DividendDetail,DataDiscriminator,Currency,Symbol,ReportDate,Gross,Withhold\n"
    data = "DividendDetail,Data,Summary,CAD,BNS,20210428,9.0\n"
    f.write_text(metadata + header + data, encoding="ISO-8859-1")
    assert _find_header_row(f, ",") == 48


def test_load_csvs_degiro_format(tmp_path):
    csv = tmp_path / "Account.csv"
    csv.write_text(
        "Data,Czas,Produkt,ISIN,Opis,Saldo,Waluta\n"
        "26-12-2023,08:01,BLACKROCK INC.,US09247X1019,Dywidenda,5.00,USD\n",
        encoding="ISO-8859-1",
    )
    dfs = load_csvs(tmp_path)
    assert "df_Account" in dfs
    assert list(dfs["df_Account"].columns) == ["Data", "Czas", "Produkt", "ISIN", "Opis", "Saldo", "Waluta"]
    assert len(dfs["df_Account"]) == 1


def test_load_csvs_ib_format(tmp_path):
    csv = tmp_path / "U15541068.2025.dividends.csv"
    csv.write_text(
        "Statement,Data,BrokerName\n"
        "Account,Data,U15541068\n"
        "DividendDetail,Header,DataDiscriminator,Currency,Symbol,Conid,Country,ReportDate,Gross,Withhold\n"
        "DividendDetail,Data,Summary,CAD,BNS,4457153,CA,20250130,9.0,-1.35,\n"
        "DividendDetail,Data,Summary,USD,AAPL,265598,US,20250215,12.5,-1.875,\n",
        encoding="ISO-8859-1",
    )
    dfs = load_csvs(tmp_path)
    assert "df_U15541068_2025_dividends" in dfs
    df = dfs["df_U15541068_2025_dividends"]
    assert "Symbol" in df.columns
    assert len(df) == 2
    assert len(df.columns) >= 10  # header row has 10 named cols + 1 unnamed trailing
    # Verify Symbol column contains tickers, not Conid numbers
    assert set(df["Symbol"]) == {"BNS", "AAPL"}


def test_load_csvs_empty_folder(tmp_path):
    assert load_csvs(tmp_path) == {}
