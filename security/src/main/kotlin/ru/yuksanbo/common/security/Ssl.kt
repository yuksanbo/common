package ru.yuksanbo.common.security

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import ru.yuksanbo.common.security.exceptions.SslInitializationException
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


object Ssl {

    private val DEFAULT_SSL_PROTOCOL = "TLS"

    fun buildNettyClientSslContext(path: String, keyStoreType: String, password: String): SslContext =
            buildNettySslContext(
                    path,
                    keyStoreType,
                    password,
                    { k, p ->
                        SslContextBuilder
                                .forClient()
                                .trustManager(buildTrustManagerFactory(k, TrustManagerFactory.getDefaultAlgorithm()))
                    }
            )

    fun buildNettyServerSslContext(path: String, keyStoreType: String, password: String): SslContext =
            buildNettySslContext(
                    path,
                    keyStoreType,
                    password,
                    { k, p -> SslContextBuilder.forServer(buildKeyManagerFactory(k, p, KeyManagerFactory.getDefaultAlgorithm())) }
            )

    fun buildNettySslContext(
            path: String,
            keyStoreType: String,
            password: String,
            b: (KeyStore, CharArray) -> SslContextBuilder
    ): SslContext {
        try {
            FileInputStream(path).use { keyStoreInputStream ->
                val passphrase = password.toCharArray()
                val keyStore = buildKeyStore(keyStoreType, keyStoreInputStream, passphrase)
                return b.invoke(keyStore, passphrase).build()
            }
        }
        catch (e: Exception) {
            throw SslInitializationException(e)
        }
    }

    fun buildJavaSslContext(path: String, keyStoreType: String, password: String): SSLContext {
        try {
            FileInputStream(path).use { keyStoreInputStream ->
                val passphrase = password.toCharArray()

                val keyStore = buildKeyStore(keyStoreType, keyStoreInputStream, passphrase)
                val kmf = buildKeyManagerFactory(keyStore, passphrase, KeyManagerFactory.getDefaultAlgorithm())
                val tms = buildTrustManagerFactory(keyStore, TrustManagerFactory.getDefaultAlgorithm())

                val result = SSLContext.getInstance(DEFAULT_SSL_PROTOCOL)
                val secureRandom = SecureRandom()
                result.init(kmf.keyManagers, tms.trustManagers, secureRandom)
                return result
            }
        }
        catch (e: Exception) {
            throw SslInitializationException(e)
        }
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, CertificateException::class, IOException::class)
    private fun buildKeyStore(keyStoreType: String, keyStoreInputStream: InputStream, keyPasswordChars: CharArray): KeyStore {
        val ks = KeyStore.getInstance(keyStoreType)
        ks.load(keyStoreInputStream, keyPasswordChars)
        return ks
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, IOException::class, CertificateException::class, UnrecoverableKeyException::class)
    fun buildKeyManagerFactory(keyStore: KeyStore, keyPasswordChars: CharArray, algorithm: String): KeyManagerFactory {
        val kmf = KeyManagerFactory.getInstance(algorithm)
        kmf.init(keyStore, keyPasswordChars)
        return kmf
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, IOException::class, CertificateException::class, UnrecoverableKeyException::class)
    private fun buildTrustManagerFactory(keyStore: KeyStore, algorithm: String): TrustManagerFactory {
        val tmf = TrustManagerFactory.getInstance(algorithm)
        tmf.init(keyStore)
        return tmf
    }

}