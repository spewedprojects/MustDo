# Codebase Analysis: _MustDo_ To-Do Application

This document provides a thorough breakdown of the **MustDo** Android application architecture, mapping out files, system relationships, color/theme management, typography application, backup/restore operations, and details on recent structural refinements and helpers.

---

## 1. Directory Structure & File Mapping

The codebase follows an MVVM architecture utilizing Jetpack Compose for the UI layer, and Room Database for data persistence.

```
app/src/main/
├── AndroidManifest.xml                        # Main configuration, permissions, components registration
├── java/com/gratus/mytodo/
│   ├── MainActivity.kt                        # Host Activity, Navigation Drawer, dynamic Edge-to-Edge system bar icons
│   ├── components/
│   │   └── NotificationReceiver.kt           # BroadcastReceiver using goAsync() to post exact alarms notifications
│   ├── data/
│   │   ├── Task.kt                           # Room Entity: Task data schema
│   │   ├── TaskDao.kt                        # Data Access Object: Queries and DB updates
│   │   ├── TaskDatabase.kt                   # Room Database initializer
│   │   ├── TaskRepository.kt                 # Repository layer abstracting DAO operations
│   │   └── utils/
│   │       └── BackupHelper.kt               # Backup serializer/deserializer to JSON
│   └── ui/
│       ├── MainViewModel.kt                  # State-holder: Business logic, stats calculation, alarm registration
│       ├── components/
│       │   ├── FaintBackground.kt            # Draws custom canvas graphics/gradients based on theme
│       │   ├── StyledTextParser.kt           # Compiles markdown bold/italic/bullets into AnnotatedStrings
│       │   └── TaskAddDialog.kt              # Scrollable dialog for task addition and modification
│       ├── screens/
│       │   ├── HomeScreen.kt                 # Daily task list view, datepicker, and HorizontalPager
│       │   ├── HistoryScreen.kt              # Calendar groups, search/filters, and cumulative zoom gestures
│       │   ├── SettingsScreen.kt             # Dark/Light preferences, theme scheme selectors, and backup options
│       │   └── StatsScreen.kt                # Scrollable circular completion progress, streaks, and weekly chart
│       ├── theme/
│       │   ├── Color.kt                      # Defines core colors, priority levels, and scheme utilities
│       │   ├── Theme.kt                      # Integrates Light/Dark palettes into SoftTodoTheme
│       │   └── Type.kt                       # Houses default typography styles with global +2sp offset
│       └── utils/
│           ├── DateTimeUtils.kt              # Thread-safe date formatters respecting 12h/24h system clock
│           └── PinchGestureHelper.kt         # Helper function for tracking cumulative pinch-to-zoom gestures
└── res/
    ├── drawable/
    │   └── icon_v3.xml                       # Vector asset used for launcher and notification small icon
    ├── values/
    │   ├── colors.xml                        # Basic standard fallback native resource colors
    │   ├── strings.xml                       # Core localization properties
    │   └── themes.xml                        # Legacy XML styles providing parent compatibility
    └── xml/
        ├── backup_rules.xml                  # Rules for Android Auto Backup
        └── data_extraction_rules.xml         # Rules for cloud/device backup extraction
```

---

## 2. System Relationships & State Propagation

The app leverages a shared view model architecture to ensure state syncs instantly across all screens:

```mermaid
graph TD
    MA[MainActivity] --> VM[MainViewModel]
    MA --> Theme[SoftTodoTheme]
    VM --> Rep[TaskRepository]
    Rep --> DAO[TaskDao]
    DAO --> DB[(TaskDatabase)]

    VM -- "Flow<List<Task>>" --> Home[HomeScreen]
    VM -- "Flow<List<Task>>" --> Hist[HistoryScreen]
    VM -- "Flow<StatsData>" --> Stats[StatsScreen]
    VM -- "theme/scheme Flow" --> Sett[SettingsScreen]

    Sett -- "Triggers preference update" --> VM
    VM -- "Re-evaluates theme mode" --> MA
```

