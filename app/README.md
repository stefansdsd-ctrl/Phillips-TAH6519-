# Headphone Companion - POC

Dit project is een minimal POC die:
- paired Bluetooth apparaten toont
- bij klik probeert verbinding te maken via GATT en leest de Battery Level (Battery Service 0x180F)

Belangrijk:
- Geef runtime permissies (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION voor legacy)
- Test op een echt Android‑apparaat (emulator ondersteunt geen echte Bluetooth)
- Voor TAH6519: Philips publiceert geen gedetailleerde frequency response grafiek; batterijspecificaties en ANC worden gebruikt vanaf fabrikant/retailer publicaties.

Volgende stappen:
- toevoegen van headphone‑profiles (JSON) met echte meetdata
- DSP engine (IIR / FIR) om compensatiefilters toe te passen
- YouTube Music integratie: search + open‑in‑YouTube to start (veilig)
