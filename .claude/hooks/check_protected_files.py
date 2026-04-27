import sys
import json
import fnmatch
import os

data = json.load(sys.stdin)
file_path = data.get('tool_input', {}).get('file_path', '')

if not file_path:
    sys.exit(0)

protected_file = 'config/protectedFiles'
if not os.path.exists(protected_file):
    sys.exit(0)

with open(protected_file) as f:
    patterns = [line.strip() for line in f if line.strip() and not line.startswith('#')]

file_name = os.path.basename(file_path)

for pattern in patterns:
    if fnmatch.fnmatch(file_name, pattern) or fnmatch.fnmatch(file_path.replace('\\', '/'), pattern):
        result = {
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": f"File matches protected pattern '{pattern}' in config/protectedFiles"
            }
        }
        print(json.dumps(result))
        sys.exit(0)
