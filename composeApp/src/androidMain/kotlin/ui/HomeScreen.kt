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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.components.BottomNavigation
import ui.components.FragranceCard
import ui.components.TopAppBar
import ui.components.TopTabs
import ui.navigation.getBottomNavTabs
import ui.theme.Cream
import ui.theme.OrangeGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTopTab by remember { mutableStateOf(0) }
    var selectedBottomTab by remember { mutableStateOf(0) }
    
    val fragrances = listOf(
        FragranceData("Velvet Orchid", "Tom Ford", 4.5f, 215, OrangeGradient),
        FragranceData("Santal 33", "Le Labo", 4.3f, 189, Color(0xFFE8D5B7))
    )
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar()
                TopTabs(
                    selectedTab = selectedTopTab,
                    onTabSelected = { selectedTopTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        bottomBar = {
            BottomNavigation(
                selectedTab = selectedBottomTab,
                onTabSelected = { selectedBottomTab = it },
                tabs = getBottomNavTabs(),
                modifier = Modifier
            )
        },
        containerColor = Cream
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(fragrances) { fragrance ->
                FragranceCard(
                    name = fragrance.name,
                    brand = fragrance.brand,
                    rating = fragrance.rating,
                    reviewCount = fragrance.reviewCount,
                    imageColor = fragrance.imageColor
                )
            }
        }
    }
}

data class FragranceData(
    val name: String,
    val brand: String,
    val rating: Float,
    val reviewCount: Int,
    val imageColor: Color
)