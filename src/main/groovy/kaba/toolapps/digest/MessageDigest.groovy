package kaba.toolapps.digest

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import java.security.MessageDigest as MD

/**
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class MessageDigest extends Verticle {

    private final ThreadLocal<MD> sha256 = new ThreadLocal<MD>() {

        @Override
        protected MD initialValue() {
            MD.getInstance("SHA-256")
        }
    }

    @Override
    def start() {
        super.start()
        vertx.eventBus.registerHandler("message-digest", { Message msg ->
            this.handle(msg)
        })
    }

    void handle(Message event) {
        def body = event.body()
        if (body != null) {
            byte[] digest1 = sha256.get().digest(body.getBytes())
            event.reply(digest1.encodeBase64().toString())
        }
        event.reply("")
    }
}
