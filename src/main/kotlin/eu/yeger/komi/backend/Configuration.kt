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

data class User(val id: Long, val name: String, @Transient val lobby: Lobby? = null)

data class Lobby(val id: Long, val name: String, val players: List<User>)

data class Message(val type: String, val data: Any)

class LobbyHandler : TextWebSocketHandler() {
    private val userMap = HashMap<WebSocketSession, User>()
    private val lobbyMap = HashMap<Long, Lobby>()

    private val uids = AtomicLong(0)
    private val lids = AtomicLong(0)

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val user = userMap.remove(session)!!
        val lobby = user.lobby

        if (lobby !== null) {
            lobbyMap[lobby.id] = lobby.copy(players = lobby.players - user)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val json = ObjectMapper().readTree(message.payload)
            if (!json.has("type")) return
            when (json.get("type").asText()) {
                "join" -> session.join(json.get("data").asText())
                "leave" -> session.leave()
                "createLobby" -> session.createLobby(json.get("data").asText())
                "joinLobby" -> session.joinLobby(json.get("data").asLong())
                "leaveLobby" -> session.leaveLobby(json.get("data").asLong())
            }
        } catch (e: Exception) {
            session.close(CloseStatus.SERVER_ERROR)
        }
    }

    private fun WebSocketSession.join(name: String) {
        userMap[this] = User(uids.getAndIncrement(), name)
        this.send(Message("lobbies", lobbyMap.values))
    }

    private fun WebSocketSession.leave() {
        userMap -= this
    }

    private fun WebSocketSession.createLobby(name: String) {
        if (userMap[this]?.lobby !== null) {
            return this.close(CloseStatus.SERVER_ERROR)
        }

        val lobby = Lobby(lids.getAndIncrement(), name, listOf())
        lobbyMap[lobby.id] = lobby
        broadcast(Message("lobbies", lobbyMap.values))
        joinLobby(lobby.id)
    }

    private fun WebSocketSession.joinLobby(id: Long) {
        val user = userMap[this]!!
        val lobby = lobbyMap[id]

        if (user.lobby !== null || lobby === null || lobby.players.size > 1) {
            return this.close(CloseStatus.SERVER_ERROR)
        }

        userMap[this] = user.copy(lobby = lobby)
        lobbyMap[id] = lobby.copy(players = lobby.players + user)
        broadcast(Message("lobbies", lobbyMap.values))
    }

    private fun WebSocketSession.leaveLobby(id: Long) {
        val user = userMap[this]!!
        val lobby = lobbyMap[id]

        if (user.lobby === null || lobby === null) {
            return this.close(CloseStatus.SERVER_ERROR)
        }

        userMap[this] = user.copy(lobby = null)
        lobbyMap[id] = lobby.copy(players = lobby.players - user)
        broadcast(Message("lobbies", lobbyMap.values))
        send(Message("joinedLobby", lobby))
    }

    private fun WebSocketSession.send(message: Message) = sendMessage(TextMessage(jacksonObjectMapper().writeValueAsString(message)))

    private fun broadcast(message: Message) = userMap.forEach { it.key.send(message) }
}
