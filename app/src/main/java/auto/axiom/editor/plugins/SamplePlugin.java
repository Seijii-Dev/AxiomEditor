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

package auto.axiom.editor.plugins;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.axiomeditor.plugins.Plugin;
import com.axiomeditor.plugins.PluginContext;

public class SamplePlugin implements Plugin {
    @Override
    public void onPluginLoaded(@NonNull PluginContext context) {
        Toast.makeText(context.getAppContext(), "Hello from the plugin!", Toast.LENGTH_SHORT).show();
    }
}
