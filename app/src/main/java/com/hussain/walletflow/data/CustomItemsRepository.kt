package com.hussain.walletflow.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists user-created categories and payment methods via DataStore.
 *
 * Each item is stored as a JSON array, e.g.:
 *   [{"name":"Pets","iconKey":"Pets","colorHex":"FF9800","type":"expense"}, ...]
 *
 * [type] is one of: "income", "expense", "payment"
 */
class CustomItemsRepository(private val context: Context) {

    companion object {
        private val CUSTOM_CATEGORIES_KEY      = stringPreferencesKey("custom_categories")
        private val CUSTOM_PAYMENT_METHODS_KEY = stringPreferencesKey("custom_payment_methods")
    }

    // ── Custom Categories ────────────────────────────────────────────────────

    val customCategoriesFlow: Flow<List<CustomItem>> =
        context.dataStore.data.map { prefs -> parseItems(prefs[CUSTOM_CATEGORIES_KEY]) }

    suspend fun addCustomCategory(item: CustomItem) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[CUSTOM_CATEGORIES_KEY]).toMutableList()
            if (list.none { it.name == item.name }) list.add(item)
            prefs[CUSTOM_CATEGORIES_KEY] = serializeItems(list)
        }
    }

    suspend fun removeCustomCategory(name: String) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[CUSTOM_CATEGORIES_KEY]).filter { it.name != name }
            prefs[CUSTOM_CATEGORIES_KEY] = serializeItems(list)
        }
    }

    // ── Custom Payment Methods ───────────────────────────────────────────────

    val customPaymentMethodsFlow: Flow<List<CustomItem>> =
        context.dataStore.data.map { prefs -> parseItems(prefs[CUSTOM_PAYMENT_METHODS_KEY]) }

    suspend fun addCustomPaymentMethod(item: CustomItem) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[CUSTOM_PAYMENT_METHODS_KEY]).toMutableList()
            if (list.none { it.name == item.name }) list.add(item)
            prefs[CUSTOM_PAYMENT_METHODS_KEY] = serializeItems(list)
        }
    }

    suspend fun removeCustomPaymentMethod(name: String) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[CUSTOM_PAYMENT_METHODS_KEY]).filter { it.name != name }
            prefs[CUSTOM_PAYMENT_METHODS_KEY] = serializeItems(list)
        }
    }

    suspend fun importCustomItems(categories: List<CustomItem>, payments: List<CustomItem>) {
        context.dataStore.edit { prefs ->
            val existingCats = parseItems(prefs[CUSTOM_CATEGORIES_KEY]).toMutableList()
            categories.forEach { cat ->
                if (existingCats.none { it.name == cat.name }) existingCats.add(cat)
                else {
                    // Update existing if it matches by name but has different color/icon
                    val index = existingCats.indexOfFirst { it.name == cat.name }
                    existingCats[index] = cat
                }
            }
            prefs[CUSTOM_CATEGORIES_KEY] = serializeItems(existingCats)

            val existingPays = parseItems(prefs[CUSTOM_PAYMENT_METHODS_KEY]).toMutableList()
            payments.forEach { pay ->
                if (existingPays.none { it.name == pay.name }) existingPays.add(pay)
                else {
                    val index = existingPays.indexOfFirst { it.name == pay.name }
                    existingPays[index] = pay
                }
            }
            prefs[CUSTOM_PAYMENT_METHODS_KEY] = serializeItems(existingPays)
        }
    }

    // ── Serialization ────────────────────────────────────────────────────────

    private fun parseItems(json: String?): List<CustomItem> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CustomItem(
                    name     = obj.getString("name"),
                    iconKey  = obj.getString("iconKey"),
                    colorHex = obj.getString("colorHex"),
                    type     = obj.optString("type", "expense")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeItems(items: List<CustomItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("name",     item.name)
                put("iconKey",  item.iconKey)
                put("colorHex", item.colorHex)
                put("type",     item.type)
            })
        }
        return arr.toString()
    }
}

/**
 * Lightweight model for a user-created category or payment method.
 * [type] is "income", "expense", or "payment".
 */
data class CustomItem(
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val type: String = "expense"
)