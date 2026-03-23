.PHONY: test-python test-typescript test-go test-java test-csharp test build-python build-typescript build-go build-java build-csharp build \
        publish-python publish-typescript publish-java publish-csharp publish clean help \
        format-python format-typescript format-go format-java format-csharp format \
        lint-python lint-typescript lint-go lint-java lint-csharp lint \
        install-python install-typescript install-go install-java install-csharp install \
        sync-autogen sync-autogen-python sync-autogen-typescript sync-autogen-go sync-autogen-java sync-autogen-csharp \
        _require-swagger-json

# Publishing tokens — sourced from environment variables:
#   PYPI_TOKEN            : PyPI API token (starts with pypi-)
#   NPM_TOKEN             : npm access token
#   SONATYPE_USERNAME     : Sonatype user token username (from central.sonatype.com)
#   SONATYPE_PASSWORD     : Sonatype user token password (from central.sonatype.com)
#   MAVEN_GPG_PASSPHRASE  : GPG key passphrase (read by maven-gpg-plugin 3.2.0+)
#   NUGET_API_KEY         : NuGet.org API key

# Optional: set to a custom settings.xml path to use instead of ~/.m2/settings.xml
# Example: MAVEN_SETTINGS_FILE=/tmp/mvn-settings.xml make publish-java
MAVEN_SETTINGS_ARG := $(if $(MAVEN_SETTINGS_FILE),-s $(MAVEN_SETTINGS_FILE),)

# google-java-format standalone JAR — installed by scripts/setup.sh (same role as goimports for Go).
# Java 16+ requires --add-exports to access internal compiler APIs used by the formatter.
GJF_JAR := $(HOME)/.local/bin/google-java-format.jar
GJF_EXPORTS := --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
               --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
               --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
               --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
               --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
GJF := java $(GJF_EXPORTS) -jar $(GJF_JAR)

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' | sort

test-python: ## Run Python SDK tests
	cd python && uv run pytest tests/ -v

test-typescript: ## Run TypeScript SDK tests
	cd typescript && npm test

test: test-python test-typescript test-go test-java test-csharp ## Run all SDK tests

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

publish: publish-python publish-typescript publish-java publish-csharp ## Build and publish all SDKs

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

format-go: ## Auto-format Go SDK (golangci-lint --fix + goimports + gci)
	cd go && golangci-lint run --fix ./... 2>/dev/null || true
	find go -name "*.go" -not -path "*/internal/autogen/*" -exec goimports -w {} +
	find go -name "*.go" -not -path "*/internal/autogen/*" -exec gci write --skip-generated -s standard -s default -s "prefix(github.com/SmrutAI)" {} +

lint-go: ## Lint Go SDK with golangci-lint (check-only)
	cd go && golangci-lint run ./...

test-go: ## Run Go SDK tests
	cd go && go test ./... -v

install-python: ## Install Python SDK dependencies (uv sync)
	cd python && uv sync

install-typescript: ## Install TypeScript SDK dependencies (npm install)
	cd typescript && npm install --silent

install-go: ## Download Go SDK dependencies (go mod download)
	cd go && go mod download

install-java: ## Download Java SDK dependencies to local Maven cache (mvn dependency:resolve)
	cd java && mvn dependency:resolve -q

install-csharp: ## Restore C# SDK NuGet dependencies (dotnet restore)
	cd csharp && dotnet restore

install: install-python install-typescript install-go install-java install-csharp ## Install/sync all SDK dependencies

format-java: ## Auto-format Java SDK with google-java-format (excludes _internal/autogen/)
	find java/src -name "*.java" -not -path "*/_internal/autogen/*" | xargs $(GJF) --replace

lint-java: ## Lint Java SDK (checkstyle + verify format, excludes _internal/autogen/)
	cd java && mvn checkstyle:check
	find java/src -name "*.java" -not -path "*/_internal/autogen/*" | xargs $(GJF) --dry-run --set-exit-if-changed

test-java: ## Run Java SDK tests
	cd java && mvn test

build-java: ## Build Java SDK (compile + package)
	cd java && mvn package -DskipTests

publish-java: build-java ## Publish Java SDK to Maven Central
	@if [ -z "$$SONATYPE_USERNAME" ]; then echo "ERROR: SONATYPE_USERNAME not set"; exit 1; fi
	cd java && mvn deploy -P release $(MAVEN_SETTINGS_ARG)

format-csharp: ## Auto-format C# SDK with dotnet-format
	cd csharp && dotnet format Smritea.Sdk.sln

lint-csharp: ## Lint C# SDK (dotnet format check + build with analyzers)
	cd csharp && dotnet format Smritea.Sdk.sln --verify-no-changes
	cd csharp && dotnet build --no-restore -warnaserror

test-csharp: ## Run C# SDK tests
	cd csharp && dotnet test

build-csharp: ## Build C# SDK
	cd csharp && dotnet build --configuration Release

publish-csharp: build-csharp ## Publish C# SDK to NuGet
	@if [ -z "$$NUGET_API_KEY" ]; then echo "ERROR: NUGET_API_KEY not set"; exit 1; fi
	cd csharp && dotnet pack --configuration Release --output ./bin/Release
	cd csharp && dotnet nuget push "bin/Release/*.nupkg" --api-key "$$NUGET_API_KEY" --source https://api.nuget.org/v3/index.json --skip-duplicate

