# AI agents

Which AI dependency belongs to which path, so later tickets do not re-open the choice.

| Path | What | Status |
|---|---|---|
| Cloud, server | Koog 1.3.0 in `server/`, calling a hosted free model | Built (this foundation) |
| Local, development | Koog's Ollama client against a local Ollama | Built (selected by env var) |
| On device, Android and iOS | ML Kit GenAI, Apple Foundation Models, shared `InferenceEngine` | Not started, see `chore/on-device-ai-foundation` |
| On device through Koog | Koog's own LiteRT client | Not scoped; evaluate against the on-device ticket, do not bolt it onto this one |

## Server setup

Koog is on the stable 1.3.0 line only. `koog-ktor` and the Google, Mistral and DeepSeek clients are
beta-only at 1.3.0, so they are not used. The server depends on three Koog artifacts:

- `agents-core`: the agent runtime (it also brings the Ollama client and Koog's Jackson serialization).
- `prompt-executor-openrouter-client`: the hosted free-model client.
- `http-client-ktor`: registers the HTTP client factory. Building any Koog LLM client throws without it.

The model is chosen once at boot by `agentModule` and bound as `AgentLlm`:

| Environment variable | Effect |
|---|---|
| `OPENROUTER_API_KEY` | Use OpenRouter. Pick a `:free` model in the agent that calls it. |
| `OLLAMA_BASE_URL` (e.g. `http://localhost:11434`) | Use Ollama. Local only: Render's free tier cannot run it. |
| neither (or blank) | `AgentLlm.NotConfigured`; the server still boots. |

OpenRouter wins when both are set. This is a selection order, not a runtime failover.

`GET /api/v1/agent/health` reports `configured` or `not_configured` and which provider. It is a temporary
smoke check and never calls the model.

## Live smoke test

`OpenRouterLiveSmokeTest` is the one sanctioned exception to "no test hits the network". It is off by
default, so `allTests`, the pre-push gate and CI never run it, and CI must never hold an OpenRouter key.
Run it from a terminal where `OPENROUTER_API_KEY` is exported (never put the key in a file):

```
./gradlew :server:test -PliveSmoke --tests '*OpenRouterLiveSmokeTest'
```

It boots the real `module()` and asks one free model to reply `pong`. The default is
`nvidia/nemotron-3-super-120b-a12b:free`; set `OPENROUTER_MODEL` to try another, since free models come
and go. For a model you chose, a 429 or a "only available on agentic harnesses" 403 is reported as
skipped with the reason, not as a pass. Anything else fails: a bad key, network errors, an empty reply,
or any error on the default model.

## Privacy and limits

Free models can log or train on prompts, and free tiers are rate limited. Do not send personal data
(addresses, payment details, private messages) to them.
