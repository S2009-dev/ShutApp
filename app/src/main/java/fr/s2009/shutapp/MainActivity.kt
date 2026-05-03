package fr.s2009.shutapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.s2009.shutapp.utils.BaseUi
import fr.s2009.shutapp.utils.Device
import fr.s2009.shutapp.utils.Divider
import fr.s2009.shutapp.utils.PreferencesManager
import fr.s2009.shutapp.utils.devicesFound
import fr.s2009.shutapp.utils.getDeviceInfo
import fr.s2009.shutapp.utils.isScanningFlow
import fr.s2009.shutapp.utils.scanLocalNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            BaseUi (this) {
                val context = LocalContext.current
                val preferencesManager = remember(context) { PreferencesManager(context) }
                val devices by preferencesManager.getDevices().collectAsState(initial = emptyList())

                Text(
                    text = if (devices.isEmpty()) stringResource(R.string.device_add_first) else stringResource(R.string.device_add),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(16.dp))
                IPInput(preferencesManager)
                Divider()
                ScanLAN()
                Divider()
                DevicesList(devices)
            }
        }
    }
}

@Composable
fun IPInput(preferencesManager: PreferencesManager) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.9f)
                .clip(RoundedCornerShape(8.dp))
                .background(colorResource(R.color.primary))
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textState = remember { mutableStateOf(TextFieldValue()) }
            val validIp = remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val resources = LocalResources.current
            val context = LocalContext.current

            Column {
                val numbersPattern = remember { Regex("^[0-9.]*$") }
                val ipPattern = remember {
                    Regex(
                        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
                    )
                }

                Text(
                    text = stringResource(R.string.ip_label)
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(.9f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        errorContainerColor = colorResource(R.color.secondary),
                        unfocusedContainerColor = colorResource(R.color.secondary),
                        focusedContainerColor = colorResource(R.color.secondary),
                        errorIndicatorColor = colorResource(R.color.accent),
                        unfocusedIndicatorColor = colorResource(R.color.accent),
                        focusedIndicatorColor = colorResource(R.color.green),
                        errorTextColor = colorResource(R.color.text),
                        unfocusedTextColor = colorResource(R.color.text),
                        focusedTextColor = colorResource(R.color.text),
                        errorPlaceholderColor = colorResource(R.color.placeholder),
                        unfocusedPlaceholderColor = colorResource(R.color.placeholder),
                        focusedPlaceholderColor = colorResource(R.color.placeholder),
                        cursorColor = colorResource(R.color.text)
                    ),
                    isError = !validIp.value,
                    placeholder = { Text(stringResource(R.string.ip_placeholder)) },
                    value = textState.value,
                    onValueChange = {
                        if (it.text.isEmpty() || (it.text.matches(numbersPattern) && it.text.length <= 15)) {
                            textState.value = it
                            validIp.value = it.text.matches(ipPattern)
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.secondary)
                ),
                enabled = validIp.value,
                border = BorderStroke(1.dp, colorResource(R.color.accent)),
                modifier = Modifier.fillMaxWidth(.4f),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    val ip = textState.value.text

                    getDeviceInfo(
                        ip = ip,
                        onResult = { device ->
                            coroutineScope.launch {
                                if (preferencesManager.hasDevice(device.ip)) {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.device_already_added),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    preferencesManager.addDevice(device)
                                }
                            }
                        },
                        onError = {
                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.ip_not_reached, ip),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            ) {
                Text(stringResource(R.string.ip_add))
            }
        }
    }
}
@Composable
fun ScanningLAN() {
    val isScanning by isScanningFlow.collectAsState()

    Column (horizontalAlignment = Alignment.CenterHorizontally){
        Text(
            text = stringResource(R.string.scan_running, isScanning),
            color = colorResource(R.color.placeholder),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(
            modifier = Modifier.scale(1.2f),
            color = colorResource(R.color.placeholder)
        )
    }
}

@Composable
fun ScanLAN() {
    val isScanning by isScanningFlow.collectAsState()
    val devicesFound by devicesFound.collectAsState()

    if(isScanning != -1) {
        ScanningLAN()
        return
    }

    Column (horizontalAlignment = Alignment.CenterHorizontally){
        Text(
            text = stringResource(R.string.scan_text),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        Button(
            colors = ButtonDefaults.buttonColors(
                colorResource(R.color.secondary)
            ),
            onClick = {
                scanLocalNetwork()
            }
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.restart),
                contentDescription = "RefreshIcon"
            )
            Spacer(Modifier.width(8.dp))
            Text(if(devicesFound.isEmpty()) stringResource(R.string.scan) else stringResource(R.string.scan_again))
        }
    }

    if(!devicesFound.isEmpty()){
        Spacer(Modifier.height(16.dp))
        DevicesFoundList(devicesFound)
    }
}

