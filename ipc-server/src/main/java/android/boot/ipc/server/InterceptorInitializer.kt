package android.boot.ipc.server

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo

object InterceptorInitializer {
    fun discoverAndInitialize(context: Context): List<IpcInterceptor> {
        val list = mutableListOf<IpcInterceptor>()
        val provider = ComponentName(
            context.packageName,
            IpcProvider::class.java.getName()
        )
        val providerInfo: ProviderInfo = context.packageManager
            .getProviderInfo(provider, PackageManager.GET_META_DATA)
        val metadata = providerInfo.metaData
        //
        val startup = "ipc-interceptor"
        val keys = metadata.keySet()
        keys.forEach {
            if (startup == it) {
                val clz = Class.forName(metadata.getString(it, null))
                if (IpcInterceptor::class.java.isAssignableFrom(clz)) {
                    val instance = clz.getDeclaredConstructor().newInstance()
                    (instance as? IpcInterceptor)?.run { list.add(this) }
                }
            }
        }
        return list.toList()
    }
}