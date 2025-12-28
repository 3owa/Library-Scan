package com.booky

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.gson.Gson
import com.booky.ui.theme.BookyTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BookyTheme {
                BookyApp()
            }
        }
    }
}

data class Book(
    val title: String,
    val author: String,
    val description: String
)

data class TakenBook(
    val book: Book,
    val dateTaken: Date
)

data class UserProfile(
    var fullName: String = "",
    var email: String = "",
    var studentId: String = ""
)

// Liste des livres non disponibles (hardcodé)
val unavailableBooks = listOf("1984")

@Composable
fun BookyApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf(UserProfile()) }

    if (isLoggedIn) {
        MainScreen(userProfile) { profile ->
            userProfile = profile
        }
    } else {
        AuthScreen { profile ->
            userProfile = profile
            isLoggedIn = true
        }
    }
}

@Composable
fun AuthScreen(onLogin: (UserProfile) -> Unit) {
    var isSignIn by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Library Scan",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isSignIn) {
                SignInForm(
                    onSignIn = { email ->
                        onLogin(UserProfile(email = email))
                    },
                    onSwitchToRegister = { isSignIn = false }
                )
            } else {
                RegisterForm(
                    onRegister = { profile ->
                        onLogin(profile)
                    },
                    onSwitchToSignIn = { isSignIn = true }
                )
            }
        }
    }
}

@Composable
fun SignInForm(
    onSignIn: (String) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                    )
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSignIn(email) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se connecter")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToRegister) {
            Text("Créer un compte")
        }
    }
}

@Composable
fun RegisterForm(
    onRegister: (UserProfile) -> Unit,
    onSwitchToSignIn: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nom complet") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = { Text("ID Étudiant") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                    )
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmer mot de passe") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Masquer" else "Afficher"
                    )
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onRegister(UserProfile(fullName, email, studentId))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("S'inscrire")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToSignIn) {
            Text("Déjà un compte? Se connecter")
        }
    }
}

@Composable
fun MainScreen(userProfile: UserProfile, onProfileUpdate: (UserProfile) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var takenBooks by remember { mutableStateOf(listOf<TakenBook>()) }
    var notifiedBooks by remember { mutableStateOf(listOf<String>()) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Accueil") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    label = { Text("IA") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profil") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeTab(
                modifier = Modifier.padding(padding),
                takenBooks = takenBooks,
                onBookTaken = { book ->
                    takenBooks = takenBooks + TakenBook(book, Date())
                },
                onBookReturned = { takenBook ->
                    takenBooks = takenBooks.filter { it != takenBook }
                }
            )
            1 -> AITab(
                modifier = Modifier.padding(padding),
                notifiedBooks = notifiedBooks,
                onNotifyMe = { bookTitle ->
                    if (!notifiedBooks.contains(bookTitle)) {
                        notifiedBooks = notifiedBooks + bookTitle
                    }
                }
            )
            2 -> ProfileTab(
                modifier = Modifier.padding(padding),
                userProfile = userProfile,
                onProfileUpdate = onProfileUpdate
            )
        }
    }
}

