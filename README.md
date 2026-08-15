<div align="center">

# 📸 ScreenShot and Volume Panel Quick Settings

**Close Panel & Take Actions Instantly**

A Quick Settings tile app for Android that instantly collapses the notification/status bar and captures a screenshot — no manual swipe-and-tap needed.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Root](https://img.shields.io/badge/root-optional-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

</div>

---

## 🧰 Features

| | Feature | Details |
|---|---|---|
| ♿ | **Core Accessibility Helper** *(Recommended)* | No root required — uses the Accessibility Service API to collapse the status bar and capture a clean screenshot |
| 🔓 | **Root Capture Mode** *(Optional)* | For rooted devices — `Simulated Power+VolDown (KeyEvent 120)` triggers the native capture popup, or `Direct Screencap (screencap -p)` writes straight to the gallery |
| 🔊 | **Volume UI Tile** | Quick access to the system volume panel |
| 🧪 | **Diagnostics** | Built-in test buttons (Snapshot / Volume UI) to verify setup |

<br>

## 📸 Screenshots

<div align="center">
<table>
<tr>
<td><img width="220" alt="Screenshot_20260621-110720" src="https://github.com/user-attachments/assets/0f3be7a0-d51b-4a17-9944-a82aa1d78de4" /></td>
<td><img width="220" alt="Screenshot_20260621-110728" src="https://github.com/user-attachments/assets/6c48f7d9-f6d9-4153-8b1a-cde0f1a2c2c5" /></td>
</tr>
</table>
</div>

<br>

## 🚀 Installation

1. Download the latest APK from [Releases](../../releases)
2. Install the APK (enable "Install unknown apps" if prompted)
3. Open the app and enable **Core Accessibility Helper** under Settings → Accessibility
4. Add the **Screenshot** and **Volume UI** tiles to your Quick Settings panel

<br>

## 🔐 Permissions

| Permission | Required For |
|---|---|
| Accessibility Service | No-root status bar collapse + screenshot |
| Root (su) | Optional Root Capture Mode |

No data is collected or transmitted. All captures are stored locally.

<br>

## ⚙️ How It Works

- **No-root mode** — the Accessibility Service detects the tile action, collapses the status bar via `performGlobalAction`, then triggers a standard screenshot
- **Root mode** — executes either a simulated `KeyEvent 120` (Power+VolDown) or a direct `screencap -p` shell command via root, then writes to the gallery

<br>

## 📄 License

MIT
