# Welcome to the ReLoop Mobile App Repository

Welcome to the official repository for the **ReLoop Mobile App**, developed as part of the Work-Integrated Learning (WIL) 2026 project by **The Avengers** team.

This Android mobile application was created for **ReLoop Technologies SA** to support digital recycling services such as user registration, pickup scheduling, recycling guidance, reward tracking, and waste classification simulation.

---

## Project Overview

The **ReLoop Mobile App** is a Kotlin-based Android application that allows users to manage recycling activities from their mobile devices. The app helps users schedule recycling pickups, view pickup history, earn reward points, redeem rewards, receive notifications, and access recycling guidance.

The application uses Firebase Authentication and Firebase Firestore as its primary backend, with local caching for offline capability and seamless user performance.

---

## Backend Integration & Database Schema (Firebase Firestore ERD)

The backend layer is organized inside the `com.example.reloop_technologies_mobile_app.backend` package, strictly isolating network operations and database interactions from UI components.

### Firebase Firestore Entity Relationship Diagram (ERD)

```
                       +----------------------+
                       |        users         |
                       +----------------------+
                       | PK  userId           |
                       |     firstName        |
                       |     lastName         |
                       |     email            |
                       |     phoneNumber      |
                       |     address          |
                       |     role             |
                       |     isActive         |
                       |     createdAt        |
                       |     updatedAt        |
                       |     profileImageUrl  |
                       +----------+-----------+
                                  |
            +---------------------+---------------------+---------------------+---------------------+
            | 1                   | 1                   | 1                   | 1                   | 1
            v 0..1                v 0..*                v 1                   v 0..*                v 0..*
     +--------------+      +--------------+      +--------------+      +--------------+      +-------------------+
     |    agents    |      |   pickups    |      |   rewards    |      |  wasteLogs   |      |   notifications   |
     +--------------+      +--------------+      +--------------+      +--------------+      +-------------------+
     | PK agentId   |      | PK pickupId  |      | PK userId    |      | PK wasteLogId|      | PK notificationId |
     | FK userId    |      | FK userId    |      |    ptsBalance|      | FK userId    |      | FK userId         |
     |    name      |      | FK agentId   |      |    totalEarned|     | FK pickupId  |      |    title          |
     |    phone     |      |    schedDate |      |    totalRedmd|     | FK wasteCatId|      |    message        |
     |    zone      |      |    status    |      |    lastUpdated|     |    weightKg  |      |    type           |
     +-------+------+      +------+-------+      +------+-------+      +------+-------+      |    sentAt         |
             |                    ^                     |                     ^              |    isRead         |
             | 1                  | 0..1                | 1                   | 0..*         |    relatedPickupId|
             +--------->----------+                     v 0..*                |              +-------------------+
                       0..*                             |                     |
                                                        v                     |
                                             +--------------------+           |
                                             |   redemptionLogs   |           |
                                             +--------------------+           |
                                             | PK redemptionId    |           |
                                             | FK userId          |           |
                                             | FK rewardItemId    |           |
                                             |    pointsUsed      |           |
                                             |    voucherCode     |           |
                                             +---------+----------+           |
                                                       |                      |
                                                       v 0..*                 |
                                             +---------+----------+           |
                                             |   rewardItems      |           |
                                             +--------------------+           |
                                             | PK rewardItemId    |           |
                                             |    itemName        |           |
                                             |    pointsCost      |           |
                                             +--------------------+           |
                                                                              |
                                             +--------------------+           |
                                             | wasteCategories    |----->-----+
                                             +--------------------+
                                             | PK wasteCategoryId |
                                             |    categoryName    |
                                             +---------+----------+
                                                       |
                                                       v 0..*
                                             +---------+----------+
                                             |  guidanceContent   |
                                             +--------------------+
                                             | PK guidanceId      |
                                             | FK wasteCategoryId |
                                             +--------------------+
```

### Firestore Collections & Field Mapping

#### 1. `users` (User Profiles)
- **Primary Key:** `userId` (string)
- **Fields:** `firstName` (string), `lastName` (string), `email` (string), `phoneNumber` (string), `address` (string), `role` (string), `isActive` (boolean), `createdAt` (timestamp), `updatedAt` (timestamp), `profileImageUrl` (string)

#### 2. `agents` (Collection Field Agents)
- **Primary Key:** `agentId` (string)
- **Foreign Key:** `userId` (string) -> `users.userId`
- **Fields:** `name` (string), `contactNumber` (string), `assignedZone` (string), `isAvailable` (boolean), `isActive` (boolean), `createdAt` (timestamp)

#### 3. `pickups` (Recycling Pickup Requests)
- **Primary Key:** `pickupId` (string)
- **Foreign Keys:** `userId` (string) -> `users.userId`, `agentId` (string, nullable) -> `agents.agentId`
- **Fields:** `scheduledDate` (timestamp), `wasteType` (string), `wasteTypes` (array<string>), `address` (string), `status` (string: Scheduled / In Progress / Completed / Cancelled), `notes` (string), `createdAt` (timestamp), `updatedAt` (timestamp), `completedAt` (timestamp, nullable), `cancelledAt` (timestamp, nullable), `cancellationReason` (string, nullable)

