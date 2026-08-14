Title: Stabilize BLE/GATT handling and add inspection models

This PR adds small, focused fixes to unblock compilation and improve BLE/GATT data handling:

- Adds GATT inspection models required by HeadphoneLabFormatter (GattInspectionResult, GattServiceModel, GattCharacteristicModel).
- Normalizes BLE service UUIDs and service-data keys to canonical UUID strings (uses ParcelUuid.uuid.toString()).
- Makes BluetoothManager/adapter lookup null-safe and reports scanner-unavailable via onError when the Bluetooth LE scanner is not available.

Rationale:
These changes fix a missing model reference that caused compilation failures and make BLE data handling more predictable across different devices and Android implementations. They are intentionally minimal and focused on stability.
