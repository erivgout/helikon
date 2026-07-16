# Testing

Run the test suite with:

```powershell
.\gradlew.bat test
```

The bootstrap tests cover module lifecycle behavior, failure isolation, setting
validation/JSON recovery, configuration round-tripping, and malformed JSON
recovery.

Manual smoke test:

1. Run `./gradlew.bat runClient` using Java 25.
2. Confirm the client starts without a configuration directory.
3. Press Right Shift and verify the placeholder GUI opens.
4. Close the client and verify `config/helikon/global.json` is created.
5. Replace `global.json` with invalid JSON, start again, and verify a
   `global.corrupt-<timestamp>.json` backup is created.
