package com.example.reloop_technologies_mobile_app.backend.api

import com.example.reloop_technologies_mobile_app.backend.models.GuidanceContent
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.models.WasteCategory
import com.example.reloop_technologies_mobile_app.backend.models.WasteLog

object RestDataSeed {

    private val now = System.currentTimeMillis()
    private const val DAY_MS = 86400000L

    // 1. Users Table (10 records)
    val seededUsers: List<UserModel> = listOf(
        UserModel(
            userId = "user_001",
            firstName = "Alex",
            lastName = "Green",
            email = "alex.green@reloop.app",
            phoneNumber = "+1 (555) 019-2834",
            address = "123 Eco Way, San Francisco, CA",
            role = "resident",
            isActive = true
        ),
        UserModel(
            userId = "user_002",
            firstName = "Sarah",
            lastName = "Jenkins",
            email = "sarah.j@reloop.app",
            phoneNumber = "+1 (555) 014-9921",
            address = "456 Solar Avenue, San Jose, CA",
            role = "resident",
            isActive = true
        ),
        UserModel(
            userId = "user_003",
            firstName = "Michael",
            lastName = "Driver",
            email = "m.driver@reloop.app",
            phoneNumber = "+1 (555) 017-8833",
            address = "789 Logistics Hub, Oakland, CA",
            role = "collector",
            isActive = true
        ),
        UserModel(
            userId = "user_004",
            firstName = "Emma",
            lastName = "Watson",
            email = "emma.watson@reloop.app",
            phoneNumber = "+1 (555) 012-3344",
            address = "101 Clean Earth Blvd, Berkeley, CA",
            role = "resident",
            isActive = true
        ),
        UserModel(
            userId = "user_005",
            firstName = "David",
            lastName = "Admin",
            email = "admin@reloop.app",
            phoneNumber = "+1 (555) 010-0000",
            address = "1 ReLoop HQ Plaza, San Francisco, CA",
            role = "admin",
            isActive = true
        ),
        UserModel(
            userId = "user_006",
            firstName = "Jessica",
            lastName = "Taylor",
            email = "jessica.t@reloop.app",
            phoneNumber = "+1 (555) 018-4411",
            address = "55 Forest Hill Lane, Palo Alto, CA",
            role = "resident",
            isActive = true
        ),
        UserModel(
            userId = "user_007",
            firstName = "Robert",
            lastName = "Chen",
            email = "robert.chen@reloop.app",
            phoneNumber = "+1 (555) 013-7722",
            address = "88 Innovation Drive, Santa Clara, CA",
            role = "resident",
            isActive = true
        ),
        UserModel(
            userId = "user_008",
            firstName = "Maria",
            lastName = "Rodriguez",
            email = "maria.r@reloop.app",
            phoneNumber = "+1 (555) 015-6699",
            address = "302 Greenway Terrace, Fremont, CA",
            role = "resident",
            isActive = true
        ),
        UserModel(
            userId = "user_009",
            firstName = "James",
            lastName = "Collector",
            email = "james.c@reloop.app",
            phoneNumber = "+1 (555) 011-8822",
            address = "404 Recycle Way, San Mateo, CA",
            role = "collector",
            isActive = true
        ),
        UserModel(
            userId = "user_010",
            firstName = "Lisa",
            lastName = "Sustainable",
            email = "lisa.s@reloop.app",
            phoneNumber = "+1 (555) 016-5544", address = "910 Harmony Street, Sunnyvale, CA", role = "resident", isActive = true
        )
    )

