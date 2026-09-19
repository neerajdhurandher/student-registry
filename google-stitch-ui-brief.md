# Student Registry UI Brief for Google Stitch

## 1. Project Detailed Description

**Project name:** Student Registry

**Platform:** Android mobile app

**Primary users:** Teachers, tutors, and small educational staff who need a simple way to manage student records from a phone.

**Core purpose:**  
+This app helps a teacher sign in with Google and manage their own student database. The app should feel modern, trustworthy, clean, and easy to use for daily academic administration. It is not a social app; it is a productivity tool for education.

**Main product goals:**
1. Let teachers securely sign in with Google.
2. Let each teacher create, view, edit, search, filter, and delete student records.
3. Allow basic student information management: name, age, contact, email, birthday, and notes.
4. Support optional cloud backup/sync.
5. Support birthday reminders and data export.
6. Keep the UI simple, mobile-first, fast, and accessible.

**Data managed per student:**
1. Name
2. Age
3. Contact number
4. Email
5. Birthday
6. Notes

**Functional behavior:**
1. Users must sign in before accessing student data.
2. After login, the teacher sees a searchable and filterable student list.
3. The teacher can add a new student or open a student record for details.
4. From the detail page, the teacher can edit or delete the student.
5. Settings allow dark theme, font scaling, notification toggle, cloud sync toggle, and sign out.
6. Export screen allows CSV and PDF export of the currently filtered list.

**Design direction:**
1. Professional, calm, academic, and trustworthy.
2. Clean card-based layout with strong hierarchy.
3. Minimal clutter; forms should be easy to scan.
4. Suitable for teachers who may use the app quickly during school hours.
5. Use a modern Material 3 Android style.

**Visual style hints from the current app:**
1. Primary color: strong blue
2. Secondary color: light blue
3. Light background: soft off-white / very light blue
4. Dark mode background: deep navy blue

**Suggested tone of the interface:**
1. Helpful
2. Clear
3. Efficient
4. Friendly but not playful

## 2. Screen-by-Screen UI Requirements

### A. Login Screen

**Purpose:**  
Allow the teacher to authenticate with Google before entering the app.

**Main content:**
1. App title: **Student Registry**
2. Supporting text explaining that Google sign-in is required to manage student records securely
3. One primary call-to-action button for Google sign-in

**Features:**
1. Clean centered layout
2. Strong first impression with simple branding
3. Large readable typography
4. Enough white space

**Buttons:**
1. **Sign in with Google**

**States to consider:**
1. Default state
2. Loading/sign-in in progress state
3. Error state area for sign-in failure message

---

### B. Student List Screen

**Purpose:**  
This is the main dashboard after login. It shows the teacher's student records and allows quick search, filtering, and navigation.

**Main content:**
1. Top app bar with title showing the teacher name
2. Search field
3. Age filter chips
4. Scrollable list of student cards
5. Floating action button for adding a student
6. Top bar action icons for export and settings

**Features:**
1. Search students by name, contact, or email
2. Filter students by:
   - All
   - Under 18
   - 18+
3. Tap a student card to open student details
4. Show empty state if no students match the filter

**Student card content:**
1. Student name
2. Short subtitle line with age and contact number

**Form elements:**
1. Search input field with placeholder/label like: **Search by name/contact/email**

**Buttons and actions:**
1. **Floating Add Button** with plus icon
2. **Export** icon button
3. **Settings** icon button
4. **Filter chips** for age categories
5. **Student card tap** action

**States to consider:**
1. Populated list
2. Empty list for first-time user
3. No search results state

---

### C. Add Student Screen

**Purpose:**  
Allow the teacher to create a new student record.

**Main content:**
1. Top app bar with back navigation
2. Form with multiple input fields
3. Full-width save button

**Form fields:**
1. **Name** (required)
2. **Age** (required, numeric)
3. **Contact**
4. **Email**
5. **Birthday** (currently text format: yyyy-MM-dd)
6. **Notes**

