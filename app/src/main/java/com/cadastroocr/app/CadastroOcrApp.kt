package com.cadastroocr.app

import android.app.Application
import com.cadastroocr.app.auth.AuthManager
import com.cadastroocr.app.data.AppDatabase
import com.cadastroocr.app.data.ClienteRepository
import com.cadastroocr.app.sync.SupabaseApiService
import com.cadastroocr.app.sync.SyncManager

class CadastroOcrApp : Application() {

    val apiService: SupabaseApiService by lazy {
        SupabaseApiService()
    }

    val authManager: AuthManager by lazy {
        AuthManager(this, apiService)
    }

    val syncManager: SyncManager by lazy {
        val db = AppDatabase.getInstance(this)
        SyncManager(this, db.clienteDao(), apiService, authManager)
    }

    val repository: ClienteRepository by lazy {
        val db = AppDatabase.getInstance(this)
        ClienteRepository(db.clienteDao(), syncManager)
    }
}

