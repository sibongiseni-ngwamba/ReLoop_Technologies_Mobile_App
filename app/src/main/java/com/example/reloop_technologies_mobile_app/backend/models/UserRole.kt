package com.example.reloop_technologies_mobile_app.backend.models

enum class UserRole(val roleKey: String, val displayName: String) {
    RESIDENT("resident", "Resident"),
    AGENT("agent", "Collection Agent"),
    ADMIN("admin", "Administrator");

    val isAgent: Boolean
        get() = this == AGENT

    val isAdmin: Boolean
        get() = this == ADMIN

    val isResident: Boolean
        get() = this == RESIDENT

    fun canSchedulePickup(): Boolean = this == RESIDENT || this == ADMIN
    fun canScanWaste(): Boolean = true
    fun canManageAllPickups(): Boolean = this == AGENT || this == ADMIN
    fun canUpdatePickupStatus(): Boolean = this == AGENT || this == ADMIN
    fun canManageUsers(): Boolean = this == ADMIN
    fun canAccessRewardsCatalog(): Boolean = true

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.lowercase()?.trim()) {
                "agent", "collector", "collection agent" -> AGENT
                "admin", "administrator" -> ADMIN
                else -> RESIDENT
            }
        }
    }
}
