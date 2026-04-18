# SkinPet 

## Workspace

**Github:**

* Repository: https://github.com/marta-almeida-bt0122/MobileAppDevelopment
* Releases: https://github.com/marta-almeida-bt0122/MobileAppDevelopment/releases

**Workspace:**

* https://upm365.sharepoint.com/sites/MobileAppDevelopment41

---

## Description

This application has been developed to raise awareness about environmental conditions and their impact on skin health through a gamified experience.

SkinCare Tamagotchi allows users to create a virtual skin character (e.g., dry, oily, or mixed skin type). The character's health (HP) dynamically changes based on real-time environmental data obtained from external APIs, including UV radiation, temperature, humidity, and air quality.

The app uses the user’s location to collect environmental data and evaluates how these conditions affect the skin. Based on this, the system provides personalized skincare recommendations (e.g., using sunscreen, hydration, or protection measures).

All interactions and environmental impacts are stored, allowing users to track their skin character evolution over time. Additionally, a ranking system compares users based on how well they maintain their character’s health.

The main objective of this project is to promote environmental awareness and healthy habits using gamification techniques.

---

## Screenshots and Navigation

### Main Activity

Displays the current status of the skin character, including HP, environmental conditions, and recommendations.

### Navigation Menu

Navigation Drawer to access all sections of the application.

### Profile

Shows user data and global score.

### Today's Activity

Displays real-time environmental data such as UV index, temperature, humidity, and air quality, along with skincare recommendations.

### Skin History

Shows historical data of the skin character (HP evolution and past environmental conditions).

### Ranking

Displays a leaderboard comparing users based on their performance.

---

## Features

### Functional Features

* Skin character creation (dry, oily, mixed)
* Real-time environmental monitoring (UV, temperature, humidity, air quality)
* Dynamic HP system based on environmental conditions
* Personalized skincare recommendations
* Notifications when conditions are harmful
* Ranking system between users
* Historical tracking of skin conditions and character evolution

### Technical Features

* Firebase Authentication (Google / Email login)
* Firebase Realtime Database (user data, ranking, character state)
* Room Database (local persistence of history)
* RESTful APIs:

  * OpenWeather API (temperature, humidity)
  * Open-Meteo API (UV index, air quality)
* Retrofit (API communication)
* Navigation Drawer (UI navigation)
* RecyclerView (data visualization)
* GPS location (device sensors)
* Notification system (alerts based on environmental conditions)

---

## How to Use

1. Launch the application.
2. Log in using email or Google authentication.
3. Create your skin character by selecting a skin type.
4. The app automatically retrieves environmental data based on your location.
5. The skin character’s health (HP) changes depending on conditions:

   * High UV → HP decreases
   * Extreme temperature → HP decreases
   * Good conditions → HP increases
6. Follow the recommendations to maintain your character’s health.
7. Check history and ranking to track your performance.

---

## Demo Video

Add here a short video demonstrating how the app works and its main features.

---

## Participants

* Marta Almeida ([marta.almeida@alumnos.upm.es](mailto:marta.almeida@alumnos.upm.es))

Workload distribution: 100%

---

## Releases

- [Latest Release (v1.0)](https://github.com/marta-almeida-bt0122/MobileAppDevelopment/releases)
