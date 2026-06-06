# TO-DOs FOR _MustDo_:
### 1. Export options:
  - Change to export in JSON and/or DB, instead of parsing the data in JSON and exporting that.
  - Remove the option of "Export to Clipboard".
  - Import to open a file picker to restore from JSON/DB, instead of requiring to past textual data in the text input.

### 2. Theme changes:
  - B&W theme:
    - Should be monochromatic. Need to remove all colors expect for priority and colors in settings screen. Certain buttons still have the fallback purple color.
    - The drawer needs to be pure white.
  - Clean Minimalism & Pastel Colorful have transparent drawer - rectify that.
  - Apart from colors, all themes should essentially look the same, for each component in the entire app.
  - Centralize the colors as well, with the correct documentation so that I can further refine it myself.

### 3. Overall app:
  - Text and icon scaling seems to be all over the place (currently they are mostly smaller), needs to be unified.
    - I want the respective font sizes across the app to 2sp higher than what it currently is.
    - Unify/Centralize this (typography?) in a separate resource so that is it easier for me to settle on a certain size and so that I don't have to manually make changes at every instance of text view.  
  - Remove the Emojis used - (DONE)
  - Refine and improve navigation stack; going back from other screens should not exit the app (_current behavior_), it should go back to main screen.

### 4. Task adding dialog:
  - Long-pressing the task will reopen the task dialog but to edit the task.
  - "Also add to other dates" shall show only 3 future dates and then a date range picker, which will allow to select single date or a range of dates.

### 5. Main screen Tasks list:
  - Find a better icon to check the task instead of the existing checkmark (This is my task, you can ignore this).
  - Increase opacity of the checked tasks to about 0.65
  - Delete button should ask for a confirmation dialog to avoid accidentally deleting tasks.
  - Swiping left/right between dates shall be smooth sliding.

### 6. History Screen:
  - No point in having a zooming function to scale up/down list items remove the zoom level control buttons and do the following:
    - Currently, pinching or zooming just scales the list items (makes them physically smaller or larger). Instead, I want the zoom level to trigger a structural change in how data is grouped and rendered.

        Please write the code to handle 4 distinct zoom states based on pinch gestures (or a zoom scale variable from 0 to 3):
        1. Level 0 (Year View): Groups all completed tasks globally by Year. Tasks should be rendered as tiny, compact visual indicators or color-coded blocks.
        2. Level 1 (Month View): Groups tasks by Month. Medium-sized list items showing short text snippets.
        3. Level 2 (Day View - Default): Groups tasks by exact Completion Date. Standard detailed list items.
        4. Level 3 (Expanded View): Keep date headers. Shows a dense, continuous vertical stream or grid of tasks with full details, notes, and tags visible.

        Requirements:
        - Track the zoom level using a gesture handler (pinch scale threshold).
        - Smoothly transition or cross-fade between these 4 layout states when thresholds are crossed.
        - Map my historical task array dynamically based on the active state.

### 7. 