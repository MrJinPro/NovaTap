# NovaTap

NovaTap is an Android automation application for touch-based interactions, designed for controlled tap, multi-tap, area-tap, swipe, and scenario-based automation workflows.

The project is developed and maintained for the NovaTap product line.

## What the app does

- Single-tap automation with configurable timing and human-like variations
- Multi-tap automation with multiple target points
- Area-based automation with allowed and blocked zones
- Swipe and gesture automation
- Scenario builder for chained actions
- Local presets, diagnostics, and usage statistics

## Build and run

Requirements:
- Android Studio
- JDK 11+
- Android SDK with API 24+ support

Steps:
1. Open the project in Android Studio.
2. Let Gradle sync complete.
3. Create a local `.env` file if your build requires environment variables.
4. Run the app on an emulator or a physical device.

## Notes

- The app uses Accessibility Service and overlay permissions for automation features.
- Some monetization and ad-related flows may be prepared in the codebase but not yet fully enabled in production.
- The project is still being refined for stability and UX across older Android devices.
