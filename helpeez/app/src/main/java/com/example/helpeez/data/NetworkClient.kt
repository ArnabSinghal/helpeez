package com.example.helpeez.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.example.helpeez.ui.screens.HomeDetailsData
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Helper: secure password hashing (matching database helper)
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun register(baseUrl: String, email: String, name: String, phone: String, passwordPlain: String, role: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/register"
            val json = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("name", name.trim())
                put("phone", phone.trim())
                put("password_hash", hashPassword(passwordPlain))
                put("role", role.trim().lowercase())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext null
                } else {
                    val respStr = response.body?.string() ?: ""
                    if (response.code == 400) {
                        try {
                            return@withContext JSONObject(respStr).getString("detail")
                        } catch (e: Exception) {
                            return@withContext "Registration failed. Email may already exist."
                        }
                    }
                    return@withContext "Server error: ${response.code}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "API connection failed. Please verify the sync server is running at the configured URL."
        }
    }

    suspend fun login(baseUrl: String, email: String, passwordPlain: String): UserData? = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/login"
            val json = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password_hash", hashPassword(passwordPlain))
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: return@withContext null
                    val respJson = JSONObject(respStr)
                    return@withContext UserData(
                        id = respJson.getInt("id"),
                        email = respJson.getString("email"),
                        name = respJson.getString("name"),
                        phone = respJson.getString("phone"),
                        role = respJson.optString("role", "owner")
                    )
                } else if (response.code == 401) {
                    return@withContext UserData(id = -1, email = "", name = "", phone = "", role = "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun saveHome(baseUrl: String, userId: Int, home: HomeDetailsData): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/homes"
            val json = JSONObject().apply {
                put("user_id", userId)
                put("name", home.name)
                put("rooms", home.rooms)
                put("halls", home.halls)
                put("balconies", home.balconies)
                put("address", home.address)
                put("sweeping", if (home.sweepingSelected) 1 else 0)
                put("utensils", if (home.utensilsSelected) 1 else 0)
                put("cooking", if (home.cookingSelected) 1 else 0)
                put("dishwasher", if (home.hasDishwasher) 1 else 0)
                put("washing_machine", if (home.hasWashingMachine) 1 else 0)
                put("special_balcony", if (home.specialBalcony) 1 else 0)
                put("special_dustbin", if (home.specialDustbin) 1 else 0)
                put("custom_request", home.customRequest)
                put("timing_slot", home.timingSlot)
                put("sunday_timing_slot", home.sundayTimingSlot)
                put("assigned_helper_id", home.assignedHelperId)
                put("regular_helper_id", home.regularHelperId)
                put("otp", home.otp)
                put("check_in_time", home.checkInTime)
                put("shift_status", home.shiftStatus)
                put("carpet_area", home.carpetArea)
                put("cleaning_duration", home.dailyCleaningDuration)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun fetchHomes(baseUrl: String, userId: Int): List<HomeDetailsData>? = withContext(Dispatchers.IO) {
        val homes = mutableListOf<HomeDetailsData>()
        try {
            val url = "${baseUrl.trimEnd('/')}/homes?user_id=$userId"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: return@withContext homes
                    val array = JSONArray(respStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        homes.add(
                            HomeDetailsData(
                                id = obj.optInt("id", 0),
                                name = obj.getString("name"),
                                rooms = obj.getInt("rooms"),
                                halls = obj.getInt("halls"),
                                balconies = obj.getInt("balconies"),
                                address = obj.getString("address"),
                                sweepingSelected = obj.getInt("sweeping") == 1,
                                utensilsSelected = obj.getInt("utensils") == 1,
                                cookingSelected = obj.getInt("cooking") == 1,
                                hasDishwasher = obj.getInt("dishwasher") == 1,
                                hasWashingMachine = obj.getInt("washing_machine") == 1,
                                specialBalcony = obj.getInt("special_balcony") == 1,
                                specialDustbin = obj.getInt("special_dustbin") == 1,
                                customRequest = obj.getString("custom_request"),
                                timingSlot = obj.getString("timing_slot"),
                                sundayTimingSlot = obj.getString("sunday_timing_slot"),
                                assignedHelperId = obj.optInt("assigned_helper_id", -1),
                                regularHelperId = obj.optInt("regular_helper_id", -1),
                                otp = obj.optString("otp", ""),
                                checkInTime = obj.optLong("check_in_time", 0L),
                                shiftStatus = obj.optString("shift_status", "pending"),
                                carpetArea = obj.optInt("carpet_area", 1000),
                                dailyCleaningDuration = obj.optInt("cleaning_duration", 60)
                            )
                        )
                    }
                    return@withContext homes
                } else {
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun fetchJobsForHelper(baseUrl: String, helperId: Int): List<HomeDetailsData>? = withContext(Dispatchers.IO) {
        val homes = mutableListOf<HomeDetailsData>()
        try {
            val url = "${baseUrl.trimEnd('/')}/homes?helper_id=$helperId"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: return@withContext homes
                    val array = JSONArray(respStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        homes.add(
                            HomeDetailsData(
                                id = obj.optInt("id", 0),
                                name = obj.getString("name"),
                                rooms = obj.getInt("rooms"),
                                halls = obj.getInt("halls"),
                                balconies = obj.getInt("balconies"),
                                address = obj.getString("address"),
                                sweepingSelected = obj.getInt("sweeping") == 1,
                                utensilsSelected = obj.getInt("utensils") == 1,
                                cookingSelected = obj.getInt("cooking") == 1,
                                hasDishwasher = obj.getInt("dishwasher") == 1,
                                hasWashingMachine = obj.getInt("washing_machine") == 1,
                                specialBalcony = obj.getInt("special_balcony") == 1,
                                specialDustbin = obj.getInt("special_dustbin") == 1,
                                customRequest = obj.getString("custom_request"),
                                timingSlot = obj.getString("timing_slot"),
                                sundayTimingSlot = obj.getString("sunday_timing_slot"),
                                assignedHelperId = obj.optInt("assigned_helper_id", -1),
                                regularHelperId = obj.optInt("regular_helper_id", -1),
                                otp = obj.optString("otp", ""),
                                checkInTime = obj.optLong("check_in_time", 0L),
                                shiftStatus = obj.optString("shift_status", "pending"),
                                carpetArea = obj.optInt("carpet_area", 1000),
                                dailyCleaningDuration = obj.optInt("cleaning_duration", 60)
                            )
                        )
                    }
                    return@withContext homes
                } else {
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun updateHomeShiftStatus(baseUrl: String, homeId: Int, status: String, checkInTime: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/homes/shift-status"
            val json = JSONObject().apply {
                put("home_id", homeId)
                put("shift_status", status)
                put("check_in_time", checkInTime)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun updateHomeHelperSync(baseUrl: String, homeId: Int, helperId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/homes/update-helper"
            val json = JSONObject().apply {
                put("home_id", homeId)
                put("helper_id", helperId)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun updateHomeHolidaySync(baseUrl: String, homeId: Int, assignedHelperId: Int, regularHelperId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/homes/update-holiday"
            val json = JSONObject().apply {
                put("home_id", homeId)
                put("assigned_helper_id", assignedHelperId)
                put("regular_helper_id", regularHelperId)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
