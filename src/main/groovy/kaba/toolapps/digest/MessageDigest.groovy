package kaba.toolapps.digest

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

/**
 *
 * @author Ryo TANAKA
 * @since 1.0
 */
class MessageDigest extends Verticle {

    @Override
    def start() {
        super.start()
        vertx.eventBus.registerHandler("message-digest", { Message msg ->
            this.handle(msg)
        })
    }

    void handle(Message event) {
        event.reply("Message is Sent!!! " + (vertx.worker ? 'Received in WORKER THREAD!!!' : 'Received in event loop....'))
    }
}