### Key Interactions:
1. **Shared ViewModel Scope:** The `MainActivity` instantiates `MainViewModel` via `by viewModels()`. This single instance is passed to all sub-screens (`HomeScreen`, `HistoryScreen`, `StatsScreen`, `SettingsScreen`), ensuring they all operate on the exact same states.
2. **Settings Propagation:** 
   - `SettingsScreen` invokes `viewModel.setTheme(mode)` and `viewModel.setColorScheme(scheme)`.
   - `MainViewModel` persists these settings in `SharedPreferences` (`soft_todo_prefs`) and publishes updates to `settingsTheme` and `settingsColorScheme` state flows.
   - `MainActivity` collects these flows as compose state. This triggers a recomposition of `SoftTodoTheme` and `FaintBackground`, changing the entire app's look instantly.
3. **Database-UI Reactive Loop:** All queries inside `TaskDao` return `Flow<List<Task>>`. Any operation (e.g. marking a task complete on the `HomeScreen`) writes to Room, which automatically forces the corresponding database flow to emit the new data. Screens instantly recompose with the updated task lists without manual refreshing.
4. **Alarms Synchronization:** When tasks are added, completed, updated, or deleted, `MainViewModel` schedules or cancels exact alarms using `AlarmManager`.

---

## 3. Backup and Restore Mechanism

Currently, backups are handled via raw JSON serialization and deserialization:

### Export Workflow
1. User clicks **Export to Device** in `SettingsScreen`.
2. The UI invokes `viewModel.exportBackup()`.
3. `exportBackup` queries `TaskDatabase` for all tasks via `getAllTasksDirect()` on `Dispatchers.IO`.
4. It iterates over the task list, converting each field into a `JSONObject` and adding them to a `JSONArray`.
5. The output is written to `todo_backup.json` inside the device's public `Downloads` directory via Android's `MediaStore`. Concurrently, a raw DB backup copy is saved to `todo_backup.db`.

### Import & Restore Workflow
1. User clicks **Import & Restore Backup** in `SettingsScreen` which opens the system file picker (`*/*`).
2. The selected file's magic bytes are read:
   - If they match `"SQLite format 3"`, the raw `.db` file is imported, overwriting `task_database`, and the app restarts.
   - Otherwise, the file is parsed as JSON, and the tasks are inserted into the database.
3. Alarms are rescheduled for any active, incomplete tasks.

---

## 4. Typography & Font Application

The app uses a centralized typography structure to maintain visual consistency across all screens:

- **Type.kt** maps material typography tokens to unified styles where every font size is increased by **+2sp** compared to standard system size rules.
- **AppFontSizes** contains helper size scales (ranging from `pico = 10.sp` to `headline = 26.sp`) that are used across all screens instead of hardcoded sizes.
- **Dynamic Zoom Sizes:** History screen text size scales dynamically based on the active pinch-to-zoom level through `AppFontSizes.titleForZoom(level)` and `AppFontSizes.bodyForZoom(level)`.

---

## 5. Theme & Color Management

Colors are managed inside `Theme.kt` and `Color.kt` through standard Compose parameters and style provider classes.

- **`SoftTodoTheme` Selection:** Translates setting codes to M3 ColorSchemes:
  - `"minimal"`: Indigo and lavender accents.
  - `"simple"`: strictly monochromatic (black/white).
  - `"colorful"`: Orchid purple + rose pastel with radial sweep animations.
  - `"system"`: Monet dynamic coloring (Android 12+).
- **Badge Colors:** Priority badges dynamically style their box container and borders based on the selected scheme (e.g. using `getMinimalPriorityColors()` or `getPriorityBoxColor()`).
- **Drawer Styling:** The navigation drawer uses a solid, non-transparent sheet background for `"minimal"` and `"colorful"` themes to prevent visual overlap issues.
- **Delete Dialog Styling:** The deletion confirmation alert dialog is styled with outlines matching the active theme's borders (black/white in Simple, slate/indigo in Minimal) to prevent it from blending into the background of monochromatic screens.

---

## 6. Recommended Helper Classes & Refinements

