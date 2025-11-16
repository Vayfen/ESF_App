package com.esf.calendar.ui.screens.login

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esf.calendar.data.remote.ESFAuthService
import com.esf.calendar.ui.components.AnimatedGradientBackground
import com.esf.calendar.ui.components.GlassCard

/**
 * Écran de connexion avec WebView pour l'authentification ESF
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Observer le changement d'état pour naviguer
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fond animé
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())

        // Contenu
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(animationSpec = tween(700)) with
                        fadeOut(animationSpec = tween(700))
            },
            label = "loginContent"
        ) { state ->
            when (state) {
                is LoginUiState.Initial, is LoginUiState.Error -> {
                    WelcomeScreen(
                        onLoginClick = { viewModel.startLogin() },
                        error = if (state is LoginUiState.Error) state.message else null
                    )
                }
                is LoginUiState.Loading -> {
                    WebViewLoginScreen(viewModel = viewModel)
                }
                is LoginUiState.Success -> {
                    // Navigation gérée par LaunchedEffect
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Écran d'accueil avec bouton de connexion
 */
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Titre animé
        Text(
            text = "ESF Calendar",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Votre emploi du temps ESF,\ntoujours avec vous",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Carte avec bouton de connexion
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowEffect = true
        ) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Se connecter avec ESF",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Message d'erreur
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Écran WebView pour l'authentification
 */
@Composable
fun WebViewLoginScreen(
    viewModel: LoginViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authService = remember { ESFAuthService(context) }
    var isPageLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instructions
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Connectez-vous avec vos identifiants ESF",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isPageLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WebView
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Configurer l'authentification
                        authService.authenticate(
                            credentials = com.esf.calendar.data.model.ESFCredentials("", ""),
                            webView = this,
                            onAuthSuccess = { cookies ->
                                // TODO: Extraire le moniteurId depuis la page ou les cookies
                                // Pour l'instant, on utilise l'ID hardcodé du script Python
                                viewModel.onAuthSuccess(cookies, "19358136")
                            },
                            onAuthError = { error ->
                                viewModel.onAuthError(error)
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
