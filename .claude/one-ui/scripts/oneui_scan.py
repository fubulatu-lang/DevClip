#!/usr/bin/env python3
"""
oneui_scan.py — deterministic One UI conformance checks.

Finds the mechanically checkable violations: hardcoded colours, uppercase
transforms, non-One-UI easing, fixed font sizes, tight margins, unlabelled
controls, blocking overlays, fixed heights on text containers.

This is a starting point for an audit, not the audit. It cannot judge structure,
motion meaning, icon metaphors, or copy quality. Read the code for those.

Usage:
    python3 oneui_scan.py <path> [--json] [--max-findings N]

Exit codes: 0 clean, 1 findings present, 2 bad usage.
"""

import argparse
import json
import os
import re
import sys
from collections import defaultdict

ONEUI_COLORS = {"#0381fe", "#0072de", "#3e91ff", "#fafafa", "#080808", "#000000"}

SKIP_DIRS = {
    ".git", "node_modules", "build", "dist", ".next", "out", "vendor",
    "Pods", ".gradle", "DerivedData", "__pycache__", ".venv", "venv",
    "coverage", ".idea", "target", "Carthage", ".dart_tool",
}

WEB_EXT = {".css", ".scss", ".sass", ".less", ".js", ".jsx", ".ts", ".tsx", ".vue", ".svelte", ".html"}
ANDROID_EXT = {".xml", ".kt", ".java"}
IOS_EXT = {".swift", ".m", ".mm"}
ALL_EXT = WEB_EXT | ANDROID_EXT | IOS_EXT

# (rule_id, severity, title, regex, platforms, hint)
CHECKS = [
    ("WRT-01", "Blocker", "Uppercase text transform",
     re.compile(r"text-transform\s*:\s*uppercase", re.I), WEB_EXT,
     "One UI never uses ALL CAPS. Remove the transform."),

    ("WRT-01", "Blocker", "Uppercase text transform",
     re.compile(r'textAllCaps\s*=\s*"true"'), ANDROID_EXT,
     "Set textAllCaps=false, or use a One UI button style."),

    ("WRT-01", "Blocker", "Uppercase text transform",
     re.compile(r"\.uppercased\(\)|\.textCase\(\.uppercase\)"), IOS_EXT,
     "Remove the uppercasing; One UI capitalises normally."),

    ("MOT-01", "Major", "Non-One-UI easing curve",
     re.compile(r"(?:transition|animation)[^;{}]*?\b(?:ease-in-out|ease-in|ease-out|linear)\b", re.I),
     WEB_EXT, "Use cubic-bezier(0.22, 0.25, 0, 1)."),

    ("MOT-01", "Major", "Material easing curve",
     re.compile(r"cubic-bezier\(\s*0?\.4\s*,\s*0\s*,\s*0?\.2\s*,\s*1\s*\)"), WEB_EXT,
     "That is Material's standard curve. One UI uses cubic-bezier(0.22, 0.25, 0, 1)."),

    ("MOT-06", "Blocker", "No reduced-motion guard in this file",
     None, None, None),  # handled specially

    ("A11Y-02", "Blocker", "Fixed pixel font size",
     re.compile(r"font-size\s*:\s*\d+(?:\.\d+)?px"), WEB_EXT,
     "Use rem so text scales to 200%."),

    ("A11Y-02", "Blocker", "Fixed font size ignores Dynamic Type",
     re.compile(r"\.font\(\s*\.system\(size:"), IOS_EXT,
     "Use Dynamic Type (.font(.body)) so text scales."),

    ("A11Y-02", "Major", "Text sized in dp instead of sp",
     re.compile(r'android:textSize\s*=\s*"\d+(?:\.\d+)?dp"'), ANDROID_EXT,
     "Use sp for text so it honours the user's font size setting."),

    ("A11Y-04", "Blocker", "Image without alt text",
     re.compile(r"<img(?![^>]*\balt\s*=)[^>]*>", re.I), WEB_EXT,
     'Add alt="", or alt="" plus aria-hidden for decorative images.'),

    ("A11Y-06", "Blocker", "Focus outline removed",
     re.compile(r"outline\s*:\s*(?:none|0)\b", re.I), WEB_EXT,
     "Replace with a visible focus style rather than removing it."),

    ("A11Y-07", "Major", "Touch target under 48dp",
     re.compile(r'android:(?:layout_)?(?:min)?(?:Width|Height|width|height)\s*=\s*"([0-9]|[1-3][0-9]|4[0-7])dp"'),
     ANDROID_EXT, "Interactive targets should be at least 48dp."),

    ("LAY-07", "Blocker", "Fixed height on a text container",
     re.compile(r"(?<!min-)(?<!max-)\bheight\s*:\s*\d+px\s*;[^}]*?(?:font|line-height|text)", re.I | re.S),
     WEB_EXT, "Use min-height so text can grow to 200%."),

    ("CMP-10", "Major", "Possible full-screen blocking overlay",
     re.compile(r"position\s*:\s*fixed[^}]*?(?:inset\s*:\s*0|top\s*:\s*0[^}]*?left\s*:\s*0)[^}]*?z-index", re.I | re.S),
     WEB_EXT, "One UI shows progress inline, not as a blocking overlay. Verify manually."),

    ("LAY-02", "Major", "vh unit instead of dvh",
     re.compile(r":\s*\d+(?:\.\d+)?vh\b"), WEB_EXT,
     "Use dvh so mobile browser chrome does not crop the interaction area."),
]

