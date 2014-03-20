package net.itr0.toolapps.digest

import com.jetdrone.vertx.yoke.Middleware
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import com.jetdrone.vertx.yoke.middleware.YokeResponse
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.core.eventbus.Message
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.json.JsonObject

/**
 *
 * @author ryotan
 * @since 1.0
 */
class DigestHandler extends Middleware {

    EventBus eventBus

    @Override
    void handle(YokeRequest request, Handler<Object> next) {
        YokeResponse response = request.response()
        eventBus.send("message-digest", createDigestRequest(request), { Message msg ->
            JsonObject res = msg.body() as JsonObject
            String status = res.getString("status")
            if ("ok" == status.toLowerCase()) {
                request.response().end(res)
            } else {
                response.setStatusCode(400)
                response.end(res)
            }
        })
    }

    @Override
    Middleware init(Vertx vertx, String mount) {
        super.init(vertx, mount)
        eventBus = new EventBus(super.vertx.eventBus())
        return this
    }

    private static JsonObject createDigestRequest(YokeRequest request) {
        def req = new JsonObject()
        def target = request.getParameter("target")
        req.putString("target", target)
    }
}
