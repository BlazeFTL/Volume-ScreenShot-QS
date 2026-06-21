# ScreenShot and Volume Panel Quick Settings

**Close Panel & Take Actions Instantly**

A Quick Settings tile app for Android that instantly collapses the notification/status bar and captures a screenshot — no manual swipe-and-tap needed.

![platform](https://img.shields.io/badge/platform-Android-3DDC84)
![license](https://img.shields.io/badge/license-MIT-blue)

## Features

- **Core Accessibility Helper (Recommended)** — No root required. Uses the Accessibility Service API to collapse the expanded status bar and capture a clean screenshot.
- **Root Capture Mode (Optional)** — For rooted devices, with two capture methods:
  - `Simulated Power + Vol Down (KeyEvent 120)` — triggers the native screen capture popup.
  - `Direct Screencap Utility (screencap -p)` — writes the capture directly to the gallery.
- **Volume UI Tile** — Quick access to the system volume panel.
- Built-in diagnostic test buttons (Snapshot / Volume UI) to verify setup.

## Screenshots

<img width="702" height="1560" alt="Screenshot_20260621-110720_Spark Launcher" src="https://github.com/user-attachments/assets/0f3be7a0-d51b-4a17-9944-a82aa1d78de4" />
<img width="1080" height="2400" alt="Screenshot_20260621-110728_Spark Launcher" src="https://github.com/user-attachments/assets/6c48f7d9-f6d9-4153-8b1a-cde0f1a2c2c5" />


## Installation

1. Download the latest APK from [Releases](../../releases).
2. Install the APK (enable "Install unknown apps" if prompted).
3. Open the app and enable **Core Accessibility Helper** under Settings → Accessibility.
4. Add the **Screenshot** and **Volume UI** tiles to your Quick Settings panel.

## Permissions

| Permission | Required For |
|---|---|
| Accessibility Service | No-root status bar collapse + screenshot |
| Root (su) | Optional Root Capture Mode |

No data is collected or transmitted. All captures are stored locally.

## How It Works

- **No-root mode**: the Accessibility Service detects the tile action, collapses the status bar via `performGlobalAction`, then triggers a standard screenshot.
- **Root mode**: executes either a simulated `KeyEvent 120` (Power+VolDown) or a direct `screencap -p` shell command via root, then writes to the gallery.

## License

MIT
