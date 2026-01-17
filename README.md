# Cognotik

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Cognotik** is a powerful AI-powered code automation framework that enables intelligent code review, generation, and modification using large language models (LLMs). It provides a flexible harness for running AI agents that can analyze, fix, and implement code changes across your projects.

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
  - [Code Reviewer](#code-reviewer)
  - [Code Fixer](#code-fixer)
  - [Code Implementer](#code-implementer)
- [GitHub Actions Integration](#github-actions-integration)
  - [Auto-Fixing Build Validator](#auto-fixing-build-validator)
  - [Agentic Issue Handler](#agentic-issue-handler)
- [API Providers](#api-providers)
- [Architecture](#architecture)
- [Contributing](#contributing)
- [License](#license)

## Features

- 🤖 **Multi-Model Support**: Works with OpenAI, Google Gemini, Anthropic Claude, and Groq
- 📝 **Intelligent Code Review**: Automatically review code against best practices and standards
- 🔧 **Automated Bug Fixing**: Parse build logs and fix compilation/test errors
- 🏗️ **Code Generation**: Generate new code implementations from natural language descriptions
- 🔄 **GitHub Actions Integration**: Seamlessly integrate with CI/CD pipelines
- 📊 **Usage Tracking**: Built-in usage monitoring and cost tracking
- 🧩 **Extensible Task System**: Modular task types for different automation scenarios
- 🌊 **Waterfall Planning**: Sophisticated multi-step task orchestration

## Quick Start

```bash
# Clone the repository
git clone https://github.com/SimiaCryptus/Cognotik.git
cd Cognotik

# Set up your API keys
export GOOGLE_API_KEY="your-google-api-key"
# or
export OPENAI_API_KEY="your-openai-api-key"

# Run code review
./gradlew codeReview -PreviewPrompt="Review and improve code quality in file (%s)"
```

## Installation

### Gradle

Add the following to your `build.gradle`:

```groovy
dependencies {
  implementation 'com.cognotik:core:2.0.44'
  implementation 'com.cognotik:webui:2.0.44'
}
```

### Maven

```xml
<dependency>
    <groupId>com.cognotik</groupId>
    <artifactId>core</artifactId>
    <version>2.0.44</version>
</dependency>
<dependency>
    <groupId>com.cognotik</groupId>
    <artifactId>webui</artifactId>
    <version>2.0.44</version>
</dependency>
```

### Building from Source

```bash
git clone https://github.com/SimiaCryptus/Cognotik.git
cd Cognotik
./gradlew build
```

## Configuration

### Environment Variables

Cognotik supports multiple AI providers. Configure your preferred provider using environment variables:

| Variable            | Description              |
|---------------------|--------------------------|
| `GOOGLE_API_KEY`    | Google Gemini API key    |
| `OPENAI_API_KEY`    | OpenAI API key           |
| `ANTHROPIC_API_KEY` | Anthropic Claude API key |
| `GROQ_API_KEY`      | Groq API key             |

### User Settings

For persistent configuration, Cognotik uses a file-based settings manager. API credentials and preferences are stored in the application's data storage root directory.

## Usage

### Code Reviewer

The Code Reviewer analyzes your source files against standards documents and suggests improvements.

#### Command Line

```bash
./gradlew codeReview \
  -PreviewPrompt="Update implementation file (%s) according to the standards documents" \
  -PreviewSrc="src/main/java" \
  -PreviewDocs="docs/best_practices.md" \
  -PreviewThreads=4
```

#### Programmatic Usage

```java
import com.simiacryptus.CodeReviewer;

public class Example {
    public static void main(String[] args) {
        CodeReviewer.main(new String[]{
            ".",                          // Root directory
            "src/main/java",              // Source directory
            "Review code in file (%s)",   // Prompt template
            "docs/standards.md",          // Reference documents
            "4"                           // Thread count
        });
    }
}
```

#### Parameters

| Parameter | Description                       | Default                                                                |
|-----------|-----------------------------------|------------------------------------------------------------------------|
| `args[0]` | Root directory                    | `.`                                                                    |
| `args[1]` | Source directory to review        | `src/main/java`                                                        |
| `args[2]` | Prompt template (`%s` = filename) | `Update implementation file (%s) according to the standards documents` |
| `args[3]` | Comma-separated reference docs    | `docs/best_practices.md`                                               |
| `args[4]` | Number of parallel threads        | `4`                                                                    |

### Code Fixer

The Code Fixer analyzes build logs and automatically fixes compilation or test errors.

#### Programmatic Usage

```java
import com.simiacryptus.CodeFixer;

public class Example {
    public static void main(String[] args) {
        // Fix build errors from build.log
        CodeFixer.main(new String[]{
            "Fix the build errors reported in build.log",
            "build.log"
        });
    }
}
```

#### How It Works

1. Parses the specified build log file
2. Identifies error patterns and affected files
3. Uses AI to understand the root cause
4. Generates and applies fixes
5. Optionally re-runs the build to verify

### Code Implementer

The Code Implementer generates new code from natural language descriptions using a sophisticated planning system.

#### Programmatic Usage

```java
import com.simiacryptus.CodeImplementer;

public class Example {
    public static void main(String[] args) {
        // Generate a new application
        CodeImplementer.main(new String[]{});
    }
}
```

#### Features

- **Brainstorming**: Generates multiple approaches to solve the problem
- **Waterfall Planning**: Breaks down complex tasks into manageable steps
- **Sub-Plan Tasks**: Handles nested task hierarchies
- **Auto-Fix Integration**: Automatically fixes issues during implementation

## GitHub Actions Integration

Cognotik provides ready-to-use GitHub Actions workflows for automated code maintenance.

### Auto-Fixing Build Validator

Automatically fixes failing builds in pull requests.

#### Setup

1. Copy `_github/workflows/autofix_build.yml` to `.github/workflows/autofix_build.yml`
2. Add your API keys as repository secrets:
  - `OPENAI_API_KEY`
  - `GOOGLE_API_KEY`

#### Workflow

```yaml
name: Auto-Fixing Build Validator

on:
  pull_request:
    branches: [ main ]

permissions:
  contents: write

jobs:
  build-and-fix:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          ref: ${{ github.head_ref }}

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Tests
        id: run_tests
        run: set -o pipefail && ./gradlew test 2>&1 | tee build.log
        continue-on-error: true

      - name: Agentic Auto-Fix
        if: steps.run_tests.outcome == 'failure'
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          GOOGLE_API_KEY: ${{ secrets.GOOGLE_API_KEY }}
        run: |
          ./gradlew codeReview \
            -PreviewPrompt="The build is failing. Review and fix the implementation in file (%s)" \
            -PreviewSrc="src/main/java" \
            -PreviewDocs="build.log"

      - name: Commit Fixes
        if: steps.run_tests.outcome == 'failure'
        uses: stefanzweifel/git-auto-commit-action@v5
        with:
          commit_message: "fix: auto-correction by agentic build validator"
```

### Agentic Issue Handler

Automatically creates pull requests to resolve GitHub issues.

#### Setup

1. Copy `_github/workflows/issue_handler.yml` to `.github/workflows/issue_handler.yml`
2. Add your API keys as repository secrets

#### How It Works

1. Triggered when an issue is opened or labeled with `agent-help`
2. Analyzes the issue title and description
3. Reviews relevant source files
4. Creates a pull request with proposed fixes

#### Workflow

```yaml
name: Agentic Issue Handler

on:
  issues:
    types: [opened, reopened, labeled]

permissions:
  contents: write
  pull-requests: write
  issues: write

jobs:
  resolve-issue:
    if: contains(github.event.issue.labels.*.name, 'agent-help')
    runs-on: ubuntu-latest
    steps:
      # ... checkout and setup steps ...
      
      - name: Run Agentic Code Reviewer
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          GOOGLE_API_KEY: ${{ secrets.GOOGLE_API_KEY }}
        run: |
          ./gradlew codeReview \
            -PreviewPrompt="Resolve the following GitHub Issue in file (%s). Issue: ${{ github.event.issue.title }}"

      - name: Create Pull Request
        uses: peter-evans/create-pull-request@v6
        with:
          commit-message: "fix: resolve issue #${{ github.event.issue.number }}"
          branch: "agent/issue-${{ github.event.issue.number }}"
```

## API Providers

Cognotik supports multiple AI providers with automatic fallback:

### Google Gemini (Default)

```java
ChatModel chatModel = GeminiModels.getGeminiFlash_30_Preview();
```

### OpenAI

```java
ChatModel chatModel = OpenAIModels.getGPT4o();
```

### Anthropic Claude

```java
ChatModel chatModel = AnthropicModels.getClaude3Sonnet();
```

### Groq

```java
ChatModel chatModel = GroqModels.getLlama3_70B();
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Cognotik                             │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Harness   │  │   Tasks     │  │   Planning          │  │
│  │  Framework  │  │   System    │  │   Engine            │  │
│  ├─────────────┤  ├─────────────┤  ├─────────────────────┤  │
│  │ FileGen     │  │ FileModify  │  │ WaterfallMode       │  │
│  │ TaskHarness │  │ AutoFix     │  │ Brainstorming       │  │
│  │ PlanHarness │  │ SubPlan     │  │ Orchestration       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    Chat Interface                       ││
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌──────────────┐  ││
│  │  │ Gemini  │ │ OpenAI  │ │Anthropic │ │    Groq      │  ││
│  │  └─────────┘ └─────────┘ └──────────┘ └──────────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐│
│  │                  Platform Services                      ││
│  │  Session Management │ Usage Tracking │ File Storage     ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **Harness Framework**: Provides execution environments for different automation scenarios
- **Task System**: Modular task types (FileModification, AutoFix, SubPlan, etc.)
- **Planning Engine**: Sophisticated multi-step task orchestration with waterfall mode
- **Chat Interface**: Unified interface for multiple AI providers
- **Platform Services**: Session management, usage tracking, and file storage

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

```bash
# Clone the repository
git clone https://github.com/SimiaCryptus/Cognotik.git
cd Cognotik

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run with debug logging
./gradlew run --debug
```

### Code Style

This project follows standard Java conventions. Please ensure your code:

- Follows the existing code style
- Includes appropriate documentation
- Has test coverage for new features

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## Support

- 📖 [Documentation](https://github.com/SimiaCryptus/Cognotik/wiki)
- 🐛 [Issue Tracker](https://github.com/SimiaCryptus/Cognotik/issues)
- 💬 [Discussions](https://github.com/SimiaCryptus/Cognotik/discussions)

---

Made with ❤️ by [SimiaCryptus](https://github.com/SimiaCryptus)