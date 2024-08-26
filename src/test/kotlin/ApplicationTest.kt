import dzmitrykashlach.com.plugins.Counters
import dzmitrykashlach.com.plugins.database
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class ApplicationTest {

    @BeforeTest
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(Counters)
        }
    }


    @Test
    fun testCreateAndDelete() = testApplication {
        var response = client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("{\"name\":\"counter-s89\",\"value\":356109}", response.bodyAsText())

        response = client.delete("/Delete") {
            parameter("counter", "counter-s89")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testGet() = testApplication {
        client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        val response = client.get("/Get") {
            parameter("counter", "counter-s89")

        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("356109", response.bodyAsText())

    }

    @Test
    fun testIncrement() = testApplication {
        client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        val response = client.patch("/Increment") {
            parameter("counter", "counter-s89")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("356110", response.bodyAsText())

    }

    @Test
    fun testGetAll() = testApplication {
        for (i in 0..5) {
            client.post("/Create") {
                setBody("{\"counter-${i}\":${i}}")
            }

        }
        val response = client.get("/GetAll")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(body.size, 6)

    }

    @Test
    fun testCreateNegative() = testApplication {
        var response = client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("{\"name\":\"counter-s89\",\"value\":356109}", response.bodyAsText())

        response = client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue {
            response.bodyAsText().contains("duplicate key value violates unique constraint \"counters_name_unique\"")
        }
    }

    @Test
    fun testGetWrongParamter() = testApplication {
        client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        val response = client.get("/Get") {
            parameter("counters", "counter-s89")

        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid parameter name", response.bodyAsText())

    }

    @Test
    fun testIncrementWrongParameter() = testApplication {
        client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        val response = client.patch("/Increment") {
            parameter("counters", "counter-s89")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid parameter name", response.bodyAsText())

    }

    @Test
    fun testDeleteNotFound() = testApplication {
        client.post("/Create") {
            setBody("{\"counter-s89\":356109}")
        }
        val response = client.delete("/Delete") {
            parameter("counter", "counter-s90")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}