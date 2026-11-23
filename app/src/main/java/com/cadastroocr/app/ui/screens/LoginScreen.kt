package com.cadastroocr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.BackgroundDark
import com.cadastroocr.app.ui.theme.BorderSubtle
import com.cadastroocr.app.ui.theme.DangerRed
import com.cadastroocr.app.ui.theme.SurfaceDark
import com.cadastroocr.app.ui.theme.TextMuted
import com.cadastroocr.app.ui.theme.TextPrimary
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.viewmodel.ClienteViewModel

@Composable
fun LoginScreen(
    viewModel: ClienteViewModel,
    onLoginSucesso: () -> Unit
) {
    var isCadastro by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone e Logo
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Cadastro OCR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Sincronização em tempo real entre celular e computador",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // Card do Formulário
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Abas Entrar / Criar Conta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = {
                                isCadastro = false
                                mensagemErro = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isCadastro) AccentGreen else Color.Transparent,
                                contentColor = if (!isCadastro) Color.Black else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Entrar", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isCadastro = true
                                mensagemErro = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCadastro) AccentGreen else Color.Transparent,
                                contentColor = if (isCadastro) Color.Black else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Criar conta", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Campo E-mail
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            mensagemErro = null
                        },
                        label = { Text("E-mail") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    // Campo Senha
                    OutlinedTextField(
                        value = senha,
                        onValueChange = {
                            senha = it
                            mensagemErro = null
                        },
                        label = { Text("Senha") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                                Icon(
                                    imageVector = if (senhaVisivel) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (mensagemErro != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = mensagemErro ?: "",
                            color = DangerRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // Botão Principal
                    Button(
                        onClick = {
                            if (email.isBlank() || senha.isBlank()) {
                                mensagemErro = "Preencha o e-mail e a senha"
                                return@Button
                            }
                            carregando = true
                            mensagemErro = null

                            if (isCadastro) {
                                viewModel.fazerCadastro(email, senha) { res ->
                                    carregando = false
                                    if (res.isSuccess) {
                                        onLoginSucesso()
                                    } else {
                                        mensagemErro = res.exceptionOrNull()?.message ?: "Erro ao criar conta"
                                    }
                                }
                            } else {
                                viewModel.fazerLogin(email, senha) { res ->
                                    carregando = false
                                    if (res.isSuccess) {
                                        onLoginSucesso()
                                    } else {
                                        mensagemErro = res.exceptionOrNull()?.message ?: "Erro ao autenticar"
                                    }
                                }
                            }
                        },
                        enabled = !carregando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (carregando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isCadastro) "Cadastrar e Sincronizar" else "Entrar no Sistema",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Opção de usar Offline
            TextButton(
                onClick = {
                    viewModel.usarModoOffline()
                    onLoginSucesso()
                }
            ) {
                Text(
                    text = "Continuar sem conta (Modo Offline)",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
