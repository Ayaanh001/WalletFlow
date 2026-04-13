package com.hussain.walletflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.TransactionDatabase
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.notification.SmsNotificationHelper
import com.hussain.walletflow.utils.SmsParser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SmsReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val currencySymbol = runBlocking {
            UserPreferencesRepository(context).currencyFlow.first()
                .let { code -> CurrencyData.currencies.find { it.code == code }?.symbol ?: code }
        }

        val pendingResult = goAsync()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val dao = TransactionDatabase.getDatabase(context).transactionDao()
                val alreadyInDb = HashSet(dao.getAllExistingSmsTexts())

                for (smsMessage in messages) {
                    val sender    = smsMessage.displayOriginatingAddress ?: continue
                    val body      = smsMessage.messageBody              ?: continue
                    val timestamp = smsMessage.timestampMillis

                    Log.d("SmsReceiver", "SMS from: $sender")

                    if (body in alreadyInDb) {
                        Log.d("SmsReceiver", "Already in DB, skipping")
                        continue
                    }

                    if (SmsParser.isBankingMessage(sender, body)) {
                        Log.d("SmsReceiver", "Banking SMS detected")
                        val transaction = SmsParser.parseTransaction(sender, body, timestamp)
                        if (transaction != null) {
                            val savedId = dao.insert(transaction)
                            if (savedId > 0) {
                                alreadyInDb.add(body)
                                Log.d("SmsReceiver", "Transaction saved: ${transaction.amount}")
                                withContext(Dispatchers.Main) {
                                    SmsNotificationHelper.postTransactionNotification(context, transaction.copy(id = savedId), currencySymbol)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error saving transaction: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}