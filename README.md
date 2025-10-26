# 🌱 LeafLoop

A gamified goal-tracking Android application that helps users achieve their objectives by breaking them down into manageable subgoals and growing virtual plants as they make progress.

## 📖 Overview

Goal Growth Tracker transforms productivity into a rewarding experience. Users create goals, receive AI-generated subgoals, and watch virtual plants grow as they complete tasks. When a goal is fully completed, the plant is added to a personal collection, providing visual motivation and a sense of achievement.

### Key Features

- 🎯 **Goal Creation**: Create custom goals with AI-powered subgoal generation
- 🌿 **Plant Growth**: Watch your plant grow through 4 stages as you complete subgoals
- ✅ **Progress Tracking**: Visual progress bars and checkboxes for each subgoal
- 🏆 **Plant Collection**: Collect fully-grown plants when goals are completed
- 📊 **Goal Navigation**: Browse through multiple active goals
- 📱 **Native Android Experience**: Smooth, intuitive mobile interface

## 🛠️ Tech Stack

### Frontend
- **Language**: Java
- **Framework**: Android SDK
- **UI Components**: Material Design, ConstraintLayout
- **HTTP Client**: OkHttp3
- **JSON Parsing**: org.json (built-in Android)

### Backend
- **API Base URL**: `http://10.0.2.2:8080/api/goals`
- **Endpoints**:
  - `GET /api/goals/` - Fetch all goals
  - `POST /api/goals/generate` - Generate goal with AI-powered subgoals
  - `PATCH /api/goals/{id}/subgoals/complete` - Mark subgoal complete
  - `DELETE /api/goals/{id}` - Delete completed goal
  - `GET /api/goals/by-title/{title}` - Fetch goal by title

### Design Patterns
- **Singleton Pattern**: CollectionManager for plant collection state
- **Model-View Pattern**: Separate model classes (MainGoalModel, SubgoalModel)
- **Async Networking**: OkHttp callbacks with UI thread handling

## 📦 Installation & Setup

### Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or newer
- **Java Development Kit (JDK)**: Version 8 or higher
- **Android SDK**: API Level 21 (Lollipop) or higher
- **Backend Server**: Running on `localhost:8080` (or update API URLs)

### Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd <project-directory>
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory
   - Wait for Gradle sync to complete

3. **Configure Backend Connection**
   
   Update the API URLs if your backend is not running on default localhost:8080:
   
   - Open `APIInteractor.java`
   - Modify `_APIurl` variable:
     ```java
     private String _APIurl = "http://YOUR_IP:YOUR_PORT/api/goals";
     ```
   
   - Update URLs in:
     - `CreateGoalActivity.java`
     - `MainPageActivity.java`
     - `PlantDetailActivity.java`

   **Note**: `10.0.2.2` is the Android emulator's alias for `localhost`

4. **Add Dependencies** (should be in `build.gradle`)
   ```gradle
   dependencies {
       implementation 'com.squareup.okhttp3:okhttp:4.9.0'
       implementation 'com.google.android.material:material:1.6.0'
       implementation 'androidx.appcompat:appcompat:1.4.0'
       implementation 'androidx.constraintlayout:constraintlayout:2.1.0'
   }
   ```

5. **Add Internet Permission**
   
   Ensure `AndroidManifest.xml` includes:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

6. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" (Shift + F10) in Android Studio
   - Select your device/emulator

## 🚀 Usage

### Creating a Goal

1. Tap the **"+"** button on the main screen
2. Enter your goal in the chat interface
3. Wait for AI to generate subgoals
4. Review subgoals in the popup
5. Return to main screen to start tracking

### Completing Subgoals

1. View current subgoal on the main screen
2. Check the checkbox when completed
3. Watch your plant grow!
4. Navigate between goals using arrow buttons

### Viewing Goal Details

1. Tap on the plant image
2. View complete goal breakdown
3. See all subgoals and their completion status

### Collecting Plants

1. Complete all subgoals for a goal
2. Watch the celebration animation
3. Plant automatically moves to collection
4. Goal is removed from active goals

## 📂 Project Structure

```
app/src/main/java/com/example/frontend/
├── APIInteractor.java           # API communication handler
├── CollectionActivity.java      # Plant collection grid view
├── CollectionManager.java       # Singleton for collection state
├── CreateGoalActivity.java      # Goal creation & AI chat
├── MainGoalModel.java          # Main goal data model
├── MainPageActivity.java       # Home screen & main UI
├── PlantDetailActivity.java    # Detailed goal view
└── SubgoalModel.java           # Subgoal data model
```

## 🎨 UI Components

- **Main Page**: Goal overview with plant visualization
- **Create Goal**: Chat-based goal creation interface
- **Plant Detail**: Comprehensive goal and subgoal breakdown
- **Collection**: Grid view of completed plants (feature in development)

## 🐛 Troubleshooting

### Common Issues

**1. Network Connection Failed**
- Ensure backend server is running
- Check if `10.0.2.2` is correct for your emulator
- Try using your computer's local IP for physical devices

**2. Goals Not Loading**
- Verify backend API is accessible
- Check logcat for detailed error messages
- Ensure JSON parsing matches backend response format

**3. UI Not Updating**
- Confirm `runOnUiThread()` is used for API callbacks
- Check if views are properly initialized in `onCreate()`

**4. Plant Animation Issues**
- Verify drawable resources exist (ic_plant_stage1-4)
- Check plant stage calculation logic

## 🔮 Future Enhancements

- [ ] Full collection gallery implementation
- [ ] Goal deadline reminders
- [ ] Social sharing of completed goals
- [ ] Custom plant varieties
- [ ] Progress statistics and analytics
- [ ] Offline mode with local caching

## 📄 License

This project was created for a hackathon. License details to be determined.



## 📞 Support

For issues or questions, please open an issue in the repository or contact the development team.

---

**Built with 💚 during [Hackathon Name]**
