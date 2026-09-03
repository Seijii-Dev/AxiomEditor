# Axiom JavaScript Extension API

Axiom extensions use a versioned ZIP package instead of an Android JAR. The first package contract is:

```text
extension-id/
├── manifest.json
├── main.js
└── optional assets
```

A manifest must include a reverse-domain `id`, display `name`, semantic `version`, relative JavaScript `main` path, and optional permission names. Unknown permissions, duplicate permissions, unsafe entry paths, missing entry files, and malformed JSON are rejected during installation.

The initial bridge is intentionally narrow. It is designed to expose editor selection read/write, current-file metadata, command registration, notifications, and extension-scoped settings. Filesystem and network APIs are reserved for later milestones and must require explicit permissions.

The Android host currently retains its native editor and existing JAR plugin implementation. The JavaScript extension manager is being introduced in parallel so the runtime and package format can be validated before the native format is deprecated.

## Example manifest

```json
{
  "id": "com.axiom.better-markdown",
  "name": "Better Markdown",
  "version": "1.0.0",
  "main": "main.js",
  "engines": { "axiom": ">=1.0.0" },
  "permissions": ["editor.read", "editor.write", "commands.register", "notifications"]
}
```

The next implementation milestone is a WebView-backed runtime adapter and an Extension Manager screen for install, enable, disable, and uninstall operations.
