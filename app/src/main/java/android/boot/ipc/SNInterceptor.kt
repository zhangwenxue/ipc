package android.boot.ipc

import android.boot.ipc.server.IpcInterceptor
import kotlinx.coroutines.delay

class SNInterceptor : IpcInterceptor {
    private var sn: String? = "no define"

    override fun getChannel() = "sn"

    override suspend fun getValue(): String? {
        delay(5000)
        return sn
    }

    override suspend fun set(value: String?) {
        delay(5000)
        sn = value
    }
}