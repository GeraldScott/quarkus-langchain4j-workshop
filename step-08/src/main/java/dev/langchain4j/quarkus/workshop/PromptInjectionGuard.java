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
        // Measured separation on the raw query: legitimate messages (incl. giving a
        // name to cancel a booking) top out around 0.75, real injections score >= 0.85.
        double result = service.isInjection(userText);
        if (result > 0.8) {
            return failure("Prompt injection detected");
        }
        return success();
    }
}
