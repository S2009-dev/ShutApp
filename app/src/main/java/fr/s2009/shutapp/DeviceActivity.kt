package fr.s2009.shutapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.s2009.shutapp.utils.BaseUi
import fr.s2009.shutapp.utils.Device
import kotlinx.coroutines.runBlocking
import fr.s2009.shutapp.utils.PreferencesManager
import fr.s2009.shutapp.utils.checkDeviceStatus
import fr.s2009.shutapp.utils.isDeviceOnlineFlow
import fr.s2009.shutapp.utils.sendCommand

class DeviceActivity : ComponentActivity() {
    private var device: Device? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityIntent = Intent(this, MainActivity::class.java)
        val ip = intent.getStringExtra("ip")

        if(ip == null) {
            startActivity(activityIntent)
            return this.finish()
        }

        runBlocking {
            device = PreferencesManager(this@DeviceActivity).getDevice(ip)

            if(device == null){
                startActivity(activityIntent)
                return@runBlocking this@DeviceActivity.finish()
            }
        }

        checkDeviceStatus("${device?.ip}")

        enableEdgeToEdge()
        setContent {
            BaseUi(this) {
                val isDeviceOnline by isDeviceOnlineFlow.collectAsState(initial = false)

                Icon(
                    modifier = Modifier.size(128.dp),
                    painter = if (device?.os == "Windows") painterResource(R.drawable.windows) else painterResource(R.drawable.linux),
                    contentDescription = "DeviceIcon",
                    tint = if(isDeviceOnline) colorResource(R.color.green) else colorResource(R.color.accent)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "${device?.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )

                Text(
                    text = "${device?.ip}",
                    color = colorResource(R.color.placeholder)
                )

                Spacer(Modifier.height(48.dp))
                RemoteActions("${device?.ip}")
                Spacer(Modifier.height(48.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(.9f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.primary)
                    ),
                    onClick = {
                        this.finish()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(R.drawable.back),
                            contentDescription = "BackIcon"
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            text = stringResource(R.string.back),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalGridApi::class)
@Composable
fun RemoteActions(ip: String) {
    Grid(
        config = {
            repeat(2) {
                column(128.dp)
            }
            repeat(2) {
                row(128.dp)
            }
            gap(32.dp)
        }
    ) {
        val isDeviceOnline by isDeviceOnlineFlow.collectAsState(initial = false)
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if(isDeviceOnline) colorResource(R.color.red) else colorResource(R.color.primary))
                .combinedClickable(
                    enabled = isDeviceOnline,
                    onLongClick = { sendCommand(ip, "shutdown") },
                    onClick = { showToast(context) }
                ),
        ) {
            Icon(
                modifier = Modifier.size(112.dp).align(Alignment.Center),
                painter = painterResource(R.drawable.shutdown),
                contentDescription = "ShutdownIcon",
                tint = if(isDeviceOnline) colorResource(R.color.text) else colorResource(R.color.placeholder)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if(isDeviceOnline) colorResource(R.color.blue) else colorResource(R.color.primary))
                .combinedClickable(
                    enabled = isDeviceOnline,
                    onLongClick = { sendCommand(ip, "reboot") },
                    onClick = { showToast(context) }
                ),
        ) {
            Icon(
                modifier = Modifier.size(112.dp).align(Alignment.Center).rotate(45f),
                painter = painterResource(R.drawable.restart),
                contentDescription = "RebootIcon",
                tint = if(isDeviceOnline) colorResource(R.color.text) else colorResource(R.color.placeholder)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if(isDeviceOnline) colorResource(R.color.yellow) else colorResource(R.color.primary))
                .combinedClickable(
                    enabled = isDeviceOnline,
                    onLongClick = { sendCommand(ip, "sleep") },
                    onClick = { showToast(context) }
                ),
        ) {
            Icon(
                modifier = Modifier.size(100.dp).align(Alignment.Center),
                painter = painterResource(R.drawable.sleep),
                contentDescription = "SleepIcon",
                tint = if(isDeviceOnline) colorResource(R.color.text) else colorResource(R.color.placeholder)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if(isDeviceOnline) colorResource(R.color.pink) else colorResource(R.color.primary))
                .combinedClickable(
                    enabled = isDeviceOnline,
                    onLongClick = { sendCommand(ip, "lock") },
                    onClick = { showToast(context) }
                ),
        ) {
            Icon(
                modifier = Modifier.size(100.dp).align(Alignment.Center),
                painter = painterResource(R.drawable.lock),
                contentDescription = "LockIcon",
                tint = if(isDeviceOnline) colorResource(R.color.text) else colorResource(R.color.placeholder)
            )
        }
    }
}

fun showToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.wrong_click),
        Toast.LENGTH_SHORT
    ).show()
}