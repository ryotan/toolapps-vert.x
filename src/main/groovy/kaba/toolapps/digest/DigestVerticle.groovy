package kaba.toolapps.digest

import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * SHA-256などのハッシュ化関数を使用して、ハッシュ値を計算するVerticle。
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class DigestVerticle extends Verticle implements Handler<Message> {

    private static final ConcurrentMap<String, ThreadLocal<MessageDigest>> DIGESTS = new ConcurrentHashMap<>()

    @Override
    def start() {
        super.start()
        vertx.eventBus.javaEventBus().registerHandler("message-digest", this)
    }

    void handle(Message event) {
        String body = event.body() as String
        if (body != null) {
            byte[] digest1 = getDigest("sha-256").digest(body.getBytes())
            event.reply(digest1.encodeBase64().toString())
        }
        event.reply("")
    }

    static MessageDigest getDigest(String algorithm) {
        ThreadLocal<MessageDigest> digest = DIGESTS.get(algorithm)
        if (digest != null) {
            return digest.get()
        }
        DIGESTS.putIfAbsent(algorithm, new ThreadLocal<MessageDigest>() {

            @Override
            protected MessageDigest initialValue() {
                return MessageDigest.getInstance(algorithm)
            }
        })
        DIGESTS.get(algorithm).get()
    }
}
