package com.example.helpeez.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.helpeez.ui.screens.HomeDetailsData
import java.security.MessageDigest

// Data class representing user credentials and profile
data class UserData(
    val id: Int,
    val email: String,
    val name: String,
    val phone: String,
    val role: String
)

// Data class representing saved simulation state
data class AppStateData(
    val simState: Int,
    val otpVerified: Boolean
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "helpeez.db"
        private const val DATABASE_VERSION = 4 // Bumped to 4 for regular_helper_id column

        // Tables names
        private const val TABLE_USERS = "users"
        private const val TABLE_HOMES = "homes"
        private const val TABLE_APP_STATE = "app_state"

        // Common Column Names
        private const val KEY_ID = "id"

        // USERS Table columns
        private const val KEY_USER_EMAIL = "email"
        private const val KEY_USER_PASSWORD = "password_hash"
        private const val KEY_USER_NAME = "name"
        private const val KEY_USER_PHONE = "phone"
        private const val KEY_USER_ROLE = "role"

        // HOMES Table columns
        private const val KEY_HOME_USER_ID = "user_id"
        private const val KEY_HOME_NAME = "name"
        private const val KEY_HOME_ROOMS = "rooms"
        private const val KEY_HOME_HALLS = "halls"
        private const val KEY_HOME_BALCONIES = "balconies"
        private const val KEY_HOME_ADDRESS = "address"
        private const val KEY_HOME_SWEEPING = "sweeping"
        private const val KEY_HOME_UTENSILS = "utensils"
        private const val KEY_HOME_COOKING = "cooking"
        private const val KEY_HOME_DISHWASHER = "dishwasher"
        private const val KEY_HOME_WASHING_MACHINE = "washing_machine"
        private const val KEY_HOME_SPECIAL_BALCONY = "special_balcony"
        private const val KEY_HOME_SPECIAL_DUSTBIN = "special_dustbin"
        private const val KEY_HOME_CUSTOM_REQUEST = "custom_request"
        private const val KEY_HOME_TIMING = "timing_slot"
        private const val KEY_HOME_SUNDAY_TIMING = "sunday_timing_slot"
        
        // Helper / Shift tracking columns
        private const val KEY_HOME_HELPER_ID = "assigned_helper_id"
        private const val KEY_HOME_REGULAR_HELPER_ID = "regular_helper_id"
        private const val KEY_HOME_OTP = "otp"
        private const val KEY_HOME_CHECK_IN = "check_in_time"
        private const val KEY_HOME_SHIFT_STATUS = "shift_status"
        
        // Quoting columns
        private const val KEY_HOME_CARPET_AREA = "carpet_area"
        private const val KEY_HOME_DURATION = "cleaning_duration"

        // APP STATE Table columns
        private const val KEY_STATE_USER_ID = "user_id"
        private const val KEY_STATE_SIM = "sim_state"
        private const val KEY_STATE_OTP = "otp_verified"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_EMAIL + " TEXT UNIQUE,"
                + KEY_USER_PASSWORD + " TEXT,"
                + KEY_USER_NAME + " TEXT,"
                + KEY_USER_PHONE + " TEXT,"
                + KEY_USER_ROLE + " TEXT" + ")")

        val createHomesTable = ("CREATE TABLE " + TABLE_HOMES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_HOME_USER_ID + " INTEGER,"
                + KEY_HOME_NAME + " TEXT,"
                + KEY_HOME_ROOMS + " INTEGER,"
                + KEY_HOME_HALLS + " INTEGER,"
                + KEY_HOME_BALCONIES + " INTEGER,"
                + KEY_HOME_ADDRESS + " TEXT,"
                + KEY_HOME_SWEEPING + " INTEGER,"
                + KEY_HOME_UTENSILS + " INTEGER,"
                + KEY_HOME_COOKING + " INTEGER,"
                + KEY_HOME_DISHWASHER + " INTEGER,"
                + KEY_HOME_WASHING_MACHINE + " INTEGER,"
                + KEY_HOME_SPECIAL_BALCONY + " INTEGER,"
                + KEY_HOME_SPECIAL_DUSTBIN + " INTEGER,"
                + KEY_HOME_CUSTOM_REQUEST + " TEXT,"
                + KEY_HOME_TIMING + " TEXT,"
                + KEY_HOME_SUNDAY_TIMING + " TEXT,"
                + KEY_HOME_HELPER_ID + " INTEGER,"
                + KEY_HOME_REGULAR_HELPER_ID + " INTEGER DEFAULT -1,"
                + KEY_HOME_OTP + " TEXT,"
                + KEY_HOME_CHECK_IN + " INTEGER,"
                + KEY_HOME_SHIFT_STATUS + " TEXT,"
                + KEY_HOME_CARPET_AREA + " INTEGER,"
                + KEY_HOME_DURATION + " INTEGER" + ")")

        val createAppStateTable = ("CREATE TABLE " + TABLE_APP_STATE + "("
                + KEY_STATE_USER_ID + " INTEGER PRIMARY KEY,"
                + KEY_STATE_SIM + " INTEGER,"
                + KEY_STATE_OTP + " INTEGER" + ")")

        db.execSQL(createUsersTable)
        db.execSQL(createHomesTable)
        db.execSQL(createAppStateTable)

        // Seed 10 helpers for random monthly assignment and holiday substitute flow
        val helperNamesAndEmails = listOf(
            Pair("Sita", "sita@helpeez.com"),
            Pair("Kamla", "kamla@helpeez.com"),
            Pair("Shanti", "shanti@helpeez.com"),
            Pair("Laxmi", "laxmi@helpeez.com"),
            Pair("Radha", "radha@helpeez.com"),
            Pair("Sunita", "sunita@helpeez.com"),
            Pair("Geeta", "geeta@helpeez.com"),
            Pair("Asha", "asha@helpeez.com"),
            Pair("Rekha", "rekha@helpeez.com"),
            Pair("Savitri", "savitri@helpeez.com")
        )
        helperNamesAndEmails.forEachIndexed { index, pair ->
            val values = ContentValues().apply {
                put(KEY_USER_EMAIL, pair.second)
                put(KEY_USER_NAME, pair.first)
                put(KEY_USER_PHONE, "+91 98765 " + String.format("%05d", index + 1))
                put(KEY_USER_ROLE, "helper")
                put(KEY_USER_PASSWORD, hashPassword("helper123"))
            }
            db.insert(TABLE_USERS, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HOMES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APP_STATE")
        onCreate(db)
    }

    // SHA-256 Hashing helper
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // Register a new user
    fun registerUser(email: String, name: String, phone: String, passwordPlain: String, role: String = "owner"): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_EMAIL, email.trim().lowercase())
            put(KEY_USER_NAME, name.trim())
            put(KEY_USER_PHONE, phone.trim())
            put(KEY_USER_ROLE, role.trim().lowercase())
            put(KEY_USER_PASSWORD, hashPassword(passwordPlain))
        }

        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    // Authenticate a user
    fun loginUser(email: String, passwordPlain: String): UserData? {
        val db = this.readableDatabase
        val passwordHash = hashPassword(passwordPlain)
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID, KEY_USER_EMAIL, KEY_USER_NAME, KEY_USER_PHONE, KEY_USER_ROLE),
            "$KEY_USER_EMAIL = ? AND $KEY_USER_PASSWORD = ?",
            arrayOf(email.trim().lowercase(), passwordHash),
            null, null, null
        )

        var user: UserData? = null
        if (cursor.moveToFirst()) {
            user = UserData(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    // Find first helper registered in the local database
    fun getFirstHelper(): UserData? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID, KEY_USER_EMAIL, KEY_USER_NAME, KEY_USER_PHONE, KEY_USER_ROLE),
            "$KEY_USER_ROLE = ?",
            arrayOf("helper"),
            null, null, "$KEY_ID ASC", "1"
        )
        
        var helper: UserData? = null
        if (cursor.moveToFirst()) {
            helper = UserData(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE))
            )
        }
        cursor.close()
        db.close()
        return helper
    }

    // Insert a new home associated with user
    fun insertHome(userId: Int, home: HomeDetailsData): Boolean {
        val db = this.writableDatabase
        
        // Auto-assign random helper if none specified
        var helperId = home.assignedHelperId
        if (helperId == -1) {
            val dbRead = this.readableDatabase
            val cursor = dbRead.query(TABLE_USERS, arrayOf(KEY_ID), "$KEY_USER_ROLE = ?", arrayOf("helper"), null, null, null)
            val helpers = mutableListOf<Int>()
            while (cursor.moveToNext()) {
                helpers.add(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)))
            }
            cursor.close()
            if (helpers.isNotEmpty()) {
                helperId = helpers.random()
            }
        }

        // Generate a random 4-digit OTP if not specified
        val finalOtp = if (home.otp.isEmpty()) (1000..9999).random().toString() else home.otp

        val values = ContentValues().apply {
            put(KEY_HOME_USER_ID, userId)
            put(KEY_HOME_NAME, home.name)
            put(KEY_HOME_ROOMS, home.rooms)
            put(KEY_HOME_HALLS, home.halls)
            put(KEY_HOME_BALCONIES, home.balconies)
            put(KEY_HOME_ADDRESS, home.address)
            put(KEY_HOME_SWEEPING, if (home.sweepingSelected) 1 else 0)
            put(KEY_HOME_UTENSILS, if (home.utensilsSelected) 1 else 0)
            put(KEY_HOME_COOKING, if (home.cookingSelected) 1 else 0)
            put(KEY_HOME_DISHWASHER, if (home.hasDishwasher) 1 else 0)
            put(KEY_HOME_WASHING_MACHINE, if (home.hasWashingMachine) 1 else 0)
            put(KEY_HOME_SPECIAL_BALCONY, if (home.specialBalcony) 1 else 0)
            put(KEY_HOME_SPECIAL_DUSTBIN, if (home.specialDustbin) 1 else 0)
            put(KEY_HOME_CUSTOM_REQUEST, home.customRequest)
            put(KEY_HOME_TIMING, home.timingSlot)
            put(KEY_HOME_SUNDAY_TIMING, home.sundayTimingSlot)
            put(KEY_HOME_HELPER_ID, helperId)
            put(KEY_HOME_REGULAR_HELPER_ID, home.regularHelperId)
            put(KEY_HOME_OTP, finalOtp)
            put(KEY_HOME_CHECK_IN, home.checkInTime)
            put(KEY_HOME_SHIFT_STATUS, home.shiftStatus)
            put(KEY_HOME_CARPET_AREA, home.carpetArea)
            put(KEY_HOME_DURATION, home.dailyCleaningDuration)
        }

        val result = db.insert(TABLE_HOMES, null, values)
        db.close()
        return result != -1L
    }

    // Fetch homes list for a user
    fun getHomesForUser(userId: Int): List<HomeDetailsData> {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_HOMES,
            null,
            "$KEY_HOME_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, "$KEY_ID ASC"
        )

        val homes = mutableListOf<HomeDetailsData>()
        while (cursor.moveToNext()) {
            homes.add(
                HomeDetailsData(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_NAME)),
                    rooms = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_ROOMS)),
                    halls = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_HALLS)),
                    balconies = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_BALCONIES)),
                    address = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_ADDRESS)),
                    sweepingSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SWEEPING)) == 1,
                    utensilsSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_UTENSILS)) == 1,
                    cookingSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_COOKING)) == 1,
                    hasDishwasher = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_DISHWASHER)) == 1,
                    hasWashingMachine = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_WASHING_MACHINE)) == 1,
                    specialBalcony = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SPECIAL_BALCONY)) == 1,
                    specialDustbin = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SPECIAL_DUSTBIN)) == 1,
                    customRequest = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_CUSTOM_REQUEST)),
                    timingSlot = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_TIMING)),
                    sundayTimingSlot = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_SUNDAY_TIMING)),
                    assignedHelperId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_HELPER_ID)),
                    regularHelperId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_REGULAR_HELPER_ID)),
                    otp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_OTP)),
                    checkInTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_HOME_CHECK_IN)),
                    shiftStatus = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_SHIFT_STATUS)),
                    carpetArea = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_CARPET_AREA)),
                    dailyCleaningDuration = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_DURATION))
                )
            )
        }
        cursor.close()
        db.close()
        return homes
    }

    // Fetch jobs assigned to a specific helper (both active assignments and on-holiday regular assignments)
    fun getJobsForHelper(helperId: Int): List<HomeDetailsData> {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_HOMES,
            null,
            "$KEY_HOME_HELPER_ID = ? OR $KEY_HOME_REGULAR_HELPER_ID = ?",
            arrayOf(helperId.toString(), helperId.toString()),
            null, null, "$KEY_ID ASC"
        )

        val homes = mutableListOf<HomeDetailsData>()
        while (cursor.moveToNext()) {
            homes.add(
                HomeDetailsData(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_NAME)),
                    rooms = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_ROOMS)),
                    halls = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_HALLS)),
                    balconies = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_BALCONIES)),
                    address = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_ADDRESS)),
                    sweepingSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SWEEPING)) == 1,
                    utensilsSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_UTENSILS)) == 1,
                    cookingSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_COOKING)) == 1,
                    hasDishwasher = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_DISHWASHER)) == 1,
                    hasWashingMachine = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_WASHING_MACHINE)) == 1,
                    specialBalcony = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SPECIAL_BALCONY)) == 1,
                    specialDustbin = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SPECIAL_DUSTBIN)) == 1,
                    customRequest = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_CUSTOM_REQUEST)),
                    timingSlot = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_TIMING)),
                    sundayTimingSlot = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_SUNDAY_TIMING)),
                    assignedHelperId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_HELPER_ID)),
                    regularHelperId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_REGULAR_HELPER_ID)),
                    otp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_OTP)),
                    checkInTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_HOME_CHECK_IN)),
                    shiftStatus = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_SHIFT_STATUS)),
                    carpetArea = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_CARPET_AREA)),
                    dailyCleaningDuration = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_DURATION))
                )
            )
        }
        cursor.close()
        db.close()
        return homes
    }

    // Retrieve single user by ID
    fun getUserById(userId: Int): UserData? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID, KEY_USER_EMAIL, KEY_USER_NAME, KEY_USER_PHONE, KEY_USER_ROLE),
            "$KEY_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )
        
        var user: UserData? = null
        if (cursor.moveToFirst()) {
            user = UserData(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    // Update shift status of a home layout
    fun updateHomeShiftStatus(homeId: Int, status: String, checkInTime: Long): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_HOME_SHIFT_STATUS, status)
            put(KEY_HOME_CHECK_IN, checkInTime)
        }
        val result = db.update(TABLE_HOMES, values, "$KEY_ID = ?", arrayOf(homeId.toString()))
        db.close()
        return result > 0
    }

    // Update helper of a home layout
    fun updateHomeHelper(homeId: Int, helperId: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_HOME_HELPER_ID, helperId)
        }
        val result = db.update(TABLE_HOMES, values, "$KEY_ID = ?", arrayOf(homeId.toString()))
        db.close()
        return result > 0
    }

    // Update holiday and replacement helper of a home layout
    fun updateHomeHoliday(homeId: Int, assignedHelperId: Int, regularHelperId: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_HOME_HELPER_ID, assignedHelperId)
            put(KEY_HOME_REGULAR_HELPER_ID, regularHelperId)
        }
        val result = db.update(TABLE_HOMES, values, "$KEY_ID = ?", arrayOf(homeId.toString()))
        db.close()
        return result > 0
    }

    // Retrieve all registered helpers
    fun getAllHelpers(): List<UserData> {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, null, "$KEY_USER_ROLE = ?", arrayOf("helper"), null, null, "$KEY_ID ASC")
        val list = mutableListOf<UserData>()
        while (cursor.moveToNext()) {
            list.add(
                UserData(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE)),
                    role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE))
                )
            )
        }
        cursor.close()
        db.close()
        return list
    }

    // Retrieve all homes in the database
    fun getAllHomesList(): List<HomeDetailsData> {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_HOMES, null, null, null, null, null, "$KEY_ID ASC")
        val homes = mutableListOf<HomeDetailsData>()
        while (cursor.moveToNext()) {
            homes.add(
                HomeDetailsData(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_NAME)),
                    rooms = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_ROOMS)),
                    halls = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_HALLS)),
                    balconies = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_BALCONIES)),
                    address = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_ADDRESS)),
                    sweepingSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SWEEPING)) == 1,
                    utensilsSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_UTENSILS)) == 1,
                    cookingSelected = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_COOKING)) == 1,
                    hasDishwasher = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_DISHWASHER)) == 1,
                    hasWashingMachine = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_WASHING_MACHINE)) == 1,
                    specialBalcony = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SPECIAL_BALCONY)) == 1,
                    specialDustbin = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_SPECIAL_DUSTBIN)) == 1,
                    customRequest = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_CUSTOM_REQUEST)),
                    timingSlot = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_TIMING)),
                    sundayTimingSlot = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_SUNDAY_TIMING)),
                    assignedHelperId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_HELPER_ID)),
                    regularHelperId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_REGULAR_HELPER_ID)),
                    otp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_OTP)),
                    checkInTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_HOME_CHECK_IN)),
                    shiftStatus = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOME_SHIFT_STATUS)),
                    carpetArea = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_CARPET_AREA)),
                    dailyCleaningDuration = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOME_DURATION))
                )
            )
        }
        cursor.close()
        db.close()
        return homes
    }

    fun clearTables() {
        val db = this.writableDatabase
        db.delete(TABLE_HOMES, null, null)
        db.delete(TABLE_APP_STATE, null, null)
        db.close()
    }
}
