package net.itr0.toolapps.server

import com.jetdrone.vertx.yoke.Middleware
import com.jetdrone.vertx.yoke.MimeType
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import com.jetdrone.vertx.yoke.middleware.YokeResponse
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.Handler
import org.vertx.java.core.file.FileProps
import org.vertx.java.core.file.FileSystem

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
        FileSystem fs = vertx.fileSystem()
        String path = request.normalizedPath()
        fs.exists(path, new SimpleEventHandler<Boolean>(next, { boolean exists ->
            if (exists) {
                fs.props(path, new SimpleEventHandler<FileProps>(next, { FileProps props ->
                    if (props.directory) {
                        fs.exists(path, new SimpleEventHandler<Boolean>(next, { boolean exist ->
                        }))
                    } else if (props.regularFile) {
                    } else {
                        next.handle(null)
                    }
                }))
            } else {
                next.handle(null)
            }
        }))
    }

    private void get(YokeRequest request, Handler<Object> next) {
        String path = "${root}${request.path()}"

        FileSystem fs = vertx.fileSystem()
        fs.props(path, new SimpleEventHandler<FileProps>(next, { FileProps props ->
            if (props.directory) {
                respondIfExists(request.response(), "${path}${index}", next)
            } else if (props.regularFile) {
                sendRequestedFile(request.response(), props, path)
            } else {
                next.handle(null)
            }
        }))
    }

    private void sendRequestedFile(YokeResponse response, FileProps prop, String path) {
        writeHeaders(response, prop, path)

        if (isFresh(path)) {
            response.setStatusCode(304)
            response.end()
        } else {
            response.sendFile(path)
        }
    }

    private void writeHeaders(YokeResponse response, FileProps prop, String path) {
        response.headers().add([
            date           : httpDateFormat.format(new Date()),
            'cache-control': "private, max-age=${maxAge}" as String,
            etag: "\"${prop.size()}-${prop.lastModifiedTime().getTime()}\"" as String,
            'last-modified': httpDateFormat.format(prop.lastModifiedTime()),
        ])

        // write content type
        String contentType = MimeType.getMime(path);
        String charset = MimeType.getCharset(contentType);
        response.setContentType(contentType, charset);
        response.putHeader("Content-Length", String.valueOf(prop.size()));
    }

    private boolean isFresh(String path) {
        false
    }

    private static class SimpleEventHandler<T> implements Handler<AsyncResult<T>> {

        Handler<Object> next
        Closure on

        SimpleEventHandler(Handler<Object> next, Closure on) {
            this.next = next
            this.on = on
        }

        @Override
        void handle(AsyncResult<T> event) {
            if (event.failed()) {
                next.handle(event.cause())
            }

            T result = event.result()
            if (!result) {
                next.handle(null)
            } else {
                on(result)
            }
        }
    }
}
