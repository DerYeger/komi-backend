package eu.yeger.komi.backend

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.yeger.komi.backend.handler.SessionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WSConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(SessionHandler(), "/", "").setAllowedOrigins("*")
    }
}

val KomiObjectMapper: ObjectMapper = jacksonObjectMapper().configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
