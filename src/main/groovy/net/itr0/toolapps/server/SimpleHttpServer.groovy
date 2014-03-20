package net.itr0.toolapps.server

import com.jetdrone.vertx.yoke.GYoke
import com.jetdrone.vertx.yoke.middleware.BodyParser
import com.jetdrone.vertx.yoke.middleware.Compress
import com.jetdrone.vertx.yoke.middleware.ErrorHandler
import com.jetdrone.vertx.yoke.middleware.Favicon
import com.jetdrone.vertx.yoke.middleware.Limit
import com.jetdrone.vertx.yoke.middleware.Logger
import com.jetdrone.vertx.yoke.middleware.ResponseTime
import com.jetdrone.vertx.yoke.middleware.Router
import com.jetdrone.vertx.yoke.middleware.Static
import com.jetdrone.vertx.yoke.middleware.Timeout
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import net.itr0.toolapps.digest.DigestHandler
import net.itr0.toolapps.digest.DigestVerticle
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
    private static final String WEB_ROOT = './client'

    @Override
    def start() {
        GYoke yoke = new GYoke(this)
        yoke.use(new Limit(5000))
        yoke.use(new Timeout(3000))
        yoke.use(new Logger())
        yoke.use(new ResponseTime())
        yoke.use(new Compress())
        yoke.use(new ErrorHandler(false))
        yoke.use(new Favicon())
        yoke.use('/', { YokeRequest req, Handler<?> next ->
            if (req.path() == '/') {
                req.response().sendFile("${WEB_ROOT}/index.html")
            } else {
                next.handle(null)
            }
        })
        yoke.use(new Static(WEB_ROOT))
        yoke.use(new BodyParser())

        Router router = new Router()
        yoke.use(router)
        router.get('/digest', new DigestHandler())

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
