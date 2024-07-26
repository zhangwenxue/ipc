package android.boot.ipc.server

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private const val TAG = "_IPCService:"
private const val SHARE_NAME = "shared_store"


class SharedStore(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE)

    fun set(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun get(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}

class IpcProvider : ContentProvider() {

    companion object {
        const val KEY = "key"
        const val VALUE = "value"
    }

    private val store by lazy { SharedStore(ctx) }
    private val ctx by lazy {
        this.context ?: throw NullPointerException("Context must not be null")
    }
    private lateinit var interceptors: List<IpcInterceptor>

    private fun newUriWithKey(key: String): Uri? {
        return Uri.parse("content://${ctx.packageName}.ipc-server/share/${key}")
    }

    override fun onCreate(): Boolean {
        log("create shared store")
        interceptors = context?.applicationContext?.let {
            InterceptorInitializer.discoverAndInitialize(it)
        } ?: emptyList()
        return true
    }

    // 读取
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        log(
            "query uri:$uri\nprojection:${projection}\nselection:$selection\n,selectionArgs:$selectionArgs\nsortOrder:$sortOrder",
        )

        val arguments = selectionArgs ?: return null
        if (arguments.size != 1) return null
        val key = arguments[0]

        var value: String? = null
        runBlocking {
            val interceptor = interceptors.find { it.getChannel() == key }
            value = if (interceptor != null) {
                interceptor.getValue()
            } else {
                store.get(key)
            }
        }

        log("query succeed. {$key:$value}")
        val cursor = MatrixCursor(arrayOf(KEY, VALUE), 1)
        cursor.addRow(arrayOf(key, value))
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        log("delete. uri:$uri,selection:$selection,selectionArgs:$selectionArgs")
        val arguments = selectionArgs ?: return -1
        if (arguments.size != 1) return -1
        val key = arguments[0]

        runBlocking {
            val interceptor = interceptors.find { it.getChannel() == key }
            if (interceptor != null) {
                interceptor.set(null)
            } else {
                store.delete(key)
            }
        }

        log("$key delete success")

        val notifyUri = newUriWithKey(key)
        if (notifyUri != null) ctx.contentResolver?.notifyChange(notifyUri, null)

        return 1
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        log("update uri:$uri\nvalues:$values\nselection:$selection\nselectionArgs:$selectionArgs")
        if (values == null) return -1
        val key = values.getAsString(KEY)
        val value = values.getAsString(VALUE)

        runBlocking {
            val interceptor = interceptors.find { it.getChannel() == key }
            if (interceptor != null) {
                interceptor.set(value)
            } else {
                store.set(key, value)
            }
        }

        log("{$key:$value} update successful")

        val notifyUri = newUriWithKey(key)
        if (notifyUri != null) ctx.contentResolver?.notifyChange(notifyUri, null)

        return 1
    }

    private fun log(log: String) {
        Log.i(TAG, "<${ctx.packageName}> $log")
    }
}