    // 2. Pickups Table (10 records)
    val seededPickups: List<PickupModel> = listOf(
        PickupModel(
            pickupId = "pickup_001",
            userId = "user_001",
            agentId = "user_003",
            scheduledDate = now - (3 * DAY_MS),
            wasteType = "Plastic Bottles & Aluminum Cans",
            wasteTypes = listOf("Plastic Bottles", "Aluminum Cans"),
            address = "123 Eco Way, San Francisco, CA",
            status = "Completed",
            notes = "Bag left outside front porch.",
            createdAt = now - (5 * DAY_MS),
            completedAt = now - (3 * DAY_MS)
        ),
        PickupModel(
            pickupId = "pickup_002",
            userId = "user_001",
            scheduledDate = now + (2 * DAY_MS),
            wasteType = "Paper & Cardboard Boxes",
            wasteTypes = listOf("Paper", "Cardboard"),
            address = "123 Eco Way, San Francisco, CA",
            status = "Scheduled",
            notes = "Flattened cardboard stacked by garage.",
            createdAt = now - (1 * DAY_MS)
        ),
        PickupModel(
            pickupId = "pickup_003",
            userId = "user_001",
            agentId = "user_009",
            scheduledDate = now + (1 * DAY_MS),
            wasteType = "Electronics & Batteries",
            wasteTypes = listOf("Electronics", "Batteries"),
            address = "123 Eco Way, San Francisco, CA",
            status = "In Progress",
            notes = "Old laptops and lithium batteries in blue container.",
            createdAt = now - (2 * DAY_MS)
        ),
        PickupModel(
            pickupId = "pickup_004",
            userId = "user_002",
            scheduledDate = now - (1 * DAY_MS),
            wasteType = "Glass Jars & Bottles",
            wasteTypes = listOf("Glass Jars", "Glass Bottles"),
            address = "456 Solar Avenue, San Jose, CA",
            status = "Completed",
            notes = "Rinsed and sorted by color.",
            createdAt = now - (4 * DAY_MS),
            completedAt = now - (1 * DAY_MS)
        ),
        PickupModel(
            pickupId = "pickup_005",
            userId = "user_004",
            scheduledDate = now + (4 * DAY_MS),
            wasteType = "Organic Compost & Green Waste",
            wasteTypes = listOf("Organic Compost", "Green Waste"),
            address = "101 Clean Earth Blvd, Berkeley, CA",
            status = "Scheduled",
            notes = "Yard trimmings and food scraps in bio-degradable bags.",
            createdAt = now
        ),
        PickupModel(
            pickupId = "pickup_006",
            userId = "user_006",
            scheduledDate = now - (6 * DAY_MS),
            wasteType = "Scrap Metal & Appliance Parts",
            wasteTypes = listOf("Scrap Metal", "Appliances"),
            address = "55 Forest Hill Lane, Palo Alto, CA",
            status = "Completed",
            notes = "Picked up from side alleyway.",
            createdAt = now - (7 * DAY_MS),
            completedAt = now - (6 * DAY_MS)
        ),
        PickupModel(
            pickupId = "pickup_007",
            userId = "user_007",
            scheduledDate = now - (2 * DAY_MS),
            wasteType = "Textiles & Used Clothing",
            wasteTypes = listOf("Textiles", "Clothing"),
            address = "88 Innovation Drive, Santa Clara, CA",
            status = "Cancelled",
            notes = "User rescheduled for next week.",
            createdAt = now - (3 * DAY_MS),
            cancelledAt = now - (2 * DAY_MS),
            cancellationReason = "User was out of town."
        ),
        PickupModel(
            pickupId = "pickup_008",
            userId = "user_008",
            scheduledDate = now + (3 * DAY_MS),
            wasteType = "Mixed Household Recyclables",
            wasteTypes = listOf("Plastics", "Paper", "Glass"),
            address = "302 Greenway Terrace, Fremont, CA",
            status = "Scheduled",
            notes = "Please ring doorbell upon arrival.",
            createdAt = now
        ),
        PickupModel(
            pickupId = "pickup_009",
            userId = "user_010",
            scheduledDate = now - (8 * DAY_MS),
            wasteType = "E-Waste Chargers & Cables",
            wasteTypes = listOf("E-Waste", "Cables"),
            address = "910 Harmony Street, Sunnyvale, CA",
            status = "Completed",
            notes = "All cords neatly bundled.",
            createdAt = now - (10 * DAY_MS),
            completedAt = now - (8 * DAY_MS)
        ),
        PickupModel(
            pickupId = "pickup_010",
            userId = "user_001",
            scheduledDate = now - (12 * DAY_MS),
            wasteType = "Plastic Packaging & Containers",
            wasteTypes = listOf("PET Plastics", "Containers"),
            address = "123 Eco Way, San Francisco, CA",
            status = "Completed",
            notes = "Clean plastic food tubs and soda bottles.",
            createdAt = now - (14 * DAY_MS),
            completedAt = now - (12 * DAY_MS)
        )
    )

