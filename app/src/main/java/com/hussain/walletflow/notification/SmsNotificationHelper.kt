package com.hussain.walletflow.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hussain.walletflow.R
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionType
import java.text.NumberFormat
import java.util.Locale

object SmsNotificationHelper {

    const val CHANNEL_ID  = "bank_sms_transactions"
    const val CHANNEL_NAME = "Bank SMS Transactions"

    const val EXTRA_TRANSACTION_ID  = "transaction_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    const val ACTION_ADD_DIRECT = "com.hussain.walletflow.ACTION_ADD_DIRECT"
    const val ACTION_DISMISS    = "com.hussain.walletflow.ACTION_DISMISS"
    const val ACTION_QUICK_ADD  = "com.hussain.walletflow.ACTION_QUICK_ADD"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Quick-add actions for detected bank transactions"
                enableVibration(true)
                setShowBadge(true)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    fun postTransactionNotification(context: Context, transaction: Transaction, currencySymbol: String = "₹") {
        createChannel(context)
        val notifId = transaction.id.toInt()
            .let { if (it == 0) System.currentTimeMillis().toInt() else it }

        val amountStr = formatAmount(transaction.amount)
        val icon  = if (transaction.type == TransactionType.INCOME) "🟢" else "🔴"
        val dir   = if (transaction.type == TransactionType.INCOME) "credited" else "debited"
        val title = "$icon $currencySymbol$amountStr $dir  •  ${transaction.bankName}"
        val body  = transaction.remark.ifBlank { "A/c ••••${transaction.accountLastFour}" }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_monochrome1)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$body\n\nCategory: ${transaction.category}  •  ${transaction.instrumentType}"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(quickAddPendingIntent(context, transaction, notifId, currencySymbol))
            .addAction(R.drawable.ic_income, "Add to Month",
                addDirectPendingIntent(context, transaction, notifId, currencySymbol))
            .addAction(R.drawable.ic_scan, "Edit & Add",
                quickAddPendingIntent(context, transaction, notifId, currencySymbol))
            .addAction(R.drawable.ic_expense, "Dismiss",
                dismissPendingIntent(context, transaction.id, notifId))

        nm.notify(notifId, builder.build())
    }

    private fun quickAddPendingIntent(ctx: Context, transaction: Transaction, notifId: Int, currencySymbol: String): PendingIntent {
        val intent = Intent(ctx, QuickAddActivity::class.java).apply {
            action = ACTION_QUICK_ADD
            putExtra(EXTRA_TRANSACTION_ID,   transaction.id)
            putExtra(EXTRA_NOTIFICATION_ID,  notifId)
            // Pass all editable fields directly — no DB lookup needed
            putExtra("tx_amount",            transaction.amount)
            putExtra("tx_type",              transaction.type.name)
            putExtra("tx_bank",              transaction.bankName)
            putExtra("tx_account",           transaction.accountLastFour)
            putExtra("tx_instrument",        transaction.instrumentType)
            putExtra("tx_remark",            transaction.remark)
            putExtra("tx_original_sms", transaction.originalSms)
            putExtra("tx_category",          transaction.category)
            putExtra("tx_payment",           transaction.paymentMethod)
            putExtra("tx_currency_symbol", currencySymbol)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(ctx, notifId * 10 + 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun addDirectPendingIntent(ctx: Context, transaction: Transaction, notifId: Int, currencySymbol: String): PendingIntent {
        val intent = Intent(ctx, NotificationActionReceiver::class.java).apply {
            action = ACTION_ADD_DIRECT
            putExtra(EXTRA_TRANSACTION_ID,  transaction.id)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
            // Same extras as quickAddPendingIntent so receiver can rebuild if needed
            putExtra("tx_amount",           transaction.amount)
            putExtra("tx_type",             transaction.type.name)
            putExtra("tx_bank",             transaction.bankName)
            putExtra("tx_account",          transaction.accountLastFour)
            putExtra("tx_instrument",       transaction.instrumentType)
            putExtra("tx_remark",           transaction.remark)
            putExtra("tx_original_sms", transaction.originalSms)
            putExtra("tx_category",         transaction.category)
            putExtra("tx_payment",          transaction.paymentMethod)
            putExtra("tx_currency_symbol", currencySymbol)
        }
        return PendingIntent.getBroadcast(ctx, notifId * 10 + 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun dismissPendingIntent(ctx: Context, txId: Long, notifId: Int): PendingIntent {
        val intent = Intent(ctx, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_TRANSACTION_ID, txId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        return PendingIntent.getBroadcast(ctx, notifId * 10 + 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun formatAmount(amount: Double): String =
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }.format(amount)

    fun cancel(context: Context, notifId: Int) =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(notifId)
}