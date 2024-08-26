package dzmitrykashlach.com.plugins

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection


val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/postgres",
    driver = "org.postgresql.Driver",
    user = "postgres",
    password = "postgres"
)

fun dbSchema() {
    transaction(database) {
        SchemaUtils.create(Counters)
    }
}

@Serializable
data class Counter(val name: String, val value: Int)

fun deserialize(payload: String): Counter {
    val json = Json.parseToJsonElement(payload)
    val name = json.jsonObject.keys.first()
    val value = json.jsonObject.getValue(name).jsonPrimitive.int
    return Counter(name, value)
}

object Counters : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50).uniqueIndex()
    val value = integer("value")

    override val primaryKey = PrimaryKey(id)
}

class CounterService {

    suspend fun <T> dbQuery(transactionIsolation: Int, block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, transactionIsolation = transactionIsolation) { block() }

    suspend fun create(counter: Counter): Int = dbQuery(Connection.TRANSACTION_SERIALIZABLE) {
        Counters.insert {
            it[name] = counter.name
            it[value] = counter.value
        }[Counters.id]
    }

    suspend fun getByName(name: String): Counter? {
        return dbQuery(Connection.TRANSACTION_REPEATABLE_READ) {
            Counters.selectAll()
                .where { Counters.name eq name }
                .map { Counter(it[Counters.name], it[Counters.value]) }
                .singleOrNull()
        }
    }

    suspend fun getAll(): List<Counter> {
        return dbQuery(Connection.TRANSACTION_REPEATABLE_READ) {
            Counters.selectAll()
                .map { Counter(it[Counters.name], it[Counters.value]) }
        }
    }

    suspend fun update(counter: Counter) {
        dbQuery(Connection.TRANSACTION_SERIALIZABLE) {
            Counters.update({ Counters.name eq counter.name }) {
                it[value] = counter.value
            }
        }
    }

    suspend fun delete(name: String): Int {
        var result = 0
        dbQuery(Connection.TRANSACTION_SERIALIZABLE) {
            result = Counters.deleteWhere { Counters.name.eq(name) }
        }
        return result
    }
}

