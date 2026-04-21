package com.lg.monkeymusicplayer.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.billing.BillingState
import com.lg.monkeymusicplayer.core.feature.Feature
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange

/**
 * PaywallScreen — muestra las features PRO y el botón de compra.
 *
 * Flujo:
 * 1. Se listan las features premium con sus iconos.
 * 2. El botón CTA muestra el precio formateado desde [BillingManager.proProductDetails].
 * 3. Al pulsar, se lanza [BillingManager.launchBillingFlow].
 * 4. [BillingState.PurchaseSuccess] cierra automáticamente la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    val billingState by viewModel.billingManager.billingState.collectAsState()
    val productDetails by viewModel.billingManager.proProductDetails.collectAsState()
    val isProUnlocked = viewModel.featureGate.isProUnlocked()

    // Cerrar automáticamente si la compra se completó
    LaunchedEffect(billingState) {
        if (billingState is BillingState.PurchaseSuccess) {
            onBack()
        }
    }

    val premiumFeatures = listOf(
        Feature.VISUALIZER     to Icons.Default.GraphicEq,
        Feature.CROSSFADE      to Icons.Default.BlurOn,
        Feature.EQUALIZER_PRESETS to Icons.Default.Tune,
        Feature.SMART_PLAYLISTS to Icons.Default.AutoAwesome,
        Feature.STATS          to Icons.Default.BarChart,
        Feature.BACKUP         to Icons.Default.CloudUpload,
        Feature.REPLAY_GAIN    to Icons.Default.VolumeUp,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A0A00),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.pro_upgrade_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
            Text(
                text = stringResource(R.string.pro_upgrade_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Lista de features
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    premiumFeatures.forEachIndexed { index, (feature, icon) ->
                        ProFeatureRow(
                            icon = icon,
                            label = feature.displayName,
                            isUnlocked = viewModel.featureGate.isUnlocked(feature)
                        )
                        if (index < premiumFeatures.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // CTA Button
            if (isProUnlocked) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryOrange.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = PrimaryOrange)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.pro_already_pro),
                            color = PrimaryOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                val isPurchasing = billingState is BillingState.Purchasing
                val isError = billingState is BillingState.Error
                val priceText = productDetails
                    ?.oneTimePurchaseOfferDetails
                    ?.formattedPrice
                    ?: stringResource(R.string.pro_upgrade_cta)

                Button(
                    onClick = { activity?.let { viewModel.launchProUpgrade(it) } },
                    enabled = !isPurchasing && billingState !is BillingState.Disconnected,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (productDetails != null)
                                "${stringResource(R.string.pro_upgrade_cta)} — $priceText"
                            else stringResource(R.string.pro_upgrade_cta),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                if (isError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (billingState as BillingState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                if (billingState is BillingState.Disconnected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.pro_billing_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: ImageVector,
    label: String,
    isUnlocked: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PrimaryOrange.copy(0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.LockOpen,
            contentDescription = null,
            tint = if (isUnlocked) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
