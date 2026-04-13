package com.hussain.walletflow.data

import android.content.Context
import androidx.compose.ui.input.key.Key.Companion.U
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val DELETE_FROM_PASSBOOK_KEY = booleanPreferencesKey("delete_from_passbook")
        val NAME_KEY = stringPreferencesKey("user_name")
        val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
        val HIDE_BALANCE_KEY = booleanPreferencesKey("hide_balance")
        val HIDE_INCOME_KEY  = booleanPreferencesKey("hide_income")
        const val DEFAULT_CURRENCY = "INR"
    }
    val currencyFlow: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[CURRENCY_KEY] ?: DEFAULT_CURRENCY
        }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { preferences -> preferences[CURRENCY_KEY] = currency }
    }

    val deleteFromPassbookFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DELETE_FROM_PASSBOOK_KEY] ?: true
        }

    suspend fun updateDeleteFromPassbook(delete: Boolean) {
        context.dataStore.edit { preferences -> preferences[DELETE_FROM_PASSBOOK_KEY] = delete }
    }

    val nameFlow: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[NAME_KEY] ?: "" }

    suspend fun updateName(name: String) {
        context.dataStore.edit { preferences -> preferences[NAME_KEY] = name }
    }

    val appLockEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[APP_LOCK_KEY] ?: false
        }

    suspend fun updateAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[APP_LOCK_KEY] = enabled }
    }

    val hideBalanceFlow: Flow<Boolean> =
        context.dataStore.data.map { it[HIDE_BALANCE_KEY] ?: false }

    suspend fun updateHideBalance(hide: Boolean) {
        context.dataStore.edit { it[HIDE_BALANCE_KEY] = hide }
    }

    val hideIncomeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[HIDE_INCOME_KEY] ?: false }

    suspend fun updateHideIncome(hide: Boolean) {
        context.dataStore.edit { it[HIDE_INCOME_KEY] = hide }
    }
}

data class Currency(val code: String, val symbol: String, val name: String)

