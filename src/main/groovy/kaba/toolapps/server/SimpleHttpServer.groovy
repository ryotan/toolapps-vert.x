package kaba.toolapps.server
import org.vertx.groovy.core.http.HttpServer
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.platform.Verticle
/**
 * シンプルなHTTPサーバ
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class SimpleHttpServer extends Verticle {

    @Override
    def start() {
        HttpServer server = vertx.createHttpServer()
        server.requestHandler { HttpServerRequest req ->
            req.response.end("Hello World!!!")
        }.listen(container.config.get("http.server.port") as int)
    }

    @Override
    def stop() {
    }
}