HEX_RE = re.compile(r"#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\b")
MARGIN_RE = re.compile(
    r"(?:padding-inline|padding-left|padding-right|margin-left|margin-right)\s*:\s*(\d+)px"
)
TOKEN_FILE_HINT = re.compile(
    r"(token|theme|palette|colou?rs?|variables|design-system|_vars)", re.I
)
REDUCED_MOTION = re.compile(r"prefers-reduced-motion|accessibilityReduceMotion|ANIMATOR_DURATION_SCALE")
ANIMATION_HINT = re.compile(r"\b(?:transition|animation|@keyframes|withAnimation|animateTo)\b", re.I)


def detect_platform(root):
    found = set()
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in filenames:
            if fn in ("AndroidManifest.xml",) or fn.startswith("build.gradle"):
                found.add("android")
            elif fn == "Package.swift" or fn.endswith(".xcodeproj") or fn.endswith(".xcworkspace"):
                found.add("ios")
            elif fn == "package.json":
                found.add("web")
            elif fn == "pubspec.yaml":
                found.add("flutter")
        for d in list(dirnames):
            if d.endswith(".xcodeproj") or d.endswith(".xcworkspace"):
                found.add("ios")
    return sorted(found) or ["unknown"]


def iter_files(root):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and not d.startswith(".")]
        for fn in filenames:
            ext = os.path.splitext(fn)[1]
            if ext in ALL_EXT:
                yield os.path.join(dirpath, fn), ext


