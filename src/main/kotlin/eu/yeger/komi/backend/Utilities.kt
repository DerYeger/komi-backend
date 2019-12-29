package eu.yeger.komi.backend

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

fun Any.toJson(): String = KomiObjectMapper.writeValueAsString(this)

fun WebSocketSession.send(message: Message) = sendMessage(TextMessage(KomiObjectMapper.writeValueAsString(message)))

fun HashMap<WebSocketSession, User>.broadcast(message: Message) = this.forEach { it.key.send(message) }

fun WebSocketSession.error(errorMessage: String) = send(Message("error", errorMessage))
