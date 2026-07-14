package dev.langchain4j.quarkus.workshop;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

import dev.langchain4j.guardrail.GuardrailException;

@WebSocket(path = "/customer-support-agent")
public class CustomerSupportAgentWebSocket {

    private final CustomerSupportAgent customerSupportAgent;

    public CustomerSupportAgentWebSocket(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    @OnOpen
    public String onOpen() {
        return "Welcome to Barkly's Bark-a-thon! How can I help you today?";
    }

    @OnTextMessage
    public String onTextMessage(String message) {
        try {
            return customerSupportAgent.chat(message);
        } catch (GuardrailException e) {
            Log.errorf(e, "Guardrail blocked the request: %s", e.getMessage());
            return "Sorry, I am unable to process your request at the moment. It's not something I'm allowed to do.";
        } catch (Exception e) {
            // Any other failure -- LLM timeout, Ollama unreachable, a tool blowing up, etc.
            // We MUST still return a message: the browser only clears its "typing" spinner when a
            // frame arrives, so an unhandled exception here would leave the user staring at an
            // eternal spinner with no response.
            Log.errorf(e, "Error handling chat message: %s", e.getMessage());
            return "Sorry, I'm having trouble responding right now. Please try again in a moment.";
        }
    }
}
