package net.itr0.toolapps.server

import com.jetdrone.vertx.yoke.Middleware
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import com.jetdrone.vertx.yoke.middleware.YokeResponse
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.file.FileSystem
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.file.FileProps

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Static file server with the given {@code root} path.
 * When directory is requested without a file name being specified,
 * <p/>
 * To enable HTTP caching, this class attach cache headers such as {@code cache-control} to response.
 * <p/>
 * Cache headers attached to response are listed below.
 * <ul>
 *     <li>Cache-Control</li>
 *     <li>Last-Modified</li>
 *     <li>ETag</li>
 *     <li>Date</li>
 * </ul>
 *
 * @author ryotan
 * @since 1.0
 */
class Static extends Middleware {

    /**
     * Root directory where to look files from.
     */
    String root

    /**
     * File name to serve when directory is requested.
     */
    String index

    /**
     * Max age allowed for cache of resources.
     */
    int maxAge

    /**
     * Date formatter for {@code date} header in HTTP response.
     */
    private final DateFormat httpDateFormat

    FileSystem fs

    /**
     * Create a new Static File Server Middleware that returns "index.html" when directory is requested,
     * and files are cached for 10 years.
     *
     * @param root the root location of the static files in the file system (relative to the main Verticle).
     */
    Static(String root) {
        this(root, 'index.html', 315360000)
    }

    /**
     * Create a new Static File Server Middleware.
     *
     * When directory is requested,
     * this middleware sends {@code index} file in {@code root} directory.
     *
     * @param root the root location of the static files in the file system (relative to the main Verticle).
     * @param index filename to send
     * @param maxAge cache-control max-age directive
     */
    Static(String root, String index, int maxAge) {
        // if the root is not empty it should end with / for convenience
        if (!"".equals(root)) {
            if (!root.endsWith("/")) {
                root = root + "/";
            }
        }

        this.root = root
        this.index = index
        this.maxAge = maxAge

        httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        httpDateFormat.timeZone = TimeZone.getTimeZone("GMT")
    }

    @Override
    void handle(YokeRequest request, Handler<Object> next) {
        def path = "${root}${request.path()}"

        fs.exists(path, { AsyncResult<Boolean> existence ->
            if (existence.failed) {
                next.handle(existence.cause)
            }

            if (existence.result) {
                sendFile(request, path, next)
            } else {
                next.handle(null)
            }
        })
    }

    private FileSystem sendFile(YokeRequest request, String path, Handler<Object> next) {
        fs.props(path, { AsyncResult<FileProps> propEvent ->
            if (propEvent.failed) {
                next.handle(propEvent.cause)
            }

            def prop = propEvent.result
            def response = request.response()

            if (prop.isDirectory()) {
                sendIndexFile(response, prop, path)
            } else {
                sendRequestedFile(response, prop, path)
            }
        })
    }

    private void sendRequestedFile(YokeResponse response, FileProps prop, String path) {
        response.headers().add([
            date           : httpDateFormat.format(new Date()),
            'cache-control': "private, max-age=${maxAge}" as String,
            etag           : "\"\"",
            'last-modified': httpDateFormat.format(prop.lastModifiedTime()),
        ])
        response.sendFile("${path}")
    }

    private void sendIndexFile(YokeResponse response, FileProps prop, String path) {
        def index = "${path}${index}"

        fs.exists(index, { AsyncResult<Boolean> indexExistence ->
            if (indexExistence.failed) {
                next.handle(indexExistence.cause)
            }
            if (indexExistence.result) {
                response.headers().add([
                    date           : httpDateFormat.format(new Date()),
                    'cache-control': "private, max-age=${maxAge}" as String,
                    etag           : "\"\"",
                    'last-modified': httpDateFormat.format(prop.lastModifiedTime()),
                ])

                response.sendFile(index)
            } else {
                next.handle(null)
            }
        })
    }

    @Override
    Middleware init(Vertx vertx, String mount) {
        super.init(vertx, mount)
        fs = new FileSystem(vertx.fileSystem())
        return this
    }
}
