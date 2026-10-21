package com.autocheck.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.net.SocketException

/**
 * Главный цикл — порт `AutoClickAPI.auto_click()` из `auth.py`.
 *
 * Каждый цикл проходит по всем аккаунтам: у каждого своя сессия (cookie). Если сессии нет —
 * выполняется вход (по логину и паролю либо подстановкой токена), затем загружается расписание и
 * нажимаются кнопки. Ошибка одного аккаунта не мешает остальным. Список аккаунтов перечитывается
 * в начале каждого цикла, поэтому изменения из настроек подхватываются без перезапуска.
 *
 * Возвращается только при фатальной ситуации (нет аккаунтов или войти не удалось ни в один);
 * остановка — отменой корутины.
 *
 * @param newClient создаёт клиента для новой сессии: у каждого аккаунта он свой.
 */
class AutoClickEngine(private val newClient: () -> SutClient) {
    private val state = AutoClickState

    private class Session(val account: Account, val client: SutClient) {
        /** Есть рабочая сессия. Сбрасывается при ошибке или истечении сессии — тогда будет новый вход. */
        var authorized = false

        /** Неверный пароль или недействительный токен: повторять бессмысленно, пока аккаунт не изменят. */
        var blocked = false
    }

    private enum class Outcome { DONE, FAILED, BLOCKED }

    private val sessions = mutableMapOf<String, Session>()

    suspend fun run(timeoutSec: Int, loadAccounts: () -> List<Account>) {
        val pauseMs = timeoutSec * 1000L
        var reportedIncomplete = 0

        while (true) {
            val all = loadAccounts()
            val accounts = all.filter { it.isComplete }

            val incomplete = all.size - accounts.size
            if (incomplete != reportedIncomplete) {
                reportedIncomplete = incomplete
                if (incomplete > 0) state.warning("Аккаунтов без данных пропущено: $incomplete")
            }

            if (accounts.isEmpty()) {
                state.error("Нет аккаунтов для проверки. Добавьте аккаунт в настройках")
                state.setStatus(EngineStatus.ERROR)
                return
            }

            syncSessions(accounts)
            val tagged = accounts.size > 1
            var anyDone = false
            val active = accounts.filterNot { sessions.getValue(it.id).blocked }

            for ((index, account) in active.withIndex()) {
                val session = sessions.getValue(account.id)
                val outcome = process(session, if (tagged) account.label else null)
                if (outcome == Outcome.DONE) {
                    anyDone = true
                    state.setStatus(EngineStatus.RUNNING)
                }
                if (index < active.lastIndex) delay(ACCOUNT_PAUSE_MS)
            }

            if (accounts.all { sessions.getValue(it.id).blocked }) {
                state.error(
                    if (tagged) "Не удалось войти ни в один аккаунт. Проверьте данные в настройках"
                    else "Не удалось войти. Проверьте данные аккаунта в настройках"
                )
                state.setStatus(EngineStatus.ERROR)
                return
            }

            if (anyDone) state.onCycleFinished()
            state.info("Следующая проверка через $timeoutSec с", LogCategory.PAUSE)
            delay(pauseMs)
        }
    }

    /** Оставляет сессии только для текущих аккаунтов; у изменённого аккаунта сессия начинается заново. */
    private fun syncSessions(accounts: List<Account>) {
        sessions.keys.retainAll(accounts.mapTo(HashSet()) { it.id })
        for (account in accounts) {
            val existing = sessions[account.id]
            if (existing == null || existing.account != account) {
                sessions[account.id] = Session(account, newClient())
            }
        }
    }

    /** Один проход по одному аккаунту — тело цикла `for account in self.accounts` из Python. */
    private suspend fun process(session: Session, tag: String?): Outcome {
        val client = session.client
        client.accountTag = tag
        try {
            if (!session.authorized && !signIn(session, tag)) return Outcome.BLOCKED

            var schedule = client.fetchSchedule()
            if (schedule is ScheduleResult.SessionExpired) {
                session.authorized = false

                if (session.account.type == AccountType.TOKEN) {
                    state.warning(tagged(tag, "Токен недействителен или истёк. Замените его в настройках"))
                    session.blocked = true
                    return Outcome.BLOCKED
                }

                state.warning(tagged(tag, "Срок сессии истёк, выполняется повторный вход"))
                if (!signIn(session, tag)) return Outcome.BLOCKED

                schedule = client.fetchSchedule()
                if (schedule is ScheduleResult.SessionExpired) {
                    state.warning(tagged(tag, "Повторный вход не помог"))
                    session.authorized = false
                    return Outcome.FAILED
                }
            }

            val ok = schedule as? ScheduleResult.Ok ?: return Outcome.FAILED
            if (ok.lessonIds.isEmpty()) {
                state.info(tagged(tag, "Нет активных занятий"), LogCategory.CHECK)
            } else {
                for (id in ok.lessonIds) {
                    if (client.startLesson(id, ok.week)) {
                        state.success(tagged(tag, "Занятие начато (id: $id)"), LogCategory.LESSON)
                        state.onLessonStarted()
                    } else {
                        state.info(tagged(tag, "Занятие (id: $id) ещё не началось"), LogCategory.CHECK)
                    }
                }
            }
            return Outcome.DONE
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // как в Python: любая ошибка сбрасывает сессию, на следующем цикле вход будет выполнен заново
            state.error(tagged(tag, "Ошибка подключения: ${describe(e)}"))
            session.authorized = false
            return Outcome.FAILED
        }
    }

    /**
     * Создаёт сессию: токен подставляется как cookie, логин и пароль отправляются на вход.
     * @return false — данные отвергнуты, аккаунт заблокирован до изменения.
     * @throws java.io.IOException при сетевых ошибках.
     */
    private suspend fun signIn(session: Session, tag: String?): Boolean {
        val account = session.account
        return when (account.type) {
            AccountType.TOKEN -> {
                state.info(tagged(tag, "Используется токен"))
                session.client.useToken(account.token)
                session.authorized = true
                true
            }

            AccountType.PASSWORD -> when (session.client.login(account.login, account.password)) {
                LoginResult.SUCCESS -> {
                    state.success(tagged(tag, "Вход выполнен"))
                    session.authorized = true
                    true
                }

                LoginResult.BAD_CREDENTIALS -> {
                    state.error(tagged(tag, "Ошибка входа: проверьте логин и пароль"))
                    session.blocked = true
                    false
                }
            }
        }
    }

    private fun tagged(tag: String?, message: String) = if (tag == null) message else "$tag: $message"

    private fun describe(e: Exception): String {
        val message = e.message.orEmpty()
        return if (e is SocketException && message.contains("EPERM")) {
            "VPN не разрешает приложению использовать выбранную сеть (EPERM). " +
                "Добавьте AutoCheck в исключения VPN или выберите маршрут «Как в системе»"
        } else {
            val type = e.javaClass.simpleName
            if (message.isBlank()) type else "$type: $message"
        }
    }

    private companion object {
        /** Пауза между аккаунтами внутри цикла (`await asyncio.sleep(1)` в Python). */
        const val ACCOUNT_PAUSE_MS = 1000L
    }
}
