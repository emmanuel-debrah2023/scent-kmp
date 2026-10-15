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

## Ask surface routes

Both routes need a signed-in user and speak A2UI v0.9 with `A2uiJson` (not the server's ContentNegotiation
`Json`, which writes explicit nulls and would break the one-message-key envelope).

| Route | Body | Returns |
|---|---|---|
| `POST /api/v1/surfaces/ask` | `{"query": "..."}` (1 to 500 characters) | `{"messages": [...]}`: createSurface, updateDataModel, updateComponents; or straight to results when nothing is left to ask |
| `POST /api/v1/surfaces/ask/actions` | an `A2uiClientMessage` with `action.name` `answer_questions` or `edit_answers` | updateDataModel and updateComponents on the request's own surface; never createSurface |

- **Surface ids:** every ask mints a fresh `ask-<uuid v4>`, shared by all messages in the batch, because v0.9
  makes a duplicate createSurface an error. The server is stateless, so it only checks an incoming id's shape,
  not that it issued it; that is enough since the id just routes the reply and the context is re-validated.
  The client must send `deleteSurface` for its previous ask surface before applying a new one.
- **Stateless answers:** the client echoes its answers in the action context (`intent`, `family`, `budget`,
  `size`). Each value is re-checked (known enum names, a finite budget above 0), unknown keys are ignored and a
  budget above the live maximum is clamped. `edit_answers` makes no model call and replays identically.
- **Data model paths:** `/answers/{intent,family,budget,size}`, `/options/{family,size}` (`{value,label}`),
  `/budget/{min,max,step}` (whole pounds, step 5), `/summary`, `/results/<i>/{name,brand,price,why}`. Prices are
  formatted strings (`£75`). Every fact lives here; components carry only wording and `DataBinding` paths.
- **Time budget:** each model step gets `ASK_BACK_BUDGET_MS` (default 12000, max 25000). Past it the model is
  cancelled, its late answer discarded and the rules answer returned. Straight-to-results makes two model steps,
  so the worst case is about 24 s.
- **Errors:** invalid input is a 400, storage or assistant failures a 503, with a short `ErrorResponse`; an
  expected failure is never a 500.

## A2UI contracts

`:contracts` holds the A2UI v0.9 message types (`A2uiServerMessage`, `A2uiClientMessage`) and the
`scent/v1` catalog (`ScentComponent`, `ScentAction`) for android, ios and jvm. Message types live here and
nowhere else: `a2ui-4k` pulls in Compose Desktop, A2CUI needs Java 21 and is alpha, and the official
Kotlin SDK pulls in Google ADK. The renderer ticket adapts these types into its library.

- Props that carry facts (options, prices, names) are `DataBinding` JSON Pointers, never literals.
- Unknown component names decode to `ScentComponent.Unknown`, so one new component does not drop a surface.
- `A2uiJson` decodes as leniently as `JsonConfig` but omits absent keys (`encodeDefaults = false`,
  `explicitNulls = false`), the only deviation: the envelope carries exactly one message key.
- `./gradlew :contracts:generateCatalogSchema` writes the catalog JSON Schema, generated from the Kotlin
  types, to `contracts/build/generated/a2ui/scent-v1.catalog.schema.json`. The ask-back agent does not put
  it in its prompt; Kotlin builds every component.

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

## Ask-back agent

`AskBackAgent` (`server/.../askback`) turns a shopper's free text into preferences, picks which
questions to ask and writes the heading, button label and one 'why' line per result. Kotlin builds
every option and price; the model only chooses and words. The route that serves it is
`feature/ask-surface-routes`.

| Environment variable | Effect |
|---|---|
| `ASK_BACK_AGENT=fake` | Canned gift answer, no model. For E2E. |
| `ASK_BACK_AGENT=rules` | No model: asks every missing question, why lines from the fragrance's own notes. |
| unset (default) | The model, with the rules agent behind it (`FallbackAskBackAgent`). |
| `AGENT_MODEL` | Model id. Default `nvidia/nemotron-3-super-120b-a12b:free` (OpenRouter) or `llama3.2` (Ollama). |

- The reply is untrusted. `AskBackValidator` is the safety boundary: enums, a budget that appears as a
  number in the query, question ids from the planner, no digits, £ or links in any model-written text,
  and why lines may only name notes the fragrance has. A broken rule triggers one retry, then rules.
- Provider failures (429, 403, timeout, not configured) come back as `AppError.AgentError`, never
  thrown, are never retried, and fall back to the rules answer. Only the error type and status are
  logged, never the query or the provider's body.
- Koog logs full prompts at DEBUG, so `logback.xml` pins `ai.koog` to INFO.
- Eval: `./gradlew :server:test -PliveSmoke --tests '*AskBackEvalLiveTest'` (key in the environment)
  runs 22 prompts, paced at 3.5 s, and writes `server/build/eval/ask-back.csv`. A free account allows
  about 50 requests a day. A recorded run is in `docs/evals/ask-back.csv`: 22 of 22 answered and valid
  first try, 19 of 22 matching the expected preferences, median latency about 10 s (max 16 s) on
  `nvidia/nemotron-3-super-120b-a12b:free`. A free model will not meet a 3 s route budget, so the
  route's timeout and rules fallback matter more than the model's accuracy.

## Privacy and limits

Free models can log or train on prompts, and free tiers are rate limited. Do not send personal data
(addresses, payment details, private messages) to them.
