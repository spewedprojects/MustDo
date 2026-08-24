package com.gratus.mytodo.ui.components.navigation

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gratus.mytodo.BuildConfig
import com.gratus.mytodo.R
import com.gratus.mytodo.ui.Screen
import androidx.compose.ui.tooling.preview.Preview
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme

/**
 * The content and the function of the drawer used in the app.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerContent(
    activeScreen: Screen,
    colorSchemeType: String,
    onSetActiveScreen: (Screen) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .testTag("nav_drawer_sheet"),
        drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
        drawerContainerColor = if (colorSchemeType == "minimal" || colorSchemeType == "colorful") {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surface
        },
        drawerTonalElevation = 0.dp
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "MustDo",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (1).sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Time-locked task tracker",
                fontSize = AppFontSizes.extraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val navItems = listOf(
            Triple(Screen.HOME, "Home", Icons.Default.Home),
            Triple(Screen.HISTORY, "History Records", Icons.Default.History),
            Triple(Screen.STATS, "Stats Analyzer", Icons.Default.Analytics),
            Triple(Screen.SETTINGS, "App Settings", Icons.Default.Settings)
        )

        navItems.forEach { (screenKey, name, icon) ->
            NavigationDrawerItem(
                icon = { Icon(imageVector = icon, contentDescription = name) },
                label = {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = AppFontSizes.medium) },
                shape = RoundedCornerShape(18.dp),
                selected = activeScreen == screenKey,
                onClick = {
                    onSetActiveScreen(screenKey)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContainerColor = Color.Transparent
                )
            )
        // Removed on 24th August 2026 in v7.1.1
        // Removed if.else condition for settings screen and related combinedclick since Issue Tracker screen is removed.
        // Issues are now handled in the separate app.
        // A future possible link to this app can be added using combined click.
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/spewedprojects/MustDo".toUri())
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.github_mark),
                contentDescription = "Star on GitHub",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            fontSize = AppFontSizes.micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, name = "App Navigation Drawer")
@Composable
fun AppDrawerContentPreview() {
    SoftTodoTheme(colorSchemeType = "colorful") {
        AppDrawerContent(
            activeScreen = Screen.SETTINGS,
            colorSchemeType = "minimal",
            onSetActiveScreen = {},
            onCloseDrawer = {}
        )
    }
}
