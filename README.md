# Welcome to the ReLoop Mobile App Repository

Welcome to the official repository for the **ReLoop Mobile App**, developed as part of the Work-Integrated Learning (WIL) 2026 project by **The Avengers** team.

This Android mobile application was created for **ReLoop Technologies SA** to support digital recycling services such as user registration, pickup scheduling, recycling guidance, reward tracking, and waste classification simulation.

---

## Project Overview

The **ReLoop Mobile App** is a Kotlin-based Android application that allows users to manage recycling activities from their mobile devices. The app helps users schedule recycling pickups, view pickup history, earn reward points, redeem rewards, receive notifications, and access recycling guidance.

The application uses Firebase Authentication and Firebase Firestore as its primary backend, with local caching for offline capability and seamless user performance.

---

## Backend Integration & Database Schema (Firebase Firestore ERD)

The backend layer is organized inside the `com.example.reloop_technologies_mobile_app.backend` package, strictly isolating network operations and business logic from UI components.

### Firebase Firestore Entity Relationship Diagram (ERD)

![ReLoop Technologies ERD](https://raw.githubusercontent.com/google-gemini/android-studio/main/docs/assets/erd_diagram.png)

The application models the following Firestore collections:
1. `users`: Stores resident user profiles (`userId`, `firstName`, `lastName`, `email`, `phoneNumber`, `address`, `role`, `isActive`, `createdAt`, `updatedAt`).
2. `pickups`: Stores scheduled waste pickup requests (`pickupId`, `userId`, `agentId`, `scheduledDate`, `wasteType`, `wasteTypes`, `address`, `status`, `notes`, `createdAt`, `updatedAt`, `completedAt`, `cancelledAt`).
3. `rewards`: Stores user point balances and levels (`userId`, `pointsBalance`, `totalEarned`, `totalRedeemed`, `lastUpdated`).
4. `rewardItems`: Available voucher reward items (`rewardItemId`, `itemName`, `description`, `pointsCost`, `isActive`, `stockQuantity`).
5. `redemptionLogs`: Logs voucher redemptions (`redemptionId`, `userId`, `rewardItemId`, `pointsUsed`, `voucherCode`, `status`, `redeemedAt`).
6. `wasteLogs`: Records AI waste scans (`wasteLogId`, `userId`, `pickupId`, `wasteCategoryId`, `weightKg`, `source`, `modelLabel`, `confidenceScore`).
7. `wasteCategories`: Waste category metadata (`wasteCategoryId`, `categoryName`, `description`, `modelLabel`, `isActive`).
8. `notifications`: User notification alerts (`notificationId`, `userId`, `message`, `type`, `sentAt`, `isRead`).
9. `pointsTransactions`: Detailed point earn/redeem ledger (`transactionId`, `userId`, `transactionType`, `points`, `description`).
10. `guidanceContent`: Sorting and recycling guidelines (`guidanceId`, `wasteCategoryId`, `title`, `content`).

### Backend Architecture (`com.example.reloop_technologies_mobile_app.backend`)
- **`models/`**: Strongly-typed data models corresponding to Firestore collections.
- **`services/`**: Low-level services (`FirebaseAuthService`, `FirestoreService`) providing authentication, error mapping, and safe operation retries.
- **`repositories/`**: Clean repository interfaces (`UserRepository`, `PickupRepository`, `RewardsRepository`, `WasteScanRepository`, `NotificationRepository`) decoupling UI views from database interactions.
- **`BackendConfig`**: Configures API endpoints and environments (Development, Staging, Production).
- **`ResultState`**: Generic sealed class wrapper (`Success`, `Error`, `Loading`) for standard error handling.

---

## Main Features

### User Features

- User registration & Authentication
- Session restoration & Password recovery
- User dashboard & Total Points Tracking
- Schedule recycling pickups
- View pickup history (Upcoming & History filtering)
- View recycling guidance & sorting rules
- Track reward points & redeem vouchers
- Notifications management with clear-all state
- Edit user profile & Eco Impact statistics
- Simulated waste classification

---

## Technologies Used

- Kotlin
- Android Studio
- Firebase Authentication
- Firebase Firestore & Analytics
- XML Layouts & Material Design
- SharedPreferences Session Storage
