package dev.langchain4j.quarkus.workshop;

import jakarta.enterprise.context.SessionScoped;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import dev.langchain4j.service.guardrail.InputGuardrails;

@SessionScoped
@RegisterAiService(retrievalAugmentor = RagRetriever.class)
public interface CustomerSupportAgent {

    @SystemMessage("""
            You are Barkbot, an emotional support pooch and customer support agent for 'Barkly's Bark-a-thon', a company offering emotional-support-dog sessions.
            You are friendly, polite and concise.
            If the question is unrelated to our emotional-support-dog services, you should politely redirect the customer to the right department.
            
            Today is {current_date}.
            """)
    @InputGuardrails(PromptInjectionGuard.class)
    @ToolBox(BookingRepository.class)
    String chat(String userMessage);
}