    // 3. Reward Items / Catalogue Table (10 records)
    val seededRewardItems: List<RewardItem> = listOf(
        RewardItem(
            rewardItemId = "item_001",
            itemName = "$5 Coffee Shop Voucher",
            description = "Valid at participating organic coffee outlets nationwide.",
            pointsCost = 500,
            isActive = true,
            stockQuantity = 50
        ),
        RewardItem(
            rewardItemId = "item_002",
            itemName = "10% Off Eco Grocery",
            description = "Discount code for sustainable, organic food products.",
            pointsCost = 800,
            isActive = true,
            stockQuantity = 35
        ),
        RewardItem(
            rewardItemId = "item_003",
            itemName = "Plant a Tree in Your Name",
            description = "We will plant a native tree to offset your carbon footprint.",
            pointsCost = 1000,
            isActive = true,
            stockQuantity = 100
        ),
        RewardItem(
            rewardItemId = "item_004",
            itemName = "$15 Eco Store Gift Card",
            description = "Redeemable online at EcoMart sustainable store.",
            pointsCost = 1500,
            isActive = true,
            stockQuantity = 20
        ),
        RewardItem(
            rewardItemId = "item_005",
            itemName = "Solar Powerbank 10,000mAh",
            description = "Portable eco-friendly solar-powered phone charger.",
            pointsCost = 2500,
            isActive = true,
            stockQuantity = 15
        ),
        RewardItem(
            rewardItemId = "item_006",
            itemName = "Stainless Steel Water Bottle",
            description = "Insulated 750ml reusable metal flask.",
            pointsCost = 1200,
            isActive = true,
            stockQuantity = 40
        ),
        RewardItem(
            rewardItemId = "item_007",
            itemName = "Reusable Bamboo Cutlery Set",
            description = "Travel pouch with bamboo fork, spoon, knife, and straw.",
            pointsCost = 600,
            isActive = true,
            stockQuantity = 60
        ),
        RewardItem(
            rewardItemId = "item_008",
            itemName = "Organic Cotton Tote Bag",
            description = "Durable, washable zero-waste shopping bag.",
            pointsCost = 400,
            isActive = true,
            stockQuantity = 75
        ),
        RewardItem(
            rewardItemId = "item_009",
            itemName = "Home Compost Bin Kit 15L",
            description = "Odor-free indoor bokashi composting starter bucket.",
            pointsCost = 3000,
            isActive = true,
            stockQuantity = 10
        ),
        RewardItem(
            rewardItemId = "item_010",
            itemName = "Electric Scooter 30-Min Pass",
            description = "Free riding credit on eco electric city scooters.",
            pointsCost = 700,
            isActive = true,
            stockQuantity = 50
        )
    )

