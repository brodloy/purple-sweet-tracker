# Purple Sweet Tracker

A RuneLite plugin for Old School RuneScape that tracks the purple sweets you eat.

- 🔊 Plays a sound every time you eat a purple sweet — choose from several game
  sounds (kerching/GE coins by default), with a volume slider.
- 🖥️ Pick how it's shown: a **draggable overlay box**, an **item-timer style infobox**
  (item icon + amount or GP value), **both**, or **off**.
- 📊 Tracks **all-time** and **this-session** sweets eaten + value, plus **sweets/hour**.
  The overlay/infobox can show either all-time or session figures.
- 🔔 Optional **milestone notification** (chat + RuneLite notifier) every N sweets.
- 🧰 Side panel (right-hand toolbar) with the stats, a **Reset** button, and a
  **Copy stats** button.
- 💾 All-time counts persist across client restarts.

## Configuration

In the plugin config (gear icon) you'll find three sections:

- **Display** — display style (overlay / infobox / both / off), infobox type (amount /
  value), whether stats shown are lifetime or session, which overlay lines to show
  (eaten / value / per hour), and the overlay title colour.
- **Sound** — master on/off, which sound, and volume.
- **Notifications** — notify every N sweets (0 disables).

## Run it locally

You need **JDK 11** installed. Point `JAVA_HOME` at it, then use the Gradle wrapper
from the project root.

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\path\to\your\jdk-11"
.\gradlew.bat run
```

macOS / Linux:

```bash
export JAVA_HOME=/path/to/your/jdk-11
./gradlew run
```

This launches RuneLite with the plugin loaded. Log in, eat a purple sweet, and watch
the counter tick up. Find **Purple Sweet Tracker** in the side toolbar for the stats.

To just compile and run the checks, use `gradlew build` instead of `run`.

## Submit it to the Plugin Hub

1. Push this project to a **public GitHub repository**.
2. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
3. Add a file named `plugins/purple-sweet-tracker` (no extension) containing:
   ```
   repository=https://github.com/<your-username>/<your-repo>.git
   commit=<full 40-character commit hash you want published>
   ```
4. Open a pull request against `runelite/plugin-hub`. A maintainer reviews it; once
   merged the plugin appears in the in-client Plugin Hub for everyone.

See the [Plugin Hub guide](https://github.com/runelite/plugin-hub) for the full rules
(icon, naming, and review requirements).
