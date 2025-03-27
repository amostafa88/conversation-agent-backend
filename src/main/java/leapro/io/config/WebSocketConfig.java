package leapro.io.config;

import leapro.io.services.RealTimeSpeechHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // #### The WebSocket Server, To interact with the front end
//    private final WebSocketHandler webSocketHandler;
//
//    public WebSocketConfig(WebSocketHandler webSocketHandler) {
//        this.webSocketHandler = webSocketHandler;
//    }
//
//    @Bean
//    public HandlerMapping handlerMapping() {
//        Map<String, WebSocketHandler> map = new HashMap<>();
//        map.put("/event-emitter", webSocketHandler);
//
//        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
//        handlerMapping.setUrlMap(map);
//        handlerMapping.setOrder(-1); // Higher precedence than other mappings
//        return handlerMapping;
//    }
//
//    @Bean
//    public WebSocketHandlerAdapter handlerAdapter() {
//        return new WebSocketHandlerAdapter();
//    }
    private final RealTimeSpeechHandler realTimeSpeechHandler;

    // Autowires the RealTimeSpeechHandler instance into the class
    @Autowired
    public WebSocketConfig(RealTimeSpeechHandler realTimeSpeechHandler) {
        this.realTimeSpeechHandler = realTimeSpeechHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registers the RealTimeSpeechHandler to handle WebSocket requests on the "/api/realtime-api" endpoint
        // Allows connections from all origins
        registry.addHandler(realTimeSpeechHandler, "/api/realtime-api").setAllowedOrigins("*");
    }

}