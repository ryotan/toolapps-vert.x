package net.itr0.toolapps.server

import com.jetdrone.vertx.yoke.middleware.YokeRequest
import com.jetdrone.vertx.yoke.middleware.YokeResponse
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.vertx.java.core.Handler
import org.vertx.java.core.MultiMap
import org.vertx.java.core.http.CaseInsensitiveMultiMap
import org.vertx.java.core.impl.DefaultVertx
import spock.lang.Shared
import spock.lang.Specification

/**
 * Spec for {@link Static}.
 *
 * @author ryotan
 * @since 1.0
 */
class StaticSpec extends Specification {

    /**
     * `root` directory for tests.
     */
    @ClassRule
    @Shared
    TemporaryFolder root = new TemporaryFolder()

    /**
     * {@link Static} under test.
     */
    Static sut = new Static(root.root.path).init(new DefaultVertx(), '/') as Static

    /**
     * Http Request Headers
     *
     * This field must be written before {@link #request}
     */
    MultiMap reqHeaders = new CaseInsensitiveMultiMap()

    /**
     * Http Response Headers
     *
     * This field must be written before {@link #response}
     */
    MultiMap resHeaders = new CaseInsensitiveMultiMap()

    /**
     * Http Response Mock
     *
     * This field must be written before {@link #request}
     */
    YokeResponse response = Mock(YokeResponse) {
        headers() >> resHeaders
    }

    /**
     * Http Request Mock
     */
    YokeRequest request = Mock(YokeRequest) {
        response() >> response
        headers() >> reqHeaders
    }

    def "returns requested file in `root` directory."() {
        given:
        request.path() >> "/some.html"

        when:
        sut.handle(request, next)

        then:
        1 * response.sendFile("${root.root.path}/some.html")
    }

    def "returns index file in `root` directory if '/' is requested."() {
        given:
        request.path() >> '/'

        when:
        sut.handle(request, next)

        then:
        1 * response.sendFile("${root.root.path}/index.html")
    }

    def setupSpec() {
        root.create()
    }

    def cleanupSpec() {
        root.delete()
    }

    /**
     * next handler mock
     */
    Handler<Object> next = Mock(Handler)
}
