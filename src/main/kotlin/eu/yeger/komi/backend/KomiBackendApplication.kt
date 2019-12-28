package eu.yeger.komi.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@SpringBootApplication
class KomiBackendApplication

fun main(args: Array<String>) {
    runApplication<KomiBackendApplication>(*args)
}

@Controller
class Controller {
    @GetMapping("/hello")
    @ResponseBody
    fun hello() = "Hello there!"
}
