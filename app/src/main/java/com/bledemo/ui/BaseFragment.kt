package com.bledemo.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.bledemo.R
import com.bledemo.utils.Session
import okhttp3.internal.notify


open class BaseFragment : Fragment() {
    private val CHANNEL_ID: String = "default"
    lateinit var mContext: Context
    lateinit var mActivity: Activity
    var session: Session? = null
    var notification: Notification? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        session = Session(context)
        mContext = context
        mActivity = context as Activity

        var builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle("BleDemo")
            .setContentText("Data Updated")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        notification = builder.build()
    }

    fun notifySaved() {

        var channelId = "Default"

        val manager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setAutoCancel(true)
            .setContentTitle("BleDemo")
            .setContentText("Data Updated")


        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

}
