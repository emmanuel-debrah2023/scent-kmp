# AI agents

Which AI dependency belongs to which path, so later tickets do not re-open the choice.

| Path | What | Status |
|---|---|---|
| Cloud, server | Koog 1.3.0 in `server/`, calling a hosted model through OpenRouter (Claude Haiku 5.5 by default) | Built (this foundation) |
| Local, development | Koog's Ollama client against a local Ollama | Built (selected by env var) |
| On device, Android and iOS | ML Kit GenAI, Apple Foundation Models, shared `InferenceEngine` | Not started, see `chore/on-device-ai-foundation` |
| On device through Koog | Koog's own LiteRT client | Not scoped; evaluate against the on-device ticket, do not bolt it onto this one |

## Server setup

Koog is on the stable 1.3.0 line only. `koog-ktor` and the Google, Mistral and DeepSeek clients are
beta-only at 1.3.0, so they are not used. The server depends on three Koog artifacts:

- `agents-core`: the agent runtime (it also brings the Ollama client and Koog's Jackson serialization).
- `prompt-executor-openrouter-client`: the hosted model client.
- `http-client-ktor`: registers the HTTP client factory. Building any Koog LLM client throws without it.

The model is chosen once at boot by `agentModule` and bound as `AgentLlm`:

| Environment variable | Effect |
|---|---|
| `OPENROUTER_API_KEY` | Use OpenRouter. The ask-back default is a paid model, so the account needs credit (see "Choosing the model"). |
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
  In v0.9 `deleteSurface` is a server-to-client message, so the client never sends one: before applying a
  new ask's batch it applies a local delete of its previous ask surface, in the same step.
- **Stateless answers:** the client echoes its answers in the action context (`intent`, `family`, `budget`,
  `size`). Each value is re-checked (known enum names, a finite budget above 0), unknown keys are ignored and a
  budget above the slider's live maximum is clamped. `edit_answers` makes no model call and replays identically.
- **Data model paths:** `/answers/{intent,family,budget,size}`, `/options/{family,size}` (`{value,label}`),
  `/budget/{min,max,step}` (step 5; both ends are multiples of the step, rounded up from the live prices, with a
  minimum of one step and the ends at least a step apart), `/summary`, `/results/<i>/{listingId,name,brand,price,meta,why}`. Prices are
  formatted strings (`£75`). Every fact lives here; components carry only wording and `DataBinding` paths.
- **Meta line:** the server formats `meta` as kind, fill, size, brand, leaving out what it does not have
  (`Sealed · 100 ml · Atelier Nord`, `Opened · 95% full · 100 ml · Atelier Nord`). Fill shows only for an opened
  bottle or a tester, and comes from the midpoint of the fill band the seller declared.
- **Layout:** the question inputs sit in a `Column` with `variant` `raised`; the heading and button stay outside
  it. A client that does not know the prop ignores it and draws a plain column.
- **AI captions:** a `caption` text says "Question written by AI" above the heading or "Reasons written by AI ·
  prices from Scent" above the cards. It is sent only when `modelWritten` is true, so the rules fallback and the
  fixed edit heading never claim AI wrote them. The reasons caption shows if any reason came from the model.
- **Opening a listing:** each result card carries an `open_listing` action whose `listingId` context entry is
  bound to `/results/<i>/listingId`. The action is client-handled (`ScentAction.handledOnClient`): the app
  navigates to the listing itself, so `POST /ask/actions` answers it with a 400 "Invalid action" and never
  calls the agent. The id comes only from server-built data and the client re-checks it (a whole number above 0)
  before it draws the card as tappable or navigates.
- **Time budget:** each model step gets `ASK_BACK_BUDGET_MS` (default 12000, max 25000). Past it the model is
  cancelled, its late answer discarded and the rules answer returned. Straight-to-results makes two model steps,
  so the worst case is about 24 s.
- **Errors:** invalid input is a 400, storage or assistant failures a 503, with a short `ErrorResponse`; an
  expected failure is never a 500.

## Timeout policy

Every client request gets 30 s, and so does a silent socket (`installScentDefaults`). There is no longer limit
for the ask routes.

- **Why the socket timeout is set:** left unset, each engine keeps its own default. OkHttp's is 10 s, which fired
  before the request timeout and showed a slow server as "You are offline". A timeout now maps to
  `NetworkError.Timeout`, which the Ask tab shows as "That took too long" with a retry.
- **Why 30 s and not more for ask:** the server's worst case is about 24 s, two model steps at 12 s plus two quick
  database reads. The ask is rarely a session's first request, because the feed loads first and starts waking the
  server. A longer limit would not save a full cold start and would leave the shopper staring at a spinner. A
  raised limit for every route would do the same to a hung feed or login.
- **Keep the budget in step:** `ASK_BACK_BUDGET_MS` can go up to 25 s, which is 50 s over two steps. Above about
  14 s the client can give up on an answer the server is still producing.
- **Retry costs quota:** retrying re-sends the whole ask, so it can spend model calls again, and a server may still
  be working on the request the client gave up on.

Before a demo, wake the instance with `./scripts/demo-warm.sh <base-url>` and ask once to check the model answers.
If an ask does time out on a cold instance, Retry succeeds once it is awake.

## A2UI contracts

`:contracts` holds the A2UI v0.9 message types (`A2uiServerMessage`, `A2uiClientMessage`) and the
`scent/v1` catalog (`ScentComponent`, `ScentAction`) for android, ios and jvm. Message types live here and
nowhere else: `a2ui-4k` pulls in Compose Desktop, A2CUI needs Java 21 and is alpha, and the official
Kotlin SDK pulls in Google ADK. The client engine (ADR-0003) reads these same types; no renderer library sits between them.

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

It boots the real `module()` and asks one model to reply `pong`. The default is
`anthropic/claude-haiku-5.5`; set `OPENROUTER_MODEL` to try another, for example a `:free` model, since free
models come and go. For a model you chose, a 429 or a "only available on agentic harnesses" 403 is reported as
skipped with the reason, not as a pass. Anything else fails: a bad key, network errors, an empty reply,
or any error on the default model.

## Ask-back agent

`AskBackAgent` (`server/.../askback`) turns a shopper's free text into preferences, picks which
questions to ask and writes the heading, button label and one 'why' line per result. Kotlin builds
every option and price; the model only chooses and words. The route that serves it is
`feature/ask-surface-routes`.

`ASK_BACK_AGENT` is a JVM system property, not an environment variable: pass it with `-D` (for example
`./gradlew :server:run -DASK_BACK_AGENT=fake`), because `Application.kt` reads it with `System.getProperty`.

| Setting | Effect |
|---|---|
| `-DASK_BACK_AGENT=fake` | Canned gift answer, no model. For E2E. |
| `-DASK_BACK_AGENT=rules` | No model: asks every missing question, why lines from the fragrance's own notes. |
| unset (default) | The model, with the rules agent behind it (`FallbackAskBackAgent`). |
| `AGENT_MODEL` | Model id. Default `anthropic/claude-haiku-5.5` (OpenRouter) or `llama3.2` (Ollama). Set it in `.env` or the environment: `./gradlew :server:run` does not forward `-DAGENT_MODEL`. |

- The reply is untrusted. `AskBackValidator` is the safety boundary: enums, a budget that appears as a
  number in the query, question ids from the planner, no digits, £ or links in any model-written text,
  and why lines may only name notes the fragrance has. A broken rule triggers one retry, then rules.
- Provider failures (429, 403, timeout, not configured) come back as `AppError.AgentError`, never
  thrown, are never retried, and fall back to the rules answer. Only the error type and status are
  logged, never the query or the provider's body.
- What Koog 1.3.0 can throw from `execute`, read from its source, all mapped to `AgentError.Unavailable`
  (or `RateLimited` for a 429): `LLMClientException` and `KoogHttpClientException` are its own.
  `IllegalArgumentException` comes from its `require` on an empty reply, `IllegalStateException` from its `check`
  on a reply's shape, and `SerializationException` from the Ollama client decoding outside its try. An
  `IOException` is caught too. Only `CancellationException` passes through, and it is caught first because on the
  JVM it is an `IllegalStateException`.
- Koog logs full prompts at DEBUG, so `logback.xml` pins `ai.koog` to INFO. `OllamaClient` logs each failure's
  message at ERROR, and that message holds the provider's error, so its logger is switched off.
- Eval: `./gradlew :server:test -PliveSmoke --tests '*AskBackEvalLiveTest'` (key in the environment) runs
  22 ask prompts, paced at 3.5 s, and times the results step at 3, 10 and 20 cards. It writes
  `server/build/eval/ask-back.csv` and `ask-back-explain.csv`; `OPENROUTER_MODEL` picks the model. Recorded
  runs are in `docs/evals/`, one pair of files per model, and are compared under "Choosing the model".
- A model that gets retired returns a 404 that falls back to rules, so nothing looks broken. Free models
  are retired without notice. `/api/v1/agent/health` never calls the model and still says `configured`. Check
  the server log for `Ask-back falling back to rules: Unavailable status=404`, and set `AGENT_MODEL` to move
  to another model without a code change. `OPENROUTER_MODEL` picks the model for the live tests only.

## Choosing the model

Each model step has a 12 s budget before the rules answer is used, so speed decides more than accuracy.
Measured on 2026-11-07 with the eval above:

| | Claude Haiku 5.5 (paid, default) | Nemotron 3 Ultra (free) |
|---|---|---|
| Ask step: answered, valid first try | 22 of 22 | 20 of 22 |
| Ask step: preferences correct | 19 of 22 | 16 of 22 |
| Ask step: median and max latency | 1.7 s, 2.2 s | 5.3 s, 20 s (two timeouts) |
| Results step, 3 cards | 3 of 3 inside 12 s (3.1 to 4.6 s) | 2 of 3 (9.2 s, 10.2 s, one provider error) |
| Results step, 10 cards | 3 of 3 (5.0 to 5.5 s) | 1 of 3 |
| Results step, 20 cards | 3 of 3 (7.9 to 9.4 s) | 0 of 3 |
| Cost | about $0.17 per 1000 calls (two calls per full ask) | free, about 50 requests a day |

On the app's demo path, Haiku 5.5 answered both steps with the model in 10 of 10 runs, against the free
model's 3 of 5 on the results step. An earlier free model, `nvidia/nemotron-3.5-lightning:free`, timed out on
16 of 22 ask prompts and was not recorded.

**Decision:** the default is `anthropic/claude-haiku-5.5`, a paid model. With a free default the rules
answer shows up in roughly a third to half of asks, and the "written by AI" captions disappear with it.
At about $0.0003 per full ask, £10 of credit covers tens of thousands of asks. The rules fallback stays in
place for provider errors. The server sends at most `MAX_CANDIDATES` (20) cards, the eval's worst case, which
leaves about 2.5 s of the budget, so raising that cap needs a fresh measurement.

To run without credit, set `AGENT_MODEL` to a `:free` model; expect the fallback rates above. An OpenRouter
key can carry its own spending limit on top of the account's credit, so check both if paid calls start
failing.

## Privacy and limits

Hosted models, and free ones in particular, can log or train on prompts, and free tiers are rate limited.
Do not send personal data (addresses, payment details, private messages) to them.
