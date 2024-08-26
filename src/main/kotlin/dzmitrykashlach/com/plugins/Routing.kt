package dzmitrykashlach.com.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

const val INVALID_PARAMETER_NAME = "Invalid parameter name"
const val COUNTER = "counter"
val mutex = Mutex()

fun Application.configureRouting() {
    routing {
        val counterService = CounterService()
        post("/Create") {
            val payload = call.receiveText()
            val counter = deserialize(payload)
            runCatching { counterService.create(counter) }.onFailure {
                call.respondText(
                    "${it.message}",
                    ContentType.Text.Plain,
                    HttpStatusCode.BadRequest
                )
            }
            call.respondText(
                Json.encodeToString(counter),
                ContentType.Text.Plain,
                HttpStatusCode.Created
            )
        }

        get("/Get") {
            kotlin.runCatching {
                val name = call.parameters[COUNTER] ?: throw IllegalArgumentException(INVALID_PARAMETER_NAME)
                val counter = counterService.getByName(name)
                if (counter != null) {
                    call.respondText(
                        Json.encodeToString(counter.value),
                        ContentType.Application.Json,
                        HttpStatusCode.OK
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }.onFailure {
                call.respondText(
                    "${it.message}",
                    ContentType.Text.Plain,
                    HttpStatusCode.BadRequest
                )
            }
        }

        get("/GetAll") {
            val counters = counterService.getAll()
            call.respondText(Json.encodeToString(counters), ContentType.Application.Json, HttpStatusCode.OK)
        }
        delete("/Delete") {
            runCatching {
                val name = call.parameters[COUNTER] ?: throw IllegalArgumentException(INVALID_PARAMETER_NAME)
                if (counterService.delete(name) == 0) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
                .onFailure {
                    call.respondText(
                        "${it.message}",
                        ContentType.Text.Plain,
                        HttpStatusCode.BadRequest
                    )
                }
            call.respond(HttpStatusCode.OK)
        }

        patch("/Increment") {
            runCatching {
                val name = call.parameters[COUNTER] ?: throw IllegalArgumentException(INVALID_PARAMETER_NAME)
                mutex.withLock {
                    val counter = counterService.getByName(name)
                    if (counter != null) {
                        val incrementedCounter = counter.copy(counter.name, counter.value + 1)
                        counterService.update(incrementedCounter)
                        call.respondText(
                            Json.encodeToString(incrementedCounter.value),
                            ContentType.Text.Plain,
                            HttpStatusCode.OK
                        )
                    }
                }
            }.onFailure {
                call.respondText(
                    "${it.message}",
                    ContentType.Text.Plain,
                    HttpStatusCode.BadRequest
                )
            }
        }
    }
}
