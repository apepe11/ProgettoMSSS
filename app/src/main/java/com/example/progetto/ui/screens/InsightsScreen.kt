package com.example.progetto.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // ADDED IMPORT
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.utils.InsightsViewModel // Adjust package if your ViewModel is elsewhere
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InsightsScreen(
    onOpenDrawer: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Insights",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 24.dp)
        )

        // The Insights ViewModel and Pie Chart Content!
        StatisticalAnalysisContent()
    }
}

@Composable
fun StatisticalAnalysisContent(
    viewModel: InsightsViewModel = viewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    val filterOptions = listOf("App Detected", "User Experienced")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. Legend at the TOP RIGHT
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                val legendItems = listOf(
                    Pair(Color(0xFFFFD700), "Happy"),
                    Pair(Color(0xFF2196F3), "Sad"),
                    Pair(Color(0xFF4CAF50), "Calm"),
                    Pair(Color(0xFF9C27B0), "Anxious"),
                    Pair(Color(0xFFFF5722), "Energetic")
                )

                legendItems.forEach { (color, label) ->
                    LegendItem(color = color, label = label)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Donut Chart in the MIDDLE (Using Geometry for Text Placement)
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Calculate if we have ANY data
            val totalData = chartData.happy + chartData.energetic + chartData.sad + chartData.calm + chartData.anxious

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 25.dp.toPx()

                if (totalData == 0f) {
                    // EMPTY STATE: Draw a light gray background circle if there's no data
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidthPx)
                    )
                } else {
                    // Bundle data into a list to make drawing dynamic and clean
                    val slices = listOf(
                        chartData.happy to Color(0xFFFFD700),
                        chartData.energetic to Color(0xFFFF5722),
                        chartData.sad to Color(0xFF2196F3),
                        chartData.calm to Color(0xFF4CAF50),
                        chartData.anxious to Color(0xFF9C27B0)
                    )

                    var currentStartAngle = -90f // Start at 12 o'clock (top)
                    val radius = size.minDimension / 2

                    // Set up the paintbrush for the native text
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textAlign = Paint.Align.CENTER
                        textSize = 45f // Adjust this size if you want the text larger or smaller
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    slices.forEach { (percentage, color) ->
                        if (percentage > 0f) {
                            val sweepAngle = percentage * 360f

                            // 1. Draw the colored slice
                            drawArc(
                                color = color,
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(strokeWidthPx, cap = StrokeCap.Round)
                            )

                            // 2. Calculate the exact middle angle of THIS specific slice
                            val midAngle = currentStartAngle + (sweepAngle / 2)
                            val midAngleRad = Math.toRadians(midAngle.toDouble())

                            // 3. Use trigonometry to find the exact X and Y coordinates on the circle line
                            val x = (size.width / 2) + (radius * cos(midAngleRad)).toFloat()
                            val y = (size.height / 2) + (radius * sin(midAngleRad)).toFloat()

                            // 4. Draw the percentage text directly onto that X/Y coordinate
                            drawContext.canvas.nativeCanvas.drawText(
                                "${(percentage * 100).toInt()}%",
                                x,
                                y - ((textPaint.descent() + textPaint.ascent()) / 2), // Vertically centers text
                                textPaint
                            )

                            // Move the starting point for the next slice
                            currentStartAngle += sweepAngle
                        }
                    }
                }
            }

            // Draw the empty state text if there is no data
            if (totalData == 0f) {
                Text("No Data", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 3. Toggle Switch
        val primaryColor = MaterialTheme.colorScheme.primary
        val lightBackground = primaryColor.copy(alpha = 0.1f)

        Row(
            modifier = Modifier
                .background(lightBackground, RoundedCornerShape(50))
                .padding(4.dp)
        ) {
            filterOptions.forEach { option ->
                val isSelected = selectedFilter == option

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) primaryColor else Color.Transparent)
                        .clickable { viewModel.setFilter(option) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else Color.Black,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(16.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun InsightsScreenPreview() {
    HeartMusicTheme {
        InsightsScreen()
    }
}