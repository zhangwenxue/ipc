package android.boot.ipc.client

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Envoy(private val serverPkg: String) {
    private val uri = Uri.parse("content://$serverPkg.ipc-server/share")
    suspend fun set(context: Context, key: String, value: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val result = context.contentResolver.update(uri, ContentValues(1).apply {
                    put("key", key)
                    put("value", value)
                }, null, null)
                if (result != 1) throw RuntimeException("Update failed $result:($key:$value)")
                Unit
            }.onFailure { it.printStackTrace() }

        }

    }

    suspend fun get(context: Context, key: String): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.query(uri, arrayOf("value"), "key=$key", arrayOf(key), null)
                    ?.use {
                        it.moveToFirst()
                        if (it.count <= 0) throw RuntimeException("No records found")
                        val index = it.getColumnIndex("value")
                        if (index == -1) throw RuntimeException("No value record found")
                        it.getString(index)!!
                    } ?: throw RuntimeException("No Courser fond for key:$key($uri)")
            }
        }
    }

    suspend fun delete(context: Context, key: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val result = context.contentResolver.delete(uri, null, arrayOf(key))
                if (result != 1) throw RuntimeException("Delete $key($uri) failed")
                Unit
            }.onFailure { it.printStackTrace() }
        }
    }

    fun observe(
        context: Context,
        key: String,
        lifecycleOwner: LifecycleOwner? = null,
        distinctUntilChanged: Boolean = true,
        observer: (Result<String>) -> Unit
    ) {
        runCatching {
            val scope = CoroutineScope(Dispatchers.IO)
            var stockResult: Result<String> = Result.success("")
            scope.launch { stockResult = get(context, key) }
            val uri = Uri.parse("content://$serverPkg.ipc-server/share/$key")
            val contentObserver = object : ContentObserver(null) {
                var currentValueInObserver = stockResult
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    CoroutineScope(Dispatchers.IO).launch {
                        val newValue = get(context, key)
                        if (!distinctUntilChanged || ((currentValueInObserver.getOrNull() != null || newValue.getOrNull() != null) && newValue.getOrNull() != currentValueInObserver.getOrNull())) {
                            withContext(Dispatchers.Main) { observer(newValue) }
                        }
                        currentValueInObserver = newValue
                    }
                }
            }

            context.contentResolver.registerContentObserver(uri, false, contentObserver)

            if (lifecycleOwner == null) {
                scope.launch(Dispatchers.Main) { observer(stockResult) }
            } else {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) { observer(stockResult) }
                }
                lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_STOP) {
                            context.contentResolver.unregisterContentObserver(contentObserver)
                        }
                    }
                })
            }
        }
    }

    fun dataFlow(context: Context, key: String): Flow<Result<String>> = callbackFlow {
        val stockResult = get(context, key)
        val uri = Uri.parse("content://$serverPkg.ipc-server/share/$key")
        trySend(stockResult)
        val contentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                CoroutineScope(Dispatchers.IO).launch {
                    trySend(get(context, key))
                }
            }
        }

        context.contentResolver.registerContentObserver(uri, false, contentObserver)
        awaitClose { context.contentResolver.unregisterContentObserver(contentObserver) }
    }
}













