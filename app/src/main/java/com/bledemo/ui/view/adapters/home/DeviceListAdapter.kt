package com.bledemo.ui.view.adapters.home

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.bledemo.R
import com.bledemo.databinding.ItemDeviceListBinding
import com.bledemo.interfaces.ListInteractionListener
import com.bledemo.ui.bluetooth.BluetoothController
import com.bledemo.ui.bluetooth.BluetoothDiscoveryDeviceListener

class DeviceListAdapter(
    private val list: List<BluetoothDevice>,
    private val interactionListener: ListInteractionListener<BluetoothDevice?>? = null,
    private val alreadyPaired: Boolean,
) :
    RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder>(),
    BluetoothDiscoveryDeviceListener {

    private var bluetooth: BluetoothController? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemDeviceListBinding.inflate(layoutInflater, parent, false)
        return DeviceListViewHolder(binding)
    }

    class DeviceListViewHolder(private val binding: ItemDeviceListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            bluetoothDevice: BluetoothDevice,
            alreadyPaired: Boolean,
            interactionListener: ListInteractionListener<BluetoothDevice?>?,
        ) {
            if (!alreadyPaired) {
                binding.deviceIcon.setImageResource(R.drawable.ic_bluetooth_searching)
            } else {
                binding.deviceIcon.setImageResource(R.drawable.ic_bluetooth_connected_black)
            }
            if (ActivityCompat.checkSelfPermission(
                    binding.root.context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Toast.makeText(binding.root.context,
                        "Permission not allowed",
                        Toast.LENGTH_SHORT)
                } else {
                    binding.deviceName.text =
                        if (!bluetoothDevice.name.isNullOrEmpty() && !bluetoothDevice.name.isNullOrBlank()) bluetoothDevice.name else "Unknown Device"
                    binding.deviceAddress.text = bluetoothDevice.address
                }
            } else {
                binding.deviceName.text =
                    if (!bluetoothDevice.name.isNullOrEmpty() && !bluetoothDevice.name.isNullOrBlank()) bluetoothDevice.name else "Unknown Device"
                binding.deviceAddress.text = bluetoothDevice.address
            }

            binding.llMain.setOnClickListener {
                interactionListener?.onItemClick(bluetoothDevice)
            }
        }
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        holder.bind(list[position], alreadyPaired, interactionListener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onDeviceDiscovered(device: BluetoothDevice?) {
        interactionListener?.endLoading(true)
        notifyDataSetChanged()
    }

    override fun onDeviceDiscoveryStarted() {
        interactionListener?.startLoading()
    }

    override fun setBluetoothController(bluetooth: BluetoothController?) {
        this.bluetooth = bluetooth
    }

    override fun onDeviceDiscoveryEnd() {
        interactionListener?.endLoading(false)
    }

    override fun onBluetoothStatusChanged() {
        bluetooth!!.onBluetoothStatusChanged()
    }

    override fun onBluetoothTurningOn() {
        interactionListener?.startLoading()
    }

    override fun onDevicePairingEnded() {
        if (bluetooth!!.isPairingInProgress) {
            val device = bluetooth!!.boundingDevice
            when (bluetooth!!.pairingDeviceStatus) {
                BluetoothDevice.BOND_BONDING -> {}
                BluetoothDevice.BOND_BONDED -> {
                    // Successfully paired.
                    interactionListener?.endLoadingWithDialog(false, device)

                    // Updates the icon for this element.
                    notifyDataSetChanged()
                }
                BluetoothDevice.BOND_NONE ->                     // Failed pairing.
                    interactionListener?.endLoadingWithDialog(true, device)
            }
        }
    }
}