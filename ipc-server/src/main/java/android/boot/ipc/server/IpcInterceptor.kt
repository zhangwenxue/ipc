package android.boot.ipc.server

interface IpcInterceptor {
    fun getChannel(): String?
    suspend fun getValue(): String?
    suspend fun set(value: String?)
}