#### 4. `wasteLogs` (AI Waste Scans & Classification Logs)
- **Primary Key:** `wasteLogId` (string)
- **Foreign Keys:** `userId` (string) -> `users.userId`, `pickupId` (string, nullable) -> `pickups.pickupId`, `wasteCategoryId` (string) -> `wasteCategories.wasteCategoryId`
- **Fields:** `weightKg` (number), `loggedAt` (timestamp), `source` (string), `modelLabel` (string, nullable), `confidenceScore` (number, nullable), `imageUrl` (string, nullable)

#### 5. `wasteCategories` (Waste Classification Metadata)
- **Primary Key:** `wasteCategoryId` (string)
- **Fields:** `categoryName` (string), `description` (string), `modelLabel` (string), `isActive` (boolean), `createdAt` (timestamp)

#### 6. `guidanceContent` (Recycling & Sorting Instructions)
- **Primary Key:** `guidanceId` (string)
- **Foreign Key:** `wasteCategoryId` (string) -> `wasteCategories.wasteCategoryId`
- **Fields:** `title` (string), `content` (string), `imagePath` (string, nullable), `isActive` (boolean), `createdAt` (timestamp), `updatedAt` (timestamp)

#### 7. `rewards` (User Reward Account Balances)
- **Primary Key:** `userId` (string) -> `users.userId`
- **Fields:** `pointsBalance` (number), `totalEarned` (number), `totalRedeemed` (number), `lastUpdated` (timestamp)

#### 8. `rewardItems` (Voucher Catalogue Items)
- **Primary Key:** `rewardItemId` (string)
- **Fields:** `itemName` (string), `description` (string), `pointsCost` (number), `isActive` (boolean), `imageUrl` (string, nullable), `stockQuantity` (number, nullable), `createdAt` (timestamp), `updatedAt` (timestamp)

#### 9. `redemptionLogs` (Voucher Redemption Ledger)
- **Primary Key:** `redemptionId` (string)
- **Foreign Keys:** `userId` (string) -> `users.userId`, `rewardItemId` (string) -> `rewardItems.rewardItemId`
- **Fields:** `pointsUsed` (number), `voucherCode` (string, nullable), `status` (string), `redeemedAt` (timestamp), `createdAt` (timestamp)

#### 10. `pointsTransactions` (Points Audit Trail & Activity Ledger)
- **Primary Key:** `transactionId` (string)
- **Foreign Key:** `userId` (string) -> `users.userId`
- **Fields:** `transactionType` (string: `earn` / `redeem` / `adjust`), `points` (number), `relatedId` (string, nullable), `description` (string), `createdAt` (timestamp)

#### 11. `notifications` (User Alerts & Notifications)
- **Primary Key:** `notificationId` (string)
- **Foreign Key:** `userId` (string) -> `users.userId`
- **Fields:** `title` (string), `message` (string), `type` (string), `sentAt` (timestamp), `isRead` (boolean), `relatedPickupId` (string, nullable), `createdAt` (timestamp)

### Relationship Summary
1. **User → Agent:** One user can be assigned `0..1` agent profile.
2. **User → Pickup:** One user can create many pickups (`0..*`).
3. **Agent → Pickup:** One agent can handle many pickups (`0..*`).
4. **User → Rewards:** One user has one rewards account (`1:1`).
5. **User → RedemptionLogs:** One user can redeem many rewards (`0..*`).
6. **RedemptionLogs → RewardItems:** Many redemptions reference one reward item (`0..* : 1`).
7. **User → WasteLogs:** One user can have many waste logs (`0..*`).
8. **WasteCategories → WasteLogs:** One category can appear in many waste logs (`1 : 0..*`).
9. **WasteCategories → GuidanceContent:** One category can have many guidance entries (`1 : 0..*`).
10. **User → Notifications:** One user can have many notifications (`0..*`).
11. **User → PointsTransactions:** One user can have many points transactions (`0..*`).

### Database Design & Security Notes
- **Authentication:** Firebase Authentication manages user identity and credentials.
- **Document Identifiers:** Firestore uses unique document IDs (`userId`, `pickupId`, etc.) matching the user session UID where applicable.
- **Data Security Rules:** Security rules isolate private user documents (`userId == request.auth.uid`), preventing unauthorized cross-user reading or modification.
- **Media Assets:** Images are stored in Firebase Storage and referenced as URLs in Firestore documents.
- **Ledger Auditability:** An explicit `pointsTransactions` ledger records all earn/redeem transactions to guarantee financial integrity.
- **Cross-Platform Compatibility:** This ERD represents the unified database schema serving both web and mobile client applications.

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

---

## Team

Work is on GitHub under [sibongiseni-ngwamba/ReLoop_Technologies_Mobile_App](https://github.com/sibongiseni-ngwamba/ReLoop_Technologies_Mobile_App).

| GitHub | Name |
| --- | --- |
| [sibongiseni-ngwamba](https://github.com/sibongiseni-ngwamba) | Sibongiseni Ngwamaba |
| [Thokozani Masondo](https://github.com/Globoysosa) | Thokozani Masondo |
| [tirisano-trigga](https://github.com/tirisanomolaolwe-byte) | Tirisano Molaolwe |
| [Katleho Kwakwari](https://github.com/Katleho-1) | Katleho Kwakwari |
| [Katlego Makhuba](https://github.com/st10366193) | Katlego Makhuba |
