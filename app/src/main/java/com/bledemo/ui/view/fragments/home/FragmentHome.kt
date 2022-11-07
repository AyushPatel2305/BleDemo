package com.bledemo.ui.view.fragments.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bledemo.R
import com.bledemo.database.model.SyncDataModel
import com.bledemo.databinding.FragmentHomeBinding
import com.bledemo.model.DeviceDetails
import com.bledemo.ui.BaseFragment
import com.bledemo.ui.view.BaseActivity
import com.bledemo.ui.view.adapters.home.TestConnectionAdapter
import com.bledemo.ui.viewModel.home.HomeViewModel
import com.bledemo.utils.gone
import com.bledemo.utils.showToast
import com.bledemo.utils.visible
import com.google.android.material.snackbar.Snackbar
import okhttp3.internal.notify
import java.util.*

class FragmentHome : BaseFragment(), View.OnClickListener {
    private val viewModel by lazy { HomeViewModel(requireContext()) }
    private lateinit var homeBinding: FragmentHomeBinding
    private val REQUEST_ENABLE_BT = 0
    private val REQUEST_DISABLE_BT = 123
    var mBlueAdapter: BluetoothAdapter? = null
    private var pairedDevices: MutableList<BluetoothDevice> = mutableListOf()
    var mGatt: BluetoothGatt? = null
    private var pairedDevice: BluetoothDevice? = null
    private var testConnectionAdapter: TestConnectionAdapter? = null
    private var testConnectionList: List<SyncDataModel> = emptyList()
    var characteristic: BluetoothGattCharacteristic? = null
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
                gatt?.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))
                    ?.getCharacteristic(UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb"))

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
                gatt?.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))
                    ?.getCharacteristic(UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb"));
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


    }
    val requestUpdateData: MutableLiveData<SyncDataModel> = MutableLiveData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        homeBinding = FragmentHomeBinding.inflate(
            inflater,
            container,
            false
        )
        homeBinding.lifecycleOwner = this
        homeBinding.model = viewModel
        return homeBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBlueAdapter = BluetoothAdapter.getDefaultAdapter()
        mBlueAdapter =
            (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        requestUpdateData.observe(viewLifecycleOwner) {
                viewModel.updateData(it)
        }

        viewModel.responseUpdateData.observe(viewLifecycleOwner) {
            notifySaved()
        }

        //check if bluetooth is available or not
        if (mBlueAdapter == null) {
            homeBinding.statusBluetoothTv.text = "Bluetooth is not available"
        } else {
            homeBinding.statusBluetoothTv.text = "Bluetooth is available"
        }

        if (mGatt != null) {
            Log.i("Rides", "Closeing bluetooth gatt on disconnect");
            mGatt!!.close()
            mGatt!!.disconnect()
            mGatt = null
        }

        if (session?.isConnected == true) {
            checkForDetails(session?.deviceData)
        }

        //set image according to bluetooth status(on/off)
        val adapter = mBlueAdapter
        if (adapter != null) {
            if (adapter.isEnabled) {
                homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_on)
            } else {
                homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_off)
            }
        }
        homeBinding.tvConnectionStatus.paintFlags =
            homeBinding.tvConnectionStatus.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        homeBinding.onBtn.setOnClickListener(this)
        homeBinding.offBtn.setOnClickListener(this)
        homeBinding.discoverableBtn.setOnClickListener(this)
        homeBinding.btnTestConnection.setOnClickListener(this)

