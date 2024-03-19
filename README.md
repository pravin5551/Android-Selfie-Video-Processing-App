CameraX Video Recorder App
Overview

This Android application demonstrates the implementation of a simple video recorder using CameraX, a Jetpack library that provides an easy-to-use API to access camera features. The app allows users to record videos with options for recording audio and saving the video to the device's external storage.
Features

    Video recording with CameraX library
    Support for recording audio with video
    Saves recorded video to device storage using MediaStore
    Timer to limit recording time to 30 seconds
    Start and stop recording functionality
    UI updates to reflect recording state

Dependencies

    Room Database: Used for data persistence.
        implementation "androidx.room:room-runtime:$room_version"
        kapt "androidx.room:room-compiler:$room_version"

    Coroutines: For managing asynchronous operations.
        implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1"

    Navigation: For navigating between fragments.
        implementation "androidx.navigation:navigation-fragment-ktx:$nav_version"
        implementation "androidx.navigation:navigation-ui-ktx:$nav_version"

    Lifecycle: Provides ViewModel and LiveData support.
        implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
        implementation "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version"
        kapt "androidx.lifecycle:lifecycle-compiler:$lifecycle_version"

    CameraX: Core library and extensions for camera functionality.
        implementation "androidx.camera:camera-core:${camerax_version}"
        implementation "androidx.camera:camera-camera2:${camerax_version}"
        implementation "androidx.camera:camera-lifecycle:${camerax_version}"
        implementation "androidx.camera:camera-video:${camerax_version}"
        implementation "androidx.camera:camera-view:${camerax_version}"
        implementation "androidx.camera:camera-mlkit-vision:${camerax_version}"
        implementation "androidx.camera:camera-extensions:${camerax_version}"

Implementation Details

    MainActivity: This is the main activity where the camera preview is displayed using CameraX. It also contains the logic for starting and stopping video recording.

    Video Recording: Video recording is handled using the CameraX VideoCapture and Recorder classes. The startRecording() function prepares the video capture with necessary options, such as file name and audio recording. It then starts the recording when the user clicks the record button.

    Timer: A timer is used to limit the recording time to 30 seconds. The startTimer() function updates the UI with the elapsed time and stops recording when the time limit is reached.

    Permissions: The app requests camera and audio recording permissions from the user at runtime.

How to Use

    Launch the app on an Android device.
    Grant camera and audio permissions when prompted.
    The camera preview will be displayed.
    Click the "Record" button to start recording.
    The recording will stop automatically after 30 seconds or when the "Stop" button is clicked.
    The recorded video will be saved to the device's external storage.

Notes

    Ensure that the app has the necessary permissions to access the camera and record audio.
    Videos are saved in the "Movies/CameraX-Video" directory on the device's external storage.
    This app demonstrates basic video recording functionality using CameraX. Additional features and optimizations can be implemented based on specific requirements.

GitHub Repository

The code for this project can be found on GitHub: [CameraX-Video-Recorder](https://github.com/pravin5551/Android-Selfie-Video-Processing-App)

Feel free to reach out for any further assistance or clarification.
