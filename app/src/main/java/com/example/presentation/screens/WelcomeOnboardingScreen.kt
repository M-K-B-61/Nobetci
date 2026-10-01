package com.example.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingStep(
    val emoji: String,
    val badgeText: String,
    val title: String,
    val description: String
)

@Composable
fun WelcomeOnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val steps = remember {
        listOf(
            OnboardingStep(
                emoji = "🚛",
                badgeText = "ADIM 1 / 3",
                title = "Araç Tipinizi Seçin",
                description = "Tır, Kamyon, Otobüs veya Otomobil... Nöbetçi, seçtiğiniz araca göre özel filtreleme ve hassasiyet algoritmalarını devreye sokar; kabin titreşimlerini ve rölanti sarsıntılarını eler."
            ),
            OnboardingStep(
                emoji = "🎯",
                badgeText = "ADIM 2 / 3",
                title = "Ön Cama Sabitleyin",
                description = "Telefonunuzu tutacağa takıp kamerayı öndeki araca doğrultun. Nöbet başladığı anda kamera önizlemesi kapatılır ve ekran tam siyah yapılarak pil tüketimi en aza indirilir."
            ),
            OnboardingStep(
                emoji = "🔔",
                badgeText = "ADIM 3 / 3",
                title = "Önünüz Açılınca Uyanın",
                description = "Kuyrukta güvenle dinlenin. Önünüzdeki araç hareket ettiğinde Nöbetçi çoklu sensör teyidiyle güçlü alarm ve titreşim döngüsü başlatarak sizi anında uyandırır."
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("welcome_onboarding_screen")
    ) {
        // Skip Button at top right
        if (!isLastStep) {
            TextButton(
                onClick = onFinishOnboarding,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Atla",
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Main Carousel Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "stepAnimation"
            ) { targetStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Big Feature Icon
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(110.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = targetStep.emoji,
                                fontSize = 52.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = targetStep.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = targetStep.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = targetStep.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Bottom Navigation & Stepper
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val isActive = index == currentStepIndex
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) colorScheme.primary else colorScheme.surfaceVariant,
                            modifier = Modifier.size(if (isActive) 12.dp else 8.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Next or Finish Button
                Button(
                    onClick = {
                        if (isLastStep) {
                            onFinishOnboarding()
                        } else {
                            currentStepIndex++
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("onboarding_action_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = if (isLastStep) "Hemen Başla" else "Devam Et",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
