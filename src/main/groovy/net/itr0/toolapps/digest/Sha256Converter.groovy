package net.itr0.toolapps.digest

import net.itr0.toolapps.domain.convert.ByteArrayConverter

import java.security.MessageDigest

/**
 * SHA-256でbyte列のハッシュ値を計算する {@link ByteArrayConverter}
 *
 * @author ryotan
 * @since 1.0
 */
class Sha256Converter extends MessageDigestConverterSupport {

    /**
     * {@link MessageDigest} のキャッシュ。
     * <p/>
     * マルチスレッドで動作しても問題ないように、 {@link ThreadLocal} に保持する。<br />
     * スレッドがスレッドプールで管理されている場合に同一スレッドから再度利用される場合を考慮して、
     * 使用後は必ず {@link MessageDigest#reset()} を呼び出すようにしないといけない。
     */
    private static final ThreadLocal<MessageDigest> DIGEST_THREAD_LOCAL = new ThreadLocal<MessageDigest>() {

        @Override
        protected MessageDigest initialValue() {
            return MessageDigest.getInstance("SHA-256")
        }
    }

    @Override
    protected MessageDigest getDigest() {
        return DIGEST_THREAD_LOCAL.get()
    }
}
