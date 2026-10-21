package com.autocheck.app.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Переход к системной настройке автозапуска.
 *
 * Публичного API для неё в Android нет: на «чистом» Android автозапуск после перезагрузки работает сам,
 * а прошивки Xiaomi, Huawei, Oppo, Vivo и др. добавляют собственный экран, где его нужно разрешить.
 * Эти экраны открываются по фирменным компонентам; на разных версиях прошивок они могут отсутствовать,
 * поэтому в крайнем случае открываются обычные настройки приложения.
 */
object AutoStartSettings {

    private val vendorScreens: Map<String, List<ComponentName>> = mapOf(
        "xiaomi" to listOf(
            cn("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ),
        "huawei" to listOf(
            cn("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            cn("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            cn("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        ),
        "honor" to listOf(
            cn("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            cn("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ),
        "oppo" to listOf(
            cn("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            cn("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            cn("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ),
        "realme" to listOf(
            cn("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            cn("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ),
        "vivo" to listOf(
            cn("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            cn("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        ),
        "oneplus" to listOf(
            cn("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        ),
        "samsung" to listOf(
            // У Samsung отдельного «автозапуска» нет: приложение не должно попадать в «спящие»
            cn("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        ),
        "asus" to listOf(
            cn("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
        ),
    )

    private val manufacturer: String
        get() = Build.MANUFACTURER.orEmpty().lowercase()

    /** Прошивка известна тем, что отдельно ограничивает автозапуск приложений. */
    fun isVendorRestricted(): Boolean = manufacturer in vendorScreens

    /**
     * Открывает экран автозапуска производителя, а если он недоступен — настройки приложения.
     * Возвращает `true`, если открылся именно фирменный экран.
     */
    fun open(context: Context): Boolean {
        vendorScreens[manufacturer].orEmpty().forEach { component ->
            val opened = runCatching {
                context.startActivity(
                    Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.isSuccess
            if (opened) return true
        }
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return false
    }

    private fun cn(pkg: String, cls: String) = ComponentName(pkg, cls)
}
