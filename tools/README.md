# Divstock Report Tools

Python CLI for generating ad-hoc markdown reports from broker CSV exports using AI.

## Setup

```bash
cd tools
python -m venv venv

# Windows:
venv\Scripts\activate
# Unix/macOS:
source venv/bin/activate

pip install -r report/requirements.txt
```

Set your API key (required at runtime):
```bash
# Windows:
set ANTHROPIC_API_KEY=sk-ant-...
# Unix/macOS:
export ANTHROPIC_API_KEY=sk-ant-...
```

## Usage

Run from the **project root** with the venv activated:

```bash
python -m tools.report \
  --description "Show dividends statistics with ticker and sum of dividends gathered" \
  --folder ./statements/ib/div \
  --output report.md
```

`--output` is optional; defaults to `report_YYYYMMDD_HHMMSS.md` in the current directory.

## Running Tests

```bash
cd tools
python -m pytest tests/ -v
```
