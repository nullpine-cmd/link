# Room, WorkManager и Compose поставляют собственные consumer-правила.
# Достаточно сохранить имена воркеров, которые WorkManager создаёт через рефлексию.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
