package com.example.samarium

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.telephony.*
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import com.example.samarium.databinding.ActivityMapsBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var cellInfoTextView: TextView
    private lateinit var locationTextView: TextView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    val cellInfoText = StringBuilder()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_PHONE_STATE] == true && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCellInfo()
            getCurrentLocation()
        } else {
            Log.d("MainActivity", "Permissions denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        locationTextView = findViewById(R.id.locationTextView)
        cellInfoTextView = findViewById(R.id.cellInfoTextView)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION))
            } else {
                getCellInfo()
                getCurrentLocation()
            }
        } else {
            getCellInfo()
            getCurrentLocation()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    // Store location data in the database
                    storeLocationData(location)
                    locationTextView.text = "Loc: $currentLatLng\n\n"
                }
            }
        }
    }

    private fun storeLocationData(location: Location) {
        val db = dbHelper.writableDatabase
        val contentValues = ContentValues()

        val eventTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        contentValues.put(DatabaseHelper.COLUMN_EVENT_TIME, eventTime)
        contentValues.put(DatabaseHelper.COLUMN_LATITUDE, location.latitude)
        contentValues.put(DatabaseHelper.COLUMN_LONGITUDE, location.longitude)

        db.insert(DatabaseHelper.TABLE_NAME, null, contentValues)
    }

    private fun getCellInfo() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val cellInfoList: List<CellInfo> = telephonyManager.allCellInfo

        val networkType = getNetworkType(telephonyManager.networkType)
        cellInfoText.append("Network Type: $networkType\n\n")

        val db = dbHelper.writableDatabase

        for (cellInfo in cellInfoList) {
            val contentValues = ContentValues()

            when (cellInfo) {
                is CellInfoLte -> {
                    val cellIdentityLte = cellInfo.cellIdentity
                    val cellSignalStrengthLte = cellInfo.cellSignalStrength

                    val plmnId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        cellIdentityLte.mobileNetworkOperator
                    } else {
                        "N/A"
                    }
                    val tac = cellIdentityLte.tac
                    val cellId = cellIdentityLte.ci
                    val rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        cellSignalStrengthLte.rsrp
                    } else {
                        "N/A"
                    }
                    val rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        cellSignalStrengthLte.rsrq
                    } else {
                        "N/A"
                    }

                    val eventTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                    contentValues.put(DatabaseHelper.COLUMN_EVENT_TIME, eventTime)
                    contentValues.put(DatabaseHelper.COLUMN_PLMN_ID, plmnId)
                    contentValues.put(DatabaseHelper.COLUMN_TAC, tac)
                    contentValues.put(DatabaseHelper.COLUMN_CELL_ID, cellId)
                    contentValues.put(DatabaseHelper.COLUMN_RSRP, rsrp.toString())
                    contentValues.put(DatabaseHelper.COLUMN_RSRQ, rsrq.toString())
                    contentValues.put(DatabaseHelper.COLUMN_TECHNOLOGY, "4G (LTE)")

                    db.insert(DatabaseHelper.TABLE_NAME, null, contentValues)

                    cellInfoText.append("Event Time: $eventTime\n")
                    cellInfoText.append("PLMN ID: $plmnId\n")
                    cellInfoText.append("TAC: $tac\n")
                    cellInfoText.append("Cell ID: $cellId\n")
                    cellInfoText.append("RSRP: $rsrp\n")
                    cellInfoText.append("RSRQ: $rsrq\n")
                    cellInfoText.append("Cell Technology: 4G (LTE)\n\n")
                }
                is CellInfoWcdma -> {
                    val cellIdentityWcdma = cellInfo.cellIdentity
                    val cellSignalStrengthWcdma = cellInfo.cellSignalStrength

                    val plmnId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        cellIdentityWcdma.mobileNetworkOperator
                    } else {
                        "N/A"
                    }
                    val lac = cellIdentityWcdma.lac
                    val cellId = cellIdentityWcdma.cid
                    val rscp = cellSignalStrengthWcdma.dbm
                    val ecn0 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cellSignalStrengthWcdma.ecNo
                    } else {
                        "N/A"
                    }

                    val eventTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                    contentValues.put(DatabaseHelper.COLUMN_EVENT_TIME, eventTime)
                    contentValues.put(DatabaseHelper.COLUMN_PLMN_ID, plmnId)
                    contentValues.put(DatabaseHelper.COLUMN_TAC, lac)
                    contentValues.put(DatabaseHelper.COLUMN_CELL_ID, cellId)
                    contentValues.put(DatabaseHelper.COLUMN_RSRP, rscp.toString())
                    contentValues.put(DatabaseHelper.COLUMN_RSRQ, ecn0.toString())
                    contentValues.put(DatabaseHelper.COLUMN_TECHNOLOGY, "3G (WCDMA)")

                    db.insert(DatabaseHelper.TABLE_NAME, null, contentValues)

                    cellInfoText.append("Event Time: $eventTime\n")
                    cellInfoText.append("PLMN ID: $plmnId\n")
                    cellInfoText.append("LAC: $lac\n")
                    cellInfoText.append("Cell ID: $cellId\n")
                    cellInfoText.append("RSCP: $rscp\n")
                    cellInfoText.append("Ec/N0: $ecn0\n")
                    cellInfoText.append("Cell Technology: 3G (WCDMA)\n\n")
                }
                is CellInfoGsm -> {
                    val cellIdentityGsm = cellInfo.cellIdentity
                    val cellSignalStrengthGsm = cellInfo.cellSignalStrength

                    val plmnId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        cellIdentityGsm.mobileNetworkOperator
                    } else {
                        "N/A"
                    }
                    val lac = cellIdentityGsm.lac
                    val cellId = cellIdentityGsm.cid
                    val rssi = cellSignalStrengthGsm.dbm
                    val eventTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                    contentValues.put(DatabaseHelper.COLUMN_EVENT_TIME, eventTime)
                    contentValues.put(DatabaseHelper.COLUMN_PLMN_ID, plmnId)
                    contentValues.put(DatabaseHelper.COLUMN_TAC, lac)
                    contentValues.put(DatabaseHelper.COLUMN_CELL_ID, cellId)
                    contentValues.put(DatabaseHelper.COLUMN_RSRP, rssi.toString())
                    contentValues.put(DatabaseHelper.COLUMN_RSRQ, "N/A")
                    contentValues.put(DatabaseHelper.COLUMN_TECHNOLOGY, "2G (GSM)")

                    db.insert(DatabaseHelper.TABLE_NAME, null, contentValues)

                    cellInfoText.append("Event Time: $eventTime\n")
                    cellInfoText.append("PLMN ID: $plmnId\n")
                    cellInfoText.append("LAC: $lac\n")
                    cellInfoText.append("Cell ID: $cellId\n")
                    cellInfoText.append("RSSI: $rssi\n")
                    cellInfoText.append("Cell Technology: 2G (GSM)\n\n")
                }
                // Add other cell info types here
                else -> {
                    cellInfoText.append("Unknown Cell Info Type\n\n")
                }
            }
        }

        cellInfoTextView.text = cellInfoText.toString()
    }

    private fun getNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
            TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G (HSPA)"
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G (EDGE/GPRS)"
            else -> "Unknown"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
                getCellInfo()
            }
        }
    }
}
