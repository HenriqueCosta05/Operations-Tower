#!/usr/bin/env bash
# Test gate for client/ (Angular + Vitest) and server/ (Spring Boot + JUnit 5).
# 1. Lints changed tests for slop (no assertions, existence-only, skipped/focused, untagged ITs).
# 2. Flags changed behavior-bearing source files that have no test.
# 3. Runs the requested test levels and fails on any red suite.
#
# Usage:
#   tests-check.sh [--scope client|server|all] [--level unit,integration,e2e] [--all] [--no-run] [--hook]
#   --hook   PreToolUse mode: only acts when the Bash command is `git commit`; exits 2 to block.
#
# Contracts this script relies on:
#   client/package.json scripts: test (single run, non-watch), test:integration, test:e2e
#   server tests carry JUnit tags: *Test = unit (untagged), *IT = @Tag("integration"), *E2EIT = @Tag("e2e")
#   Maven or Gradle wrapper; Gradle tasks: test, integrationTest, e2eTest
# Env: TESTS_CHECK_STRICT=0 downgrades "missing test" findings to warnings.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

SCOPE=all LEVELS="unit,integration,e2e" ALL=0 RUN=1 HOOK=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --scope) SCOPE="$2"; shift ;;
    --level) LEVELS="$2"; shift ;;
    --all) ALL=1 ;;
    --no-run) RUN=0 ;;
    --hook) HOOK=1 ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
  shift
done

if [[ $HOOK -eq 1 && ! -t 0 ]]; then
  COMMAND="$(python3 -c 'import sys,json
try: print(json.load(sys.stdin).get("tool_input", {}).get("command", ""))
except Exception: print("")')"
  [[ "$COMMAND" =~ git[[:space:]]+commit ]] || exit 0
fi

has_level() { [[ ",$LEVELS," == *",$1,"* ]]; }
in_scope() { [[ "$SCOPE" == all || "$SCOPE" == "$1" ]]; }

changed() {
  if [[ $ALL -eq 1 ]]; then
    git ls-files -co --exclude-standard client server
  else
    { git ls-files -m -o --exclude-standard; git diff --cached --name-only --diff-filter=ACMR; }
  fi | sort -u | grep -Ev '(^|/)(node_modules|dist|target|build|\.angular)/' || true
}

mapfile -t CHANGED < <(changed)
FAILURES=()

ANALYZER='
import glob, os, re, sys

mode, files = sys.argv[1], sys.argv[2:]

TS_TEST = re.compile(r"(?m)^[ \t]*(?:f|x)?(?:it|test)(?:\.\w+)*\(\s*([\"\x27`])(.*?)\1")
TS_ASSERT = re.compile(r"\bexpect\w*\(|\.verify\(|\bassert\w*\(|\.toThrow|\bfail\(")
TS_WEAK = re.compile(r"expect\([^)]*\)\.(?:toBeTruthy|toBeDefined|not\.toBeNull|not\.toBeUndefined)\(\)")
J_TEST = re.compile(r"(?m)^[ \t]*@(?:Test|ParameterizedTest|RepeatedTest|TestFactory)\b")
J_ASSERT = re.compile(r"\bassert\w*\(|\bverify\w*\(|\.andExpect\(|\.expectStatus|\bfail\(|StepVerifier")
J_WEAK = re.compile(r"assertNotNull\(|assertThat\([^;]*\)\.isNotNull\(\)|assertTrue\(true\)")


def line_of(text, pos):
    return text.count("\n", 0, pos) + 1


def blocks(text, pattern):
    marks = [m.start() for m in pattern.finditer(text)]
    for i, start in enumerate(marks):
        yield start, text[start : marks[i + 1] if i + 1 < len(marks) else len(text)]


def slop_ts(path, text):
    for m in re.finditer(r"\.only\(|\bf(?:it|describe)\(", text):
        yield line_of(text, m.start()), "focused test (.only/fit/fdescribe) hides the rest of the suite"
    for m in re.finditer(r"\bx(?:it|describe)\(|\.skip\(", text):
        yield line_of(text, m.start()), "skipped test: fix it or delete it"
    for m in re.finditer(r"expect\((?:true|false|1)\)\.", text):
        yield line_of(text, m.start()), "tautological assertion"
    for start, body in blocks(text, TS_TEST):
        asserts = len(TS_ASSERT.findall(body))
        if asserts == 0:
            yield line_of(text, start), "test has no assertion"
        elif len(TS_WEAK.findall(body)) >= len(re.findall(r"\bexpect\(", body)) > 0:
            yield line_of(text, start), "existence-only assertion (toBeTruthy/toBeDefined) proves nothing about behavior"