//        if (savedInstanceState == null) viewModel.init()
    }

    val registerForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_on)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.onBtn -> {
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
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        registerForResult.launch(enableBtIntent)
                    }
                } else {
                    if (!mBlueAdapter!!.isEnabled) {
                        mBlueAdapter!!.enable()
                        homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_on)
                    } else {
                        showToast("Bluetooth is already on")
                    }
                }
            }
            R.id.offBtn -> {
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
                            ), REQUEST_DISABLE_BT
                        )
                    } else {
                        if (mBlueAdapter!!.isEnabled) {
                            mBlueAdapter!!.disable()
                            showToast("Turning Bluetooth Off")
                            homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_off)
                        } else {
                            showToast("Bluetooth is already off")
                        }
                    }
                } else {
                    if (mBlueAdapter!!.isEnabled) {
                        mBlueAdapter!!.disable()
                        showToast("Turning Bluetooth Off")
                        homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_off)
                    } else {
                        showToast("Bluetooth is already off")
                    }
                }

            }
            R.id.discoverableBtn -> {
                if (canGetLocation()) {
                    if (mBlueAdapter!!.isEnabled) {
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
                            if (ContextCompat.checkSelfPermission(
                                    requireActivity(),
                                    Manifest.permission.BLUETOOTH_SCAN
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    ActivityCompat.requestPermissions(
                                        requireActivity(), arrayOf(
                                            Manifest.permission.BLUETOOTH_SCAN,
                                        ), 111
                                    )
                                } else {
                                    val bundle = Bundle()
                                    bundle.putString("deviceListType", "All")
                                    findNavController().navigate(R.id.to_device_list, bundle)
                                }
                            } else {
                                val bundle = Bundle()
                                bundle.putString("deviceListType", "All")
                                findNavController().navigate(R.id.to_device_list, bundle)
                            }
                        }
                    } else {
                        showToast("Turn on bluetooth to get available devices")
                    }
                } else {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    showToast("Please turn on location")
                }
            }
            R.id.btnTestConnection -> {
                setData()
            }
        }
    }

    fun canGetLocation(): Boolean {
        return isLocationEnabled(requireContext().applicationContext) // application context
    }

    fun isLocationEnabled(context: Context): Boolean {
        var locationMode = 0
        val locationProviders: String
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        } else {
            locationProviders =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_MODE)
            !TextUtils.isEmpty(locationProviders)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (!mBlueAdapter!!.isEnabled) {
                        mBlueAdapter!!.isEnabled
                        homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_on)
                    } else {
                        showToast("Bluetooth is already on")
                    }
                } else {
                    showToast("Permission denied. Unable to use bluetooth")
                }
            }
            REQUEST_DISABLE_BT -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (mBlueAdapter!!.isEnabled) {
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                ActivityCompat.requestPermissions(
                                    requireActivity(), arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ), REQUEST_DISABLE_BT
                                )
                            } else {
                                mBlueAdapter!!.disable()
                                showToast("Turning Bluetooth Off")
                                homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_off)
                            }
                        } else {
                            mBlueAdapter!!.disable()
                            showToast("Turning Bluetooth Off")
                            homeBinding.bluetoothIv.setImageResource(R.drawable.ic_action_off)
                        }
                    } else {
                        showToast("Bluetooth is already off")
                    }
                } else {
                    showToast("could't on bluetooth")
                }
            }
            0 -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    val bundle = Bundle()
                    bundle.putString("deviceListType", "All")
                    findNavController().navigate(R.id.to_device_list, bundle)
                } else {
                    showToast("could't on location")
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkForDetails(deviceData: DeviceDetails?) {
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
                }
            } else {
                for (i in mBlueAdapter!!.bondedDevices.indices) {
                    pairedDevices.add(mBlueAdapter!!.bondedDevices.elementAt(i))
                }
            }
        } else {
            //bluetooth is off so can't get paired devices
            showToast("Turn on bluetooth to get paired devices")
        }

        if (pairedDevices.isNotEmpty()) {
            for (i in pairedDevices.indices) {
                if (pairedDevices[i].name == deviceData?.deviceName) {
                    pairedDevice = pairedDevices[i]
                }
            }
            homeBinding.llTestConnection.visible()
            SyncData(pairedDevice)
            setData()
        } else {
            homeBinding.llTestConnection.gone()
        }
    }

    private fun setData() {
        homeBinding.rvTestConnections.layoutManager = LinearLayoutManager(requireContext())
        homeBinding.tvConnectionStatus.text = "Connection Successful"
        homeBinding.ivConnectionStatus.setImageResource(R.drawable.ic_action_black)
        testConnectionList = ((context as BaseActivity).syncDataDao.fetchDeviceData()).reversed()
        testConnectionAdapter = TestConnectionAdapter(testConnectionList)
        homeBinding.rvTestConnections.adapter = testConnectionAdapter
    }

    private fun SyncData(device: BluetoothDevice?) {
        if (device != null) {
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

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (mGatt != null) {
            Log.i("Rides", "Closeing bluetooth gatt on disconnect");
            mGatt!!.close()
            mGatt!!.disconnect()
            mGatt = null
        }
        if (session?.isConnected == true) {
            checkForDetails(session?.deviceData)
        }
    }

    @SuppressLint("HardwareIds")
    fun saveData(byteArray: ByteArray?) {
        if (byteArray == null) return
        val model = SyncDataModel()
        if (session?.isConnected == true) {
            model.macAddress = session?.deviceData?.address
            model.deviceName = session?.deviceData?.deviceName
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

        /*view?.let {
            Snackbar.make(it, "Data Saved.", Snackbar.LENGTH_SHORT).show()
        }*/
    }

}