#!/usr/bin/env python3
"""Expand abbreviated JetBrains copyright lines to the full Apache 2.0 header,
keeping JetBrains attribution (no ASF language)."""
import re, subprocess, sys

LICENSE_BODY = [
    "",
    'Licensed under the Apache License, Version 2.0 (the "License");',
    "you may not use this file except in compliance with the License.",
    "You may obtain a copy of the License at",
    "",
    "    http://www.apache.org/licenses/LICENSE-2.0",
    "",
    "Unless required by applicable law or agreed to in writing, software",
    'distributed under the License is distributed on an "AS IS" BASIS,',
    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
    "See the License for the specific language governing permissions and",
    "limitations under the License.",
]

def block_header(years):
    lines = [f"Copyright {years} JetBrains s.r.o. and contributors."] + LICENSE_BODY
    out = ["/*"]
    for l in lines:
        out.append((" * " + l).rstrip())
    out.append(" */")
    return "\n".join(out)

def xml_header(years):
    lines = [f"Copyright {years} JetBrains s.r.o. and contributors."] + LICENSE_BODY
    out = ["<!--"]
    for l in lines:
        out.append(("  " + l).rstrip())
    out.append("-->")
    return "\n".join(out)

def hash_header(years):
    lines = [f"Copyright {years} JetBrains s.r.o. and contributors."] + LICENSE_BODY
    return "\n".join(("# " + l).rstrip() for l in lines)

ABBREV = r"Copyright (2000-\d{4}) JetBrains s\.r\.o\. and contributors\. Use of this source code is governed by the Apache 2\.0 license\."

RE_SLASH = re.compile(r"^// " + ABBREV + r"$", re.M)
RE_BLOCK = re.compile(r"^/\*\n \* " + ABBREV + r"\n \*/$", re.M)
RE_XML   = re.compile(r"^<!-- " + ABBREV + r" ?-->$", re.M)
RE_HASH  = re.compile(r"^# " + ABBREV + r"$", re.M)

files = subprocess.run(
    ["git", "grep", "-l", "JetBrains s.r.o. and contributors. Use of this source code"],
    capture_output=True, text=True, check=True,
).stdout.splitlines()

changed = 0
for path in files:
    with open(path, encoding="utf-8", newline="") as f:
        text = f.read()
    orig = text
    text = RE_BLOCK.sub(lambda m: block_header(m.group(1)), text)
    text = RE_SLASH.sub(lambda m: block_header(m.group(1)), text)
    text = RE_XML.sub(lambda m: xml_header(m.group(1)), text)
    text = RE_HASH.sub(lambda m: hash_header(m.group(1)), text)
    if text != orig:
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(text)
        changed += 1
    else:
        print(f"UNMATCHED: {path}", file=sys.stderr)

print(f"changed {changed} of {len(files)} files")
