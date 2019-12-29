package eu.yeger.komi.backend.handler

import eu.yeger.komi.backend.KomiObjectMapper
import eu.yeger.komi.backend.User
import eu.yeger.komi.backend.error
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.atomic.AtomicLong

class SessionHandler : TextWebSocketHandler() {
    private val sessions = HashMap<WebSocketSession, User>()
    private val uids = AtomicLong(0)

    private val lobbyHandler = LobbyHandler(sessions)

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val user = sessions.remove(session)!!
        val lobby = user.lobby

        if (lobby !== null) {
            lobbyHandler.removeUserFromLobby(user, lobby)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val json = KomiObjectMapper.readTree(message.payload)

            if (!json.has("type") || !json.has("data")) return session.error("Invalid message format: ${message.payload}")

            when (json.get("type").asText()) {
                "join" -> join(session, json.get("data").asText())
                "createLobby" -> lobbyHandler.createLobby(session, json.get("data").asText())
                "joinLobby" -> lobbyHandler.joinLobby(session, json.get("data").asLong())
                "leaveLobby" -> lobbyHandler.leaveLobby(session, json.get("data").asLong())
            }
        } catch (e: Exception) {
            session.error(e.toString())
        }
    }

    private fun join(session: WebSocketSession, name: String) {
        sessions[session] = User(session, uids.getAndIncrement(), name)
        lobbyHandler.sendLobbiesTo(session)
    }
}
