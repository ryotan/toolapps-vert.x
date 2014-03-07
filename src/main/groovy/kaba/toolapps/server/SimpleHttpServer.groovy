package kaba.toolapps.server
import com.jetdrone.vertx.yoke.GYoke
import com.jetdrone.vertx.yoke.middleware.BodyParser
import com.jetdrone.vertx.yoke.middleware.ErrorHandler
import com.jetdrone.vertx.yoke.middleware.Favicon
import com.jetdrone.vertx.yoke.middleware.GRouter
import com.jetdrone.vertx.yoke.middleware.Logger
import com.jetdrone.vertx.yoke.middleware.Static
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import kaba.toolapps.digest.DigestVerticle
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.Handler
/**
 * シンプルなHTTPサーバ
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class SimpleHttpServer extends Verticle {

    private static final List<String> workers = Collections.unmodifiableList([
        DigestVerticle.canonicalName,
    ])

    @Override
    def start() {
        GYoke yoke = new GYoke(this)
        yoke.use(new Logger())
        yoke.use(new ErrorHandler(false))
        yoke.use(new Favicon())
        yoke.use('/', { YokeRequest req, Handler<Object> next ->
            if (req.path() == '/') {
                req.response().sendFile('./client/index.html')
            } else {
                next.handle(null)
            }
        })
        yoke.use('/', new Static('./client'))
        yoke.use('/', new BodyParser())
        yoke.use(
            new GRouter().
                get('/encrypt') { YokeRequest request, Handler<?> next ->
                    request.response().end('Hello, Encrypted world!!!')
                }.
                get('/digest') { YokeRequest request, Handler<?> next ->
                    vertx.eventBus.send("message-digest", [target: request.getParameter("target")], { Message msg ->
                        request.response().end("${msg.body()}\nHello, Digesting world!!!")
                    })
                }
        )

        workers.each {
            // デプロイされた Worker Verticle は、この Verticle がアンデプロイされた時に、
            // 自動的にアンデプロイされる。
            deployWorker(it)
        }

        int port = container.config['http.server.port'] as int
        yoke.listen(port, "0.0.0.0")
        container.logger.info("Vert.x http server is listening on 0.0.0.0:${port}")
    }

    @Override
    def stop() {
    }

    /**
     * {@code worker} をGroovyのWorker Verticleとしてコンテナにデプロイする。
     *
     * @param worker デプロイしたいWorker Verticle
     */
    private void deployWorker(String worker) {
        container.deployWorkerVerticle("groovy:${worker}", { AsyncResult<String> result ->
            if (result.failed()) {
                throw new RuntimeException("Failed to deploy ${worker}.", result.cause())
            }
            container.logger.info("Successfully deployed ${worker}.")
        })
    }
}