format: format-python format-typescript format-go format-java format-csharp ## Auto-format all SDKs

lint: lint-python lint-typescript lint-go lint-java lint-csharp ## Lint all SDKs

build: build-python build-typescript build-go build-java build-csharp ## Build all SDKs

# ---------------------------------------------------------------------------
# Autogen sync — regenerate _internal/autogen from an OpenAPI spec
# Usage: make sync-autogen SWAGGER_JSON=/path/to/swagger.json
# ---------------------------------------------------------------------------
SWAGGER_JSON ?=

_require-swagger-json:
	@test -n "$(SWAGGER_JSON)" || { echo "ERROR: SWAGGER_JSON is required. Usage: make sync-autogen SWAGGER_JSON=/path/to/spec.json"; exit 1; }

sync-autogen: sync-autogen-python sync-autogen-typescript sync-autogen-go sync-autogen-java sync-autogen-csharp ## Regenerate all autogen clients from OpenAPI spec

sync-autogen-python: _require-swagger-json ## Regenerate Python autogen from OpenAPI spec
	@echo "Generating Python SDK -> python/src/smritea/_internal/autogen ..."
	@rm -rf python/src/smritea/_internal/autogen
	@openapi-generator-cli generate \
		--config openapitools.json \
		-i $(SWAGGER_JSON) \
		-g python \
		-o /tmp/smritea-public-sdk-py \
		--additional-properties=packageName=smritea._internal.autogen.smritea_cloud_sdk
	@cp -r /tmp/smritea-public-sdk-py/smritea/_internal/autogen python/src/smritea/_internal/autogen
	@rm -rf /tmp/smritea-public-sdk-py
	@echo "  ✓ Python autogen synced"

sync-autogen-typescript: _require-swagger-json ## Regenerate TypeScript autogen from OpenAPI spec
	@echo "Generating TypeScript SDK -> typescript/src/_internal/autogen ..."
	@rm -rf typescript/src/_internal/autogen
	@mkdir -p typescript/src/_internal/autogen
	@openapi-generator-cli generate \
		--config openapitools.json \
		-i $(SWAGGER_JSON) \
		-g typescript-fetch \
		-o typescript/src/_internal/autogen
	@echo "  ✓ TypeScript autogen synced"

sync-autogen-go: _require-swagger-json ## Regenerate Go autogen from OpenAPI spec
	@echo "Generating Go SDK -> go/internal/autogen ..."
	@rm -rf go/internal/autogen
	@mkdir -p go/internal/autogen
	@openapi-generator-cli generate \
		--config openapitools.json \
		-i $(SWAGGER_JSON) \
		-g go \
		-o /tmp/smritea-public-sdk-go \
		--additional-properties=packageName=autogen,withGoMod=false
	@cp /tmp/smritea-public-sdk-go/*.go go/internal/autogen/
	@rm -rf /tmp/smritea-public-sdk-go
	@echo "  ✓ Go autogen synced"

sync-autogen-java: _require-swagger-json ## Regenerate Java autogen from OpenAPI spec
	@echo "Generating Java SDK -> java/src/autogen/java/ai/smritea/sdk/_internal/autogen ..."
	@rm -rf java/src/autogen/java/ai/smritea/sdk/_internal/autogen
	@mkdir -p java/src/autogen/java/ai/smritea/sdk/_internal/autogen
	@openapi-generator-cli generate \
		--config openapitools.json \
		-i $(SWAGGER_JSON) \
		-g java \
		-o /tmp/smritea-public-sdk-java \
		--additional-properties=library=native,hideGenerationTimestamp=true,invokerPackage=ai.smritea.sdk._internal.autogen,apiPackage=ai.smritea.sdk._internal.autogen.api,modelPackage=ai.smritea.sdk._internal.autogen.model
	@cp -r /tmp/smritea-public-sdk-java/src/main/java/ai/smritea/sdk/_internal/autogen/. java/src/autogen/java/ai/smritea/sdk/_internal/autogen/
	@rm -rf /tmp/smritea-public-sdk-java
	@echo "  ✓ Java autogen synced"

sync-autogen-csharp: _require-swagger-json ## Regenerate C# autogen from OpenAPI spec
	@echo "Generating C# SDK -> csharp/_internal/autogen ..."
	@rm -rf csharp/_internal/autogen
	@mkdir -p csharp/_internal/autogen
	@openapi-generator-cli generate \
		--config openapitools.json \
		-i $(SWAGGER_JSON) \
		-g csharp \
		-o /tmp/smritea-public-sdk-csharp \
		--additional-properties=packageName=Smritea.Internal.Autogen,optionalProjectFile=false,optionalAssemblyInfo=false,library=httpclient,nullableReferenceTypes=true
	@cp -r /tmp/smritea-public-sdk-csharp/src/Smritea.Internal.Autogen/. csharp/_internal/autogen/
	@rm -rf /tmp/smritea-public-sdk-csharp
	@echo "  ✓ C# autogen synced"

clean: ## Clean build artifacts
	rm -rf python/dist
	rm -rf typescript/dist
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
