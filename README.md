# Study Mentor — AI Homework Tutor & Problem Solver

> An intelligent, multimodal Android educational assistant and math solver built with Jetpack Compose, CameraX, and Cloud Firestore.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.4.1-green.svg?logo=android)](https://developer.android.com/studio/releases/gradle-plugin)
[![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.02.01-purple.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min_SDK-29-orange.svg)](https://developer.android.com/tools/releases/platforms#10)
[![Target SDK](https://img.shields.io/badge/Target_SDK-37-orange.svg)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-orange.svg?logo=firebase)](https://firebase.google.com)

---

## 1. Overview

**Study Mentor** is a modern, native Android application engineered to help students learn and solve academic problems independently. By combining on-device computer vision utilities with an LLM completion backend, the application transforms a smartphone camera into an interactive math and science tutor.

### The Problem It Solves
When students encounter complex equations, homework questions, or textbook concepts, traditional search engines often return dense reference material without structured explanations. Study Mentor allows learners to point their camera at any printed or handwritten problem, isolate the target question with a resizable crop frame, and receive immediate, step-by-step guidance formatted for mobile readability.

### Target Audience
- Middle school, high school, and university students seeking on-demand homework guidance.
- Independent learners reviewing mathematics, physics, biology, and humanities concepts.

### Key Capabilities
- **On-Device Optical Problem Isolation:** Live CameraX viewfinder featuring an interactive, touch-draggable bounding frame that crops questions directly on the device prior to upload.
- **Real-Time Streaming Tutoring:** Token-by-token Server-Sent Events (SSE) chat responses providing direct, stepwise problem breakdowns.
- **Cross-Device Learning History:** Cloud Firestore persistence for conversation sessions and messages, enabling users to review previous questions.
- **Study Habit Analytics:** Continuous tracking of AI tutoring sessions, 7-day activity volume, and consecutive daily study streaks.

---

## 2. Key Features

### Implemented Features

- **Live Camera Scanner & Problem Viewfinder**
  - CameraX live preview with automatic sensor orientation compensation (0°, 90°, 180°, 270° Exif rotation).
  - Tap-to-focus functionality (`FocusMeteringAction`).
  - Flashlight / torch toggle support for low-light environments.
  - Zero-recomposition touch-draggable crop frame (`CropFrameState`) with corner resizing brackets and center translation.
  - Memory-safe image processing pipeline: bound pre-decoding (`inSampleSize`), proportional downscaling (max dimension 1024px), and 80% JPEG compression to prevent `OutOfMemoryError` (OOM).

- **Streaming Multimodal AI Mentorship**
  - Multimodal input support (text query and/or Base64 JPEG payload).
  - Real-time Server-Sent Events (SSE) parsing streaming token chunks over HTTP to Compose state.
  - Plain-text and Unicode formatting guardrails (`⊥`, `∥`, `⇒`) that strip raw Markdown and LaTeX delimiters for mobile screens.
  - Quick action suggestion pills (*"Solve step-by-step"*, *"Explain concept"*, *"Check my work"*).

- **Authentication & User Management**
  - Email and password sign-in and account creation powered by Firebase Authentication.
  - Real-time client-side input validation for email syntax, display name length, and password complexity.
  - Password strength meter (0 to 4 score) evaluating length, letters, numbers, and symbols.
  - Password reset emails and email verification dispatch.
  - Profile customization screen for editing public display names and managing sessions.

- **Library & Session History**
  - Cloud Firestore synchronization across devices (`users/{uid}/chat_sessions/{sessionId}/messages`).
  - Searchable conversation library with real-time title filtering.
  - Seamless session resumption: reloads full chat history back into the interactive solver screen.
  - Unauthenticated access gates prompting users to sign in.

- **Study Progress & Habit Tracker**
  - Dynamic 7-day activity bar chart illustrating daily AI study sessions.
  - Study streak calculator evaluating consecutive active days based on Firestore session timestamps.
  - Total conversation session counter.

### Incomplete & Planned Features

- **Quiz Challenges & Practice Module (`PracticeScreen.kt`):** Currently a static UI mockup containing hardcoded challenge cards and non-functional buttons (`/* TODO */`).
- **Subject Accuracy Analytics:** Metrics for quiz completion rates, correct answer counts, and subject breakdowns in the progress dashboard are currently placeholders awaiting backend quiz engine integration.

---

## 3. Screenshots

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="Study Mentor Application Icon" />
  <br>
  <em>Study Mentor Official Application Icon</em>
</p>

<!-- 
NOTE: UI screenshots will be added below. Recommended dimensions: 1080x2400 (9:20 aspect ratio).
-->

| Home & Progress Dashboard | Camera Scanner & Viewfinder | Streaming AI Tutor | Chat Library History |
| :---: | :---: | :---: | :---: |
| *(Add `docs/screenshots/home.png`)* | *(Add `docs/screenshots/camera_crop.png`)* | *(Add `docs/screenshots/chat_stream.png`)* | *(Add `docs/screenshots/library.png`)* |

| User Profile Screen | Sign In Screen | Account Registration | Practice Screen (Mockup) |
| :---: | :---: | :---: | :---: |
| *(Add `docs/screenshots/profile.png`)* | *(Add `docs/screenshots/login.png`)* | *(Add `docs/screenshots/register.png`)* | *(Add `docs/screenshots/practice.png`)* |

---

## 4. Technology Stack

### Programming Language
- **Kotlin `2.2.10`:** Modern language features, coroutines, and structured concurrency.

### Android Platform & Architecture
- **Compile SDK:** `37`
- **Target SDK:** `37`
- **Min SDK:** `29` (Android 10+)
- **Architecture Pattern:** MVVM (Model-View-ViewModel) + Repository Pattern + Unidirectional Data Flow (UDF).
- **Jetpack ViewModel `2.11.0`:** Manages screen states surviving configuration changes.
- **Kotlin Coroutines & Flow:** Asynchronous execution with `StateFlow`, `callbackFlow`, and `Dispatchers.IO`.

### UI Framework
- **Jetpack Compose (BOM `2026.02.01`):** 100% declarative UI toolkit without XML layouts.
- **Material Design 3:** Modern themed components, typography, dynamic colors, and surface elevations.
- **Navigation Compose `2.8.5`:** Single-activity navigation graph (`NavHost`).
- **Coil Compose `2.6.0`:** Asynchronous image loading for gallery photo previews.

### Camera & Hardware
- **CameraX `1.4.1`:** `camera-core`, `camera-camera2`, `camera-lifecycle`, and `camera-view` for live preview, touch metering, torch control, and image capture.
- **AndroidX FileProvider:** Secure temporary file URI generation in private app cache.

### Backend & Cloud Services
- **Firebase Authentication (`firebase-auth:24.2.0`):** Secure credential management, email verification, and password resets.
- **Cloud Firestore (`firebase-firestore` via Firebase BOM `33.4.0`):** Real-time NoSQL cloud database for chat histories and study logs.
- **Google Services Gradle Plugin `4.5.0`:** Firebase client credential processing.

### Networking & AI
- **HttpURLConnection (Java Standard Library):** Low-overhead HTTP/1.1 client for streaming completions.
- **Server-Sent Events (SSE):** Real-time token streaming using `org.json.JSONObject`.
- **Target LLM Model:** `vgpt/gpt-6.1-sol` (hosted via OpenAI-compatible proxy endpoint).

### Build Tools & Testing
- **Gradle `9.6.0`** with Android Gradle Plugin (AGP) `9.4.1`.
- **Secrets Gradle Plugin `2.0.1`:** BuildConfig field injection from `local.properties`.
- **JUnit `4.13.2`:** Local JVM unit test runner.
- **AndroidX Test / Espresso `3.7.0`:** On-device instrumentation testing support.

---

## 5. Application Architecture

Study Mentor adopts a clean, modular Model-View-ViewModel (MVVM) architecture with unidirectional data flow.

```mermaid
flowchart TD
    subgraph UI_Layer ["UI Layer (Jetpack Compose)"]
        MainActivity["MainActivity (Edge-to-Edge Host)"]
        MainScreen["MainScreen (NavHost + Bottom Bar + FAB)"]
        HomeScreen["HomeScreen"]
        CameraScreen["CameraScannerScreen"]
        LibraryScreen["LibraryScreen"]
        ProfileScreen["ProfileScreen"]
        AuthScreens["LoginScreen / RegisterScreen"]
    end

    subgraph ViewModel_Layer ["ViewModel Layer"]
        AuthVM["AuthViewModel"]
        ChatVM["ChatViewModel"]
        LibraryVM["LibraryViewModel"]
        ProgressVM["ProgressViewModel"]
    end

    subgraph Repository_Layer ["Repository & Data Layer"]
        ChatRepo["FirestoreChatRepository"]
        ProgressRepo["ProgressRepository"]
        AuthService["Firebase Authentication"]
        OpenAIClient["OpenAIApiClient (Singleton)"]
    end

    subgraph Remote_Layer ["Remote Services"]
        FirebaseAuthRemote["Firebase Auth"]
        FirestoreDB["Cloud Firestore (users/{uid})"]
        AIProxy["AI Completion Proxy (api.vilao.ai)"]
    end

    MainActivity --> MainScreen
    MainScreen --> HomeScreen & CameraScreen & LibraryScreen & ProfileScreen & AuthScreens

    HomeScreen -.-> ProgressVM & AuthVM
    CameraScreen --> ChatVM
    LibraryScreen --> LibraryVM & AuthVM
    ProfileScreen --> AuthVM
    AuthScreens --> AuthVM

    AuthVM --> AuthService --> FirebaseAuthRemote
    ChatVM --> ChatRepo & OpenAIClient
    LibraryVM --> ChatRepo
    ProgressVM --> ProgressRepo

    ChatRepo --> FirestoreDB
    ProgressRepo --> FirestoreDB
    OpenAIClient --> AIProxy
```

### Layer Responsibilities

1. **UI Layer (`com.example.study_mentor.ui`):**
   Composables observe immutable StateFlow objects and emit UI events. Screens handle user input, permission requests, touch gestures, and animations.
2. **ViewModel Layer:**
   Holds business logic, processes images asynchronously on background threads (`Dispatchers.IO`), and exposes UI states (`ChatUiState`, `AuthState`, `ProgressUiState`).
3. **Repository Layer (`com.example.study_mentor.data`):**
   Abstracts data operations. Wraps Firebase Firestore snapshot listeners into reactive Kotlin `Flow` streams (`callbackFlow`) and handles CRUD transactions.
4. **Network & Client Layer (`OpenAIApiClient.kt`):**
   Manages HTTP connections, builds multimodal JSON request payloads, and handles streaming Server-Sent Events line-by-line.

---

## 6. Project Structure

```
Study_Mentor/
├── gradle/
│   ├── libs.versions.toml             # Centralized dependency catalog
│   └── wrapper/                       # Gradle wrapper binaries and properties
├── build.gradle.kts                   # Top-level build file
├── gradle.properties                  # JVM parameters, caching, and Java home settings
├── settings.gradle.kts                # Project name and repository management
├── app/
│   ├── build.gradle.kts               # App-level build configurations and dependencies
│   ├── google-services.json           # Firebase credentials (not tracked in VCS)
│   └── src/
│       ├── test/java/com/example/study_mentor/
│       │   ├── AuthValidationTest.kt  # 15 Unit tests for form validation and strength scoring
│       │   └── ExampleUnitTest.kt     # Default JVM unit test
│       ├── androidTest/java/com/example/study_mentor/
│       │   └── ExampleInstrumentedTest.kt # Android instrumentation test
│       └── main/
│           ├── AndroidManifest.xml    # App permissions, application, and FileProvider declarations
│           ├── res/                   # Drawables, mipmaps, values, and XML configurations
│           └── java/com/example/study_mentor/
│               ├── MainActivity.kt    # Root ComponentActivity hosting MainScreen
│               ├── OpenAIApiClient.kt # Singleton client for streaming LLM completions
│               ├── GeminiApiClient.kt # Legacy placeholder (deprecated)
│               ├── data/
│               │   ├── ChatModels.kt  # Data models: ChatSession, ChatMessage
│               │   ├── FirestoreChatRepository.kt # Firestore chat operations & streams
│               │   └── ProgressRepository.kt      # Firestore study session listener
│               └── ui/
│                   ├── MainScreen.kt              # Top-level scaffold, navigation, bottom bar
│                   ├── HomeScreen.kt              # Overview banner, suggested prompts, progress
│                   ├── CameraScannerScreen.kt     # CameraX, resizable crop frame, chat sheet
│                   ├── ChatViewModel.kt           # Image compression, session & streaming state
│                   ├── LibraryScreen.kt           # Saved conversations list and search
│                   ├── LibraryViewModel.kt        # Chat session data binding
│                   ├── ProfileScreen.kt           # Account details, display name, password reset
│                   ├── PracticeScreen.kt          # Quiz challenge screen (UI mockup)
│                   ├── ProgressDashboardSection.kt# 7-day activity chart and streak display
│                   ├── ProgressViewModel.kt       # Activity metrics and streak calculations
│                   ├── LoginScreen.kt             # Sign-in form with validation
│                   ├── RegisterScreen.kt          # Account creation with strength meter
│                   ├── AuthViewModel.kt           # Firebase Auth logic & error handling
│                   ├── AuthValidation.kt          # Pure validation rules & scoring
│                   └── theme/                     # Compose Color, Shape, Theme, and Type
```

---

## 7. Installation and Setup

### Prerequisites
- **Android Studio:** Meerkat (2024.3+) or Ladybug (2024.2+) recommended.
- **Java Development Kit (JDK):** JDK 17, 21, or 25 installed and configured.
- **Android SDK:** Platform API 37 installed with Android SDK Build-Tools.
- **Physical Device or Emulator:** Running Android 10 (API level 29) or higher with camera support.

### Step-by-Step Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/Study_Mentor.git
   cd Study_Mentor
   ```

2. **Configure Firebase:**
   - Create a project on the [Firebase Console](https://console.firebase.google.com).
   - Enable **Firebase Authentication** (Email/Password provider).
   - Enable **Cloud Firestore** in production or test mode.
   - Register an Android app using package name: `com.example.study_mentor`.
   - Download the generated `google-services.json` file and place it in the `app/` directory:
     ```
     Study_Mentor/app/google-services.json
     ```

3. **Configure API Credentials:**
   Create a `local.properties` file in the project root directory (if not already generated by Android Studio) and supply your AI completion API key:
   ```properties
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   AI_API_KEY=your_actual_ai_proxy_api_key_here
   ```

4. **Open in Android Studio & Sync:**
   - Launch Android Studio and choose **Open an Existing Project**.
   - Select the `Study_Mentor` root directory.
   - Allow Gradle to sync dependencies and run indexers.

5. **Build and Run:**
   - To build a debug APK via CLI:
     ```bash
     ./gradlew assembleDebug
     ```
   - To execute local unit tests:
     ```bash
     ./gradlew testDebugUnitTest
     ```
   - Connect your device (with USB debugging enabled) and click **Run 'app'** in Android Studio.

---

## 8. Configuration

| Configuration Item | File Location | Purpose | Required Action |
| :--- | :--- | :--- | :--- |
| **Android SDK Path** | `local.properties` | Points Gradle to the local Android SDK directory. | Set `sdk.dir=/path/to/sdk`. |
| **AI API Key** | `local.properties` | Injected into `BuildConfig.AI_API_KEY` for bearer authentication. | Set `AI_API_KEY=<your_api_key>`. |
| **Firebase Services** | `app/google-services.json` | Configures mobile SDK app ID, project number, and Firestore database. | Download from Firebase Console and save to `app/`. |
| **JVM Toolchain** | `gradle.properties` | Configures memory allocation and Java installation path. | Ensure `org.gradle.java.home` points to a valid JDK installation. |

> **Security Notice:** Never commit `local.properties` or production `google-services.json` files containing live credentials to version control. Both are protected by `.gitignore`.

---

## 9. Application Workflow

### 1. Photo Homework Solving Flow

```mermaid
sequenceDiagram
    autonumber
    actor Student as Learner
    participant Scanner as CameraScannerScreen
    participant Crop as CropFrameState
    participant VM as ChatViewModel
    participant Client as OpenAIApiClient
    participant Firestore as Cloud Firestore

    Student->>Scanner: Opens Camera Scanner ("Ask AI")
    Scanner->>Crop: User drags viewfinder corners over problem
    Student->>Scanner: Taps Capture / Solve button
    Scanner->>Scanner: Captures image & crops bounding rect
    Scanner->>VM: sendImageAndPrompt(croppedUri, context, prompt)
    VM->>VM: Downsamples bitmap (max 1024px) & compresses to JPEG
    VM->>Firestore: Creates session & stores user message
    VM->>Client: askQuestion(prompt, base64Image)
    Client-->>VM: Streams SSE text chunks in real-time
    VM-->>Scanner: Updates UI character-by-character
    Client-->>VM: Stream completes ([DONE])
    VM->>Firestore: Saves final AI response to Firestore
```

### 2. User Authentication & Profile Flow
1. **Registration:** User enters name, email, and password. `AuthValidation` checks form constraints, and `AuthViewModel` initiates `createUserWithEmailAndPassword`.
2. **Profile & Verification:** Upon creation, display name is attached via `updateProfile` and an email verification link is dispatched.
3. **Session Persistence:** `FirebaseAuth.AuthStateListener` maintains user state across application restarts.
4. **Session Resumption:** Tapping any conversation card in the Library fetches messages ordered by timestamp and displays them in the chat interface.

---

## 10. API and External Services

### 1. AI Chat Completions Proxy
- **Endpoint:** `https://api.vilao.ai/v1/chat/completions`
- **Method:** `POST`
- **Protocol:** HTTP/1.1 with Server-Sent Events (`Accept: text/event-stream`)
- **Authorization:** `Bearer BuildConfig.AI_API_KEY`
- **Model:** `vgpt/gpt-6.1-sol`
- **Payload Structure:**
  ```json
  {
    "model": "vgpt/gpt-6.1-sol",
    "stream": true,
    "max_tokens": 1500,
    "messages": [
      {
        "role": "system",
        "content": "You are a concise, helpful study mentor. Provide direct, step-by-step answers..."
      },
      {
        "role": "user",
        "content": [
          {"type": "text", "text": "Solve this math problem..."},
          {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
        ]
      }
    ]
  }
  ```

### 2. Google Cloud Firestore
- **Structure:**
  ```
  users/{uid}/
    └── chat_sessions/{sessionId}/
          ├── (title, createdAt)
          └── messages/{messageId}/
                └── (id, role, text, timestamp)
  ```
- **Operations:** Asynchronous document creation (`set().await()`) and real-time query subscriptions (`addSnapshotListener`).

---

## 11. Testing

### Verified Test Suite
The project includes unit test coverage for pure business logic and authentication validation routines.

- **Unit Test Location:** `app/src/test/java/com/example/study_mentor/AuthValidationTest.kt`
- **Scope:** 15 test cases evaluating:
  - Email syntax and whitespace handling.
  - Name length boundaries (2–60 characters).
  - Login password acceptance.
  - Registration password strength constraints (minimum 8 characters, letters, and numbers).
  - Password confirmation matching.
  - Password strength scoring bounds.

### Executing Tests

To run local unit tests from the terminal:
```bash
./gradlew testDebugUnitTest
```

To run Android instrumentation tests on an active device/emulator:
```bash
./gradlew connectedDebugAndroidTest
```

*(Note: All 15 unit tests in `AuthValidationTest` have been executed and verified passing during codebase analysis.)*

---

## 12. Current Development Status

| Feature Area | Status | Remarks |
| :--- | :--- | :--- |
| **CameraX Viewfinder** | Complete | Supports live preview, tap focus, and torch toggle. |
| **Interactive Crop Frame** | Complete | Touch-draggable frame with zero recomposition overhead during drag. |
| **AI Streaming Solver** | Complete | Multimodal text + image streaming via Server-Sent Events. |
| **Firebase Auth Flow** | Complete | Login, registration, password reset, and profile updates. |
| **Chat History / Library** | Complete | Cloud Firestore sync and in-app search filter. |
| **Habit & Streak Analytics** | Complete | 7-day bar chart and consecutive study streak calculation. |
| **Daily Practice & Quizzes** | Mockup | `PracticeScreen.kt` has static UI; actions marked with `/* TODO */`. |
| **Subject Accuracy Metrics** | Incomplete | Quiz accuracy fields in progress dashboard are pending quiz backend. |

---

## 13. Contributing

Contributions are welcome. Please follow these steps:

1. **Fork the Repository:** Create a personal fork on GitHub.
2. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Adhere to Code Standards:**
   - Maintain idiomatic Kotlin coding conventions (`kotlin.code.style=official`).
   - Keep UI components decoupled within Jetpack Compose guidelines.
   - Write unit tests for new business logic or validation rules.
4. **Run Verification:** Ensure all existing unit tests pass before committing:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. **Open a Pull Request:** Submit a clear, descriptive PR targeting the `main` branch.

---

## 14. License

A formal software license has not yet been specified for this project. All rights are reserved by the original authors. If you plan to use or redistribute this code, please reach out to the project maintainers.

