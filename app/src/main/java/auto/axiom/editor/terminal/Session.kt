/*
 * This file is part of Axiom Editor.
 *
 * Axiom Editor is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Axiom Editor is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Axiom Editor.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package auto.axiom.editor.terminal

import auto.axiom.editor.activities.TerminalActivity
import auto.axiom.editor.extensions.child
import auto.axiom.editor.extensions.tmpDir
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

// https://github.com/RohitKushvaha01/ReTerminal/blob/main/app/src/main/java/com/rk/terminal/terminal/MkSession.kt
object Session {
    fun createSession(
        activity: TerminalActivity,
        sessionClient: TerminalSessionClient,
        sessionId: String
    ): TerminalSession {
        with(activity) {
            val envVariables = mapOf(
                "ANDROID_ART_ROOT" to System.getenv("ANDROID_ART_ROOT"),
                "ANDROID_DATA" to System.getenv("ANDROID_DATA"),
                "ANDROID_I18N_ROOT" to System.getenv("ANDROID_I18N_ROOT"),
                "ANDROID_ROOT" to System.getenv("ANDROID_ROOT"),
                "ANDROID_RUNTIME_ROOT" to System.getenv("ANDROID_RUNTIME_ROOT"),
                "ANDROID_TZDATA_ROOT" to System.getenv("ANDROID_TZDATA_ROOT"),
                "BOOTCLASSPATH" to System.getenv("BOOTCLASSPATH"),
                "DEX2OATBOOTCLASSPATH" to System.getenv("DEX2OATBOOTCLASSPATH"),
                "EXTERNAL_STORAGE" to System.getenv("EXTERNAL_STORAGE")
            )

            val workingDir = if (intent.hasExtra("cwd")) {
                intent.getStringExtra("cwd").toString()
            } else {
                home.absolutePath
            }

            val tmpDir = File(activity.tmpDir, "terminal/$sessionId")

            if (tmpDir.exists()) {
                tmpDir.deleteRecursively()
                tmpDir.mkdirs()
            } else {
                tmpDir.mkdirs()
            }

            val env = mutableListOf(
                "PROOT_TMP_DIR=${tmpDir.absolutePath}",
                "HOME=${home.absolutePath}",
                "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "PREFIX=${prefix.absolutePath}",
                "LD_LIBRARY_PATH=${lib.absolutePath}",
                "ALPINE=${alpineDir.absolutePath}",
                "LINKER=${Executor.linker}",
                "PROOT=${
                    File(filesDir, "proot").apply {
                        if (exists().not()) {
                            assets.open("terminal/proot").use {
                                writeBytes(it.readBytes())
                            }
                        }
                        setExecutable(true)
                    }.absolutePath
                }"
            )

            env.addAll(envVariables.map { "${it.key}=${it.value}" })

            val initHost = bin.child("init-host").apply {
                writeText(
                    assets.open("terminal/init-host.sh").bufferedReader().use { it.readText() })
            }
            bin.child("init").apply {
                writeText(assets.open("terminal/init.sh").bufferedReader().use { it.readText() })
            }

            val shell = "/system/bin/sh"
            val args = arrayOf("-c", initHost.absolutePath)

            return TerminalSession(
                shell,
                workingDir,
                args,
                env.toTypedArray(),
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                sessionClient
            )
        }
    }
}
