.PHONY: generate test-python test-typescript test build-python build-typescript clean help

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' | sort

generate: ## Regenerate auto-gen SDK from smritea-cloud (run when API changes)
	cd .. && make generate-public-sdk

test-python: ## Run Python SDK tests
	cd python && uv run pytest tests/ -v

test-typescript: ## Run TypeScript SDK tests
	cd typescript && npm test

test: test-python test-typescript ## Run all SDK tests

build-python: ## Build Python SDK wheel
	cd python && uv build

build-typescript: ## Build TypeScript SDK
	cd typescript && npm run build

clean: ## Clean build artifacts
	rm -rf python/dist
	rm -rf typescript/dist
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
