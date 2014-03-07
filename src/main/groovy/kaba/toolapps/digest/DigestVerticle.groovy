package kaba.toolapps.digest
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message

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
class DigestVerticle extends Verticle implements Handler<Message> {

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

    void handle(Message event) {
        String body = event.body() as String
        if (body != null) {
            try {
                byte[] digest1 = getDigest("sha256").digest(body.getBytes())
                event.reply(digest1.encodeBase64().toString())
            } catch (NoSuchAlgorithmException e) {
                event.reply("No Such Algorithm sha256. Cause: ${e.message}")
            }
        }
        event.reply("")
    }

    /**
     * アルゴリズム名を指定して、 {@link MessageDigest} を取得する。
     *
     * @param algorithm {@link MessageDigest} のアルゴリズム名
     * @return {@code algorithm} に対応する {@link MessageDigest}
     */
    private static MessageDigest getDigest(String algorithm) {
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
