**DeviceHive Android Gateway for BLE devices**
--------------------------------

This is demo for Bluetooth Low Energy(BLE) devices makes it possible to scan & connect with BLE devices. It also saves BLE raw data to local database & calls api to save database to cloud.<br>

[Demo Video](https://drive.google.com/file/d/1HxOtPsGNLujUC6DqwYlv0zBvQbbGR5Bp/view?usp=sharing)<br>

**Setup**
==================
1. Clone project from <i>master</i> branch to android studio
2. Build project<br>
### Changes in FragmentHome & DevicesFragment<br>
4. Change UUIDs in <i>onServicesDiscovered()</i> & in <i>onDescriptorWrite()</i> to the UUIDs of the BLE devices you want to use. (Note: This project contains UUIDs of Noise Colorfit Pro 2 fitness band)<br>
5. Add Data Parsing SDK of your BLE Device and add <i>parse()</i> in <i>onCharacteristicChanged()</i> and <i>onCharacteristicWrite()</i><br>
### Changes in Retrofit
6. Change baseUrl in Networking.kt to your API url<br>
7. Change url in APIInterface <i>updateData()</i> to the url pointing to your API<br><br>
8. Run Project


**Usage**
==================
1. Turn on your bluetooth or click on <i>Turn On</i> button in app.
2. Click on Discover to get all available and paired devices
3. Click on any BLE device and pair with it.
4. After device is paired click on Discover and click on Paired Device to get Device Data.

**Features**
==================
- Turn on/off Device Bluetooth
- Scan BLE Devices
- Pair with BLE Devices
- Read Raw data of BLE Device
- Save Raw data to local Database using ROOM
- Save Raw Data to cloud Database 
- Get Push Notification when data is sucessfully uploaded to cloud
