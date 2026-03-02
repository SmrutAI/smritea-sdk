.PHONY: test-python test-typescript test build-python build-typescript \
        publish-python publish-typescript publish clean help \
        lint-python lint-typescript lint

# Publishing tokens — sourced from environment variables:
#   PYPI_TOKEN   : PyPI API token (starts with pypi-)
#   NPM_TOKEN    : npm access token

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' | sort

test-python: ## Run Python SDK tests
	cd python && uv run pytest tests/ -v

test-typescript: ## Run TypeScript SDK tests
	cd typescript && npm test

test: test-python test-typescript ## Run all SDK tests

build-python: ## Build Python SDK wheel
	cd python && uv build

build-typescript: ## Build TypeScript SDK
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

lint-python: ## Lint Python SDK with ruff (excludes _internal/autogen)
	cd python && uvx ruff check src/smritea
	cd python && uvx ruff format --check src/smritea

lint-typescript: ## Type-check and lint TypeScript SDK (excludes _internal/autogen)
	cd typescript && npm install --silent
	cd typescript && npm run typecheck
	cd typescript && npm run lint

lint: lint-python lint-typescript ## Lint all SDKs

clean: ## Clean build artifacts
	rm -rf python/dist
	rm -rf typescript/dist
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
