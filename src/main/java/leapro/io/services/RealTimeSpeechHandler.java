package leapro.io.services;

import okhttp3.*;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * RealTimeSpeechHandler handles WebSocket communication for real-time speech processing.
 * It acts as a bridge between the client and OpenAI's Realtime API.
 */
@Component
public class RealTimeSpeechHandler extends TextWebSocketHandler {

    /** URL for the OpenAI Realtime API endpoint */
    private static final String AZURE_OPENAI_WS_URL = "wss://aiwhisperandothers-eus-oepnai-01.openai.azure.com/openai/realtime?api-version=2024-10-01-preview&deployment=gpt-4o-realtime-preview";

    /** OpenAI API key (replace with your own) */
    private static final String OPENAI_API_KEY = "";

    /** OkHttpClient instance for creating HTTP connections */
    private final OkHttpClient client = new OkHttpClient();

    /** WebSocket connection to OpenAI Realtime API */
    private WebSocket openAIWebSocket;

    /** WebSocket session with the client */
    private WebSocketSession clientSession;


//    String sessionUpdateMessage = "{ " +
//            "\"type\": \"session.update\", " +
//            "\"session\": { " +
//            "\"instructions\": \"You are a helpful assistant.\","+
//            "\"turn_detection\": { \"type\": \"none\" }, " +
//            "\"input_audio_transcription\": { \"model\": \"whisper-1\" } " +
//            "} " +
//            "}";

    String sessionUpdateMessage = "{ " +
            "\"type\": \"session.update\", " +
            "\"session\": { " +
            "\"instructions\": \"You are a helpful assistant.\","+
            "\"turn_detection\": { \"type\": \"server_vad\", \"threshold\": 0.5, \"prefix_padding_ms\": 300,\"silence_duration_ms\": 500 }, " +
            "\"voice\": \"alloy\","+
            "\"temperature\": 1,"+
            "\"input_audio_transcription\": { \"model\": \"whisper-1\" } " +
            "} " +
            "}";

    private static final String template1 = """
            You are a helpful customer services agent at BANB ABC.
            The user name is Ahmed 
            Use the information from the DOCUMENTS section to augment answers.
            If you don't find the answer say sorry the information is not available now.
            Start by saying Welcome Ahmed, How i can help you today?

            DOCUMENTS:
            {documents}
            """;


    @Autowired
    VectorStore vectorStore;

    /**
     * Invoked after a WebSocket connection with the client is established.
     * Establishes a separate WebSocket connection with the OpenAI Realtime API.
     * @param session the WebSocket session with the client
     * @throws Exception if an error occurs during connection establishment
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.clientSession = session;
        System.out.println("Connection Established Session Id: " + session.getId());

        // Build request for Azure OpenAI Realtime API connection
        Request request = new Request.Builder()
                .url(AZURE_OPENAI_WS_URL)
                .addHeader("api-key", OPENAI_API_KEY)
                .build();

        //Create WebSocket connection to Azure OpenAI Realtime API
        openAIWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("Connected to Azure OpenAI Realtime API.");

                //to link the RAG
                SystemPromptTemplate promptTemplate1 = new SystemPromptTemplate(template1);
                final var systemMessage1 = promptTemplate1.createMessage(Map.of("documents", getDocPrompt() ));

                String tmp = "{ " +
                        "\"type\": \"session.update\", " +
                        "\"session\": { " +
                        "\"instructions\": \""+systemMessage1.getText()+"\", " +
                        "\"turn_detection\": { \"type\": \"server_vad\", \"threshold\": 0.5, \"prefix_padding_ms\": 300,\"silence_duration_ms\": 500 }, " +
                        "\"voice\": \"alloy\","+
                        "\"temperature\": 1,"+
                        "\"input_audio_transcription\": { \"model\": \"whisper-1\" } " +
                        "} " +
                        "}";
                //~
                String sessionUpdateMessage1 = tmp.replaceAll("\\r\\n|\\r|\\n", " ");

                openAIWebSocket.send(sessionUpdateMessage1);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("Message Received from Azure OpenAI Realtime API.: "+text);

                try {
                    if (clientSession.isOpen()) {
                        clientSession.sendMessage(new TextMessage(text));
                    }
                } catch (Exception e) {
                    System.err.println("Error forwarding message to client: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("OpenAI WebSocket connection failed: " + t.getMessage());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("OpenAI WebSocket closed: " + reason);
            }
        });
    }

    /**
     * Handles incoming text messages from the client.
     * Forwards the message to the OpenAI Realtime API connection.
     * @param session the WebSocket session with the client
     * @param message the incoming text message
     * @throws Exception if an error occurs during message processing
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //System.out.println("Message from " + session.getId() + ": " + message);

        if (openAIWebSocket != null) {
            openAIWebSocket.send(message.getPayload());
        }
    }

    /**
     * Invoked after the WebSocket connection with the client is closed.
     * Closes the connection with the OpenAI Realtime API if it's still open.
     * @param session the WebSocket session with the client
     * @param status the close status
     * @throws Exception if an error occurs during connection closure
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Session Closed : " + session.getId());
        if (openAIWebSocket != null) {
            openAIWebSocket.close(1000, "Client closed connection");
        }
    }

    ///RAG pattern, querying the docs
    String getDocPrompt(){
        // Ensure the query matches the index schema
        final var candidateDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("*") //we will take all document for now
//                        .query(question)
                        .topK(5)
                        .build()
        );

        System.out.println("Retrieved Documents: " + candidateDocs.size());
        for (Document doc : candidateDocs) {
            System.out.println("Doc Content: " + doc.getFormattedContent());
        }
//        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(5).build());


        //final var userMessage = new UserMessage(question);

        String docPrompts = candidateDocs.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n"));

        System.out.println("Final Prompt to GPT-4o:\n" + docPrompts);

        return docPrompts;
    }
}
