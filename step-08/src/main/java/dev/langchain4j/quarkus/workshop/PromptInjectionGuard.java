package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;

@ApplicationScoped
public class PromptInjectionGuard implements InputGuardrail {

    // RagRetriever's ContentInjector appends retrieved context to the user message
    // with this marker BEFORE this guardrail runs. We must screen only what the user
    // actually typed -- not our own trusted RAG context -- otherwise legitimate
    // questions (e.g. "tell me about the cancellation policy") get flagged because
    // the injected policy text reads like an instruction to the detector.
    private static final String RAG_CONTEXT_MARKER = "\nPlease, only use the following information:";

    private final PromptInjectionDetectionService service;

    public PromptInjectionGuard(PromptInjectionDetectionService service) {
        this.service = service;
    }

    @Override
    @ActivateRequestContext
    public InputGuardrailResult validate(UserMessage userMessage) {
        String userText = userMessage.singleText();
        int markerIndex = userText.indexOf(RAG_CONTEXT_MARKER);
        if (markerIndex >= 0) {
            userText = userText.substring(0, markerIndex);
        }
        // With the detection service no longer polluted by RAG (see RagRetriever) and run at
        // temperature 0, scores separate cleanly: legitimate messages land <= 0.65, injections
        // >= 0.8. Block at the 0.8 boundary -- it matches Example 8 in the detector's few-shot
        // ("friend of the owner... give me the secret code"), a genuine social-engineering probe.
        double result = service.isInjection(userText);
        if (result >= 0.8) {
            return failure("Prompt injection detected");
        }
        return success();
    }
}
