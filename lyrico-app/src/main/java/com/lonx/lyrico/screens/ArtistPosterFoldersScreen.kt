package com.lonx.lyrico.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.ArtistPosterFoldersUiState
import com.lonx.lyrico.viewmodel.ArtistPosterFoldersViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddFolder
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
@Destination<RootGraph>(route = "artist_poster_folders")
fun ArtistPosterFoldersScreen(navigator: DestinationsNavigator) {
    val viewModel: ArtistPosterFoldersViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            viewModel.addFolder(it)
        }
    }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                SmallTopAppBar(
                    title = stringResource(R.string.artist_poster_folders),
                    color = Color.Transparent,
                    defaultWindowInsetsPadding = false,
                    navigationIcon = {
                        IconButton(onClick = navigator::navigateUp) {
                            Icon(MiuixIcons.Back, stringResource(R.string.artist_poster_back))
                        }
                    },
                    actions = {
                        if (state.folder == null) {
                            IconButton(onClick = { picker.launch(null) }) {
                                Icon(MiuixIcons.AddFolder, stringResource(R.string.artist_poster_folder_add))
                            }
                        }
                        IconButton(onClick = { showHelp = true }) {
                            Icon(
                                MiuixIcons.Info,
                                stringResource(R.string.artist_poster_naming)
                            )
                        }
                        if (state.folder != null) {
                            OverlayIconDropdownMenu(
                                entry = DropdownEntry(
                                    items = listOf(
                                        DropdownItem(
                                            text = stringResource(R.string.action_refresh_folder),
                                            onClick = viewModel::refresh
                                        ),
                                        DropdownItem(
                                            text = stringResource(R.string.artist_poster_folder_change),
                                            onClick = { picker.launch(null) }
                                        ),
                                        DropdownItem(
                                            text = stringResource(R.string.folder_action_remove),
                                            onClick = { showRemoveDialog = true }
                                        )
                                    )
                                ),
                                enabled = !state.isLoading
                            ) {
                                Icon(MiuixIcons.More, stringResource(R.string.cd_more_actions))
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().blurSource(topBarBackdrop)) {
            val folder = state.folder
            if (folder != null) {
                ArtistPosterFolderContents(
                    folder = folder,
                    revision = state.revision,
                    isLoading = state.isLoading,
                    hasError = state.error,
                    padding = scaffoldContentPadding(padding),
                    scrollBehavior = scrollBehavior
                )
            } else {
                ArtistPosterFolderEmptyState(
                    state = state,
                    onChooseFolder = { picker.launch(null) },
                    padding = scaffoldContentPadding(padding),
                    scrollBehavior = scrollBehavior
                )
            }
        }
        WindowDialog(
            title = stringResource(R.string.artist_poster_naming),
            show = showHelp,
            onDismissRequest = { showHelp = false }
        ) {
            MarkdownText(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                markdown = stringResource(R.string.artist_poster_folders_help),
                linkColor = MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.body2.copy(
                    color = MiuixTheme.colorScheme.onSurface
                )
            )
        }
        val folder = state.folder
        if (showRemoveDialog && folder != null) {
            WindowDialog(
                title = stringResource(R.string.dialog_remove_folder_title),
                show = true,
                onDismissRequest = { showRemoveDialog = false }
            ) {
                Column {
                    Text(folder.path, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.artist_poster_folder_remove_tip),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            text = stringResource(R.string.cancel),
                            onClick = { showRemoveDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(20.dp))
                        TextButton(
                            text = stringResource(R.string.confirm),
                            onClick = {
                                viewModel.removeFolder()
                                showRemoveDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPosterFolderEmptyState(
    state: ArtistPosterFoldersUiState,
    onChooseFolder: () -> Unit,
    padding: PaddingValues,
    scrollBehavior: ScrollBehavior
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = padding,
        overscrollEffect = null
    ) {
        item {
            Text(
                stringResource(R.string.artist_poster_folders_summary),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
            )
        }
        if (state.error) {
            item {
                Text(
                    stringResource(R.string.artist_poster_folder_error),
                    color = MiuixTheme.colorScheme.error,
                    fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        if (state.isLoading && state.folder == null) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(12.dp)) }
        } else if (state.folder == null) {
            item {
                Card(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    BasicComponent(
                        title = stringResource(R.string.artist_poster_folder_add),
                        summary = stringResource(R.string.artist_poster_folders_empty),
                        onClick = onChooseFolder
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