1. **`DateTimeUtils`:** Thread-safe utility helper that centralizes all Date/Time formatting and parsing operations. It queries `DateFormat.is24HourFormat(context)` to dynamically display reminder timestamps in either 12-hour or 24-hour format matching the system preferences.
2. **`PinchGestureHelper`:** Implements `detectPinchZoom` which accumulates relative zoom events over a single gesture cycle and triggers zoom-in or zoom-out callbacks only when threshold boundaries (e.g., > 1.25x or < 0.75x) are crossed, resolving slow gesture dead-zones.
3. **`NotificationReceiver`:** Extends `BroadcastReceiver` and uses `goAsync()` to run database lookups on a background coroutine while keeping the receiver process alive. Uses a local app drawable resource `R.drawable.icon_v3` for the notification's small icon.
4. **`USE_EXACT_ALARM`:** The app declares `USE_EXACT_ALARM` permission in its manifest. This grants the app the ability to schedule exact alarm events at install-time on Android 13+ without requiring manual user toggles in system settings.

---

## 7. Evolution & Fundamental Changes Summary

The application has undergone several key transitions since its initial version:

| Feature / Area | Initial Version | Current Refined Version |
| :--- | :--- | :--- |
| **Pinch-to-Zoom** | Zoom gestures were broken because they processed instantaneous frame-deltas, failing to register slow gestures. | Uses a custom `PinchGestureHelper` for smooth transitions across 4 custom zoom levels: Year (12-month grids, click to zoom), Month (week grids, click to zoom), Week (day grids with 1-line titles + summary, click to zoom), and Regular View (detailed task list). |
| **Reminder Dialog Flow** | Required the user to pick a date first, then pick a time. | Directly opens the `TimePickerDialog`, automatically applying the chosen time to the task's main target date. |
| **Landscape Usability** | Dialogs and Stats screens overflowed and got cut off in landscape mode. | Dialog content is fully scrollable. The Stats Screen utilizes weights for an adaptive layout with no vertical scrollbar (side-by-side top cards in portrait, stacked left cards in landscape; chart on bottom in portrait, chart on right in landscape). |
| **Notification Reliability** | Fails silently on Android 13+ due to lack of pre-granted exact alarm permissions. Queries database asynchronously in `BroadcastReceiver` causing process termination. | Uses the pre-granted `USE_EXACT_ALARM` permission, implements `goAsync()` in the BroadcastReceiver to protect background queries, and uses a local drawable to prevent system icon resource crashes. |
| **System Bar Icons** | Icons remained dark when the system was in light mode and the app was manually toggled to dark mode, making them invisible. | Dynamically checks calculated theme state in `LaunchedEffect` and calls `enableEdgeToEdge()` with custom `SystemBarStyle` configs on theme change. |
| **Time Format Preference** | Hardcoded to 12-hour format across the app. | Dynamically queries system settings using `is24HourFormat(context)` and formats both pickers and visual text labels accordingly. |
| **Layout Outlines & Dialogs** | Alert dialogs had default styling without borders, blending into monochromatic themes. | Applied custom borders to `AlertDialog` to match the outline properties of Clean Minimalism and Simple B&W themes. |
| **Stats Completion Metric** | Calculated rate and done ratios using all tasks, including those scheduled in the future. | Stats completion rate and "x of y Done" counts are filtered to only evaluate tasks up to today's date. |
| **History Records List** | Displayed future tasks alongside completed and past tasks. | Filters out future tasks from the History records by default, but displays them when the user explicitly searches/filters by a date or month pattern. |
| **Dialog State Rotation** | Closing the add/edit dialog on rotation caused data loss. | Moves dialog state and edited task items to `MainViewModel` and implements `rememberSaveable` on dialog input fields to persist state across device rotations. |
| **Alarm Time Info** | Hides alert time info from completed tasks or once the alarm time has passed. | Keeps the "Alert scheduled: ..." info text visible on the home task card even after it has triggered or the task has been marked complete. |
| **Header Date text** | Displayed formatted date in TopAppBar for all active dates. | Displays `"Today"` instead of formatted date in the TopAppBar of `MainActivity` if the active date matches the current system date. |
