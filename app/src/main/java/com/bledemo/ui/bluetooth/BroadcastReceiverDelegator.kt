package com.bledemo.ui.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.bledemo.ui.bluetooth.BluetoothController
import com.bledemo.ui.bluetooth.BluetoothController.Companion.deviceToString
import java.io.Closeable

class BroadcastReceiverDelegator(
    private val context: Context,
    // Callback for Bluetooth events.
    private val listener: BluetoothDiscoveryDeviceListener?, bluetooth: BluetoothController?
) : BroadcastReceiver(), Closeable {

    //Tag string used for logging.
    private val TAG = "BroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Incoming intent : $action")
        when (action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(
                    TAG, "Device discovered! " + deviceToString(
                        context as Activity,
                        device!!
                    )
                )
                listener?.onDeviceDiscovered(device)
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                // Discovery has ended.
                Log.d(TAG, "Discovery ended.")
                listener?.onDeviceDiscoveryEnd()
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                // Discovery state changed.
                Log.d(TAG, "Bluetooth state changed.")
                listener?.onBluetoothStatusChanged()
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                // Pairing state has changed.
                Log.d(TAG, "Bluetooth bonding state changed.")
                listener?.onDevicePairingEnded()
            }
            else -> {}
        }
    }

    //Called when device discovery starts.
    fun onDeviceDiscoveryStarted() {
        listener?.onDeviceDiscoveryStarted()
    }

    //Called when device discovery ends.
    fun onDeviceDiscoveryEnd() {
        listener?.onDeviceDiscoveryEnd()
    }

    // Called when the Bluetooth has been enabled.
    fun onBluetoothTurningOn() {
        listener?.onBluetoothTurningOn()
    }

    override fun close() {
        context.unregisterReceiver(this)
    }

    /**
     * Instantiates a new BroadcastReceiverDelegator.
     *
     * @param context   the context of this object.
     * @param listener  a callback for handling Bluetooth events.
     * @param bluetooth a controller for the Bluetooth.
     */
    init {
        listener?.setBluetoothController(bluetooth)
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(this, filter)
    }
}