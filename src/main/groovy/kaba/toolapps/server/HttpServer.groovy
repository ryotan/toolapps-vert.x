package kaba.toolapps.server

import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.platform.Verticle

/**
 * シンプルなHTTPサーバ
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class HttpServer extends Verticle {

    @Override
    def start() {
        vertx.createHttpServer().requestHandler { HttpServerRequest req ->
            req.response.end("Hello World!!!")
        }.listen(4321)
    }

    @Override
    def stop() {
    }
}
