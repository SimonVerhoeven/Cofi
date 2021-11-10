package com.omelan.cofi.pages.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.preferences.core.edit
import com.omelan.cofi.*
import com.omelan.cofi.R
import com.omelan.cofi.components.PiPAwareAppBar
import com.omelan.cofi.components.createAppBarBehavior
import com.omelan.cofi.ui.spacingDefault
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun AppSettings(
    goBack: () -> Unit,
    goToAbout: () -> Unit,
) {
    val dataStore = LocalSettingsDataStore.current
    suspend fun togglePiPSetting() {
        dataStore.edit { settings ->
            val currentPiPState = settings[PIP_ENABLED] ?: PIP_DEFAULT_VALUE
            settings[PIP_ENABLED] = !currentPiPState
        }
    }

    suspend fun toggleDingSetting() {
        dataStore.edit { settings ->
            val currentDingState = settings[DING_ENABLED] ?: DING_DEFAULT_VALUE
            settings[DING_ENABLED] = !currentDingState
        }
    }

    suspend fun selectCombineMethod(combineMethod: CombineWeight) {
        dataStore.edit {
            it[COMBINE_WEIGHT] = combineMethod.name
        }
    }

    val isPiPEnabledFlow = dataStore.data.map { preferences ->
        preferences[PIP_ENABLED] ?: PIP_DEFAULT_VALUE
    }
    val combineWeightFlow = dataStore.data.map { preferences ->
        preferences[COMBINE_WEIGHT] ?: COMBINE_WEIGHT_DEFAULT_VALUE
    }
    val isDingEnabled = dataStore.data.map { preferences ->
        preferences[DING_ENABLED] ?: DING_DEFAULT_VALUE
    }.collectAsState(initial = DING_DEFAULT_VALUE)
    val isPiPEnabled = isPiPEnabledFlow.collectAsState(initial = PIP_DEFAULT_VALUE)
    val combineWeightState =
        combineWeightFlow.collectAsState(initial = COMBINE_WEIGHT_DEFAULT_VALUE)
    val showCombineWeightDialog = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appBarBehavior = createAppBarBehavior()

    Scaffold(
        topBar = {
            PiPAwareAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = appBarBehavior,
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .nestedScroll(appBarBehavior.nestedScrollConnection)
                .fillMaxSize()
        ) {
            item {
                ListItem(
                    text = {
                        Text(text = stringResource(id = R.string.settings_pip_item))
                    },
                    icon = {
                        Icon(
                            painterResource(id = R.drawable.ic_picture_in_picture),
                            contentDescription = null
                        )
                    },
                    modifier = settingsItemModifier.clickable(
                        onClick = {
                            coroutineScope.launch {
                                togglePiPSetting()
                            }
                        },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ),
                    trailing = {
                        Switch(
                            checked = isPiPEnabled.value,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    togglePiPSetting()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            )
                        )
                    }
                )
            }
            item {
                ListItem(
                    text = {
                        Text(text = stringResource(id = R.string.settings_ding_item))
                    },
                    icon = {
                        Icon(
                            painterResource(id = R.drawable.ic_sound),
                            contentDescription = null
                        )
                    },
                    modifier = settingsItemModifier.clickable(
                        onClick = {
                            coroutineScope.launch {
                                toggleDingSetting()
                            }
                        },
                    ),
                    trailing = {
                        Switch(
                            checked = isDingEnabled.value,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    toggleDingSetting()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            )
                        )
                    }
                )
            }
            item {
                ListItem(
                    text = {
                        Text(
                            text =
                            stringResource(
                                id =
                                stringToCombineWeight(combineWeightState.value).settingsStringId
                            )
                        )
                    },
                    overlineText = {
                        Text(text = stringResource(id = R.string.settings_combine_weight_item))
                    },

                    icon = {
                        Icon(Icons.Rounded.List, contentDescription = null)
                    },
                    modifier = settingsItemModifier.clickable(
                        onClick = {
                            showCombineWeightDialog.value = true
                        },
                    ),
                )
                if (showCombineWeightDialog.value) {
                    CombineWeightDialog(
                        dismiss = { showCombineWeightDialog.value = false },
                        selectCombineMethod = {
                            coroutineScope.launch {
                                selectCombineMethod(it)
                                showCombineWeightDialog.value = false
                            }
                        },
                        combineWeightState = combineWeightState.value
                    )
                }
            }
            item {
                ListItem(
                    text = {
                        Text(text = stringResource(id = R.string.settings_bug_item))
                    },
                    icon = {
                        Icon(
                            painterResource(id = R.drawable.ic_bug_report),
                            contentDescription = null
                        )
                    },
                    modifier = settingsItemModifier.clickable(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("rozPierog@Gmail.com"))
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    context.resources.getString(R.string.bug_report_title)
                                )
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                startActivity(context, intent, null)
                            }
                        }
                    ),
                )
            }
            item {
                ListItem(
                    text = {
                        Text(text = stringResource(id = R.string.settings_about_item))
                    },
                    icon = {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                    },
                    modifier = settingsItemModifier.clickable(onClick = goToAbout)
                )
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun CombineWeightDialog(
    dismiss: () -> Unit,
    selectCombineMethod: (CombineWeight) -> Unit,
    combineWeightState: String
) {
    Dialog(
        onDismissRequest = dismiss
    ) {
        Column(
            modifier = Modifier
                .background(
                    shape = RoundedCornerShape(28.0.dp),
                    color = MaterialTheme.colorScheme.surface
                )
                .padding(top = spacingDefault, bottom = spacingDefault)
        ) {
            CombineWeight.values().forEach {
                ListItem(
                    text = { Text(stringResource(id = it.settingsStringId)) },
                    modifier = Modifier.selectable(
                        selected = combineWeightState == it.name,
                        onClick = { selectCombineMethod(it) },
                    ),
                    icon = {
                        RadioButton(
                            selected = combineWeightState == it.name,
                            onClick = { selectCombineMethod(it) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.secondary,
                            )
                        )
                    }
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Preview
@Composable
fun SettingsPagePreview() {
    AppSettings(goBack = { }, goToAbout = { })
}