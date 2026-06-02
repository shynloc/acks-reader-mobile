package studio.acks.reader.ui

import androidx.compose.runtime.Composable
import studio.acks.reader.AppScreen
import studio.acks.reader.AppUiState
import studio.acks.reader.BuildConfig
import studio.acks.reader.ReaderViewModel
import studio.acks.reader.ui.sheets.OnboardingSheet

@Composable
fun AcksApp(state: AppUiState, vm: ReaderViewModel, onPickFile: () -> Unit) {
    AcksTheme(appTheme = state.settings.appTheme) {
        when (state.screen) {
            AppScreen.RECENT       -> RecentScreen(state = state, vm = vm, onPickFile = onPickFile)
            AppScreen.PREVIEW      -> PreviewScreen(state = state, vm = vm)
            AppScreen.CARD_PREVIEW -> CardPreviewScreen(state = state, vm = vm)
            AppScreen.ABOUT        -> AboutScreen(vm = vm)
            AppScreen.SETTINGS     -> SettingsScreen(
                defaultTheme        = state.settings.defaultTheme,
                defaultViewport     = state.settings.defaultViewport,
                defaultHtmlMode     = state.settings.defaultHtmlMode,
                fontScale           = state.settings.fontScale,
                appTheme            = state.settings.appTheme,
                enableMermaid       = state.settings.enableMermaid,
                enableMath          = state.settings.enableMath,
                versionName         = BuildConfig.VERSION_NAME,
                vm                  = vm,
                onPickFile          = onPickFile,
                onShowOnboarding    = { vm.showOnboarding() },
                fontSourceOverride  = state.settings.fontSourceOverride,
                resolvedFontSource  = state.settings.resolvedFontSource
            )
        }

        if (state.showOnboarding) {
            OnboardingSheet(
                onDismiss = { vm.dismissOnboarding() },
                onTryNow  = { vm.dismissOnboarding(); onPickFile() }
            )
        }
    }
}
