package com.cadastroocr.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco de dados local (SQLite via Room). Os dados dos clientes persistem
 * entre aberturas do app e reinícios do celular — nada aqui depende de
 * internet (Seção 14/16 do briefing).
 */
@Database(entities = [Cliente::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clienteDao(): ClienteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cadastro_ocr.db"
                )
                    // Só as versões 1 e 2 (anteriores à sincronização, sem dados valiosos na nuvem)
                    // podem ser recriadas. A partir da versão 3 NUNCA se apaga o banco: ao mudar o
                    // schema (aumentar `version`), é obrigatório escrever uma Migration com
                    // `.addMigrations(...)`, senão o Room recusa abrir em vez de apagar os clientes.
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
