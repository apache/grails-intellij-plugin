#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
"""Replace the JetBrains-attributed Apache 2.0 headers with the canonical ASF header.

The JetBrains software grant relocated the copyright notice to NOTICE, and the ASF source
header policy (https://www.apache.org/legal/src-headers.html#headers) requires the header
itself to carry no copyright notice.  This rewrites the header in place, in whichever
comment syntax the file already uses, and changes nothing else.

The header text is read from the HEADER file at the repository root, so that file stays the
single definition of what the header says.

    ./etc/bin/apply-asf-headers.py            rewrite
    ./etc/bin/apply-asf-headers.py --check    change nothing, and fail if anything would

--check is the CI guard.  RAT already proves every non-excluded file carries an Apache 2.0
header, but it cannot tell the two forms apart -- the JetBrains-attributed boilerplate and
the ASF header are both the ALv2 appendix to RAT, which is why it was green before this
sweep and stays green after.  This closes exactly that gap, and being anchored on the
copyright notice rather than on a file list, it needs no copy of RAT's exclusions.

Exits non-zero if any file carries a JetBrains-attributed header this script cannot
identify, so a file is never silently left behind.
"""
import argparse
import os
import re
import subprocess
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

COPYRIGHT = re.compile(r"Copyright\s+\d{4}(?:-\d{4})?\s+JetBrains\s+s\.r\.o\.")
LICENSE_END = re.compile(r"limitations under the License\.")
# What may legitimately sit above a file's licence header.
PRELUDE = re.compile(r"\s*$|\s*(#!|<\?xml|//|#|/\*|\*|<!--)")

# Files that mention JetBrains outside a license header: prose about where the codebase came
# from, and NOTICE, which is where the grant puts the attribution and so must keep it.
SKIP = {
    "NOTICE",
    "README.md",
    "AGENTS.md",
    "MIGRATION-PLAN.md",
    "IMPROVEMENT-PLAN.md",
    "etc/bin/apply-asf-headers.py",
}

# Comment syntax by extension: (opening line, line prefix, closing line). A prefix is applied
# to every line of the header text, which the HEADER file already indents by one space.
BLOCK = ("/*", " *", " */")
HASH = (None, "#", None)
XML = ("<!--", " ", "-->")

BY_EXTENSION = {
    ".java": BLOCK,
    ".kt": BLOCK,
    ".kts": BLOCK,
    ".gradle": BLOCK,
    ".groovy": BLOCK,
    ".gdsl": BLOCK,
    ".flex": BLOCK,
    ".properties": HASH,
    ".py": HASH,
    ".sh": HASH,
    ".yml": HASH,
    ".yaml": HASH,
    ".xml": XML,
    ".tld": XML,
    ".html": XML,
}

# Extensionless files whose comment syntax cannot be inferred from a suffix: the
# META-INF/services provider-configuration files, which take '#' comments.
SERVICES = re.compile(r"/META-INF/services/")


def header_lines():
    with open(os.path.join(REPO_ROOT, "HEADER"), encoding="utf-8") as handle:
        return handle.read().rstrip("\n").split("\n")


def render(style, lines):
    """Render the header text in one comment syntax."""
    opening, prefix, closing = style
    rendered = []
    if opening is not None:
        rendered.append(opening)
    for line in lines:
        # No trailing whitespace on the blank lines inside the header.
        rendered.append((prefix + line).rstrip() if line.strip() else prefix.rstrip())
    if closing is not None:
        rendered.append(closing)
    return rendered


def style_for(path):
    if SERVICES.search("/" + path):
        return HASH
    return BY_EXTENSION.get(os.path.splitext(path)[1])


def find_block(lines, style):
    """Locate the license comment block: (start, end) inclusive, or None.

    Anchored on the JetBrains copyright line and bounded by the comment syntax, so the shape
    of the boilerplate in between -- and there are several -- does not matter.
    """
    copyright_at = next((i for i, line in enumerate(lines) if COPYRIGHT.search(line)), None)
    if copyright_at is None:
        return None

    opening, _, closing = style
    if opening is not None:
        start = next((i for i in range(copyright_at, -1, -1) if lines[i].lstrip().startswith(opening)), None)
        end = next((i for i in range(copyright_at, len(lines)) if closing.strip() in lines[i]), None)
    else:
        # A '#' block has no delimiters: it starts at the copyright line and ends on the last
        # line of the licence text, so any comment that follows it is left alone.
        start = copyright_at
        end = next((i for i in range(copyright_at, len(lines)) if LICENSE_END.search(lines[i])), None)

    if start is None or end is None or end < start:
        return None
    # A header sits at the top of the file. Only a shebang, an XML declaration, blank lines or
    # other comments may precede it -- the generated JFlex lexers, for one, put two "// Generated
    # by" lines first. Anything else means this is a comment from the body of the file.
    if not all(PRELUDE.match(line) for line in lines[:start]):
        return None
    # Guard against matching a comment that merely mentions the copyright.
    if not any("Apache License" in line for line in lines[start:end + 1]):
        return None
    return start, end


def tracked_files():
    out = subprocess.run(["git", "-C", REPO_ROOT, "ls-files", "-z"],
                         capture_output=True, check=True).stdout
    return [entry for entry in out.decode("utf-8").split("\0") if entry]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true",
                        help="change nothing, and exit non-zero if anything would change")
    args = parser.parse_args()

    lines_of_header = header_lines()
    rewritten, skipped, unhandled = [], [], []

    for path in tracked_files():
        if path in SKIP:
            continue
        absolute = os.path.join(REPO_ROOT, path)
        try:
            with open(absolute, encoding="utf-8") as handle:
                text = handle.read()
        except (UnicodeDecodeError, IsADirectoryError, FileNotFoundError):
            continue
        if not COPYRIGHT.search(text):
            continue

        style = style_for(path)
        if style is None:
            unhandled.append((path, "no comment syntax known for this file type"))
            continue

        newline = "\r\n" if "\r\n" in text else "\n"
        lines = text.split(newline)
        found = find_block(lines, style)
        if found is None:
            unhandled.append((path, "no license header found around the copyright notice"))
            continue

        start, end = found
        replacement = lines[:start] + render(style, lines_of_header) + lines[end + 1:]
        updated = newline.join(replacement)
        if updated == text:
            skipped.append(path)
            continue
        if not args.check:
            with open(absolute, "w", encoding="utf-8", newline="") as handle:
                handle.write(updated)
        rewritten.append(path)

    for path, reason in unhandled:
        print(f"UNHANDLED {path}: {reason}", file=sys.stderr)

    if args.check:
        for path in rewritten:
            print(f"JetBrains-attributed header: {path}", file=sys.stderr)
        if rewritten or unhandled:
            print(f"\n{len(rewritten) + len(unhandled)} file(s) do not carry the canonical ASF "
                  f"header from HEADER.\nRun ./etc/bin/apply-asf-headers.py to fix.", file=sys.stderr)
            return 1
        print("all tracked files carry the canonical ASF header")
        return 0

    print(f"rewrote {len(rewritten)} files")
    if unhandled:
        print(f"\n{len(unhandled)} file(s) left untouched -- see above", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
