# TO-DOs FOR _MustDo_ #1 (COMPLETED):
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

**_All the above tasks were marked done on 07/06/2026_**
*********

# NEW TODO #2 (COMPLETED):

### 1. Zoom pinch doesn't work:  - (DONE)
  - Identify the root cause and assess whether it's ideal to make a separate helper class to handle this function so that it's maintenance friendly.  - (DONE)

### 2. Task dialog: - (DONE)
  - The range picker needs to be changed and replaced with modern one. (But the issue is ComponentAvtivity vs AppCompatActivity, due to MDC). - (DONE)
  - Setting reminder should jump straight to time picker and not date first then time. - (DONE)
  - Make the Dialog **CONTENT** scrollable; so that is usable in landscape mode. - (DONE)

### 3. Overall app: - (DONE)
  - At every instance of time/date/range picker in the app, should follow the system default when it comes to 12hr clock or 24hr clock and the first day of week for date/range pickers. - (DONE)
  - Notification system isn't working as intended; when the time of notifying arrives, there are no notifications sent out by the app even while full permissions are granted. - (DONE)
  - When system is in light mode - systembar icons are in dark, and the app is in dark mode (set manually) - the systembar icons remain dark, they should compliment the theme mode of the app. - (DONE)
  - The delete confirmation dialog also has to follow the app's current theme and color scheme, it currently does not. - (DONE)

### 4. History screen and stats screen: - (DONE)
  - Disable future dates tasks history screen. - (DONE)
  - Make stats screen content scrollable so that it can be usable in landscape mode. - (DONE)
  - Total Count of tasks-$y (in Completion rate card "$x of $y Done") should only show total until today's date in Stats screen. - (DONE)

### 5. Codebase analysis:
  - Update the @codebase_analysis.md with latest findings, structure and workings of the application. - (DONE)
  - Add a summary section to it; in it, explain how fundamentally have things changed over time - use the current codebase analysis file to compare against. - (DONE)


All the above tasks were marked done on 10/06/2026 0015hrs.
*********

# TODO #3 (COMPLETED):

### 1. Make home screen widget: - (DONE)
  - That shows today's tasks. - (DONE)
  - Has ability to mark tasks as completed. - (DONE)
  - Shows priority level - (DONE)

### 2. Task dialog: - (DONE)
  - Reminder notification: Ability to repeat itself after $x minutes. Button for this can grouped along with existing "Schedule" button. Repeat 1x/2x/3x/4x with certain interval. - (DONE)
  - Add interval setting in settings screen, this shall be applied to all intervals. - (DONE)

### 3. History screen: - (DONE)
  - The pinch gesture and navigation drawer gesture interfere with each other, leading to pinch to occur in specific axis to work properly. - (DONE)
    - Since two taps are need to pinch, single tap swipe gesture should automatically be disregarded when this zooming operation is going on.
  - Along with this, add 2 floating action buttons (horizontally placed) at the bottom right of this screen with +/- zoom buttons to do the zooming is user is not in position to use the gesture. - (DONE)

**_All the above tasks were marked done on 10/06/2026 1315hrs_**
*********

# TODO #4:

### 1. Widget:  - (DONE)
  - Add widget dark mode
  - Rectify proguard-rules for preservation of classes during minification - why release build widget gave issues.

### 2. Notification: - (DONE)
  - Add snooze-like functionality - mark complete button and stop button.

### 3. Homescreen task items: - (DONE)
  - Rectify alert time changing as per the repeated times.
  - Add suspended table in database to stop the alert (snooze/dismiss-like) but keep the record.

**_All the above tasks were marked done on 10/06/2026 2240hrs_**
*********

# TODO #5:

