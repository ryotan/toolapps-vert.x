package net.itr0.toolapps.server

import com.jetdrone.vertx.yoke.middleware.YokeRequest
import com.jetdrone.vertx.yoke.middleware.YokeResponse
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.vertx.java.core.Handler
import spock.lang.Shared
import spock.lang.Specification

/**
 * Spec for {@link Static}.
 *
 * @author ryotan
 * @since 1.0
 */
class StaticSpec extends Specification {

    @ClassRule
    @Shared
    TemporaryFolder root = new TemporaryFolder()

    Static sut = new Static(root.root.path)

    def "returns index file in `root` directory if '/' is requested."() {
        given:
        def request = Stub(YokeRequest)
        def response = Mock(YokeResponse)
        request.path() >> "/"
        request.response() >> response


        when:
        sut.handle(request, new NopHandler())

        then:
        1 * response.sendFile(root.root.path + '/index.html')
    }

    private def setupStaticFiles() {
        root.newFolder("dir1")
        root.newFolder("dir1/dir2")
        root.newFile("dir1/dir2/index.html")
    }

    def setupSpec() {
        root.create()
    }

    def cleanupSpec() {
        root.delete()
    }

    private static class NopHandler implements Handler<Object> {

        @Override
        void handle(Object event) {
        }
    }
}
