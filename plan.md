I want to build an Android app using Kotlin that allows teachers to store and manage student details. The app will have the following features:
1. ** Google Sign-In**: Teachers can sign in using their Google accounts for secure access.
2. ** Add Student Details**: Teachers can add new student details, including name, age, contact and other information.
3. ** View Student List**: Teachers can view a list of all students they have added, with options to search and filter the list.
4. ** Edit Student Details**: Teachers can edit existing student details if there are any changes.
5. ** Delete Student Details**: Teachers can delete student details if they are no longer needed.
6. ** Data Storage**: The app will use a local database (like Room) to store student details securely on the device and if device is connected to internet then sync the data to Google Firebase for backup and remote access.
7. ** User Interface**: The app will have a user-friendly interface with intuitive navigation and responsive design to ensure a smooth user experience.
8. ** Notifications**: The app will send notifications to teachers for important updates, such as reminders of upcoming birthday of students. 
9. ** Data Export**: Teachers can export student details to a CSV or PDF file for offline access or sharing with others.


Technical Requirements:
- **Programming Language**: Kotlin
- **Development Environment**: Android Studio
- **Database**: Room for local storage, Firebase for cloud backup
- **Authentication**: Google Sign-In for secure access
- **UI Framework**: Jetpack Compose for building the user interface
- **Notifications**: Use Android's Notification Manager to handle notifications


UI Design:
- The app will have a clean and modern design, with a focus on usability and accessibility following Material Design guidelines.
- The main screens will include:
1. **Login Screen**: For Google Sign-In authentication.
2. **Student List Screen**: Displays a list of students with options to search and filter.
3. **Add/Edit Student Screen**: Form for adding or editing student details.
4. **Student Detail Screen**: Displays detailed information about a selected student.
5. **Settings Screen**: Allows teachers to manage app settings, including notification preferences
6. **Export Screen**: Provides options to export student details to CSV or PDF.
7. **Theme and Accessibility Options**: Allows users to switch between light and dark themes and adjust font sizes for better accessibility.
8. Use blue and white color scheme for a clean and professional look, with clear typography and intuitive icons for easy navigation.


APP Flow: 
1. **Login**: Teacher opens the app and signs in using Google Sign-In.
2. **Student List**: After successful login, the teacher is directed to the Student List screen where they can view all students.
3. **Add Student**: Teacher can click on an "Add Student" button to navigate to the Add Student screen, fill in the details, and save. (this shouldn't be the bottom sheet, it should be a separate form screen)
4. **Edit Student**: Teacher can select a student from the list to view details and edit them if necessary.
5. **Delete Student**: Teacher can delete a student from the Student Detail screen.(ask for confirmation before deletion)
6. **Export Data**: Teacher can export student details from the Student List screen based on current filtered data to CSV or PDF.

