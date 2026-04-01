import os
import pytest
import pandas as pd
from unittest.mock import patch, MagicMock
from tools.report.llm_executor import build_schema, _extract_code, _run_sandboxed, execute


# --- Schema building ---

def test_build_schema_includes_df_name():
    df = pd.DataFrame({"Symbol": ["BNS"], "Gross": [9.0]})
    schema = build_schema({"df_test": df})
    assert "df_test" in schema


def test_build_schema_includes_column_names():
    df = pd.DataFrame({"Symbol": ["BNS"], "Gross": [9.0]})
    schema = build_schema({"df_test": df})
    assert "Symbol" in schema
    assert "Gross" in schema


def test_build_schema_multiple_dfs():
    df1 = pd.DataFrame({"A": [1]})
    df2 = pd.DataFrame({"B": [2]})
    schema = build_schema({"df_one": df1, "df_two": df2})
    assert "df_one" in schema
    assert "df_two" in schema


# --- Code extraction ---

def test_extract_code_from_python_fence():
    response = '```python\nresult = df_test[["Symbol"]]\n```'
    assert _extract_code(response) == 'result = df_test[["Symbol"]]'


def test_extract_code_fallback_no_fence():
    response = 'result = df_test[["Symbol"]]'
    assert _extract_code(response) == 'result = df_test[["Symbol"]]'


def test_extract_code_strips_whitespace():
    response = '```python\n  result = df_test  \n```'
    assert _extract_code(response) == "result = df_test"


# --- Sandboxed execution ---

def test_run_sandboxed_returns_dataframe():
    df = pd.DataFrame({"Symbol": ["BNS", "AAPL"], "Gross": [9.0, 12.5]})
    code = 'result = df_test.groupby("Symbol")["Gross"].sum().reset_index()'
    result = _run_sandboxed(code, {"df_test": df})
    assert isinstance(result, pd.DataFrame)
    assert set(result["Symbol"]) == {"BNS", "AAPL"}


def test_run_sandboxed_blocks_open():
    code = 'result = open("/etc/passwd")'
    with pytest.raises(RuntimeError, match="Code execution failed"):
        _run_sandboxed(code, {})


def test_run_sandboxed_blocks_import():
    code = 'import os; result = os.listdir(".")'
    with pytest.raises(RuntimeError, match="Code execution failed"):
        _run_sandboxed(code, {})


def test_run_sandboxed_missing_result_raises():
    code = "x = 1"
    with pytest.raises(RuntimeError, match="'result' must be a DataFrame"):
        _run_sandboxed(code, {})


def test_run_sandboxed_wrong_result_type_raises():
    code = "result = 42"
    with pytest.raises(RuntimeError, match="'result' must be a DataFrame"):
        _run_sandboxed(code, {})


# --- Claude API integration (mocked) ---

def test_execute_calls_claude_returns_dataframe():
    df = pd.DataFrame({"Symbol": ["BNS", "AAPL"], "Gross": [9.0, 12.5]})
    dataframes = {"df_divs": df}

    mock_text = '```python\nresult = df_divs.groupby("Symbol")["Gross"].sum().reset_index()\n```'
    mock_response = MagicMock()
    mock_response.content = [MagicMock(text=mock_text)]

    with patch("tools.report.llm_executor.anthropic.Anthropic") as mock_cls:
        mock_client = MagicMock()
        mock_cls.return_value = mock_client
        mock_client.messages.create.return_value = mock_response
        with patch.dict(os.environ, {"ANTHROPIC_API_KEY": "test-key"}):
            result = execute("Show dividends by ticker", dataframes)

    assert isinstance(result, pd.DataFrame)
    assert "Symbol" in result.columns
    assert len(result) == 2
