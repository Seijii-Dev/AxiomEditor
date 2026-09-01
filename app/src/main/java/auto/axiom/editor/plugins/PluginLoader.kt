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
package auto.axiom.editor.plugins

import android.content.Context
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ToastUtils
import auto.axiom.editor.PluginConstants
import auto.axiom.editor.extensions.extractZipFile
import auto.axiom.editor.extensions.toFile
import auto.axiom.editor.plugins.internal.PluginInfo
import auto.axiom.editor.utils.runOnUiThread
import auto.axiom.editor.utils.showShortToast
import com.axiomeditor.plugins.Plugin
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PluginLoader {
    fun loadPlugins(context: Context): List<Pair<PluginInfo, Plugin>> {
        val plugins = mutableListOf<Plugin>()
        val pluginInfos = mutableListOf<PluginInfo>()

        val pluginsPath = PluginConstants.PLUGIN_HOME_PATH.toFile()
        FileUtils.createOrExistsDir(pluginsPath)

        pluginsPath.listFiles()?.forEach { file ->
            val properties = file.resolve("plugin.properties")
            if (!properties.exists()) {
                throw IllegalArgumentException("Plugin directory ${file.name} does not contain plugin.properties")
            }

            val pluginInfo = PluginInfo(properties)
            pluginInfo.pluginFileName?.let {
                val jarFilePath = file.resolve(it).apply {
                    setWritable(false)
                    setReadable(true, true)
                }

                val dexClassLoader = DexClassLoader(
                    jarFilePath.absolutePath,
                    null,
                    null,
                    context.applicationContext.classLoader
                )

                val pluginClass = dexClassLoader.loadClass(pluginInfo.mainClass)

                if (Plugin::class.java.isAssignableFrom(pluginClass)) {
                    val constructor = pluginClass.getConstructor()
                    plugins.add(constructor.newInstance() as Plugin)
                    pluginInfos.add(pluginInfo)
                } else {
                    throw IllegalArgumentException("Class does not implement Plugin interface")
                }
            } ?: runOnUiThread {
                showShortToast(context, "Plugin file not found for ${file.name}")
            }
        }

        return pluginInfos.zip(plugins)
    }

    suspend fun extractPluginZip(pluginZipFile: File): File {
        return withContext(Dispatchers.IO) {
            val path = "${PluginConstants.PLUGIN_HOME_PATH}/${pluginZipFile.nameWithoutExtension}"
            val internalFile = path.toFile()
            runCatching {
                FileUtils.createOrExistsDir(internalFile)
                pluginZipFile.extractZipFile(internalFile)
            }.onFailure {
                ToastUtils.showShort(it.message)
            }

            val properties = internalFile.resolve("plugin.properties")
            if (!properties.exists()) {
                throw IllegalArgumentException("Plugin directory ${internalFile.name} does not contain plugin.properties")
            }

            val pluginInfo = PluginInfo(properties)
            internalFile.apply {
                if (pluginInfo.name.isNullOrBlank()) {
                    throw NullPointerException("Plugin name is empty.")
                }
                FileUtils.rename(this, pluginInfo.name)
            }
        }
    }
}
