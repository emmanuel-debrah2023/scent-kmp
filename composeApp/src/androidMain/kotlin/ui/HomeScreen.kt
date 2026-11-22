package org.scent.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.scent.project.domain.model.Fragrance
import ui.components.BottomNavigation
import ui.components.FragranceCard
import ui.components.TopAppBar
import ui.components.TopTabs
import ui.home.toFragranceCardUi
import ui.navigation.mainNavTabs
import ui.theme.ScentSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTopTab by remember { mutableStateOf(0) }
    var selectedBottomTab by remember { mutableStateOf(0) }

    val fragrances =
        listOf(
            Fragrance(id = 1, name = "Velvet Orchid", brand = "Tom Ford", rating = 4.5f, reviewCount = 215),
            Fragrance(id = 2, name = "Santal 33", brand = "Le Labo", rating = 4.3f, reviewCount = 189),
        )

    Scaffold(
        topBar = {
            Column {
                TopAppBar()
                TopTabs(
                    selectedTab = selectedTopTab,
                    onTabSelected = { selectedTopTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        },
        bottomBar = {
            BottomNavigation(
                selectedTab = selectedBottomTab,
                onTabSelected = { selectedBottomTab = it },
                tabs = mainNavTabs(),
                modifier = Modifier,
            )
        },
        containerColor = ScentSurface,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(fragrances) { fragrance ->
                // TODO(feature/fragrance-favourites): the toggle is local state; persist it once favourites exist.
                var isFavourite by rememberSaveable(fragrance.id) { mutableStateOf(false) }
                FragranceCard(
                    card = fragrance.toFragranceCardUi(),
                    // TODO(chore/legacy-android-home-screen): the legacy screen has no navigation.
                    onClick = {},
                    isFavourite = isFavourite,
                    onToggleFavourite = { isFavourite = !isFavourite },
                )
            }
        }
    }
}
