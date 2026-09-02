package com.example.bettermarkdown;

import androidx.annotation.NonNull;
import com.axiomeditor.plugins.Editor;
import com.axiomeditor.plugins.Plugin;
import com.axiomeditor.plugins.PluginContext;
import com.axiomeditor.plugins.command.EditorCommand;
import com.axiomeditor.plugins.editor.Range;

/**
 * Better MarkDown adds lightweight Markdown formatting actions to Axiom Editor.
 */
public final class BetterMarkDownPlugin implements Plugin {
    private static final int MENU_BASE = 24000;

    public BetterMarkDownPlugin() {
    }

    @Override
    public void onPluginLoaded(@NonNull PluginContext context) {
        context.addMenu("Markdown: Bold", MENU_BASE + 1,
                () -> wrapSelection(context, "**", "**", "Select text to make it bold."));
        context.addMenu("Markdown: Italic", MENU_BASE + 2,
                () -> wrapSelection(context, "*", "*", "Select text to make it italic."));
        context.addMenu("Markdown: Inline code", MENU_BASE + 3,
                () -> wrapSelection(context, "`", "`", "Select code to format it inline."));
        context.addMenu("Markdown: Link", MENU_BASE + 4,
                () -> wrapSelection(context, "[", "](https://)", "Select link text first."));
        context.addMenu("Markdown: Heading 1", MENU_BASE + 5,
                () -> prefixSelection(context, "# ", "Select a line or text for a heading."));
        context.addMenu("Markdown: Heading 2", MENU_BASE + 6,
                () -> prefixSelection(context, "## ", "Select a line or text for a heading."));
        context.addMenu("Markdown: Bullet list", MENU_BASE + 7,
                () -> prefixSelection(context, "- ", "Select a line or text for a list item."));
        context.addMenu("Markdown: Help", MENU_BASE + 8, () -> context.showDialog(
                "Better MarkDown",
                "Select text, then use the Markdown menu to apply bold, italic, inline code, links, headings, or list markers.",
                "OK"
        ));

        context.registerCommand(new FormatCommand(
                "better-markdown.bold",
                "Markdown: Bold",
                "Ctrl+Alt+B",
                () -> wrapSelection(context, "**", "**", "Select text to make it bold.")
        ));
        context.log("Better MarkDown loaded");
    }

    private static void wrapSelection(
            PluginContext context,
            String before,
            String after,
            String emptyMessage
    ) {
        Editor editor = context.getEditor();
        Range selection = editor.getSelectionRange();
        if (selection == null) {
            context.toast(emptyMessage);
            return;
        }
        String selected = editor.getText(selection);
        if (selected == null || selected.isEmpty()) {
            context.toast(emptyMessage);
            return;
        }
        editor.replaceText(selection, before + selected + after);
    }

    private static void prefixSelection(
            PluginContext context,
            String prefix,
            String emptyMessage
    ) {
        Editor editor = context.getEditor();
        Range selection = editor.getSelectionRange();
        if (selection == null) {
            context.toast(emptyMessage);
            return;
        }
        String selected = editor.getText(selection);
        if (selected == null || selected.isEmpty()) {
            context.toast(emptyMessage);
            return;
        }
        String[] lines = selected.split("\\n", -1);
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) formatted.append('\n');
            formatted.append(prefix).append(lines[i]);
        }
        editor.replaceText(selection, formatted.toString());
    }

    private static final class FormatCommand implements EditorCommand {
        private final String id;
        private final String name;
        private final String keyBinding;
        private final Runnable action;

        private FormatCommand(String id, String name, String keyBinding, Runnable action) {
            this.id = id;
            this.name = name;
            this.keyBinding = keyBinding;
            this.action = action;
        }

        @NonNull
        @Override
        public String getCommandId() {
            return id;
        }

        @NonNull
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getKeyBinding() {
            return keyBinding;
        }

        @Override
        public void execute(@NonNull Editor editor) {
            action.run();
        }
    }
}
