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
                    // O schema ainda está sendo ajustado durante o desenvolvimento.
                    // Isso apaga e recria o banco quando a versão muda, em vez de travar
                    // com erro. Antes de publicar a versão final para uso real, troque
                    // isso por uma Migration de verdade para não perder dados dos clientes.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
