package kaba.toolapps.server

import com.jetdrone.vertx.yoke.GYoke
import com.jetdrone.vertx.yoke.middleware.BodyParser
import com.jetdrone.vertx.yoke.middleware.ErrorHandler
import com.jetdrone.vertx.yoke.middleware.Favicon
import com.jetdrone.vertx.yoke.middleware.GRouter
import com.jetdrone.vertx.yoke.middleware.Logger
import com.jetdrone.vertx.yoke.middleware.Static
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import kaba.toolapps.digest.MessageDigest
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Handler

/**
 * シンプルなHTTPサーバ
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class SimpleHttpServer extends Verticle {

    @Override
    def start() {
        GYoke yoke = new GYoke(this)
        yoke.use(new Logger())
        yoke.use(new ErrorHandler(false))
        yoke.use(new Favicon())
        yoke.use('/', new Static('./client'))
        yoke.use('/', new BodyParser())
        yoke.use(
            new GRouter().
                get('/encrypt') { YokeRequest request, Handler<?> next ->
                    request.response().end('Hello, Encrypted world!!!')
                }.
                get('/digest') { YokeRequest request, Handler<?> next ->
                    vertx.eventBus.send("message-digest", request.body(), { Message msg ->
                        request.response().end("${msg.body()}\nHello, Digesting world!!!")
                    })
                }
        )

        container.deployWorkerVerticle("groovy:${MessageDigest.canonicalName}")

        int port = container.config['http.server.port'] as int
        yoke.listen(port, "0.0.0.0")
        container.logger.info("Vert.x http server is listening on 0.0.0.0:${port}")
    }

    @Override
    def stop() {
    }
}