def scan(root):
    findings = []
    hex_by_file = defaultdict(set)
    files_seen = 0

    for path, ext in iter_files(root):
        try:
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                text = f.read()
        except OSError:
            continue
        files_seen += 1
        rel = os.path.relpath(path, root)
        lines = text.splitlines()

        for rule, sev, title, rx, exts, hint in CHECKS:
            if rx is None or exts is None or ext not in exts:
                continue
            for m in rx.finditer(text):
                line_no = text.count("\n", 0, m.start()) + 1
                findings.append({
                    "rule": rule, "severity": sev, "title": title,
                    "file": rel, "line": line_no,
                    "snippet": lines[line_no - 1].strip()[:110] if line_no <= len(lines) else "",
                    "hint": hint,
                })

        # hardcoded colours outside the token layer
        if not TOKEN_FILE_HINT.search(rel):
            for m in HEX_RE.finditer(text):
                val = m.group(0).lower()
                if len(val) == 4:  # expand #abc
                    val = "#" + "".join(c * 2 for c in val[1:])
                if val[:7] not in ONEUI_COLORS:
                    hex_by_file[rel].add(val[:7])

        # side margins under 24
        for m in MARGIN_RE.finditer(text):
            if int(m.group(1)) < 24:
                line_no = text.count("\n", 0, m.start()) + 1
                findings.append({
                    "rule": "LAY-01", "severity": "Major",
                    "title": f"Side padding {m.group(1)}px is under the 24 minimum",
                    "file": rel, "line": line_no,
                    "snippet": lines[line_no - 1].strip()[:110] if line_no <= len(lines) else "",
                    "hint": "One UI requires at least 24 on both sides for curved screen edges.",
                })

        # animation without a reduced-motion guard
        if ANIMATION_HINT.search(text) and not REDUCED_MOTION.search(text):
            findings.append({
                "rule": "MOT-06", "severity": "Blocker",
                "title": "Animation with no reduced-motion guard in this file",
                "file": rel, "line": 1, "snippet": "",
                "hint": "Guard motion behind prefers-reduced-motion / accessibilityReduceMotion. "
                        "Check whether a global guard covers this before reporting.",
            })

    for rel, vals in sorted(hex_by_file.items()):
        if len(vals) >= 3:
            findings.append({
                "rule": "OUI-COL-02", "severity": "Major",
                "title": f"{len(vals)} hardcoded colours outside the token layer",
                "file": rel, "line": 1,
                "snippet": ", ".join(sorted(vals)[:8]),
                "hint": "Move colours into named tokens so light and dark stay in sync.",
            })

    return findings, files_seen


def main():
    ap = argparse.ArgumentParser(description="Static One UI conformance checks.")
    ap.add_argument("path")
    ap.add_argument("--json", action="store_true", help="emit JSON")
    ap.add_argument("--max-findings", type=int, default=200)
    args = ap.parse_args()

    if not os.path.isdir(args.path):
        print(f"not a directory: {args.path}", file=sys.stderr)
        return 2

    platforms = detect_platform(args.path)
    findings, files_seen = scan(args.path)

    order = {"Blocker": 0, "Major": 1, "Minor": 2}
    findings.sort(key=lambda f: (order.get(f["severity"], 9), f["rule"], f["file"], f["line"]))
    shown = findings[: args.max_findings]

    if args.json:
        print(json.dumps({
            "platforms": platforms,
            "files_scanned": files_seen,
            "total": len(findings),
            "findings": shown,
        }, indent=2))
        return 1 if findings else 0

    counts = defaultdict(int)
    for f in findings:
        counts[f["severity"]] += 1

    print(f"Platform(s): {', '.join(platforms)}")
    print(f"Files scanned: {files_seen}")
    print(f"Findings: {len(findings)}  "
          f"(Blocker {counts['Blocker']}, Major {counts['Major']}, Minor {counts['Minor']})")

    if not findings:
        print("\nNo mechanically detectable issues. Structure, motion meaning,")
        print("copy and icon quality still need a manual read.")
        return 0

    by_rule = defaultdict(list)
    for f in shown:
        by_rule[(f["rule"], f["severity"], f["title"])].append(f)

    for (rule, sev, title), group in sorted(
        by_rule.items(), key=lambda kv: (order.get(kv[0][1], 9), kv[0][0])
    ):
        print(f"\n{'─' * 72}\n{rule} · {sev} · {title}  ({len(group)})")
        if group[0].get("hint"):
            print(f"  → {group[0]['hint']}")
        for f in group[:8]:
            loc = f"{f['file']}:{f['line']}"
            print(f"    {loc}" + (f"   {f['snippet']}" if f["snippet"] else ""))
        if len(group) > 8:
            print(f"    … and {len(group) - 8} more")

    if len(findings) > len(shown):
        print(f"\n{len(findings) - len(shown)} further findings suppressed "
              f"(raise --max-findings).")

    print("\nThis covers only what is mechanically checkable. Structure, layout")
    print("intent, component limits, motion meaning, icon metaphors and copy")
    print("quality need the pillar skills and a manual read.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