def slop_java(path, text):
    name = os.path.basename(path)
    for m in re.finditer(r"@Disabled(?!\s*\()|@Ignore\b", text):
        yield line_of(text, m.start()), "disabled test without a reason"
    for m in re.finditer(r"Thread\.sleep\(", text):
        yield line_of(text, m.start()), "Thread.sleep makes tests flaky: use Awaitility or a fake clock"
    for m in re.finditer(r"System\.out\.print", text):
        yield line_of(text, m.start()), "print statement in a test"
    if name.endswith("IT.java") and "@Tag(" not in text:
        yield 1, "integration/e2e test must carry @Tag(\"integration\") or @Tag(\"e2e\") or it is never run"
    if name.endswith("Test.java") and "@SpringBootTest" in text:
        yield line_of(text, text.index("@SpringBootTest")), "unit test boots a Spring context: use plain JUnit + fakes, or rename to *IT"
    for start, body in blocks(text, J_TEST):
        total = len(J_ASSERT.findall(body))
        if total == 0:
            yield line_of(text, start), "test has no assertion"
        elif len(J_WEAK.findall(body)) >= total:
            yield line_of(text, start), "existence-only assertion (assertNotNull/assertTrue(true)) proves nothing about behavior"


def missing_client(path):
    if not path.endswith(".ts") or path.endswith((".spec.ts", ".d.ts")):
        return
    if not re.search(r"\.(service|facade|store|strategy|pipe|guard|interceptor|resolver|validator|mapper|util|policy|factory|reducer)\.ts$", path):
        return
    text = open(path, encoding="utf-8").read()
    if not re.search(r"\bclass\s|export\s+(?:const|function)\s", text):
        return
    base = path[:-3]
    if not any(os.path.exists(base + s) for s in (".spec.ts", ".integration.spec.ts")):
        yield f"no {os.path.basename(base)}.spec.ts next to {path}"


def missing_server(path):
    name = os.path.basename(path)
    m = re.match(r"(\w+?(?:Service|UseCase|Handler|Strategy|Policy|Validator|Factory|Calculator|Mapper|Controller|Listener|Decorator|Specification|Resolver))\.java$", name)
    if not m or "/src/main/" not in path:
        return
    text = open(path, encoding="utf-8").read()
    if re.search(r"\binterface\s+" + m.group(1) + r"\b", text):
        return
    hits = [h for suffix in ("Test", "IT", "E2EIT") for h in glob.glob(f"server/src/test/**/{m.group(1)}{suffix}.java", recursive=True)]
    if not hits:
        yield f"no {m.group(1)}Test/IT found for {path}"


for path in files:
    if not os.path.isfile(path):
        continue
    text = open(path, encoding="utf-8").read()
    if mode == "slop":
        gen = slop_ts(path, text) if path.endswith(".spec.ts") else slop_java(path, text)
        for line, msg in gen:
            print(f"{path}:{line}: {msg}")
    else:
        gen = missing_client(path) if path.startswith("client/") else missing_server(path)
        for msg in gen:
            print(msg)
'

