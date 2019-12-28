package eu.yeger.komi.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KomiBackendApplication

fun main(args: Array<String>) {
	runApplication<KomiBackendApplication>(*args)
}
