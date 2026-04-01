import os
import re

import anthropic
import pandas as pd


def build_schema(dataframes: dict[str, pd.DataFrame]) -> str:
    """Build a human-readable schema string for the LLM prompt.

    Includes column names with dtypes and up to 3 sample rows per DataFrame.
    """
    parts = []
    for name, df in dataframes.items():
        col_info = ", ".join(f"{col} ({dtype})" for col, dtype in zip(df.columns, df.dtypes))
        sample = df.head(3).to_string(index=False)
        parts.append(f"{name}: columns=[{col_info}]\nsample:\n{sample}")
    return "\n\n".join(parts)


def _extract_code(response_text: str) -> str:
    """Extract Python code from a response. Handles both ```python and plain ``` fences."""
    match = re.search(r"```(?:python)?\s*(.*?)```", response_text, re.DOTALL)
    if match:
        return match.group(1).strip()
    return response_text.strip()


def _run_sandboxed(code: str, dataframes: dict[str, pd.DataFrame]) -> pd.DataFrame:
    """Execute LLM-generated code in a restricted environment.

    The sandbox blocks builtins (no open(), no import) but NOTE: pd.read_* functions
    are blocked via pre-execution pattern scan rather than by restricting the pd module.
    """
    if re.search(r"\bpd\.read_", code):
        raise RuntimeError(
            "Code uses pd.read_* which is not permitted. Only use the provided df_* variables."
        )
    allowed_globals: dict = {"__builtins__": {}, "pd": pd}
    allowed_globals.update(dataframes)
    try:
        exec(code, allowed_globals)  # noqa: S102
    except Exception as e:
        raise RuntimeError(f"Code execution failed:\n{code}\n\nError: {e}") from e
    result = allowed_globals.get("result")
    if not isinstance(result, pd.DataFrame):
        raise RuntimeError(
            f"'result' must be a DataFrame, got {type(result).__name__}.\nCode:\n{code}"
        )
    return result


def execute(description: str, dataframes: dict[str, pd.DataFrame]) -> pd.DataFrame:
    schema = build_schema(dataframes)
    prompt = (
        "You are a data analyst. You have these pandas DataFrames available:\n\n"
        f"<schema>\n{schema}\n</schema>\n\n"
        f'User request: "{description}"\n\n'
        "Write Python/pandas code that produces a DataFrame called `result` answering the request.\n"
        "Rules:\n"
        "- Only use the df_* variables listed above and standard pandas operations\n"
        "- The final line must assign to `result`\n"
        "- Do not import anything\n"
        "- Do not read files or make network calls\n"
        "- Do not call pd.read_csv, pd.read_json, or any pd.read_* functions\n"
    )
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        raise ValueError("ANTHROPIC_API_KEY environment variable is not set")
    client = anthropic.Anthropic(api_key=api_key)
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=2048,
        messages=[{"role": "user", "content": prompt}],
    )
    if not message.content or not hasattr(message.content[0], "text"):
        raise RuntimeError(f"Unexpected API response format: {message.content}")
    response_text = message.content[0].text
    code = _extract_code(response_text)
    return _run_sandboxed(code, dataframes)
