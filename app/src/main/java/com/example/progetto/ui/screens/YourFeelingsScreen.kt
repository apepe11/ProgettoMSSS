package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.theme.HeartMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourFeelingsScreen(
    onOpenDrawer: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Your feelings", 
                        fontSize = 20.sp,
                        color = Color.Black
                    ) 
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onOpenDrawer() }
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(6) { // Mostra 6 elementi come esempio
                FeelingEntryItem()
            }
        }
    }
}

@Composable
fun FeelingEntryItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    ) {
        // Colonna 1°
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(50.dp)
                .border(0.5.dp, Color.Gray),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "1°",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Colonna 2°
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(50.dp)
                .border(0.5.dp, Color.Gray),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "2°",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Colonna Description
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "DESCRIPTION",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun YourFeelingsScreenPreview() {
    HeartMusicTheme {
        YourFeelingsScreen()
    }
}
