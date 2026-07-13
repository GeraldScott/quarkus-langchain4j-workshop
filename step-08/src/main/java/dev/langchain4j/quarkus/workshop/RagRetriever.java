package dev.langchain4j.quarkus.workshop;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

// Exposed as a Supplier<RetrievalAugmentor> (NOT an unqualified RetrievalAugmentor bean) so that
// Quarkus does NOT auto-wire it into every @RegisterAiService. RAG must apply ONLY to the customer
// support agent -- if it also augments PromptInjectionDetectionService, the detector runs its own
// retrieval over the whole detection prompt and appends unrelated policy chunks, which dilutes the
// injection signal and lets attacks slip past the guardrail. CustomerSupportAgent references this
// explicitly via @RegisterAiService(retrievalAugmentor = RagRetriever.class).
@ApplicationScoped
public class RagRetriever implements Supplier<RetrievalAugmentor> {

    private final RetrievalAugmentor augmentor;

    public RagRetriever(EmbeddingStore store, EmbeddingModel model) {
        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(3)
                .build();

        this.augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(new ContentInjector() {
                    @Override
                    public ChatMessage inject(List<Content> list, ChatMessage chatMessage) {
                        StringBuffer prompt = new StringBuffer(((UserMessage) chatMessage).singleText());
                        prompt.append("\nPlease, only use the following information:\n");
                        list.forEach(content -> prompt.append("- ").append(content.textSegment().text()).append("\n"));
                        return new UserMessage(prompt.toString());
                    }
                })
                .build();
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }
}