object CurrencyData {
    val currencies =
        listOf(
            // Asia & South Asia
            Currency("INR", "₹", "Indian Rupee"),
            Currency("JPY", "¥", "Japanese Yen"),
            Currency("CNY", "¥", "Chinese Yuan"),
            Currency("SGD", "S$", "Singapore Dollar"),
            Currency("HKD", "HK$", "Hong Kong Dollar"),
            Currency("KRW", "₩", "South Korean Won"),
            Currency("THB", "฿", "Thai Baht"),
            Currency("MYR", "RM", "Malaysian Ringgit"),
            Currency("IDR", "Rp", "Indonesian Rupiah"),
            Currency("PHP", "₱", "Philippine Peso"),
            Currency("VND", "₫", "Vietnamese Dong"),
            Currency("PKR", "₨", "Pakistani Rupee"),
            Currency("BDT", "৳", "Bangladeshi Taka"),
            Currency("LKR", "Rs", "Sri Lankan Rupee"),
            Currency("NPR", "Rs", "Nepalese Rupee"),
            Currency("MMK", "K", "Myanmar Kyat"),
            Currency("KHR", "៛", "Cambodian Riel"),
            Currency("LAK", "₭", "Lao Kip"),
            Currency("MNT", "₮", "Mongolian Tögrög"),
            Currency("TWD", "NT$", "Taiwan Dollar"),
            Currency("KZT", "₸", "Kazakhstani Tenge"),
            Currency("UZS", "сум", "Uzbekistani Som"),
            Currency("GEL", "₾", "Georgian Lari"),
            Currency("AMD", "֏", "Armenian Dram"),
            Currency("AZN", "₼", "Azerbaijani Manat"),
            Currency("AFN", "؋", "Afghan Afghani"),

            // Middle East
            Currency("AED", "د.إ", "UAE Dirham"),
            Currency("SAR", "﷼", "Saudi Riyal"),
            Currency("ILS", "₪", "Israeli Shekel"),
            Currency("QAR", "﷼", "Qatari Riyal"),
            Currency("KWD", "د.ك", "Kuwaiti Dinar"),
            Currency("BHD", "BD", "Bahraini Dinar"),
            Currency("OMR", "﷼", "Omani Rial"),
            Currency("JOD", "JD", "Jordanian Dinar"),
            Currency("LBP", "ل.ل", "Lebanese Pound"),
            Currency("IQD", "ع.د", "Iraqi Dinar"),
            Currency("IRR", "﷼", "Iranian Rial"),
            Currency("YER", "﷼", "Yemeni Rial"),

            // Europe
            Currency("USD", "$", "US Dollar"),
            Currency("EUR", "€", "Euro"),
            Currency("GBP", "£", "British Pound"),
            Currency("CHF", "Fr", "Swiss Franc"),
            Currency("SEK", "kr", "Swedish Krona"),
            Currency("NOK", "kr", "Norwegian Krone"),
            Currency("DKK", "kr", "Danish Krone"),
            Currency("PLN", "zł", "Polish Zloty"),
            Currency("CZK", "Kč", "Czech Koruna"),
            Currency("HUF", "Ft", "Hungarian Forint"),
            Currency("RON", "lei", "Romanian Leu"),
            Currency("BGN", "лв", "Bulgarian Lev"),
            Currency("HRK", "kn", "Croatian Kuna"),
            Currency("UAH", "₴", "Ukrainian Hryvnia"),
            Currency("RUB", "₽", "Russian Ruble"),
            Currency("TRY", "₺", "Turkish Lira"),
            Currency("ISK", "kr", "Icelandic Króna"),
//            Currency("HUF", "Ft", "Hungarian Forint"),
            Currency("ALL", "L", "Albanian Lek"),
            Currency("BAM", "KM", "Bosnia-Herzegovina Mark"),
            Currency("MKD", "ден", "Macedonian Denar"),
            Currency("RSD", "дин.", "Serbian Dinar"),
            Currency("MDL", "L", "Moldovan Leu"),
            Currency("BYN", "Br", "Belarusian Ruble"),

            // Americas
            Currency("CAD", "C$", "Canadian Dollar"),
            Currency("MXN", "$", "Mexican Peso"),
            Currency("BRL", "R$", "Brazilian Real"),
            Currency("ARS", "$", "Argentine Peso"),
            Currency("CLP", "$", "Chilean Peso"),
            Currency("COP", "$", "Colombian Peso"),
            Currency("PEN", "S/.", "Peruvian Sol"),
//            Currency("UYU", "$U", "Uruguayan Peso"),
            Currency("BOB", "Bs.", "Bolivian Boliviano"),
            Currency("PYG", "₲", "Paraguayan Guaraní"),
            Currency("VES", "Bs.S", "Venezuelan Bolívar"),
            Currency("GTQ", "Q", "Guatemalan Quetzal"),
            Currency("HNL", "L", "Honduran Lempira"),
            Currency("CRC", "₡", "Costa Rican Colón"),
            Currency("NIO", "C$", "Nicaraguan Córdoba"),
            Currency("DOP", "RD$", "Dominican Peso"),
            Currency("JMD", "J$", "Jamaican Dollar"),
            Currency("TTD", "TT$", "Trinidad & Tobago Dollar"),
            Currency("BBD", "Bds$", "Barbadian Dollar"),
            Currency("BSD", "B$", "Bahamian Dollar"),

            // Oceania
            Currency("AUD", "A$", "Australian Dollar"),
            Currency("NZD", "NZ$", "New Zealand Dollar"),
            Currency("FJD", "FJ$", "Fijian Dollar"),
            Currency("PGK", "K", "Papua New Guinean Kina"),
            Currency("WST", "WS$", "Samoan Tālā"),
            Currency("TOP", "T$", "Tongan Paʻanga"),

            // Africa
            Currency("ZAR", "R", "South African Rand"),
            Currency("NGN", "₦", "Nigerian Naira"),
            Currency("KES", "KSh", "Kenyan Shilling"),
            Currency("GHS", "₵", "Ghanaian Cedi"),
            Currency("EGP", "£", "Egyptian Pound"),
            Currency("MAD", "د.م.", "Moroccan Dirham"),
            Currency("TND", "د.ت", "Tunisian Dinar"),
            Currency("DZD", "دج", "Algerian Dinar"),
            Currency("ETB", "Br", "Ethiopian Birr"),
            Currency("TZS", "TSh", "Tanzanian Shilling"),
            Currency("UGX", "USh", "Ugandan Shilling"),
            Currency("RWF", "Fr", "Rwandan Franc"),
            Currency("ZMW", "ZK", "Zambian Kwacha"),
            Currency("MWK", "MK", "Malawian Kwacha"),
            Currency("MOZ", "MT", "Mozambican Metical"),
            Currency("AOA", "Kz", "Angolan Kwanza"),
            Currency("XOF", "Fr", "West African CFA Franc"),
            Currency("XAF", "Fr", "Central African CFA Franc"),
            Currency("MUR", "₨", "Mauritian Rupee"),
            Currency("SCR", "₨", "Seychellois Rupee"),
            Currency("LYD", "ل.د", "Libyan Dinar"),
            Currency("SDG", "ج.س.", "Sudanese Pound"),
            Currency("SOS", "Sh", "Somali Shilling"),
            Currency("MZN", "MT", "Mozambican Metical"),
            Currency("BWP", "P", "Botswana Pula"),
            Currency("NAD", "N$", "Namibian Dollar"),
            Currency("ZWL", "Z$", "Zimbabwean Dollar"),
            Currency("MGA", "Ar", "Malagasy Ariary"),
            Currency("GMD", "D", "Gambian Dalasi"),
            Currency("GNF", "Fr", "Guinean Franc"),
            Currency("SLL", "Le", "Sierra Leonean Leone"),
            Currency("LRD", "L$", "Liberian Dollar"),
            Currency("CVE", "$", "Cape Verdean Escudo"),
            Currency("STN", "Db", "São Tomé & Príncipe Dobra"),
            Currency("DJF", "Fr", "Djiboutian Franc"),
            Currency("KMF", "Fr", "Comorian Franc"),
            Currency("ERN", "Nfk", "Eritrean Nakfa"),
            Currency("SSP", "£", "South Sudanese Pound"),
        )
}