package eu.yeger.komi.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.atomic.AtomicLong

@Configuration
@EnableWebSocket
class WSConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(LobbyHandler(), "/lobby").setAllowedOrigins("*")
    }
}

data class User(val id: Long)
data class Lobby(val id: Long) {
    val players: MutableList<User> = mutableListOf()
}

data class Message(val type: String, val data: Any)

class LobbyHandler : TextWebSocketHandler() {
    private val sessions = HashMap<WebSocketSession, User>()
    private val lobbies = HashSet<Lobby>()

    private val uids = AtomicLong(0)
    private val lids = AtomicLong(0)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session] = User(uids.getAndIncrement())
        session.send(Message("lobbies", lobbies))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val json = ObjectMapper().readTree(message.payload)
        when (json.get("type").asText()) {
            "leaveLobby" -> leaveLobby(session)
            "createLobby" -> createLobby()
        }
    }

    private fun leaveLobby(session: WebSocketSession) {
        sessions -= session
    }

    private fun createLobby() {
        lobbies += Lobby(lids.getAndIncrement())
        broadcast(Message("lobbies", lobbies))
    }

    private fun WebSocketSession.send(message: Message) = sendMessage(TextMessage(jacksonObjectMapper().writeValueAsString(message)))

    private fun broadcast(message: Message) = sessions.forEach { it.key.send(message) }
}
