package eu.yeger.komi.backend

import org.springframework.web.socket.WebSocketSession

data class Message(val type: String, val data: String)

data class User(
        @Transient val session: WebSocketSession,
        val id: Long,
        val name: String,
        @Transient val lobby: Lobby? = null
)

data class Lobby(
        val id: Long,
        val name: String,
        val players: List<User>
)