@Composable
fun DeviceFoundItem(device: Device, modifier: Modifier?) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val preferencesManager = remember(context) { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier ?: Modifier.size(128.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.secondary)
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = {
            coroutineScope.launch {
                if (preferencesManager.hasDevice(device.ip)){
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.device_already_added),
                            Toast.LENGTH_SHORT
                        ).show()

                        devicesFound.value = devicesFound.value.minus(device)
                    }
                } else {
                    devicesFound.value = devicesFound.value.minus(device)
                    preferencesManager.addDevice(device)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(64.dp),
                painter = if (device.os == "windows") painterResource(R.drawable.windows) else painterResource(R.drawable.linux),
                contentDescription = "DeviceIcon",
                tint = colorResource(R.color.primary)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = device.name,
                color = colorResource(R.color.text),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = device.ip,
                color = colorResource(R.color.placeholder),
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalGridApi::class)
@Composable
fun DevicesFoundList(devices: List<Device>) {
    Grid(
        config = {
            repeat(2) {
                column(128.dp)
            }
            gap(8.dp)
        }
    ) {
        val isOdd = devices.size % 2 != 0

        for(i in 0 until devices.size / 2) {
            DeviceFoundItem(devices[i], null)
        }

        if(isOdd) {
            DeviceFoundItem(
                devices[devices.size - 1],
                Modifier
                    .size(128.dp)
                    .gridItem(columnSpan = 2, alignment = Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceItem(device: Device) {
    val context = LocalContext.current
    val preferencesManager = remember(context) { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val openAlertDialog = remember { mutableStateOf(false) }

    if (openAlertDialog.value) {
        BasicAlertDialog(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colorResource(R.color.primary))
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.accent),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 16.dp),
            onDismissRequest = { openAlertDialog.value = false }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(R.drawable.trash),
                    contentDescription = "DialogIcon",
                    tint = colorResource(R.color.placeholder)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dialog_text, device.name),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        modifier = Modifier.scale(.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.accent)
                        ),
                        onClick = {
                            coroutineScope.launch {
                                preferencesManager.removeDevice(device)
                                openAlertDialog.value = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.dialog_confirm))
                    }

                    Button(
                        modifier = Modifier.scale(.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.secondary)
                        ),
                        onClick = { openAlertDialog.value = false }
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            }

        }
    }

    Card (
        modifier = Modifier
            .height(128.dp)
            .fillMaxWidth(.9f),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.secondary)
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            val intent = Intent(context, DeviceActivity::class.java)

            intent.putExtra("ip", device.ip)
            context.startActivity(intent)
        }
    ){
        Row (
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                modifier = Modifier.size(64.dp),
                painter = if (device.os == "windows") painterResource(R.drawable.windows) else painterResource(R.drawable.linux),
                contentDescription = "DeviceIcon",
                tint = colorResource(R.color.primary)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = device.ip,
                    textAlign = TextAlign.Center,
                    color = colorResource(R.color.placeholder),
                    modifier = Modifier
                        .fillMaxWidth(.5f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorResource(R.color.primary))
                        .border(
                            width = 1.dp,
                            color = colorResource(R.color.accent),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = device.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            IconButton (
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorResource(R.color.accent)
                ),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    openAlertDialog.value = true
                }
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(R.drawable.trash),
                    contentDescription = "TrashIcon",
                    tint = colorResource(R.color.text)
                )
            }
        }
    }
}

@Composable
fun DevicesList(devices: List<Device>) {
    LazyColumn(
        modifier = Modifier.height(192.dp)
    ) {
        items(devices) { device ->
            DeviceItem(device)
            Spacer(Modifier.height(16.dp))
        }
    }
}