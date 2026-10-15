package org.scent.project

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import kotlinx.coroutines.awaitCancellation

/** One step of a scripted model conversation. */
sealed interface Step {
    data class Reply(
        val text: String,
    ) : Step

    data class Fail(
        val error: Throwable,
    ) : Step

    data object Hang : Step
}

/** A fake model for tests: replays [steps] in order and records every prompt it was sent. */
class ScriptedLLMClient(
    private val steps: List<Step>,
) : LLMClient() {
    val prompts = mutableListOf<Prompt>()

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant {
        prompts += prompt
        return when (val step = steps[minOf(prompts.size, steps.size) - 1]) {
            is Step.Reply -> Message.Assistant(MessagePart.Text(step.text), ResponseMetaInfo.Empty)
            is Step.Fail -> throw step.error
            Step.Hang -> awaitCancellation()
        }
    }

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): LLMChoice = error("not used")

    override suspend fun moderate(
        prompt: Prompt,
        model: LLModel,
    ): ModerationResult = error("not used")

    override fun llmProvider(): LLMProvider = LLMProvider.OpenRouter

    override fun close() = Unit
}
