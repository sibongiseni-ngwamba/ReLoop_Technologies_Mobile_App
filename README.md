# Welcome to the ReLoop Mobile App Repository

Welcome to the official repository for the **ReLoop Mobile App**, developed as part of the Work-Integrated Learning (WIL) 2026 project by **The Avengers** team.

This Android mobile application was created for **ReLoop Technologies SA** to support digital recycling services such as user registration, pickup scheduling, recycling guidance, reward tracking, and waste classification simulation.

---

## Project Overview

The **ReLoop Mobile App** is a Kotlin-based Android application that allows users to manage recycling activities from their mobile devices. The app helps users schedule recycling pickups, view pickup history, earn reward points, redeem rewards, receive notifications, and access recycling guidance.

The application uses a local Room database, meaning it can run without Firebase or a live online server. This makes it suitable for academic demonstration, testing, and local development.

---

## Main Features

### User Features

- User registration
- User login
- Persistent login session
- User dashboard
- Schedule recycling pickups
- View pickup history
- View recycling guidance
- Track reward points
- Redeem rewards
- View notifications
- Edit user profile
- Simulated waste classification

### Admin Features

- Admin login
- Admin dashboard
- View total users
- View total pickups
- Manage users
- Manage pickup requests
- Update pickup status
- Manage reward items
- View system reports

---
## Technologies Used

- Kotlin
- Android Studio
- XML Layouts
- AzureSQL
- ViewModel
- LiveData
- RecyclerView
- Material Components
- SharedPreferences

---

## Project Architecture

The app follows the **MVVM architecture pattern**:

```text
Model - View - ViewModel
