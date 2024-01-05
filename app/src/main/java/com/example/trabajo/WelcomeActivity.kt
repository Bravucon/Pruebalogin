package com.example.trabajo

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


private lateinit var analytics: FirebaseAnalytics
private lateinit var auth: FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        private const val GOOGLE_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val signButton = findViewById<Button>(R.id.signButton)
        val emailTextview = findViewById<TextView>(R.id.emailTextview)
        val editTextTextPassword = findViewById<TextView>(R.id.editTextTextPassword)
        val logBotton = findViewById<Button>(R.id.logBotton)
        val googleButton = findViewById<Button>(R.id.googleButton)


        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message", "Integracion de Firebase completa")
        analytics.logEvent("InitScreen", bundle)
        session()

        signButton.setOnClickListener {
            if (emailTextview.text.isNotEmpty() && editTextTextPassword.text.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    emailTextview.text.toString(),
                    editTextTextPassword.text.toString()
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                    } else {
                        showAlert("Error")
                    }
                }
            }
        }
        logBotton.setOnClickListener {
            if (emailTextview.text.isNotEmpty() && editTextTextPassword.text.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    emailTextview.text.toString(),
                    editTextTextPassword.text.toString()
                )
                    .addOnCompleteListener {
                        if (it.isSuccessful) {

                            val homeIntent: Intent = Intent(this, HomeActivity::class.java).apply {
                                putExtra("email", emailTextview.text.toString())
                                putExtra("provider", ProviderType.BASIC.toString())
                            }
                            startActivity(homeIntent)

                        } else {
                            showAlert("logBotton")
                        }
                    }
            }
        }
        googleButton.setOnClickListener {

            // CONFIGURACION

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleClient = GoogleSignIn.getClient(this, googleConf)

            //googleClient.signOut()

            val signInIntent = googleClient.signInIntent //gpt

            startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
        }

    }


    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent: Intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }

    private fun showAlert(er: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Error de Usuario o Contraseña $er")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        val authLayout = findViewById<LinearLayout>(R.id.authLayout)
        authLayout.visibility = View.VISIBLE
    }

    private fun session() {
        val authLayout = findViewById<LinearLayout>(R.id.authLayout)
        val prefs =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email != null && provider != null) {
            authLayout.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Asegurarse de que auth esté inicializado
        auth = FirebaseAuth.getInstance()

        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                showAlert("log mal")


            }
        }
    }


    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión con éxito
                    Log.d(TAG, "signInWithCredential:success")

                    // Obtener información del usuario
                    val user = auth.currentUser
                    val email = user?.email
                    val provider = user?.providerId

                    // Llamar a la función showHome y pasar los datos
                    showHome(email?:"", ProviderType.GOOGLE)

                } else {
                    // Si el inicio de sesión con Firebase falla, muestra un mensaje al usuario
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

}