# TO-DOs FOR _MustDo_:
### 1. Export options:
  - Change to export in JSON and/or raw DB, instead of parsing the data in JSON and exporting that. - (DONE)
  - Remove the option of "Export to Clipboard". - (DONE)
  - Import to open a file picker to restore from JSON/DB, instead of requiring to past textual data in the text input. - (DONE)

### 2. Theme changes:
  - B&W theme:
    - Should be monochromatic. Need to remove all colors expect for priority and colors in settings screen. Certain buttons still have the fallback purple color. - (DONE)
    - The drawer needs to be pure white. - (DONE)
  - Clean Minimalism & Pastel Colorful have transparent drawer - rectify that. - (DONE)
  - Apart from colors, all themes should essentially look the same, for each component in the entire app. - (DONE)
  - Centralize the colors as well, with the correct documentation so that I can further refine it myself. - (DONE)

### 3. Overall app:
  - Text and icon scaling seems to be all over the place (currently they are mostly smaller), needs to be unified. - (DONE)
    - I want the respective font sizes across the app to 2sp higher than what it currently is. - (DONE)
    - Unify/Centralize this (typography?) in a separate resource so that is it easier for me to settle on a certain size and so that I don't have to manually make changes at every instance of text view. - (DONE)
  - Remove the Emojis used - (DONE)
  - Refine and improve navigation stack; going back from other screens should not exit the app (_current behavior_), it should go back to main screen. - (DONE)

### 4. Task adding dialog:
  - Long-pressing the task will reopen the task dialog but to edit the task. - (DONE)
  - "Also add to other dates" shall show only 3 future dates and then a date range picker, which will allow to select single date or a range of dates. - (DONE)

### 5. Main screen Tasks list:
  - Find a better icon to check the task instead of the existing checkmark (This is my task, you can ignore this).
  - Increase opacity of the checked tasks to about 0.65 - (DONE)
  - Delete button should ask for a confirmation dialog to avoid accidentally deleting tasks. - (DONE)
  - Swiping left/right between dates shall be smooth sliding. - (DONE)

### 6. History Screen:
  - No point in having a zooming function to scale up/down list items remove the zoom level control buttons and do the following: - (DONE)
    - Currently, pinching or zooming just scales the list items (makes them physically smaller or larger). Instead, I want the zoom level to trigger a structural change in how data is grouped and rendered. - (DONE)

        Please write the code to handle 4 distinct zoom states based on pinch gestures (or a zoom scale variable from 0 to 3): - (DONE)
        1. Level 0 (Year View): Groups all completed tasks globally by Year. Tasks should be rendered as tiny, compact visual indicators or color-coded blocks. - (DONE)
        2. Level 1 (Month View): Groups tasks by Month. Medium-sized list items showing short text snippets. - (DONE)
        3. Level 2 (Day View - Default): Groups tasks by exact Completion Date. Standard detailed list items. - (DONE)
        4. Level 3 (Expanded View): Keep date headers. Shows a dense, continuous vertical stream or grid of tasks with full details, notes, and tags visible. - (DONE)

        Requirements:
        - Track the zoom level using a gesture handler (pinch scale threshold). - (DONE)
        - Smoothly transition or cross-fade between these 4 layout states when thresholds are crossed. - (DONE)
        - Map my historical task array dynamically based on the active state. - (DONE)

All the above tasks were marked done on 07/06/2026
*********

Next TODO:
### 1. Zoom pinch doesn't work.

### 2. Task dialog:
  - The range picker need to be changed and replaced with modern one. (But the issue is ComponentAvtivity vs AppCompatActivity, due to MDC).
  - Setting reminder should jump straight to time picker and not date first then time.

### 3. Overall app:
  - At every instance of time/date/range picker in the app, should follow the system default when it comes to 12hr clock or 24hr clock and the first day of week for date/range pickers.
  - Notification system isn't working as intended; when the time of notifying arrives, there are no notifications sent out by th app.
  - When system is in light mode - systembar icons are in dark, and the app is in dark mode (set manually) - the systembar icons remain dark, they should compliment the theme mode of the app.

### 4. History screen and stats screen:
  - Disable future dates tasks history screen.
  - 
  - Total Count of tasks-$y (in Completion rate card "$x of $y Done") should only show until today's date in Stats screen.