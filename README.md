# NIK AI Mobile

A local-first Android AI assistant project that combines a native mobile interface with on-device/local LLM inference, persistent memory, conversation history, and document RAG.

## Source availability

This public repository is a sanitized portfolio edition. It contains selected documentation, configuration examples, architecture material, and representative code suitable for public review. The complete working development repository and full implementation are maintained privately.

## Project goal

NIK AI Mobile explores how much of a practical AI-assistant stack can run locally around an Android phone while keeping model inference and application data under user control.

The project grew from a local Flask + `llama.cpp` assistant into a native Android client with persistent memory, documents, RAG, secure same-device communication, and a Termux-managed local model runtime.

## Current capabilities

- Native Android interface built with Jetpack Compose
- Local GGUF inference through `llama.cpp` / `llama-server`
- Termux runtime helpers for starting, stopping, and checking the local AI stack
- Selectable local model profiles
- Flask application backend
- Streaming chat with STOP/cancellation behavior
- Persistent conversations and messages
- Long-term memory
- Document upload, parsing, indexing, and retrieval
- Normal and Strict RAG modes
- Evidence-oriented document responses
- Secure same-device transport between Android and the local backend
- Hotspot-based second-device access path
- Settings and runtime status handling

## Architecture

The system separates responsibilities across three layers:

1. **Android app** — native UI, user interaction, streaming consumption, navigation, lifecycle behavior, and endpoint selection.
2. **Flask backend** — application orchestration, prompts, persistent memory, conversation history, document processing, retrieval, RAG, settings, and evidence handling.
3. **llama.cpp runtime** — local language-model inference through an OpenAI-compatible server endpoint.

This separation keeps the Android client focused on UX while the backend owns persistent AI data and retrieval logic.

## Local model runtime

The Termux helper can run supported local GGUF models through `llama-server`. Model files are local artifacts and are intentionally excluded from Git.

The current helper supports explicit model selection rather than silently switching models. Runtime processes, logs, local tokens, and model files remain outside version control.

## RAG and memory

NIK AI Mobile includes separate persistent systems for:

- conversation history
- long-term memory
- uploaded documents
- document retrieval and evidence

Strict RAG is designed to answer from retrieved evidence rather than silently substituting unsupported model knowledge when document evidence is insufficient.

## Security and privacy

The repository excludes local secrets and runtime data through `.gitignore`, including:

- `.env` files
- transport tokens
- Android signing files and keystores
- local databases
- uploaded documents
- local settings
- model files
- private keys
- logs and process files

The same-device transport uses a private credential path, while hotspot access follows a separate local-network route.

## Technology stack

- Kotlin
- Android / Jetpack Compose
- Python
- Flask
- SQLite
- `llama.cpp`
- GGUF local models
- Termux
- Local HTTP streaming

## Validation

The project has been developed in staged phases with automated Python tests, Android JVM tests, release builds, signing verification, and physical-device regression checks. Individual phase documents in the private development repository retain the detailed development and validation history.

## Repository notes

Model binaries, APK signing material, local databases, runtime tokens, user documents, and device-specific private data are intentionally not included in this public portfolio repository.

## Roadmap

The current mobile release is a completed local-AI foundation. Future work may include additional model benchmarking, stronger packaging/installation, improved multi-device behavior, and integration with broader NIK AI agent/tool capabilities.

## Media

Screenshots and a short demonstration video will be added in a later portfolio pass.
