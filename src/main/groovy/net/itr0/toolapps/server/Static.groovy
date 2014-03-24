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
import java.text.ParseException
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
        // Static files may be requested with GET or HEAD.
        if (!"GET".equals(request.method()) && !"HEAD".equals(request.method())) {
            next.handle(null);
        } else {
            FileSystem fs = vertx.fileSystem()

            String path = request.normalizedPath()
            if (path == null) {
                // If path is not specified, static file can't be resolved.
                // So let the next middleware to handle it
                next.handle(null)
            }

            // static file path is relative to `root` excluding mount path.
            String file = "${root}${path.substring(mount.length())}"
            fs.exists(file, new SimpleEventHandler<Boolean>(next, { boolean exists ->
                if (exists) {
                    fs.props(file, new SimpleEventHandler<FileProps>(next, { FileProps props ->
                        if (props.directory) {
                            handleDirectory(request, file, next)
                        } else if (props.regularFile) {
                            respond(request, file, props)
                        } else {
                            // If file is not a regular file or a directory,
                            // this middleware can't respond a static file.
                            next.handle(null)
                        }
                    }))
                } else {
                    next.handle(null)
                }
            }))
        }
    }

    private void handleDirectory(YokeRequest request, String path, Handler<Object> next) {
        FileSystem fs = vertx.fileSystem()

        String file = "${path}/${index}"
        fs.exists(file, new SimpleEventHandler<Boolean>(next, { boolean exists ->
            if (exists) {
                fs.props(file, new SimpleEventHandler<FileProps>(next, { FileProps props ->
                    if (props.regularFile) {
                        respond(request, file, props)
                    } else {
                        next.handle(null)
                    }
                }))
            }
        }))
    }

    private void respond(YokeRequest request, String path, FileProps prop) {
        YokeResponse response = request.response()
        writeHeaders(response, prop, path)

        if (isFresh(request)) {
            response.setStatusCode(304)
            response.end()
        } else if ("HEAD".equals(request.method())) {
            response.end();
        } else {
            response.sendFile(path);
        }
    }

    private void writeHeaders(YokeResponse response, FileProps prop, String path) {
        // write content type
        String contentType = MimeType.getMime(path);
        String charset = MimeType.getCharset(contentType);
        response.setContentType(contentType, charset);
        response.putHeader("Content-Length", String.valueOf(prop.size()));

        response.headers().add([
            date           : httpDateFormat.format(new Date()),
            'cache-control': "private, max-age=${maxAge}" as String,
            etag           : "\"${prop.size()}-${prop.lastModifiedTime().getTime()}\"" as String,
            'last-modified': httpDateFormat.format(prop.lastModifiedTime()),
        ])
    }

    private boolean isFresh(YokeRequest request) {
        // defaults
        boolean etagMatches = true;
        boolean notModified = true;

        // fields
        String modifiedSince = request.getHeader("if-modified-since");
        String noneMatch = request.getHeader("if-none-match");
        String[] noneMatchTokens = null;
        String lastModified = request.response().getHeader("last-modified");
        String etag = request.response().getHeader("etag");

        // unconditional request
        if (modifiedSince == null && noneMatch == null) {
            return false;
        }

        // parse if-none-match
        if (noneMatch != null) {
            noneMatchTokens = noneMatch.split(" *, *");
        }

        // if-none-match
        if (noneMatchTokens != null) {
            etagMatches = false;
            for (String s : noneMatchTokens) {
                if (etag.equals(s) || "*".equals(noneMatchTokens[0])) {
                    etagMatches = true;
                    break;
                }
            }
        }

        // if-modified-since
        if (modifiedSince != null) {
            try {
                Date modifiedSinceDate = httpDateFormat.parse(modifiedSince);
                Date lastModifiedDate = httpDateFormat.parse(lastModified);
                notModified = lastModifiedDate.getTime() <= modifiedSinceDate.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
                notModified = false;
            }
        }

        return etagMatches && notModified
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

