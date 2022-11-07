package com.bledemo.ui.view.fragments.devices

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bledemo.database.model.SyncDataModel
import com.bledemo.databinding.FragmentDevicesBinding
import com.bledemo.interfaces.ListInteractionListener
import com.bledemo.model.DeviceDetails
import com.bledemo.ui.bluetooth.BluetoothController
import com.bledemo.ui.view.BaseActivity
import com.bledemo.ui.BaseFragment
import com.bledemo.ui.view.adapters.home.DeviceListAdapter
import com.bledemo.ui.viewModel.home.HomeViewModel
import com.bledemo.utils.gone
import com.bledemo.utils.showToast
import com.bledemo.utils.visible
import com.google.android.material.snackbar.Snackbar
import okhttp3.internal.notify
import java.util.*

class DevicesFragment : BaseFragment(),
    ListInteractionListener<BluetoothDevice?> {

    private lateinit var binding: FragmentDevicesBinding
    private val viewModel by lazy { HomeViewModel(requireContext()) }
    var mBlueAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 0
    private val CONNECT_DEVICES = 11
    var discoveredDevicesAdapter: DeviceListAdapter? = null
    var pairedDevicesAdapter: DeviceListAdapter? = null
    private var discoveredDevices: MutableList<BluetoothDevice> = mutableListOf()
    private var pairedDevices: MutableList<BluetoothDevice> = mutableListOf()
    private var bondingProgressDialog: ProgressDialog? = null
    private var bluetooth: BluetoothController? = null
    var mGatt: BluetoothGatt? = null
    private var pairedDevice: BluetoothDevice? = null
    var characteristic: BluetoothGattCharacteristic? = null
    val requestUpdateData: MutableLiveData<SyncDataModel> = MutableLiveData()
    val bleGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == STATE_CONNECTED) {
                gatt?.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            // add service uuid and characterstic uuid having your device data here
            characteristic =
                gatt?.getService(UUID.fromString("00000af0-0000-1000-8000-00805f9b34fb"))
                    ?.getCharacteristic(UUID.fromString("00000af7-0000-1000-8000-00805f9b34fb"))

            gatt?.setCharacteristicNotification(characteristic, true)

            val descriptor: BluetoothGattDescriptor? =
                characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

            if (characteristic != null)
                gatt?.readCharacteristic(characteristic)

            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            Handler(Looper.getMainLooper()).postDelayed({
                gatt?.writeDescriptor(descriptor)
            }, 2000)
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int,
        ) {
            // add service uuid and characterstic uuid having your device data here
            characteristic =
                gatt?.getService(UUID.fromString("00000af0-0000-1000-8000-00805f9b34fb"))
                    ?.getCharacteristic(UUID.fromString("00000af7-0000-1000-8000-00805f9b34fb"))
            Handler(Looper.getMainLooper()).postDelayed({
                gatt?.readCharacteristic(characteristic)
            }, 2000)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = characteristic.value
            Log.e("onCharacteristicChanged: ", value.toString())
            view?.let {
                Snackbar.make(it, "Saving data...", Snackbar.LENGTH_SHORT).show()
            }
            //Parse data to get values
//            parseData()
            saveData(value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int,
        ) {
            val value = characteristic?.value
            saveData(value)
            Log.e("onCharacteristicRead: ", Arrays.toString(value))
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int,
        ) {
            Log.e("onDescriptorRead: ", descriptor?.value.toString())
            Handler(Looper.getMainLooper()).postDelayed({
                gatt?.readCharacteristic(characteristic)
            }, 2000)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int,
        ) {
            if (status == 0) {
                val value = characteristic?.value
                Log.e("onCharacteristicWrite: ", Arrays.toString(value))
//Parse data to get values
//            parseData()
                saveData(value!!)
            } else {
                Log.e("onCharacteristicWrite: ", "ERROR....")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDevicesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Ensures that the Bluetooth is available on this device before proceeding.
        ensureBluetoothAvailable()

        binding.rvPairedDeviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAvailableDeviceList.layoutManager = LinearLayoutManager(requireContext())
        pairedDevicesAdapter = DeviceListAdapter(pairedDevices, this, true)
        discoveredDevicesAdapter = DeviceListAdapter(discoveredDevices, this, false)
        binding.rvPairedDeviceList.adapter = pairedDevicesAdapter
        binding.rvAvailableDeviceList.adapter = discoveredDevicesAdapter
        mBlueAdapter =
            (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        requestUpdateData.observe(viewLifecycleOwner) {
            viewModel.updateData(it)
        }
        viewModel.responseUpdateData.observe(viewLifecycleOwner) {
            notifySaved()
        }

        // Sets up the bluetooth controller.
        bluetooth = BluetoothController(
            requireActivity(),
            BluetoothAdapter.getDefaultAdapter(),
            pairedDevicesAdapter
        )
        getPairedDevices()
        getAvailableDevices()
    }

    private fun getAvailableDevices() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ), 0
            )
        } else {
            requireContext().registerReceiver(
                receiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            requireContext().registerReceiver(
                receiver,
                IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            )
            requireContext().registerReceiver(
                receiver,
                IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            )
            mBlueAdapter?.startDiscovery().toString()
        }
    }

    private fun getPairedDevices() {
        if (mBlueAdapter!!.isEnabled) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        requireActivity(), arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ), REQUEST_ENABLE_BT
                    )
                } else {
                    for (i in mBlueAdapter!!.bondedDevices.indices) {
                        pairedDevices.add(mBlueAdapter!!.bondedDevices.elementAt(i))
                    }
                    pairedDevicesAdapter?.notifyDataSetChanged()
                }
            } else {
                for (i in mBlueAdapter!!.bondedDevices.indices) {
                    pairedDevices.add(mBlueAdapter!!.bondedDevices.elementAt(i))
                }
                pairedDevicesAdapter?.notifyDataSetChanged()
            }
        } else {
            //bluetooth is off so can't get paired devices
            showToast("Turn on bluetooth to get paired devices")
        }
    }

    private fun ensureBluetoothAvailable() {
        val hasBluetooth: Boolean =
            requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!hasBluetooth) {
            val dialog: AlertDialog = AlertDialog.Builder(requireContext()).create()
            dialog.setTitle("Bluetooth not available")
            dialog.setMessage(
                "\n" +
                        "It seems that this device doesn't have a Bluetooth controller available. Blue Pair works only with Bluetooth, so please try again on a different device. The app will be now terminated."
            )
            dialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, "OK"
            ) { dialog, which -> // Closes the dialog and terminates the activity.
                dialog.dismiss()
                requireActivity().finish()
            }
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    for (i in mBlueAdapter!!.bondedDevices.indices) {
                        pairedDevices.add(mBlueAdapter!!.bondedDevices.elementAt(i))
                    }
                    pairedDevicesAdapter?.notifyDataSetChanged()
                } else {
                    showToast("could't on bluetooth")
                }
            }
            0 -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    requireContext().registerReceiver(
                        receiver,
                        IntentFilter(BluetoothDevice.ACTION_FOUND)
                    )
                    requireContext().registerReceiver(
                        receiver,
                        IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    )
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(
                                requireActivity(), arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                ), REQUEST_ENABLE_BT
                            )
                        } else {
                            requireContext().registerReceiver(
                                receiver,
                                IntentFilter(BluetoothDevice.ACTION_FOUND)
                            )
                            requireContext().registerReceiver(
                                receiver,
                                IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                            )
                            requireContext().registerReceiver(
                                receiver,
                                IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                            )
                            mBlueAdapter?.startDiscovery().toString()
                        }
                    } else {
                        requireContext().registerReceiver(
                            receiver,
                            IntentFilter(BluetoothDevice.ACTION_FOUND)
                        )
                        requireContext().registerReceiver(
                            receiver,
                            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                        )
                        requireContext().registerReceiver(
                            receiver,
                            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                        )
                        mBlueAdapter?.startDiscovery().toString()
                    }
                } else {
                    showToast("could't on location")
                }
            }
            CONNECT_DEVICES -> {
//                requireContext().sendBroadcast(intent)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.e("BluetoothAdapter.ACTION_DISCOVERY_STARTED", "discovery started")
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    addDiscoveredDevice(context, device)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {

                }
            }
        }
    }

    private fun addDiscoveredDevice(context: Context, device: BluetoothDevice) {
        if (!discoveredDevices.contains(device)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        requireActivity(), arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT
                        ), REQUEST_ENABLE_BT
                    )
                } else {
                    if (device.bondState == BluetoothDevice.BOND_BONDED)
                        return

                    Log.e(
                        "Device Details :- ",
                        "Device Name : ${device.name} ----- Device Address : ${device.address}"
                    )
                    discoveredDevices.add(device)
                }
            } else {
                if (device.bondState == BluetoothDevice.BOND_BONDED)
                    return
                Log.e(
                    "Device Details :- ",
                    "Device Name : ${device.name} ----- Device Address : ${device.address}"
                )
                discoveredDevices.add(device)
            }
            discoveredDevicesAdapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        bluetooth!!.close()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        // Stoops the discovery.
        if (bluetooth != null) {
            bluetooth!!.cancelDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onItemClick(device: BluetoothDevice?) {
        if (mGatt != null) {
            Log.i("Rides", "Closing bluetooth gatt on disconnect");
            mGatt!!.close()
            mGatt!!.disconnect()
            mGatt = null
        }
        Log.d("Home Fragment", "Item clicked : " + device?.let {
            BluetoothController.deviceToString(
                requireActivity(),
                it
            )
        })
        if (bluetooth!!.isAlreadyPaired(device)) {
            Log.d("Home Fragment", "Device already paired!")
            view?.let {
                Snackbar.make(it, "Fetching data from device...", Snackbar.LENGTH_SHORT).show()
            }
            readData(device)
        } else {
            Log.d("Home Fragment", "Device not paired. Pairing.")
            val outcome = device?.let { bluetooth!!.pair(it) }

            // Prints a message to the user.
            val deviceName = BluetoothController.getDeviceName(requireActivity(), device)
            if (outcome == true) {
                // The pairing has started, shows a progress dialog.
                Log.d("Home Fragment", "Showing pairing dialog")
                bondingProgressDialog = ProgressDialog.show(
                    requireContext(), "",
                    "Pairing with device $deviceName...", true, false
                )
            } else {
                Log.d("Home Fragment", "Error while pairing with device $deviceName!")
                Toast.makeText(
                    requireContext(),
                    "Error while pairing with device $deviceName!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun startLoading() {
    }

    override fun endLoading(partialResults: Boolean) {
    }

    @SuppressLint("MissingPermission")
    override fun endLoadingWithDialog(error: Boolean, device: BluetoothDevice?) {
        if (bondingProgressDialog != null) {
            val message: String
            val deviceName = BluetoothController.getDeviceName(requireActivity(), device)

            // Gets the message to print.
            if (error) {
                view?.let {
                    Snackbar.make(
                        it,
                        "Failed pairing with device $deviceName!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                // Dismisses the progress dialog and prints a message to the user.
                bondingProgressDialog!!.dismiss()
                // Cleans up state.
                bondingProgressDialog = null
            } else {
                val details = DeviceDetails()
                details.deviceName = device?.name
                details.address = device?.address
                session?.deviceData = details
                session?.savedDeviceData = details
                view?.let {
                    Snackbar.make(
                        it, "Successfully paired with device $deviceName!", Snackbar.LENGTH_SHORT
                    ).show()
                }
                // Dismisses the progress dialog and prints a message to the user.
                bondingProgressDialog!!.dismiss()
                // Cleans up state.
                bondingProgressDialog = null
                findNavController().navigateUp()
            }
        }
    }

    private fun readData(device: BluetoothDevice?) {
        if (device != null) {
            session?.clearData()
            val details = DeviceDetails()
            details.deviceName = device.name
            details.address = device.address
            session?.deviceData = details
            session?.savedDeviceData = details
            pairedDevice = device
//            Log.e("Device Class: ", device.bluetoothClass.deviceClass.toString())
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        requireActivity(), arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                        ), 1122
                    )
                } else {
                    mGatt = device.connectGatt(context, true, bleGattCallback, 2)
                }
            } else {
                mGatt = device.connectGatt(context, true, bleGattCallback, 2)
            }
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    fun saveData(byteArray: ByteArray?) {
        if (byteArray == null) return
        val model = SyncDataModel()
        if (pairedDevice != null) {
            model.macAddress = pairedDevice?.address
            model.deviceName = pairedDevice?.name
        }
        model.uuid = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ANDROID_ID
        )

        model.data = byteArray.toString()
        model.timeInMillis = System.currentTimeMillis()

        // insert data in local db
        (context as BaseActivity).syncDataDao.insertAll(model)
        requestUpdateData.postValue(model)
    }

}