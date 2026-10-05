#!/usr/bin/env bash
# Strips multi-line comments from client/ (Angular) and server/ (Spring Boot), then
# hands Claude a simplification review prompt (adapted from Anthropic's /simplify skill).
#
# Usage:
#   cleanup-codebase.sh [--all] [--dry-run] [--keep-docs] [--no-prompt]
#   cleanup-codebase.sh --hook     # Stop-hook mode: reads hook JSON on stdin, blocks once
#
# Removed:   whole-line comment blocks spanning 2+ lines (/* */, /** */, runs of //, <!-- -->)
# Preserved: single-line comments, trailing comments, license headers and tool directives
#            (eslint-*, @ts-*, prettier-ignore, NOSONAR, @SuppressWarnings, #region, ...)
# Scope:     files changed in git (modified, staged, untracked); --all = every file in src/
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

ALL=0 DRY=0 KEEP_DOCS=0 PROMPT=1 HOOK=0
for arg in "$@"; do
  case "$arg" in
    --all) ALL=1 ;;
    --dry-run) DRY=1 ;;
    --keep-docs) KEEP_DOCS=1 ;;
    --no-prompt) PROMPT=0 ;;
    --hook) HOOK=1 ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) echo "unknown option: $arg" >&2; exit 64 ;;
  esac
done

EXCLUDE='(^|/)(node_modules|dist|build|target|out|\.angular|\.gradle|generated|coverage)/'
INCLUDE='^(client|server)/.*\.(ts|java|html)$'

list_files() {
  if [[ $ALL -eq 1 ]]; then
    git ls-files -co --exclude-standard client server
  else
    { git ls-files -m -o --exclude-standard; git diff --cached --name-only --diff-filter=ACMR; }
  fi | sort -u | grep -E "$INCLUDE" | grep -Ev "$EXCLUDE" || true
}

