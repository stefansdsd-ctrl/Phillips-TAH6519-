# Philips TAH6519 Companion App - Project Context & Conventions

Welcome to the Philips TAH6519 Premium Headphone Companion App codebase. This file serves as the core reference for future AI agents and developers to preserve and extend the custom visual and functional systems implemented for these headphones.

---

## 🎧 About the Philips TAH6519 / TAH6509
The **Philips TAH6519** (related to the premium 6000-series, e.g., TAH6509) is a state-of-the-art wireless over-ear headphone designed for audio purists and active travelers. 
- **Driver Size:** 40 mm high-performance neodymium acoustic drivers.
- **Active Noise Canceling (ANC):** Hybrid ANC with deep-silence capability, including automatic environment detection.
- **Battery Life:** 40 hours of playtime with ANC enabled, and up to 80 hours with ANC disabled.
- **Connectivity:** Bluetooth 5.3 with dual-point Multipoint connection support.
- **Charging:** Ultra-fast charging via USB-C (approx. 15 minutes of charging offers up to 5 hours of playback).

---

## 🎨 Design Philosophy & Themes
The application features an immersive, negative-space oriented design layout that aligns with high-end premium companion standards.

### Core Visual Themes (`Color.kt`)
1. **Philips Studio (Default):** Royal Blue (`#0066FF`) and Sky Blue primary tones combined with a deep slate background.
2. **Cyberpunk Neon:** Bright violet and electric cyan neon accents.
3. **Carbon Amber:** Jet black panels with warm gold/amber highlights.
4. **Nordic Frost:** Deep slate layout with calming aurora green indicators.
5. **High-Contrast Dark Mode (New):** A specialized pure pitch-black theme (`#000000`) paired with highly visible, crisp yellow, cyan, and white elements for maximum visibility in dark and outdoor environments.

---

## 🛠️ Key Architectural Components

### 1. Battery Status Display & Estimators
The battery component includes a three-way toggle switcher to render status dynamically based on user preferences:
- **Headset Art (`Tah6519HeadphoneBatteryArt`):** An aesthetic, custom-painted vector illustration of the headphones that scales and lights up depending on ANC modes.
- **Accu Meters (`PhilipsPremiumBatteryIndicator`):** A sophisticated dynamic analog needle gauge meter showing live percentages.
- **Accu Balk (`PhilipsHeadphoneProgressBar`):** A beautiful modern linear progress bar reflecting precise status with charging animation pulses.
- **Estimate Calculator:** Generates live playtime remaining based on the ANC status (calculating between the official 40h/80h spec bounds).

### 2. Equalizer & Preset Engine
The audio profiling screen includes a fully functional 5-Band / 10-Band audio equalizer:
- **Preset Options:** 
  - *Bass:* Custom low-frequency boost (`+8dB`, `+6dB`, etc.) for deep bass response.
  - *Treble:* Crisp high-frequency shelf for enhanced instrument clarity.
  - *Vocal:* Boosted midrange to bring out lyrics and speech.
  - *Dynamic Bass, Flat, and Philips Signature.*
- **Dual-Band Controls:** Users can switch between a high-precision **10-Band equalizer** (`60Hz` - `16kHz` individual sliders) and a simpler, simplified **5-Band equalizer** (averaging pairs of bands under *Bass, Lo-Mid, Mid, Hi-Mid, Treble*).
- **Frequency Response Graph (`FrequencyResponseGraph`):** Real-time drawing of the active sound profile using custom canvas graphics and smooth bezier curves.

### 3. Interactive Tools & Diagnostics
- **Interactive Hearing Test (Gehoortest):** A step-by-step diagnostic test that plays specific frequencies (250Hz, 1kHz, 4kHz, 8kHz) and asks users to adjust their threshold to formulate a custom audio calibration curve.
- **Safe Sound exposure (Gehoorbescherming):** Live decibel exposure tracking that alerts users if their long-term average exceeds 85 dB.
- **Find My Headphones:** An audio beacon locator that plays a ping sound on the physical headphones to assist in locating them.
- **Dual-Device Connection Manager:** Live control panel to connect, disconnect, or prioritize multi-point active sources.

---

## ⚠️ Implementation Safeguards (For Agents)
When modifying this app, please respect the following guidelines:
1. **Never revert the High-Contrast dark theme toggle** in the top bar or settings panel. It has been specifically crafted for outdoor visibility.
2. **Ensure testTags remain intact** for all navigation and UI elements (e.g. `theme_toggle_button`, `high_contrast_switch`, `eq_mode_5_band`, `eq_mode_10_band`).
3. **Keep the Kotlin Gradle configuration stable.** Do not modify `build.gradle.kts` versions or classpath structures unless explicitly prompted.
