package com.example.samarium

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoGsm
import android.telephony.TelephonyManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_PHONE_STATE] == true && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCellInfo()
        } else {
            // Handle permission denial
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION))
            } else {
                getCellInfo()
            }
        } else {
            getCellInfo()
        }
    }

    private fun getCellInfo() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val cellInfoList: List<CellInfo> = telephonyManager.allCellInfo
        val cellInfoText = StringBuilder()

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
            }
        }

        db.close()

        findViewById<TextView>(R.id.cellInfoTextView).text = cellInfoText.toString()
    }

    private fun getNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G (HSPA+)"
            TelephonyManager.NETWORK_TYPE_HSPA -> "3G (HSPA)"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G (EDGE)"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G (GPRS)"
            else -> "Unknown"
        }
    }
}
