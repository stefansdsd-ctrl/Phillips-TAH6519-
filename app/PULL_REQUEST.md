# Pull Request: Add headphone companion POC, EQ UI, and parametric EQ application

This branch (feature/headphone-poc) contains:

- A Bluetooth GATT proof-of-concept that lists paired devices and reads the Battery Level (GATT Battery Service 0x180F / Characteristic 0x2A19).
- A Room-based profile data model (HeadphoneProfile), DAO, repository, and a sample Philips TAH6519 JSON profile in assets.
- An Equalizer UI (EqScreen) supporting 5-band and 10-band modes with sliders and required testTags (eq_mode_5_band, eq_mode_10_band, theme_toggle_button, high_contrast_switch).
- An EqualizerManager that wraps Android's media Equalizer and maps parametric EQ bands to hardware bands; used as a first-step to apply EQ settings.

Notes / Next steps:
- The current EqualizerManager uses audioSessionId=0 by default. For production, use the player's audioSessionId (e.g., ExoPlayer.audioSessionId) to target the active playback session.
- To support high-quality compensation filters (FIR/convolution), implement a native audio processor (NDK) or a custom ExoPlayer AudioProcessor and integrate measured FR/IR data.
- I recommend creating a PR from feature/headphone-poc into main and running the integration tests on a physical device.

Suggested PR title: "Add headphone companion POC: Bluetooth battery GATT, profiles, and EQ UI"
Suggested PR body: Use the text above under this file.

How to create the PR locally (GitHub CLI):

  gh pr create --base main --head feature/headphone-poc --title "Add headphone companion POC: Bluetooth battery GATT, profiles, and EQ UI" --body-file app/PULL_REQUEST.md

Or create via GitHub UI using branch feature/headphone-poc.
