package net.itr0.toolapps.server

import com.jetdrone.vertx.yoke.Middleware
import com.jetdrone.vertx.yoke.middleware.YokeRequest
import org.vertx.java.core.Handler

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Static file server with the given {@code root} path.
 * When directory is requested without a file name being specified,
 * <p/>
 * To enable HTTP caching,
 * this class attach cache headers such as {@code cache-control} to response.
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

    private final DateFormat httpDateFormat

    Static(String root) {
        this(root, 'index.html', 315360000)
    }

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
        if (request.path() == '/') {
            request.response().headers().add([
                "date"         : httpDateFormat.format(new Date()),
                "cache-control": "public, max-age=${maxAge}",
            ])
            request.response().sendFile("${root}index.html")
        } else {
            next.handle(null)
        }
    }
}
