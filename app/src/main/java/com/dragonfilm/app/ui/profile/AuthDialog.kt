package com.dragonfilm.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    authManager: AuthManager,
    onDismiss: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DFSpacing.md)
                .glassCard(cornerRadius = DFRadius.xl),
            color = DFColor.CardBgSolid,
            shape = RoundedCornerShape(DFRadius.xl)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DFSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isRegisterMode) "Đăng Ký Tài Khoản" else "Đăng Nhập",
                        style = DFTypography.title,
                        color = DFColor.Text
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = DFColor.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher (Đăng nhập / Đăng ký)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(DFColor.Bg3)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (!isRegisterMode) DFColor.Gold else Color.Transparent)
                            .clickable {
                                isRegisterMode = false
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đăng Nhập",
                            style = DFTypography.caption.copy(
                                color = if (!isRegisterMode) Color(0xFF07080A) else DFColor.TextDim
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (isRegisterMode) DFColor.Gold else Color.Transparent)
                            .clickable {
                                isRegisterMode = true
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đăng Ký",
                            style = DFTypography.caption.copy(
                                color = if (isRegisterMode) Color(0xFF07080A) else DFColor.TextDim
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tên tài khoản") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DFRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DFColor.Gold,
                        unfocusedBorderColor = DFColor.Border,
                        focusedTextColor = DFColor.Text,
                        unfocusedTextColor = DFColor.Text,
                        focusedLabelColor = DFColor.Gold,
                        unfocusedLabelColor = DFColor.TextMuted
                    )
                )

                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DFRadius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DFColor.Gold,
                            unfocusedBorderColor = DFColor.Border,
                            focusedTextColor = DFColor.Text,
                            unfocusedTextColor = DFColor.Text,
                            focusedLabelColor = DFColor.Gold,
                            unfocusedLabelColor = DFColor.TextMuted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DFRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DFColor.Gold,
                        unfocusedBorderColor = DFColor.Border,
                        focusedTextColor = DFColor.Text,
                        unfocusedTextColor = DFColor.Text,
                        focusedLabelColor = DFColor.Gold,
                        unfocusedLabelColor = DFColor.TextMuted
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        style = DFTypography.small.copy(color = DFColor.Crimson)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = DFColor.Gold, spotColor = DFColor.Gold)
                        .clip(CircleShape)
                        .background(DFColor.GoldGradient)
                        .clickable(enabled = !isLoading && username.isNotEmpty() && password.isNotEmpty()) {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    if (isRegisterMode) {
                                        val res = ApiClient.service.register(
                                            mapOf("username" to username.trim(), "password" to password, "email" to email.trim())
                                        )
                                        if (res.ok && res.token != null && res.user != null) {
                                            authManager.setSession(res.token, res.user)
                                            onDismiss()
                                        } else {
                                            errorMessage = res.error ?: "Đăng ký không thành công"
                                        }
                                    } else {
                                        val res = ApiClient.service.login(
                                            mapOf("username" to username.trim(), "password" to password)
                                        )
                                        if (res.ok && res.token != null && res.user != null) {
                                            authManager.setSession(res.token, res.user)
                                            onDismiss()
                                        } else {
                                            errorMessage = res.error ?: "Sai tên đăng nhập hoặc mật khẩu"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Không thể kết nối máy chủ. Kiểm tra mạng."
                                }
                                isLoading = false
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DFColor.Bg,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "Tạo Tài Khoản" else "Đăng Nhập",
                            style = DFTypography.headline.copy(color = Color(0xFF07080A))
                        )
                    }
                }
            }
        }
    }
}
