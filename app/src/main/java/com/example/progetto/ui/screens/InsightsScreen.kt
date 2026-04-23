package com.example.progetto.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.theme.HeartMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onOpenDrawer: () -> Unit = {}
) {
    var showStatistics by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Your Insights",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showStatistics) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    StatisticalAnalysisContent()
                }
            } else {
                FavouriteSongsContent(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it }
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            HeartButton(
                text = if (showStatistics) "Favourite Songs" else "Statistical Analysis",
                onClick = { showStatistics = !showStatistics },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun StatisticalAnalysisContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Grafico a ciambella (Donut Chart) stilizzato come nel bozzetto
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 25.dp.toPx()
                // Esempio di segmenti colorati (come nel disegno)
                drawArc(Color.Red, -90f, 72f, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawArc(Color.Black, -18f, 72f, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawArc(Color.Green, 54f, 36f, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawArc(Color.Blue, 90f, 72f, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawArc(Color(0xFFFFD700), 162f, 108f, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
            // Percentuali (posizionate approssimativamente come nel bozzetto)
            Text("20%", modifier = Modifier.align(Alignment.TopCenter).offset(y = (-30).dp), fontWeight = FontWeight.Bold)
            Text("20%", modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = 20.dp), fontWeight = FontWeight.Bold)
            Text("10%", modifier = Modifier.align(Alignment.CenterEnd).offset(x = 30.dp), fontWeight = FontWeight.Bold)
            Text("10%", modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = (-20).dp), fontWeight = FontWeight.Bold)
            Text("30%", modifier = Modifier.align(Alignment.BottomStart).offset(x = (-10).dp, y = (-20).dp), fontWeight = FontWeight.Bold)
            Text("20%", modifier = Modifier.align(Alignment.CenterStart).offset(x = (-30).dp), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Legenda
        val legendItems = listOf(
            Pair(Color.Red, "SAD"), Pair(Color.Green, "Y"),
            Pair(Color(0xFFFFD700), "HAPPY"), Pair(Color.Blue, "Z"),
            Pair(Color.Cyan, "X"), Pair(Color.Black, "V")
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            for (i in legendItems.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(legendItems[i].first, legendItems[i].second)
                    LegendItem(legendItems[i+1].first, legendItems[i+1].second)
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(100.dp)) {
        Box(modifier = Modifier.size(16.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FavouriteSongsContent(searchQuery: String, onSearchChange: (String) -> Unit) {
    val songs = listOf("SONG 1", "SONG 2", "SONG 3", "SONG 4", "SONG 5", "SONG 6", "SONG 7")
    
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search for Playlist, Emotion, Song", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.LightGray,
                unfocusedBorderColor = Color.LightGray
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Top 10 songs",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(songs) { song ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).border(1.dp, Color.Gray, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = song, fontSize = 16.sp, color = if (song == "SONG 7") Color(0xFF9C27B0) else Color.Black)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InsightsScreenPreview() {
    HeartMusicTheme {
        InsightsScreen()
    }
}
