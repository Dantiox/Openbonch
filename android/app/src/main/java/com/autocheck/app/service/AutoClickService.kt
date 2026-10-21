package com.autocheck.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.autocheck.app.MainActivity
import com.autocheck.app.R
import com.autocheck.app.data.AutoClickEngine
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.NetworkRouter
import com.autocheck.app.data.SettingsStore
import com.autocheck.app.data.SutClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground-сервис: держит цикл проверки живым, пока экран выключен.
 *
 * У приложения одно уведомление с постоянным id. Оно обновляется на месте и не создаётся заново:
 * пока сервис работает, в нём кнопка «Остановить», после остановки — кнопка «Включить».
 *
 * Штатные действия (actions) Android показывает только в раскрытом уведомлении, поэтому кнопка
 * дополнительно встроена в свёрнутый вид (см. `notification_collapsed.xml`).
 */
class AutoClickService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engineJob: Job? = null
    private var notificationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /** Фиксированное время уведомления: иначе при каждом обновлении оно «всплывало» бы наверх списка. */
    private val postedAt = System.currentTimeMillis()

    @Volatile
    private var currentContent = NotificationContent("AutoCheck подключается", "Выполняется вход в личный кабинет")

    private val notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Статус работы", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Одно постоянное уведомление: состояние автопосещения и кнопка включения или остановки"
        channel.setShowBadge(false)
        notificationManager.createNotificationChannel(channel)
        // Канал прошлых версий больше не используется
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val job = engineJob
            if (job?.isActive == true) job.cancel() else stopSelf()
            return START_NOT_STICKY
        }
        return try {
            // startForegroundService() требует вызвать startForeground() при каждом запуске
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildRunningNotification(currentContent),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
            startEngine()
            START_STICKY
        } catch (e: Exception) {
            // например, ограничения запуска foreground-сервисов из фона
            AutoClickState.error("Не удалось запустить сервис: ${e.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun startEngine() {
        if (engineJob?.isActive == true) return

        acquireWakeLock()

        val store = SettingsStore(this)
        val credentials = store.load()
        AutoClickState.setNotificationPrefs(store.loadNotifications())
        // Записи журнала до этого момента относятся к прошлым запускам и в уведомлении не показываются
        val baselineId = AutoClickState.logs.value.lastOrNull()?.id ?: 0L

        AutoClickState.resetForNewRun()
        if (!credentials.isComplete) {
            AutoClickState.error("Добавьте хотя бы один аккаунт в настройках")
            AutoClickState.setStatus(EngineStatus.ERROR)
            finishService()
            return
        }
        val accountCount = credentials.accounts.count { it.isComplete }
        AutoClickState.info("Запуск автопосещения. Аккаунтов: $accountCount. Маршрут: ${credentials.route.title}")

        // Обновляем то же самое уведомление, и только когда его содержимое действительно изменилось
        notificationJob = scope.launch {
            combine(
                AutoClickState.logs,
                AutoClickState.status,
                AutoClickState.notificationPrefs,
            ) { logs, status, prefs ->
                NotificationPresenter.running(status, logs, prefs, baselineId, credentials.timeoutSec)
            }
                .distinctUntilChanged()
                .collect { content ->
                    currentContent = content
                    notificationManager.notify(NOTIFICATION_ID, buildRunningNotification(content))
                }
        }

        engineJob = scope.launch {
            val router = NetworkRouter(applicationContext)
            try {
                router.start()
                // Аккаунты перечитываются каждый цикл, остальные настройки действуют до перезапуска
                AutoClickEngine { SutClient(credentials.route, router) }
                    .run(credentials.timeoutSec) { store.loadAccounts() }
            } finally {
                router.stop()
                withContext(NonCancellable) {
                    if (AutoClickState.status.value != EngineStatus.ERROR) {
                        AutoClickState.setStatus(EngineStatus.STOPPED)
                        AutoClickState.info("Автопосещение остановлено")
                    }
                    // Дожидаемся завершения обновлений, иначе они могли бы вернуть уведомление «работает»
                    // уже после перехода в состояние «остановлено»
                    notificationJob?.cancelAndJoin()
                    finishService()
                }
            }
        }
    }

    /** Заменяет рабочее уведомление на «остановлено» (с кнопкой «Включить») либо убирает его. */
    private fun finishService() {
        // DETACH оставляет уведомление на месте, и следующий notify() с тем же id обновляет его,
        // а не создаёт новое
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        if (AutoClickState.notificationPrefs.value.controlWhenStopped) {
            val content = NotificationPresenter.stopped(AutoClickState.status.value, AutoClickState.logs.value)
            notificationManager.notify(NOTIFICATION_ID, buildStoppedNotification(content))
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
        stopSelf()
    }

    private fun baseNotification(
        content: NotificationContent,
        actionLabel: String,
        actionIntent: PendingIntent,
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setColor(ContextCompat.getColor(this, R.color.accent))
            // Заголовок и текст нужны для экрана блокировки, часов и скринридеров
            .setContentTitle(content.title)
            .setContentText(content.text)
            // Системная «шапка» и штатные действия сохраняются, содержимое — своё
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView(content, actionLabel, actionIntent))
            .setCustomBigContentView(expandedView(content))
            // В раскрытом виде кнопка показывается как обычное действие
            .addAction(0, actionLabel, actionIntent)
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setWhen(postedAt)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)

    /** Свёрнутый вид: заголовок, одна строка текста и кнопка управления. */
    private fun collapsedView(
        content: NotificationContent,
        actionLabel: String,
        actionIntent: PendingIntent,
    ) = RemoteViews(packageName, R.layout.notification_collapsed).apply {
        setTextViewText(R.id.notification_title, content.title)
        setTextViewText(R.id.notification_text, content.text)
        setTextViewText(R.id.notification_action, actionLabel)
        setOnClickPendingIntent(R.id.notification_action, actionIntent)
    }

    /** Раскрытый вид: полный текст события. */
    private fun expandedView(content: NotificationContent) =
        RemoteViews(packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notification_title, content.title)
            setTextViewText(R.id.notification_text, content.text)
        }

    private fun buildRunningNotification(content: NotificationContent): Notification =
        baseNotification(content, "Остановить", stopIntent())
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun buildStoppedNotification(content: NotificationContent): Notification =
        baseNotification(content, "Включить", startIntent())
            .setOngoing(false)
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this, 1,
        Intent(this, AutoClickService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun startIntent(): PendingIntent = PendingIntent.getForegroundService(
        this, 2,
        Intent(this, AutoClickService::class.java).setAction(ACTION_START),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "autocheck:engine")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "autocheck_status"
        private const val LEGACY_CHANNEL_ID = "autocheck_running"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "com.autocheck.app.START"
        private const val ACTION_STOP = "com.autocheck.app.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AutoClickService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, AutoClickService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