TEST_FILES=() SRC_FILES=()
for f in "${CHANGED[@]}"; do
  case "$f" in
    client/*.spec.ts|server/src/test/*.java) in_scope "${f%%/*}" && TEST_FILES+=("$f") ;;
    client/src/*.ts|server/src/main/*.java) in_scope "${f%%/*}" && SRC_FILES+=("$f") ;;
  esac
done

if [[ ${#TEST_FILES[@]} -gt 0 ]]; then
  SLOP="$(python3 -c "$ANALYZER" slop "${TEST_FILES[@]}")"
  [[ -z "$SLOP" ]] || FAILURES+=("TEST QUALITY — fix or delete these tests:"$'\n'"$SLOP")
fi

if [[ ${#SRC_FILES[@]} -gt 0 ]]; then
  MISSING="$(python3 -c "$ANALYZER" missing "${SRC_FILES[@]}")"
  if [[ -n "$MISSING" ]]; then
    if [[ "${TESTS_CHECK_STRICT:-1}" == "1" ]]; then
      FAILURES+=("MISSING TESTS — behavior-bearing code changed without a test:"$'\n'"$MISSING")
    else
      echo "warning: missing tests"$'\n'"$MISSING" >&2
    fi
  fi
fi

has_script() { grep -q "\"$1\"[[:space:]]*:" client/package.json; }

run_step() {
  local label="$1"; shift
  echo "==> $label" >&2
  local log; log="$(mktemp)"
  if ! "$@" >"$log" 2>&1; then
    FAILURES+=("FAILED: $label"$'\n'"$(tail -n 60 "$log")")
  fi
  rm -f "$log"
}

run_client() {
  [[ -f client/package.json ]] || { echo "client: no package.json yet, skipping" >&2; return; }
  local level script
  for level in unit integration e2e; do
    has_level "$level" || continue
    case "$level" in unit) script=test ;; integration) script=test:integration ;; e2e) script=test:e2e ;; esac
    if has_script "$script"; then
      CI=1 run_step "client $level" bash -c "cd client && npm run --silent $script"
    else
      echo "client: no '$script' script, skipping $level" >&2
    fi
  done
}

server_runner() {
  if [[ -x server/mvnw ]]; then echo "mvn:./mvnw"
  elif [[ -f server/pom.xml ]]; then echo "mvn:mvn"
  elif [[ -x server/gradlew ]]; then echo "gradle:./gradlew"
  else echo ""; fi
}

run_server() {
  local runner tool cmd level
  runner="$(server_runner)"
  [[ -n "$runner" ]] || { echo "server: no Maven/Gradle project yet, skipping" >&2; return; }
  tool="${runner%%:*}" cmd="${runner#*:}"
  for level in unit integration e2e; do
    has_level "$level" || continue
    if [[ "$tool" == mvn ]]; then
      case "$level" in
        unit) run_step "server unit" bash -c "cd server && $cmd -B -ntp -q test -Dgroups='!integration & !e2e' -Djacoco.skip=true" ;;
        *) run_step "server $level" bash -c "cd server && $cmd -B -ntp -q verify -Dgroups=$level -Djacoco.skip=true" ;;
      esac
    else
      local task=test; [[ "$level" == integration ]] && task=integrationTest; [[ "$level" == e2e ]] && task=e2eTest
      if (cd server && $cmd -q tasks --all 2>/dev/null | grep -q "^$task "); then
        run_step "server $level" bash -c "cd server && $cmd -q $task"
      else
        echo "server: no '$task' task, skipping $level" >&2
      fi
    fi
  done
}

if [[ $RUN -eq 1 ]]; then
  in_scope client && run_client
  in_scope server && run_server
fi

if [[ ${#FAILURES[@]} -eq 0 ]]; then
  echo "tests-check: OK" >&2
  exit 0
fi

{
  printf '%s\n\n' "${FAILURES[@]}"
  cat <<'GUIDE'
WHAT TO TEST (and what not to)
- Test behavior at the public seam: given a state/input, when an action happens, then an observable outcome.
  Name tests by scenario and outcome ("rejects a transfer above the daily limit"), never "should work".
- Unit: pure logic only — every branch, boundary value, error path and state transition of services, policies,
  strategies, mappers, pipes, guards. Use hand-written fakes for your own ports; mock only true I/O edges.
- Integration: real boundaries — HTTP adapters (HttpTestingController/MSW), Spring slices (@WebMvcTest, @DataJpaTest
  with Testcontainers): serialization, validation, query correctness, error-to-status mapping, security rules.
- E2E: a few critical user journeys end to end (happy path + the one failure that costs money or data). Never
  re-test what unit/integration already cover.
- Do NOT write: "should create" smoke tests, getter/setter tests, tests that re-assert mocks you just configured,
  full-DOM snapshots, DI wiring checks, tests of framework behavior, or one test per method regardless of risk.
- One reason to fail per test. No logic (if/for) inside tests. No sleeps. No shared mutable state between tests.
GUIDE
} >&2

[[ $HOOK -eq 1 ]] && exit 2
exit 1
