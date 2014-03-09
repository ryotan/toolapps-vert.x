package kaba.toolapps.digest

import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message
import org.vertx.java.core.json.JsonObject

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * SHA-256などのハッシュ化関数を使用して、ハッシュ値を計算するVerticle。
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class DigestVerticle extends Verticle implements Handler<Message<JsonObject>> {

    /**
     * {@link MessageDigest} のキャッシュ。
     *
     * マルチスレッドで動作しても問題ないように、 {@link ThreadLocal} に保持するようにしてみたんだけど。。。
     */
    private static final ConcurrentMap<String, ThreadLocal<MessageDigest>> DIGESTS = new ConcurrentHashMap<>()

    @Override
    def start() {
        super.start()
        vertx.eventBus.javaEventBus().registerHandler("message-digest", this)
    }

    void handle(Message<JsonObject> event) {
        JsonObject body = event.body()
        String target = body.getString("target")
        container.logger.info("target: ${target}")
        if (target) {
            try {
                byte[] digest = getDigest("sha-256").digest(target.getBytes())
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
        res.putString("status", "ng")
        res.putString("message", "Digest source string must be specified.")
        event.reply(res)
    }

    /**
     * アルゴリズム名を指定して、 {@link MessageDigest} を取得する。
     *
     * @param algorithm {@link MessageDigest} のアルゴリズム名
     * @return {@code algorithm} に対応する {@link MessageDigest}
     */
    private static MessageDigest getDigest(String algorithm) {
        ThreadLocal<MessageDigest> digest = DIGESTS.get(algorithm)
        if (digest) {
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