simplify_prompt() {
  cat <<'PROMPT'
Review the files listed above for reuse, quality, efficiency and altitude, then fix what you find.
This is a quality pass only: do not hunt for bugs and do not change behavior. Run the tests afterwards.

1. REUSE
   - Search the codebase for an existing utility, facade, service, mapper or Spring bean before keeping new code.
   - Replace hand-rolled logic that the framework already provides (Angular signals/computed/async pipe,
     RxJS operators, Spring's Optional/Streams/Validation/ResponseStatusException, Java records, Collections).
   - Flag copy-pasted blocks across features; extract only when 3+ sites share the same reason to change.

2. QUALITY
   - Redundant state: state that can be derived (computed() / a method) instead of stored and synced.
   - Parameter sprawl: 4+ arguments, boolean flags, or repeated argument groups -> introduce a value object/options record.
   - Leaky abstractions: third-party or transport types (HttpResponse, JPA entities, DTOs) crossing layer boundaries.
   - Stringly-typed code: string/int constants that should be a union type, enum or sealed type.
   - Dead code, unused imports/params/exports, commented-out code, speculative generality (unused hooks, flags, interfaces).
   - Naming that needs a comment to be understood: rename instead of commenting.
   - Deep nesting: invert conditions, use guard clauses and early returns; split functions doing more than one thing.
   - Error handling: no swallowed exceptions, no null returns where Optional/empty collections fit, no catch-as-if.

3. EFFICIENCY
   - Unnecessary work: repeated computation, redundant HTTP/DB calls, N+1 queries, fetching whole entities for one field.
   - Missed concurrency: independent awaits/requests done sequentially (forkJoin/combineLatest, CompletableFuture).
   - Hot paths: work inside templates or change-detection cycles (method calls in templates -> computed/pipe,
     missing trackBy/track, missing OnPush), per-request object churn on the server.
   - Leaks: un-unsubscribed streams (use takeUntilDestroyed/async pipe), unclosed resources (try-with-resources).
   - Over-fetching/over-broad queries: add projections, pagination, indexes where the query pattern demands them.

4. ALTITUDE
   - Is each change at the right layer? Business rules belong in domain/application code, not controllers or components.
   - Is the abstraction level consistent inside each function (no mixing orchestration with low-level detail)?
   - Remove a pattern that does not pay for itself (a Strategy with one implementation, a Factory that only calls new).

Apply fixes directly. Skip false positives silently. Finish with a one-line summary of what changed.
PROMPT
}

STRIPPER='
import bisect, re, sys

DIRECTIVE = re.compile(
    r"eslint|prettier-ignore|@ts-|istanbul|c8 ignore|noinspection|NOSONAR|CHECKSTYLE|checkstyle|"
    r"@formatter|spotless|SuppressWarnings|#region|#endregion|<editor-fold|sourceMappingURL|"
    r"/\s*<reference|@license|@preserve|[Cc]opyright|SPDX|\bLicen[sc]e\b"
)
REGEX_PREV = set("(,=:[!&|?{};")


def find_comments(src):
    spans, i, n, prev = [], 0, len(src), ""
    while i < n:
        c, two = src[i], src[i : i + 2]
        if two == "//":
            j = src.find("\n", i)
            j = n if j < 0 else j
            spans.append((i, j, False))
            i = j
            continue
        if two == "/*":
            j = src.find("*/", i + 2)
            if j < 0:
                return None
            spans.append((i, j + 2, True))
            i = j + 2
            continue
        if src.startswith(chr(34) * 3, i):
            j = src.find(chr(34) * 3, i + 3)
            while j > 0 and src[j - 1] == chr(92):
                j = src.find(chr(34) * 3, j + 1)
            if j < 0:
                return None
            i, prev = j + 3, chr(34)
            continue
        if c in "\"\x27`":
            i += 1
            while i < n and src[i] != c:
                if src[i] == chr(92):
                    i += 1
                elif src[i] == "\n" and c != "`":
                    return None
                i += 1
            if i >= n:
                return None
            i, prev = i + 1, c
            continue
        if c == "/" and (prev == "" or prev in REGEX_PREV):
            j, in_class = i + 1, False
            while j < n and src[j] != "\n":
                if src[j] == chr(92):
                    j += 1
                elif src[j] == "[":
                    in_class = True
                elif src[j] == "]":
                    in_class = False
                elif src[j] == "/" and not in_class:
                    break
                j += 1
            if j < n and src[j] == "/":
                i, prev = j + 1, "/"
                continue
        if not c.isspace():
            prev = c
        i += 1
    return spans


def strip_slash_comments(src, keep_docs):
    spans = find_comments(src)
    if spans is None:
        return None
    lines = src.split("\n")
    offsets = [0]
    for ln in lines[:-1]:
        offsets.append(offsets[-1] + len(ln) + 1)

    def line_of(off):
        return bisect.bisect_right(offsets, off) - 1

    doomed, line_comments, removed = set(), {}, 0
    for start, end, is_block in spans:
        first, last = line_of(start), line_of(max(end - 1, start))
        text = src[start:end]
        if src[offsets[first] : start].strip():
            continue
        if is_block:
            tail_end = offsets[last] + len(lines[last])
            if src[end:tail_end].strip() or last == first or DIRECTIVE.search(text):
                continue
            if keep_docs and text.startswith("/**"):
                continue
            doomed.update(range(first, last + 1))
            removed += 1
        else:
            line_comments[first] = text
    run = []
    for idx in sorted(line_comments) + [None]:
        if run and (idx is None or idx != run[-1] + 1):
            if len(run) >= 2 and not any(DIRECTIVE.search(line_comments[r]) for r in run):
                doomed.update(run)
                removed += 1
            run = []
        if idx is not None:
            run.append(idx)
    if not doomed:
        return src, 0
    out, just_deleted = [], False
    for idx, ln in enumerate(lines):
        if idx in doomed:
            just_deleted = True
            continue
        if just_deleted and not ln.strip() and out and not out[-1].strip():
            just_deleted = False
            continue
        just_deleted = False
        out.append(ln)
    return "\n".join(out), removed


def strip_html_comments(src):
    count = 0

    def repl(m):
        nonlocal count
        if "\n" in m.group(1) and not DIRECTIVE.search(m.group(1)):
            count += 1
            return ""
        return m.group(0)

    return re.sub(r"^[ \t]*<!--(.*?)-->[ \t]*\n", repl, src, flags=re.S | re.M), count


dry, keep_docs, files = sys.argv[1] == "1", sys.argv[2] == "1", sys.argv[3:]
total = 0
for path in files:
    with open(path, encoding="utf-8") as fh:
        src = fh.read()
    if path.endswith(".html"):
        result = strip_html_comments(src)
    else:
        result = strip_slash_comments(src, keep_docs)
    if result is None:
        print(f"skipped (could not parse safely): {path}", file=sys.stderr)
        continue
    new, removed = result
    if removed:
        total += removed
        print(f"{path}: {removed} comment block(s)")
        if not dry:
            with open(path, "w", encoding="utf-8") as fh:
                fh.write(new)
verb = "would be removed" if dry else "removed"
print(f"total: {total} comment block(s) {verb}")
'

if [[ $HOOK -eq 1 ]]; then
  PAYLOAD="$(cat || true)"
  ACTIVE="$(printf '%s' "$PAYLOAD" | python3 -c 'import sys,json
try: print(str(json.load(sys.stdin).get("stop_hook_active", False)).lower())
except Exception: print("false")')"
  [[ "$ACTIVE" == "true" ]] && exit 0
fi

mapfile -t FILES < <(list_files)
if [[ ${#FILES[@]} -eq 0 ]]; then
  [[ $HOOK -eq 1 ]] || echo "no client/ or server/ source files to clean" >&2
  exit 0
fi

REPORT="$(python3 -c "$STRIPPER" "$DRY" "$KEEP_DOCS" "${FILES[@]}" 2>&1 || true)"

if [[ $HOOK -eq 1 ]]; then
  [[ $PROMPT -eq 1 ]] || exit 0
  STATE_FILE="$(git rev-parse --git-dir)/cleanup-hook.state"
  FINGERPRINT="$(printf '%s\0' "${FILES[@]}" | xargs -0 sha1sum | sha1sum | cut -d' ' -f1)"
  [[ "$(cat "$STATE_FILE" 2>/dev/null || true)" == "$FINGERPRINT" ]] && exit 0
  printf '%s' "$FINGERPRINT" >"$STATE_FILE"
  python3 - "$REPORT" "$(printf '%s\n' "${FILES[@]}")" "$(simplify_prompt)" <<'PY'
import json, sys
report, files, prompt = sys.argv[1:4]
reason = f"Codebase cleanup ran.\n{report}\n\nFiles in scope:\n{files}\n\n{prompt}"
print(json.dumps({"decision": "block", "reason": reason}))
PY
  exit 0
fi

echo "$REPORT"
if [[ $PROMPT -eq 1 ]]; then
  echo
  echo "Files in scope:"
  printf '  %s\n' "${FILES[@]}"
  echo
  simplify_prompt
fi