**Features:**
1. Easy-to-fill form layout
2. Strong input grouping and spacing
3. Clear required field indication
4. Mobile-friendly keyboard behavior

**Buttons:**
1. **Back**
2. **Save**

**States to consider:**
1. Blank new form
2. Validation error state for missing name or invalid age

---

### D. Edit Student Screen

**Purpose:**  
Allow the teacher to update an existing student record.

**Main content:**
1. Same layout and fields as Add Student
2. Existing values pre-filled

**Form fields:**
1. **Name**
2. **Age**
3. **Contact**
4. **Email**
5. **Birthday**
6. **Notes**

**Buttons:**
1. **Back**
2. **Save**

**Design note:**  
Keep Add and Edit as the same reusable form screen, with only the title changing:
1. **Add Student**
2. **Edit Student**

---

### E. Student Detail Screen

**Purpose:**  
Show the full information for a selected student and provide edit/delete actions.

**Main content:**
1. Top app bar with back navigation
2. Student full name as prominent heading
3. Detail rows or cards for:
   - Age
   - Contact
   - Email
   - Birthday
   - Notes
4. Action buttons at the bottom

**Features:**
1. Easy-to-scan detail layout
2. Strong emphasis on student name
3. Notes section can be visually separated

**Buttons:**
1. **Back**
2. **Edit**
3. **Delete**

**Dialog/confirmation needed:**
1. When Delete is pressed, show a confirmation dialog
2. Dialog title: **Delete student**
3. Dialog message should include the student name
4. Dialog buttons:
   - **Delete**
   - **Cancel**

---

### F. Settings Screen

**Purpose:**  
Allow the teacher to personalize the app and manage preferences.

**Main content:**
1. Top app bar with back navigation
2. Settings list / grouped preference layout
3. One section for appearance
4. One section for notifications and sync
5. Sign out button at the bottom

**Features:**
1. Dark theme toggle
2. Birthday notifications toggle
3. Cloud backup sync toggle
4. Font size slider
5. Current font scale indicator

**Form controls:**
1. **Switch** for Dark theme
2. **Switch** for Birthday notifications
3. **Switch** for Cloud backup sync
4. **Slider** for Font size

**Buttons:**
1. **Back**
2. **Sign out**

**States to consider:**
1. Standard settings state
2. Dark mode preview-friendly visuals

---

### G. Export Screen

**Purpose:**  
Allow the teacher to export the currently filtered student list.

**Main content:**
1. Top app bar with back navigation
2. Short explanatory text
3. Count of records currently in scope
4. Two large export buttons

**Features:**
1. Clearly explain that export uses the current filtered list
2. Show record count prominently
3. Keep actions simple and obvious

**Buttons:**
1. **Back**
2. **Export CSV**
3. **Export PDF**

---

## 3. App Navigation Flow

1. Login Screen
2. Student List Screen
3. Student Detail Screen
4. Add/Edit Student Screen
5. Settings Screen
6. Export Screen

**Flow details:**
1. After successful login, go to Student List
2. From Student List:
   - open Student Detail
   - open Add Student
   - open Settings
   - open Export
3. From Student Detail:
   - edit student
   - delete student
   - go back

## 4. UX Notes for Google Stitch

1. Design for Android phone screens first.
2. Use Material 3 patterns and components.
3. Keep forms clean and spacious.
4. Make the main dashboard feel productive and organized.
5. Use card-based presentation for student records.
6. Use a floating action button for adding a student.
7. Make empty states feel helpful, not blank.
8. Keep export and settings in the top app bar of the student list screen.
9. Keep typography readable for teachers of different age groups.
10. Make dark mode visually polished, not just inverted colors.

## 5. Preferred Deliverables from Stitch

Generate:
1. A polished login screen
2. A main student list/dashboard screen
3. A reusable add/edit student form screen
4. A student detail screen
5. A settings/preferences screen
6. An export screen
7. Consistent component styles for buttons, cards, form inputs, chips, switches, and dialogs
