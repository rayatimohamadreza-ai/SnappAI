package com.edison.tamirkar

import android.content.Context
import android.os.Handler
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.util.concurrent.Executors

object OfflineEngine {
    interface Callback {
        fun onSuccess(text: String)
        fun onError(error: String)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null
    @Volatile private var ready = false
    private var loading = false

    @JvmStatic fun isReady(): Boolean = ready

    @JvmStatic fun load(context: Context, modelPath: String, callback: Callback) {
        executor.execute {
            if (ready && engine?.engineConfig?.modelPath == modelPath) {
                Handler(context.mainLooper).post { callback.onSuccess("مدل آفلاین آماده است.") }
                return@execute
            }
            if (loading) {
                Handler(context.mainLooper).post { callback.onError("بارگذاری مدل در حال انجام است؛ کمی صبر کن.") }
                return@execute
            }
            loading = true
            ready = false
            try {
                conversation?.close()
                conversation = null
                engine?.close()
                engine = null
                val file = File(modelPath)
                require(file.isFile && file.length() > 100L * 1024L * 1024L) {
                    "فایل مدل ناقص یا پیدا نشد. دوباره دانلودش کن."
                }
                val candidate = Engine(
                    EngineConfig(
                        modelPath = file.absolutePath,
                        cacheDir = context.cacheDir.absolutePath
                    )
                )
                candidate.initialize()
                engine = candidate
                conversation = candidate.createConversation()
                ready = true
                Handler(context.mainLooper).post { callback.onSuccess("مدل آفلاین آماده‌ی گفت‌وگو است.") }
            } catch (t: Throwable) {
                ready = false
                try { conversation?.close() } catch (_: Throwable) {}
                conversation = null
                try { engine?.close() } catch (_: Throwable) {}
                engine = null
                Handler(context.mainLooper).post {
                    callback.onError("اجرای مدل ناموفق بود: ${t.message ?: "خطای ناشناخته"}")
                }
            } finally {
                loading = false
            }
        }
    }

    @JvmStatic fun send(context: Context, prompt: String, callback: Callback) {
        executor.execute {
            if (!ready || conversation == null) {
                Handler(context.mainLooper).post { callback.onError("ابتدا مدل آفلاین را بارگذاری کن.") }
                return@execute
            }
            try {
                val answer = conversation!!.sendMessage(prompt)
                Handler(context.mainLooper).post { callback.onSuccess(answer) }
            } catch (t: Throwable) {
                Handler(context.mainLooper).post {
                    callback.onError("پاسخ آفلاین ناموفق بود: ${t.message ?: "خطای ناشناخته"}")
                }
            }
        }
    }

    @JvmStatic fun close() {
        executor.execute {
            ready = false
            try { conversation?.close() } catch (_: Throwable) {}
            conversation = null
            try { engine?.close() } catch (_: Throwable) {}
            engine = null
        }
    }
}
