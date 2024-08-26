package dzmitrykashlach.com

import dzmitrykashlach.com.plugins.configureRouting
import dzmitrykashlach.com.plugins.dbSchema
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore

val keyStoreFile = File("build/keystore.jks")
fun main(args: Array<String>) {
    embeddedServer(Netty, environment()).start(wait = true)

}

fun Application.module() {
    dbSchema()
    configureRouting()
}

fun environment() = applicationEngineEnvironment {
    log = LoggerFactory.getLogger("ktor.application")
    connector {
        port = 8080
    }
    sslConnector(
        keyStore = keyStore(),
        keyAlias = "counter-certificate",
        keyStorePassword = { "123456".toCharArray() },
        privateKeyPassword = { "cert-pass".toCharArray() }) {
        port = 8443
        keyStorePath = keyStoreFile
    }
    module(Application::module)
}

fun keyStore(): KeyStore {
    val keyStore = buildKeyStore {
        certificate("counter-certificate") {
            password = "cert-pass"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, "123456")
    return keyStore
}