### 1. Task: - (DONE - 21/06/2026)
  - Add option for sub-tasks. - (DONE - 21/06/2026)
  - Add option for category. Via tags. -  Category wise groups will be visible on main screen. - (DONE - 21/06/2026)
  - _**Broad Directions for subtasks and category:**_
    - **Task Item:**
      - Group Tasks into Large Category Cards like we currently have for singular task item.
      - On left, colored (according to color scheme) category icon; next to it, category/tag label; on right,number of tasks in the category and on right of this - a dropdown button to collapse/expand the category. It can more or less look like this: `[icon] Personal       [2]   ▼`
      - Tasks shall look exactly like they do right now, with sub-tasks (if present) shall be displayed as nested list items but below the description (if any). These sub-tasks shall have their own check/uncheck icon like what we have for widget, while the main task will retain existing check/uncheck icons without changes.
      - If all subtasks are marked checked, the main task will automatically be checked. This shall follow the existing method to indicate the same - strikethrough and gray/monochrome indicating disabled.
    - **Add task dialog:**
      - **Category:**
        - Two column layout at the place of priority section. On left, will be existing priority selection; and on right, will be Category selection.
        - Add 3-5 inbuilt categories/tags (Errands, Work, Personal, Health, Learning, etc.) and add the ability to add a custom category/tag via "+" button. The icons for these tags/categories shall be used from an existing pool of icons - for custom tag/category, if the keywords are matching a known category give it that icon and if not, use generic tag icon.
        - The tags/category chips shall also be colored (according to color scheme and should match with color on task item list) and should have a "X" button to basically delete the tag/category.
      - **Sub-tasks:**
        - Below Priority/Category, a card appearance will be there (or without card, if ti doesn't fit the aesthetic):
        - > `Sub-tasks                     0/3`
        - > ` Research options          X`
        - > ` Compare & shortlist       X`
        - > ` Make decision             X`
        - `+ Add sub-task`
    - If a task is added without assigning it a certain category, display it on main screen like current behavior. - (DONE - 21/06/2026)
    - This would require addition of column/s for tasks table, yes. But assess whether you need to add a new table to database to store categories. - (DONE - 21/06/2026)
    - All these new changes shall also be visible for task edit dialog. - (DONE - 21/06/2026)
    - Copy/Paste shall also include these new additions. - (DONE - 21/06/2026)

  - Copy/paste actions: - (DONE - 16/06/2026)
    - Show "Copy" button only when editing a task, in the same row as Footer actions, on left. Show "Paste" button only when adding a new task in the same place as mentioned for "Copy" button. - (DONE)
    - Show "Paste" button (with icon) as a floating button next to or above the add task floating action button on screens except "Today". This will basically paste the copied task to this new date. - (DONE)

### 2. Home screen: - (DONE - 16/06/2026)
  - Just as "Today" is visible to today's date. Make it so that "Yesterday" and "Tomorrow" for the respective dates. - (DONE)

### 3. Notification: - (DONE - 22/06/2026)
  - Add new button to notification - "Snooze". Hitting snooze will pop up system dialog that asks the duration (5/10/15/30mins).
  - The snooze function is different from the existing "Stop" and "Mark Complete" functions. Snooze will basically pause the reminder system for the selected duration, on resuming, the repetition of the reminder will continue at its own set duration.
  - This duration can be written to prefs file only, no need to write it to the database since it will carry no meaning. Or if there is a better way of doing it, show me.

**_All the above tasks were marked done on 21/06/2026_**
*********

# TODO #6:

### 1. Copy/paste function: - (DONE - 26/06/2026)
 - Need to change the current logic of appearance of paste action button on all dates except today.
 - The logic should be more like: it should only appear on all dates except the date from which the task is copied from.
 - Action button animation: it shouldn't preload on already, only when the respective date pages are opened. It should pop up from behind the add task button, and should pop back down behind the add task button.

### 2. Notifications and Reminders:
 - Currently, the notifications pop up and show to use a regular notification with system default notification sound.
 - I want to add new option in add dialog to let the user choose the scheduled reminder to be an alarm sort of thing or just notification.
 - Consequently, some way of managing alarm tones should be baked in the settings; either custom or access system default options.
 - If Alarm: A new UI screen that will pop on screen to show the task, with the existing buttons - "Mark Complete","Stop" and "Snooze". The behavior will be same as is exhibited by notifications. The only difference is the screen and tone. For the sake of aggressively reminding the user of the task.
 - Assess how can this be handled if the screen is locked?