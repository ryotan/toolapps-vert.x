package net.itr0.toolapps.digest

import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message
import org.vertx.java.core.json.JsonObject

import java.security.NoSuchAlgorithmException

/**
 * SHA-256などのハッシュ化関数を使用して、ハッシュ値を計算するVerticle。
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class DigestVerticle extends Verticle implements Handler<Message<JsonObject>> {

    @Override
    def start() {
        super.start()
        vertx.eventBus.javaEventBus().registerHandler("message-digest", this)
    }

    void handle(Message<JsonObject> event) {
        JsonObject body = event.body()
        String target = body.getString("target")
        if (target) {
            try {
                byte[] digest = new Sha256Converter().convert(target.getBytes())
                JsonObject res = new JsonObject()
                res.putString("status", "ok")
                res.putString("raw", target)
                res.putString("digest", digest.encodeBase64().toString())
                event.reply(res)
            } catch (NoSuchAlgorithmException e) {
                JsonObject res = new JsonObject()
                res.putString("status", "ng")
                res.putString("raw", target)
                res.putString("message", "No Such Algorithm sha256. Cause: ${e.message}")
                event.reply(res)
            }
        }
        JsonObject res = new JsonObject()
        res.putString("status", "ok")
        res.putString("raw", target)
        res.putString("digest", "")
        event.reply(res)
    }
}
