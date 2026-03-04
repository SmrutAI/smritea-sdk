.PHONY: test-python test-typescript test-go test build-python build-typescript build-go \
        publish-python publish-typescript publish clean help \
        format-python format-typescript format-go format \
        lint-python lint-typescript lint-go lint \
        setup-go

# Publishing tokens — sourced from environment variables:
#   PYPI_TOKEN   : PyPI API token (starts with pypi-)
#   NPM_TOKEN    : npm access token

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' | sort

test-python: ## Run Python SDK tests
	cd python && uv run pytest tests/ -v

test-typescript: ## Run TypeScript SDK tests
	cd typescript && npm test

test: test-python test-typescript test-go ## Run all SDK tests

build-python: ## Build Python SDK wheel
	rm -rf python/dist
	cd python && uv build

build-typescript: ## Build TypeScript SDK
	rm -rf typescript/dist
	cd typescript && npm run build

publish-python: build-python ## Build and publish Python SDK to PyPI (requires PYPI_TOKEN env var)
	@if [ -z "$$PYPI_TOKEN" ]; then \
		echo "ERROR: PYPI_TOKEN environment variable is not set"; exit 1; \
	fi
	cd python && UV_PUBLISH_TOKEN="$$PYPI_TOKEN" uv publish

publish-typescript: build-typescript ## Build and publish TypeScript SDK to npm (requires NPM_TOKEN env var)
	@if [ -z "$$NPM_TOKEN" ]; then \
		echo "ERROR: NPM_TOKEN environment variable is not set"; exit 1; \
	fi
	cd typescript && npm set "//registry.npmjs.org/:_authToken=$$NPM_TOKEN" && npm publish --access public

publish: publish-python publish-typescript ## Build and publish both SDKs

format-python: ## Auto-format Python SDK with ruff (format + fix auto-fixable lint issues)
	cd python && uvx ruff format src/smritea
	cd python && uvx ruff check --fix src/smritea

lint-python: ## Lint Python SDK with ruff
	cd python && uvx ruff check src/smritea
	cd python && uvx ruff format --check src/smritea

format-typescript: ## Auto-fix TypeScript SDK with ESLint (--fix)
	cd typescript && npm install --silent
	cd typescript && npm run lint:fix

lint-typescript: ## Lint TypeScript SDK
	cd typescript && npm install --silent
	cd typescript && npm run typecheck
	cd typescript && npm run lint

build-go: ## Build Go SDK (verify compilation — no binary output needed for a library)
	cd go && go build ./...

setup-go: ## Install Go SDK tooling (golangci-lint, goimports, gci)
	@command -v golangci-lint > /dev/null 2>&1 || (echo "Installing golangci-lint..." && brew install golangci-lint)
	@command -v goimports > /dev/null 2>&1 || go install golang.org/x/tools/cmd/goimports@latest
	@command -v gci > /dev/null 2>&1 || go install github.com/daixiang0/gci@latest

format-go: ## Auto-format Go SDK (golangci-lint --fix + goimports + gci)
	cd go && golangci-lint run --fix ./... 2>/dev/null || true
	find go -name "*.go" -not -path "*/internal/autogen/*" -exec goimports -w {} +
	find go -name "*.go" -not -path "*/internal/autogen/*" -exec gci write --skip-generated -s standard -s default -s "prefix(github.com/SmrutAI)" {} +

lint-go: ## Lint Go SDK with golangci-lint (check-only)
	cd go && golangci-lint run ./...

test-go: ## Run Go SDK tests
	cd go && go test ./... -v

format: format-python format-typescript format-go ## Auto-format all SDKs

lint: lint-python lint-typescript lint-go ## Lint all SDKs

clean: ## Clean build artifacts
	rm -rf python/dist
	rm -rf typescript/dist
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
