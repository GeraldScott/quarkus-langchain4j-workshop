# Design: Migrate quarkus-langchain4j-workshop to latest Quarkus/LangChain4j on olduvai Ollama

**Date:** 2026-07-11
**Status:** Approved (design), pending spec review

## Goal

Take a two-year-old Quarkus + LangChain4j workshop that runs against OpenAI (`gpt-4o`, port 8080)
and:

1. Update it to the latest Quarkus / LangChain4j.
2. Reconfigure it to use a **local Ollama server on host `olduvai`**, running on **port 9876**.
3. Drive Chrome through each of the 8 tutorial steps and verify behavior against the tutorial docs.

## Current state (verified)

- 8 standalone Maven modules `step-01` … `step-08` (root pom aggregates them; **no shared parent** —
  each opens independently, by design). Java 21.
- **Quarkus 3.15.1 + quarkus-langchain4j 0.18.0**, all using OpenAI `gpt-4o` via
  `quarkus-langchain4j-openai`, API key from `OPENAI_API_KEY`. Runs on port 8080.
- Step progression:
  - 01 basic chat → 02 chat params (temperature/max-tokens/frequency-penalty) → 03 system message →
    04 streaming (`Multi<String>`) → 05 easy-RAG (uses **OpenAI embeddings**) →
    06 pgvector + **in-process BGE ONNX embeddings** (384-dim, `BgeSmallEnQuantizedEmbeddingModel`) →
    07 + function-calling (booking tools, `@ToolBox(BookingRepository.class)`, Panache + PostgreSQL) →
    08 + guardrails (`PromptInjectionGuard implements InputGuardrail`, plus a second AiService that
    returns a bare float 0.0–1.0).
- Steps 06–08 embed **locally in-JVM** — only the **chat model** ever needs olduvai.

## Target environment (verified)

- **olduvai Ollama**: `http://olduvai.internal.archton.io:11434` (= `192.168.1.6`), Ollama v0.31.1,
  no auth, LAN-scoped. RTX 2060 6GB; sweet spot 3–4B models; first cold model load ~50s.
- Installed models: `llama3.2`, `nomic-embed-text`. **`qwen2.5:3b` to be pulled.**
- This host: podman 5.8.2 **and** docker 29.6.1 present (pgvector Dev Services will work), Java 21.0.9.
- SSH access to olduvai is available (pull models directly).

## Decisions

### D1 — Versions: matched pair, not strictly-newest Quarkus
Latest quarkus-langchain4j is **1.12.0**; its parent pom pins **Quarkus 3.33.2** and
langchain4j-core **1.17.2**. Newest Quarkus overall (3.37.2) is *untested* with the 1.12.0 extension.
**Use the matched pair: Quarkus 3.33.2 + quarkus-langchain4j 1.12.0.** Update the two version
properties in each of the 8 step poms.

### D2 — Ollama wiring: the Ollama extension (not the OpenAI-compat shim)
Swap `quarkus-langchain4j-openai` → `quarkus-langchain4j-ollama` (1.12.0) in every step pom. Uses
Ollama's **native tool API** (most robust for the function-calling steps). `OPENAI_API_KEY` removed.

Base `application.properties` for each step:
```properties
quarkus.http.port=9876
quarkus.langchain4j.ollama.base-url=http://olduvai.internal.archton.io:11434
quarkus.langchain4j.ollama.chat-model.model-name=qwen2.5:3b
quarkus.langchain4j.ollama.timeout=120s          # olduvai cold-load is ~50s
quarkus.langchain4j.ollama.log-requests=true
quarkus.langchain4j.ollama.log-responses=true
```
**Param mapping caveat:** steps 02+ set OpenAI `max-tokens` and `frequency-penalty`. The Ollama
extension options differ (`temperature`, `top-p`, `top-k`, `num-predict` ≈ max-tokens; no direct
`frequency-penalty`). Map `temperature`, translate `max-tokens`→`num-predict`, drop unsupported.

### D3 — Chat model: `qwen2.5:3b`
The workshop's steps 07–08 need reliable function-calling and a guardrail that returns a bare float.
A 3B `llama3.2` is weak at both; the user's olduvai notes flag `qwen2.5` for native function-calling
and structured output. `qwen2.5:3b` stays 100% on the RTX 2060 (~3.5GB, ~50 tok/s). Pull via SSH.

### D4 — Embeddings
- **Steps 06–08:** keep the in-process **BGE ONNX** model (384-dim); pgvector dimension stays 384.
  It's a taught feature and never used OpenAI.
- **Step 05 (easy-RAG):** point at olduvai's **`nomic-embed-text`**
  (`quarkus.langchain4j.ollama.embedding-model.model-name=nomic-embed-text`) so it truly "uses olduvai."

### D5 — Port: 9876 via `quarkus.http.port=9876` in each step.

## Migration surface (0.18 → 1.12) — the real work

Compile each step and fix breakages, consulting context7 for the 1.12 API. Anticipated hot spots:
- **Guardrails** (08): `InputGuardrail` / `InputGuardrailResult` / `success()` / `failure()` —
  package or signature likely moved between 0.18 and 1.12.
- **`@ToolBox` / `@RegisterAiService` / `@SystemMessage` / `@UserMessage`** (03, 07, 08) — verify.
- **Streaming `Multi<String>`** (04); **pgvector** config keys (06–08); **easy-RAG** keys (05).
- Possible two `EmbeddingModel` beans in 06–08 (BGE + any Ollama default) → disambiguate
  (existing `quarkus.langchain4j.embedding-model.provider=...BgeSmallEnQuantizedEmbeddingModel`).

## Browser verification (end state)

Sequentially for each step 01→08 (one app at a time on 9876, per the docs' "stop before next step"):
1. `./mvnw quarkus:dev` in `step-XX/`; wait for startup (06–08 auto-start pgvector via Dev Services).
2. Drive Chrome (claude-in-chrome tools) to `http://localhost:9876`; perform that step's interactions
   drawn from `docs/docs/step-XX.md`; screenshot.
3. **Pass criterion = behavioral match, not exact wording.** A local qwen2.5:3b won't reproduce
   gpt-4o's exact text, so verify the *capability* the docs demonstrate (greets, refuses off-topic,
   retrieves from RAG, cancels a booking via tool, blocks prompt injection), not literal strings.
4. Stop the app; continue. Report per-step: what was observed vs. what the docs expect.

## Non-goals

- Not rewriting the tutorial docs (they remain the behavioral reference; still reference OpenAI/8080).
- Not hoisting step poms to a shared parent (keeps each step standalone).
- Not changing app logic/UI beyond what the version migration forces.

## Prerequisites / order of operations

1. `ssh olduvai` → `ollama pull qwen2.5:3b`; confirm `ollama ps` stays 100% GPU.
2. Migrate poms + properties + code, step by step, compiling each.
3. Browser-verify each step against its docs.

## Risks

- **qwen2.5:3b tool-calling** may still occasionally misfire on cancel-booking; mitigation = the model
  choice already trades up from llama3.2; if a step is flaky, note it rather than over-tune.
- **Guardrail float parsing** — if 1.12 changed how a `double`-returning AiService is parsed, adapt.
- **First request latency** (~50s cold load) — handled by `timeout=120s`.
