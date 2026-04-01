import os
import re

import anthropic
import pandas as pd


def build_schema(dataframes: dict[str, pd.DataFrame]) -> str:
    parts = []
    for name, df in dataframes.items():
        cols = ", ".join(df.columns.tolist())
        sample = df.head(3).to_string(index=False)
        parts.append(f"{name}: columns=[{cols}]\nsample:\n{sample}")
    return "\n\n".join(parts)


def _extract_code(response_text: str) -> str:
    match = re.search(r"```python\s*(.*?)```", response_text, re.DOTALL)
    if match:
        return match.group(1).strip()
    return response_text.strip()


def _run_sandboxed(code: str, dataframes: dict[str, pd.DataFrame]) -> pd.DataFrame:
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
    )
    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=1024,
        messages=[{"role": "user", "content": prompt}],
    )
    response_text = message.content[0].text
    code = _extract_code(response_text)
    return _run_sandboxed(code, dataframes)