    // 4. Points Transactions Table (10 records)
    val seededTransactions: List<PointsTransaction> = listOf(
        PointsTransaction(
            transactionId = "tx_001",
            userId = "user_001",
            transactionType = "earn",
            points = 1000,
            description = "Welcome Eco Signup Bonus",
            createdAt = now - (15 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_002",
            userId = "user_001",
            transactionType = "earn",
            points = 150,
            relatedId = "pickup_001",
            description = "Completed Pickup: Plastic Bottles & Aluminum Cans",
            createdAt = now - (3 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_003",
            userId = "user_001",
            transactionType = "earn",
            points = 50,
            description = "AI Waste Scan Bonus: Plastic Bottle",
            createdAt = now - (2 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_004",
            userId = "user_001",
            transactionType = "redeem",
            points = -500,
            relatedId = "red_001",
            description = "Redeemed: $5 Coffee Shop Voucher",
            createdAt = now - (1 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_005",
            userId = "user_001",
            transactionType = "earn",
            points = 150,
            relatedId = "pickup_010",
            description = "Completed Pickup: Plastic Packaging",
            createdAt = now - (12 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_006",
            userId = "user_002",
            transactionType = "earn",
            points = 1000,
            description = "Welcome Eco Signup Bonus",
            createdAt = now - (10 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_007",
            userId = "user_002",
            transactionType = "earn",
            points = 150,
            relatedId = "pickup_004",
            description = "Completed Pickup: Glass Jars & Bottles",
            createdAt = now - (1 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_008",
            userId = "user_004",
            transactionType = "earn",
            points = 1000,
            description = "Welcome Eco Signup Bonus",
            createdAt = now - (5 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_009",
            userId = "user_006",
            transactionType = "earn",
            points = 150,
            relatedId = "pickup_006",
            description = "Completed Pickup: Scrap Metal",
            createdAt = now - (6 * DAY_MS)
        ),
        PointsTransaction(
            transactionId = "tx_010",
            userId = "user_001",
            transactionType = "earn",
            points = 100,
            description = "Weekly Zero-Waste Streak Milestone",
            createdAt = now - (6 * DAY_MS)
        )
    )

    // 5. Redemption Logs Table (10 records)
    val seededRedemptionLogs: List<RedemptionLog> = listOf(
        RedemptionLog(
            redemptionId = "red_001",
            userId = "user_001",
            rewardItemId = "item_001",
            pointsUsed = 500,
            voucherCode = "RELOOP-849201",
            status = "Redeemed",
            redeemedAt = now - (1 * DAY_MS),
            createdAt = now - (1 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_002",
            userId = "user_002",
            rewardItemId = "item_002",
            pointsUsed = 800,
            voucherCode = "RELOOP-391024",
            status = "Redeemed",
            redeemedAt = now - (2 * DAY_MS),
            createdAt = now - (2 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_003",
            userId = "user_004",
            rewardItemId = "item_003",
            pointsUsed = 1000,
            voucherCode = "RELOOP-TREE-092",
            status = "Redeemed",
            redeemedAt = now - (4 * DAY_MS),
            createdAt = now - (4 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_004",
            userId = "user_006",
            rewardItemId = "item_008",
            pointsUsed = 400,
            voucherCode = "RELOOP-551920",
            status = "Redeemed",
            redeemedAt = now - (5 * DAY_MS),
            createdAt = now - (5 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_005",
            userId = "user_001",
            rewardItemId = "item_007",
            pointsUsed = 600,
            voucherCode = "RELOOP-771829",
            status = "Redeemed",
            redeemedAt = now - (7 * DAY_MS),
            createdAt = now - (7 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_006",
            userId = "user_007",
            rewardItemId = "item_010",
            pointsUsed = 700,
            voucherCode = "RELOOP-SCOOT-11",
            status = "Redeemed",
            redeemedAt = now - (8 * DAY_MS),
            createdAt = now - (8 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_007",
            userId = "user_008",
            rewardItemId = "item_001",
            pointsUsed = 500,
            voucherCode = "RELOOP-119284",
            status = "Redeemed",
            redeemedAt = now - (9 * DAY_MS),
            createdAt = now - (9 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_008",
            userId = "user_010",
            rewardItemId = "item_006",
            pointsUsed = 1200,
            voucherCode = "RELOOP-BOTTLE-44",
            status = "Redeemed",
            redeemedAt = now - (10 * DAY_MS),
            createdAt = now - (10 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_009",
            userId = "user_002",
            rewardItemId = "item_004",
            pointsUsed = 1500,
            voucherCode = "RELOOP-CARD-88",
            status = "Redeemed",
            redeemedAt = now - (12 * DAY_MS),
            createdAt = now - (12 * DAY_MS)
        ),
        RedemptionLog(
            redemptionId = "red_010",
            userId = "user_001",
            rewardItemId = "item_002",
            pointsUsed = 800,
            voucherCode = "RELOOP-GROC-22",
            status = "Redeemed",
            redeemedAt = now - (14 * DAY_MS),
            createdAt = now - (14 * DAY_MS)
        )
    )

    // 6. Waste Logs / AI Scans Table (10 records)
    val seededWasteLogs: List<WasteLog> = listOf(
        WasteLog(
            wasteLogId = "log_001",
            userId = "user_001",
            wasteCategoryId = "cat_001",
            weightKg = 0.35,
            loggedAt = now - (1 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "PET Plastic Water Bottle",
            confidenceScore = 0.98
        ),
        WasteLog(
            wasteLogId = "log_002",
            userId = "user_001",
            wasteCategoryId = "cat_002",
            weightKg = 0.15,
            loggedAt = now - (2 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Aluminum Soda Can",
            confidenceScore = 0.96
        ),
        WasteLog(
            wasteLogId = "log_003",
            userId = "user_001",
            wasteCategoryId = "cat_003",
            weightKg = 1.20,
            loggedAt = now - (3 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Cardboard Shipping Box",
            confidenceScore = 0.94
        ),
        WasteLog(
            wasteLogId = "log_004",
            userId = "user_002",
            wasteCategoryId = "cat_004",
            weightKg = 0.50,
            loggedAt = now - (4 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Clear Glass Sauce Jar",
            confidenceScore = 0.97
        ),
        WasteLog(
            wasteLogId = "log_005",
            userId = "user_004",
            wasteCategoryId = "cat_005",
            weightKg = 0.80,
            loggedAt = now - (5 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Electronic Phone Charger & Cable",
            confidenceScore = 0.92
        ),
        WasteLog(
            wasteLogId = "log_006",
            userId = "user_006",
            wasteCategoryId = "cat_006",
            weightKg = 2.50,
            loggedAt = now - (6 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Organic Kitchen Scraps",
            confidenceScore = 0.91
        ),
        WasteLog(
            wasteLogId = "log_007",
            userId = "user_007",
            wasteCategoryId = "cat_007",
            weightKg = 0.20,
            loggedAt = now - (7 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "AA Alkaline Battery",
            confidenceScore = 0.99
        ),
        WasteLog(
            wasteLogId = "log_008",
            userId = "user_008",
            wasteCategoryId = "cat_008",
            weightKg = 1.80,
            loggedAt = now - (8 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Cotton Denim Jacket",
            confidenceScore = 0.89
        ),
        WasteLog(
            wasteLogId = "log_009",
            userId = "user_010",
            wasteCategoryId = "cat_009",
            weightKg = 0.40,
            loggedAt = now - (9 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Paint Can Container",
            confidenceScore = 0.88
        ),
        WasteLog(
            wasteLogId = "log_010",
            userId = "user_001",
            wasteCategoryId = "cat_010",
            weightKg = 3.10,
            loggedAt = now - (10 * DAY_MS),
            source = "Mobile AI Scan",
            modelLabel = "Rubber Bicycle Tyre",
            confidenceScore = 0.95
        )
    )

    // 7. Waste Categories Table (10 records)
    val seededWasteCategories: List<WasteCategory> = listOf(
        WasteCategory(
            wasteCategoryId = "cat_001",
            categoryName = "Plastics",
            description = "PET, HDPE, soda bottles, milk jugs, food tubs.",
            modelLabel = "Plastic Bottle",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_002",
            categoryName = "Metals & Cans",
            description = "Aluminum beverage cans, steel food tins, foil.",
            modelLabel = "Aluminum Can",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_003",
            categoryName = "Paper & Cardboard",
            description = "Newspapers, cereal boxes, corrugated cardboard.",
            modelLabel = "Cardboard Box",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_004",
            categoryName = "Glass",
            description = "Clear, green, and brown glass bottles and jars.",
            modelLabel = "Glass Jar",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_005",
            categoryName = "E-Waste & Gadgets",
            description = "Cell phones, chargers, laptops, circuit boards.",
            modelLabel = "Electronic Gadget",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_006",
            categoryName = "Organic & Compost",
            description = "Fruit peelings, vegetable remains, coffee grounds.",
            modelLabel = "Organic Waste",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_007",
            categoryName = "Batteries",
            description = "AA, AAA, 9V, Lithium-ion cell phone batteries.",
            modelLabel = "Battery",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_008",
            categoryName = "Textiles & Clothes",
            description = "Wearable clothes, linens, fabrics, shoes.",
            modelLabel = "Textile",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_009",
            categoryName = "Hazardous Waste",
            description = "Paint cans, motor oil, household cleaning chemicals.",
            modelLabel = "Hazardous Container",
            isActive = true
        ),
        WasteCategory(
            wasteCategoryId = "cat_010",
            categoryName = "Rubber & Tyres",
            description = "Bicycle and auto tyres, rubber mats.",
            modelLabel = "Rubber Tyre",
            isActive = true
        )
    )

    // 8. Notifications Table (10 records)
    val seededNotifications: List<NotificationModel> = listOf(
        NotificationModel(
            notificationId = "notif_001",
            userId = "user_001",
            title = "Pickup Confirmed!",
            message = "Your pickup request for Plastic Bottles & Aluminum Cans is scheduled.",
            type = NotificationModel.TYPE_PICKUP_CONFIRMATION,
            sentAt = now - (3 * DAY_MS),
            isRead = true,
            relatedPickupId = "pickup_001"
        ),
        NotificationModel(
            notificationId = "notif_002",
            userId = "user_001",
            title = "Points Credited 🎉",
            message = "You earned +150 eco points for completing your recycling pickup!",
            type = NotificationModel.TYPE_REWARD,
            sentAt = now - (3 * DAY_MS),
            isRead = true,
            relatedPickupId = "pickup_001"
        ),
        NotificationModel(
            notificationId = "notif_003",
            userId = "user_001",
            title = "Collector On The Way",
            message = "Driver Michael is approaching your pickup address.",
            type = NotificationModel.TYPE_STATUS_CHANGE,
            sentAt = now - (1 * DAY_MS),
            isRead = false,
            relatedPickupId = "pickup_003"
        ),
        NotificationModel(
            notificationId = "notif_004",
            userId = "user_001",
            title = "AI Waste Scan Bonus",
            message = "Your PET Plastic Bottle scan was verified! +50 pts awarded.",
            type = NotificationModel.TYPE_REWARD,
            sentAt = now - (2 * DAY_MS),
            isRead = true
        ),
        NotificationModel(
            notificationId = "notif_005",
            userId = "user_001",
            title = "Voucher Redeemed",
            message = "Voucher RELOOP-849201 generated for $5 Coffee Shop Voucher.",
            type = NotificationModel.TYPE_REWARD,
            sentAt = now - (1 * DAY_MS),
            isRead = false
        ),
        NotificationModel(
            notificationId = "notif_006",
            userId = "user_001",
            title = "Upcoming Pickup Reminder",
            message = "Reminder: Paper & Cardboard pickup is scheduled for tomorrow morning.",
            type = NotificationModel.TYPE_PICKUP_REMINDER,
            sentAt = now - (12 * HOUR_MS),
            isRead = false,
            relatedPickupId = "pickup_002"
        ),
        NotificationModel(
            notificationId = "notif_007",
            userId = "user_001",
            title = "Eco Level Up!",
            message = "Congratulations! You have reached Eco Champion status.",
            type = NotificationModel.TYPE_INFO,
            sentAt = now - (5 * DAY_MS),
            isRead = true
        ),
        NotificationModel(
            notificationId = "notif_008",
            userId = "user_001",
            title = "New Recycling Guide Available",
            message = "Check out our new guide on properly disposing of e-waste safely.",
            type = NotificationModel.TYPE_INFO,
            sentAt = now - (7 * DAY_MS),
            isRead = true
        ),
        NotificationModel(
            notificationId = "notif_009",
            userId = "user_001",
            title = "Weekly Impact Summary",
            message = "You offset 4.2 kg of CO2 emissions this week! Keep it up.",
            type = NotificationModel.TYPE_INFO,
            sentAt = now - (8 * DAY_MS),
            isRead = true
        ),
        NotificationModel(
            notificationId = "notif_010",
            userId = "user_001",
            title = "Community Challenge",
            message = "Join the 10-bag community recycling drive this weekend!",
            type = NotificationModel.TYPE_INFO,
            sentAt = now - (10 * DAY_MS),
            isRead = true
        )
    )

    private const val HOUR_MS = 3600000L

    // 9. Recycling Guidance Table (10 records)
    val seededGuidanceContent: List<GuidanceContent> = listOf(
        GuidanceContent(
            guidanceId = "guide_001",
            wasteCategoryId = "cat_001",
            title = "How to Recycle Plastics Correctly",
            content = "1. Look for resin codes #1 (PET) and #2 (HDPE).\n2. Rinse containers to remove food liquid residues.\n3. Keep caps on or discard separately according to local bin guidelines."
        ),
        GuidanceContent(
            guidanceId = "guide_002",
            wasteCategoryId = "cat_002",
            title = "Metal & Aluminum Can Preparation",
            content = "1. Aluminum cans are 100% infinitely recyclable.\n2. Do not crush cans completely if your local facility uses optical sorting.\n3. Remove paper labels from steel food tins when possible."
        ),
        GuidanceContent(
            guidanceId = "guide_003",
            wasteCategoryId = "cat_003",
            title = "Cardboard & Paper Sorting Tips",
            content = "1. Flatten all corrugated cardboard shipping boxes before pickup.\n2. Keep paper dry and clean from oil or grease spots (e.g. pizza boxes with heavy grease belong in compost)."
        ),
        GuidanceContent(
            guidanceId = "guide_004",
            wasteCategoryId = "cat_004",
            title = "Glass Bottles & Jars Safety",
            content = "1. Separate clear glass from colored (green/brown) jars.\n2. Do not mix window panes, mirrors, or ceramics with bottle glass as they melt at different temperatures."
        ),
        GuidanceContent(
            guidanceId = "guide_005",
            wasteCategoryId = "cat_005",
            title = "E-Waste & Electronics Recycling",
            content = "1. Wipe personal data off old smartphones and laptops.\n2. Remove detachable lithium batteries prior to disposal.\n3. Use ReLoop scheduled pickups to ensure heavy metals stay out of landfills."
        ),
        GuidanceContent(
            guidanceId = "guide_006",
            wasteCategoryId = "cat_006",
            title = "Home Composting 101",
            content = "1. Balance green nitrogen materials (food scraps) with brown carbon materials (dry leaves, sawdust).\n2. Keep compost moist like a wrung-out sponge and aerate weekly."
        ),
        GuidanceContent(
            guidanceId = "guide_007",
            wasteCategoryId = "cat_007",
            title = "Battery Handling & Disposal",
            content = "1. Tape terminals on 9V and lithium batteries with electrical tape.\n2. Store spent batteries in a cool, dry plastic container prior to drop-off."
        ),
        GuidanceContent(
            guidanceId = "guide_008",
            wasteCategoryId = "cat_008",
            title = "Textile & Clothing Circularity",
            content = "1. Clean and dry clothes before donation or textile recycling.\n2. Unusable rags can be shredded for industrial insulation and furniture padding."
        ),
        GuidanceContent(
            guidanceId = "guide_009",
            wasteCategoryId = "cat_009",
            title = "Household Hazardous Waste Guidelines",
            content = "1. Keep hazardous liquids (paint, motor oil) in original sealed containers with labels visible.\n2. Never pour chemicals into storm drains or regular trash bins."
        ),
        GuidanceContent(
            guidanceId = "guide_010",
            wasteCategoryId = "cat_010",
            title = "Tyres & Rubber Recycling",
            content = "1. Tyres are shredded into rubber mulch for playgrounds or asphalt roads.\n2. Schedule a ReLoop bulky item pickup for old bicycle and car tyres."
        )
    )

    // 10. Reward Accounts Map (Default seeded balances for accounts)
    val seededAccounts: Map<String, RewardAccount> = mapOf(
        "user_001" to RewardAccount(
            userId = "user_001",
            pointsBalance = 800,
            totalEarned = 1300,
            totalRedeemed = 500
        ),
        "user_002" to RewardAccount(
            userId = "user_002",
            pointsBalance = 350,
            totalEarned = 1150,
            totalRedeemed = 800
        ),
        "user_004" to RewardAccount(
            userId = "user_004",
            pointsBalance = 0,
            totalEarned = 1000,
            totalRedeemed = 1000
        )
    )
}
