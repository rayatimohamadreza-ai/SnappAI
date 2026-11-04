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
    private var activeModelPath: String? = null

    @JvmStatic fun isReady(): Boolean = ready

    @JvmStatic fun load(context: Context, modelPath: String, callback: Callback) {
        executor.execute {
            if (ready && activeModelPath == modelPath) {
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
                val oldConversation = conversation
                conversation = null
                try { oldConversation?.close() } catch (_: Throwable) {}
                val oldEngine = engine
                engine = null
                try { oldEngine?.close() } catch (_: Throwable) {}
                activeModelPath = null
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
                activeModelPath = modelPath
                ready = true
                Handler(context.mainLooper).post { callback.onSuccess("مدل آفلاین آماده‌ی گفت‌وگو است.") }
            } catch (t: Throwable) {
                ready = false
                val failedConversation = conversation
                conversation = null
                try { failedConversation?.close() } catch (_: Throwable) {}
                val failedEngine = engine
                engine = null
                try { failedEngine?.close() } catch (_: Throwable) {}
                activeModelPath = null
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
                val answer = conversation!!.sendMessage(prompt).toString()
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
            val oldConversation = conversation
            conversation = null
            try { oldConversation?.close() } catch (_: Throwable) {}
            val oldEngine = engine
            engine = null
            try { oldEngine?.close() } catch (_: Throwable) {}
            activeModelPath = null
        }
    }
}
