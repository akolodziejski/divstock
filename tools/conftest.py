import sys
from pathlib import Path

# Add project root to sys.path so 'tools' package is importable from tests
sys.path.insert(0, str(Path(__file__).parent.parent))
