package eu.yeger.komi.backend.handler

import eu.yeger.komi.backend.*
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.atomic.AtomicLong

class LobbyHandler(private val sessions: HashMap<WebSocketSession, User>) {
    private val lobbyMap = HashMap<Long, Lobby>()
    private val lids = AtomicLong(0)

    fun sendLobbiesTo(session: WebSocketSession) {
        session.send(Message(type = "lobbies", data = lobbyMap.values.toJson()))
    }

    fun broadcastLobbies() {
        sessions.broadcast(Message(type = "lobbies", data = lobbyMap.values.toJson()))
    }

    fun createLobby(session: WebSocketSession, name: String) {
        when {
            sessions[session]?.lobby !== null -> session.error("You are already in a lobby")
            else -> {
                val lobby = Lobby(lids.getAndIncrement(), name, listOf())
                lobbyMap[lobby.id] = lobby
                addUserToLobby(sessions[session]!!, lobby)
                broadcastLobbies()
            }
        }
    }

    fun joinLobby(session: WebSocketSession, id: Long) {
        val user = sessions[session]!!
        val lobby = lobbyMap[id]

        when {
            user.lobby !== null -> session.error("You are already in a lobby")
            lobby === null -> session.error("Lobby does not exist")
            lobby.players.size > 1 -> session.error("Lobby is full")
            else -> {
                addUserToLobby(user, lobby)
                session.send(Message(type = "joinedLobby", data = lobby.toJson()))
                broadcastLobbies()
            }
        }
    }

    fun leaveLobby(session: WebSocketSession, id: Long) {
        val user = sessions[session]!!
        val lobby = lobbyMap[id]

        when {
            user.lobby === null -> session.error("You are not in a lobby")
            lobby === null -> session.error("Lobby does not exist")
            else -> {
                removeUserFromLobby(user, lobby)
                session.send(Message(type = "leftLobby", data = lobby.toJson()))
                broadcastLobbies()
            }
        }
    }

    fun addUserToLobby(user: User, lobby: Lobby) {
        sessions[user.session] = user.copy(lobby = lobby)
        lobbyMap[lobby.id] = lobby.copy(players = lobby.players + user)
    }

    fun removeUserFromLobby(user: User, lobby: Lobby) {
        lobbyMap[lobby.id] = lobby.copy(players = lobby.players - user)
        sessions[user.session] = user.copy(lobby = null)
    }
}