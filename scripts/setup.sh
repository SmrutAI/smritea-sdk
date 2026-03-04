#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
FAILURES=()

# ── Helpers ───────────────────────────────────────────────────────────
_info()  { printf "\033[36m→ %s\033[0m\n" "$1"; }
_ok()    { printf "\033[32m✓ %s\033[0m\n" "$1"; }
_warn()  { printf "\033[33m⚠ %s\033[0m\n" "$1"; }
_err()   { printf "\033[31m✗ %s\033[0m\n" "$1"; }

_track_failure() { FAILURES+=("$1"); }

_report_failures() {
    echo ""
    if [ ${#FAILURES[@]} -eq 0 ]; then
        _ok "All setup steps completed successfully!"
    else
        _err "The following setup steps failed:"
        for f in "${FAILURES[@]}"; do
            echo "  - $f"
        done
        exit 1
    fi
}

_check_cmd() {
    if command -v "$1" &>/dev/null; then
        _ok "$1 found: $(command -v "$1")"
        return 0
    else
        _warn "$1 not found"
        return 1
    fi
}

# ── Per-language setup ────────────────────────────────────────────────
setup_python() {
    _info "Setting up Python SDK tools..."
    if ! _check_cmd uv; then
        _info "Installing uv..."
        if command -v brew &>/dev/null; then
            brew install uv
        else
            curl -LsSf https://astral.sh/uv/install.sh | sh
        fi
    fi
    _info "Installing Python SDK dependencies..."
    cd "$SDK_ROOT/python" && uv sync
    _ok "Python SDK setup complete"
}

setup_typescript() {
    _info "Setting up TypeScript SDK tools..."
    if ! _check_cmd node; then
        _err "Node.js not found. Install Node 18+ via: brew install node"
        _track_failure "TypeScript: node not found"
        return
    fi
    _info "Installing TypeScript SDK dependencies..."
    cd "$SDK_ROOT/typescript" && npm install
    _ok "TypeScript SDK setup complete"
}

setup_go() {
    _info "Setting up Go SDK tools..."
    if ! _check_cmd go; then
        _err "Go not found. Install Go 1.22+ via: brew install go"
        _track_failure "Go: go not found"
        return
    fi
    if ! _check_cmd golangci-lint; then
        _info "Installing golangci-lint..."
        brew install golangci-lint 2>/dev/null || go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest
    fi
    if ! _check_cmd goimports; then
        _info "Installing goimports..."
        go install golang.org/x/tools/cmd/goimports@latest
    fi
    if ! _check_cmd gci; then
        _info "Installing gci..."
        go install github.com/daixiang0/gci@latest
    fi
    _ok "Go SDK setup complete"
}

setup_java() {
    _info "Setting up Java SDK tools..."
    if ! _check_cmd java; then
        _err "JDK not found. Install JDK 11+ via: brew install openjdk"
        _track_failure "Java: java not found"
        return
    fi
    if ! _check_cmd mvn; then
        _err "Maven not found. Install Maven 3+ via: brew install maven"
        _track_failure "Java: mvn not found"
        return
    fi
    _info "Downloading Java SDK dependencies..."
    cd "$SDK_ROOT/java" && mvn dependency:resolve -q
    _ok "Java SDK setup complete"
}

setup_csharp() {
    _info "Setting up C# SDK tools..."
    if ! _check_cmd dotnet; then
        _err ".NET SDK not found. Install .NET 8+ via: brew install --cask dotnet-sdk"
        _track_failure "C#: dotnet not found"
        return
    fi
    _info "Restoring C# SDK dependencies..."
    cd "$SDK_ROOT/csharp" && dotnet restore
    _ok "C# SDK setup complete"
}

setup_hooks() {
    _info "Setting up pre-commit hooks..."
    if ! _check_cmd pre-commit; then
        _info "Installing pre-commit..."
        pip3 install pre-commit 2>/dev/null || brew install pre-commit
    fi
    cd "$SDK_ROOT" && pre-commit install
    _ok "Pre-commit hooks installed"
}

setup_all() {
    setup_python  || _track_failure "Python setup"
    setup_typescript || _track_failure "TypeScript setup"
    setup_go      || _track_failure "Go setup"
    setup_java    || _track_failure "Java setup"
    setup_csharp  || _track_failure "C# setup"
    setup_hooks   || _track_failure "Hooks setup"
}

# ── Main ──────────────────────────────────────────────────────────────
if [ $# -gt 0 ]; then
    case "$1" in
        python)     setup_python ;;
        typescript) setup_typescript ;;
        go)         setup_go ;;
        java)       setup_java ;;
        csharp)     setup_csharp ;;
        hooks)      setup_hooks ;;
        all)        setup_all ;;
        *)
            echo "Usage: $0 [python|typescript|go|java|csharp|hooks|all]"
            exit 1
            ;;
    esac
else
    echo ""
    echo "smritea SDK — Development Setup"
    echo "================================"
    echo ""
    echo "  1) Python SDK tools"
    echo "  2) TypeScript SDK tools"
    echo "  3) Go SDK tools"
    echo "  4) Java SDK tools"
    echo "  5) C# SDK tools"
    echo "  6) Pre-commit hooks"
    echo "  7) All of the above"
    echo ""
    read -rp "Choose [1-7]: " choice
    case "$choice" in
        1) setup_python ;;
        2) setup_typescript ;;
        3) setup_go ;;
        4) setup_java ;;
        5) setup_csharp ;;
        6) setup_hooks ;;
        7) setup_all ;;
        *) echo "Invalid choice"; exit 1 ;;
    esac
fi

_report_failures