@Composable
fun HomeTab(
    modifier: Modifier = Modifier,
    takenBooks: List<TakenBook>,
    onBookTaken: (Book) -> Unit,
    onBookReturned: (TakenBook) -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }
    var scannedBook by remember { mutableStateOf<Book?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Bienvenue sur Library Scan!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showScanner = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner un livre")
        }

        if (showScanner) {
            Spacer(modifier = Modifier.height(16.dp))
            QRScannerComponent(
                onQRScanned = { book ->
                    scannedBook = book
                    showScanner = false
                },
                onClose = { showScanner = false }
            )
        }

        if (scannedBook != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val isAvailable = !unavailableBooks.contains(scannedBook!!.title)

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = scannedBook!!.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = scannedBook!!.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scannedBook!!.description,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (!isAvailable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Non disponible actuellement",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { scannedBook = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Réinitialiser")
                        }
                        Button(
                            onClick = {
                                onBookTaken(scannedBook!!)
                                scannedBook = null
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isAvailable
                        ) {
                            Text("Prendre")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Livres empruntés",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (takenBooks.isEmpty()) {
            Text(
                text = "Aucun livre emprunté",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            takenBooks.forEach { takenBook ->
                TakenBookCard(
                    takenBook = takenBook,
                    onReturn = { onBookReturned(takenBook) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TakenBookCard(
    takenBook: TakenBook,
    onReturn: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = takenBook.book.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = takenBook.book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Emprunté le: ${dateFormat.format(takenBook.dateTaken)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Filled.KeyboardReturn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remettre")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerComponent(
    onQRScanned: (Book) -> Unit,
    onClose: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(
                    onQRCodeScanned = { qrContent ->
                        try {
                            val gson = Gson()
                            val book = gson.fromJson(qrContent, Book::class.java)
                            if (book.title.isNotEmpty() && book.author.isNotEmpty()) {
                                onQRScanned(book)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permission caméra requise",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() }
                    ) {
                        Text("Autoriser")
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fermer",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val barcodeScanner = BarcodeScanning.getClient()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                if (!hasScanned) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    if (barcode.valueType == Barcode.TYPE_TEXT) {
                                        barcode.rawValue?.let { qrContent ->
                                            hasScanned = true
                                            onQRCodeScanned(qrContent)
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }
}

@Composable
fun AITab(
    modifier: Modifier = Modifier,
    notifiedBooks: List<String>,
    onNotifyMe: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestedBooks by remember { mutableStateOf<List<Book>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Assistant IA",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Décrivez vos préférences de lecture") },
            placeholder = { Text("Ex: Romans de science-fiction, livres de développement personnel...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                suggestedBooks = listOf(
                    Book(
                        title = "1984",
                        author = "George Orwell",
                        description = "Un roman dystopique qui explore les thèmes de la surveillance totalitaire et de la manipulation de la vérité."
                    ),
                    Book(
                        title = "L'Étranger",
                        author = "Albert Camus",
                        description = "Un roman philosophique qui examine l'absurdité de l'existence humaine à travers l'histoire de Meursault."
                    ),
                    Book(
                        title = "Sapiens",
                        author = "Yuval Noah Harari",
                        description = "Une brève histoire de l'humanité qui explore comment Homo sapiens en est venu à dominer le monde."
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Suggérer des livres")
        }

        if (suggestedBooks != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Livres suggérés",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            suggestedBooks!!.forEach { book ->
                BookSuggestionCard(
                    book = book,
                    isNotified = notifiedBooks.contains(book.title),
                    onNotifyMe = { onNotifyMe(book.title) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BookSuggestionCard(
    book: Book,
    isNotified: Boolean,
    onNotifyMe: () -> Unit
) {
    val isAvailable = !unavailableBooks.contains(book.title)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isAvailable) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isNotified) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Notification activée") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null
                            )
                        },
                        enabled = false
                    )
                } else {
                    Button(
                        onClick = onNotifyMe,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Me notifier quand disponible")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProfileTab(
    modifier: Modifier = Modifier,
    userProfile: UserProfile,
    onProfileUpdate: (UserProfile) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedProfile by remember { mutableStateOf(userProfile) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mon Profil",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = {
                if (isEditing) {
                    onProfileUpdate(editedProfile)
                } else {
                    editedProfile = userProfile.copy()
                }
                isEditing = !isEditing
            }) {
                Icon(
                    imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                    contentDescription = if (isEditing) "Sauvegarder" else "Modifier"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isEditing) {
            OutlinedTextField(
                value = editedProfile.fullName,
                onValueChange = { editedProfile = editedProfile.copy(fullName = it) },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = editedProfile.email,
                onValueChange = { editedProfile = editedProfile.copy(email = it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = editedProfile.studentId,
                onValueChange = { editedProfile = editedProfile.copy(studentId = it) },
                label = { Text("ID Étudiant") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            ProfileInfoItem("Nom complet", userProfile.fullName)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileInfoItem("Email", userProfile.email)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileInfoItem("ID Étudiant", userProfile.studentId)
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifEmpty { "Non renseigné" },
            style = MaterialTheme.typography.bodyLarge
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}