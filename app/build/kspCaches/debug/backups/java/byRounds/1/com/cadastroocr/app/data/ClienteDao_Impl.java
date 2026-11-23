package com.cadastroocr.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ClienteDao_Impl implements ClienteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Cliente> __insertionAdapterOfCliente;

  private final EntityDeletionOrUpdateAdapter<Cliente> __deletionAdapterOfCliente;

  private final EntityDeletionOrUpdateAdapter<Cliente> __updateAdapterOfCliente;

  public ClienteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCliente = new EntityInsertionAdapter<Cliente>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `clientes` (`id`,`nomeCompleto`,`cpf`,`dataNascimento`,`nomeDaMae`,`nomeDoPai`,`rg`,`dataExpedicaoRg`,`email`,`telefone`,`rua`,`numero`,`complemento`,`cep`,`bairro`,`cidade`,`plano`,`valorPlano`,`formaPagamento`,`diaVencimento`,`criadoEm`,`atualizadoEm`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Cliente entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNomeCompleto());
        statement.bindString(3, entity.getCpf());
        statement.bindString(4, entity.getDataNascimento());
        statement.bindString(5, entity.getNomeDaMae());
        statement.bindString(6, entity.getNomeDoPai());
        statement.bindString(7, entity.getRg());
        statement.bindString(8, entity.getDataExpedicaoRg());
        statement.bindString(9, entity.getEmail());
        statement.bindString(10, entity.getTelefone());
        statement.bindString(11, entity.getRua());
        statement.bindString(12, entity.getNumero());
        statement.bindString(13, entity.getComplemento());
        statement.bindString(14, entity.getCep());
        statement.bindString(15, entity.getBairro());
        statement.bindString(16, entity.getCidade());
        statement.bindString(17, entity.getPlano());
        statement.bindString(18, entity.getValorPlano());
        statement.bindString(19, entity.getFormaPagamento());
        statement.bindString(20, entity.getDiaVencimento());
        statement.bindLong(21, entity.getCriadoEm());
        statement.bindLong(22, entity.getAtualizadoEm());
      }
    };
    this.__deletionAdapterOfCliente = new EntityDeletionOrUpdateAdapter<Cliente>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `clientes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Cliente entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCliente = new EntityDeletionOrUpdateAdapter<Cliente>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `clientes` SET `id` = ?,`nomeCompleto` = ?,`cpf` = ?,`dataNascimento` = ?,`nomeDaMae` = ?,`nomeDoPai` = ?,`rg` = ?,`dataExpedicaoRg` = ?,`email` = ?,`telefone` = ?,`rua` = ?,`numero` = ?,`complemento` = ?,`cep` = ?,`bairro` = ?,`cidade` = ?,`plano` = ?,`valorPlano` = ?,`formaPagamento` = ?,`diaVencimento` = ?,`criadoEm` = ?,`atualizadoEm` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Cliente entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNomeCompleto());
        statement.bindString(3, entity.getCpf());
        statement.bindString(4, entity.getDataNascimento());
        statement.bindString(5, entity.getNomeDaMae());
        statement.bindString(6, entity.getNomeDoPai());
        statement.bindString(7, entity.getRg());
        statement.bindString(8, entity.getDataExpedicaoRg());
        statement.bindString(9, entity.getEmail());
        statement.bindString(10, entity.getTelefone());
        statement.bindString(11, entity.getRua());
        statement.bindString(12, entity.getNumero());
        statement.bindString(13, entity.getComplemento());
        statement.bindString(14, entity.getCep());
        statement.bindString(15, entity.getBairro());
        statement.bindString(16, entity.getCidade());
        statement.bindString(17, entity.getPlano());
        statement.bindString(18, entity.getValorPlano());
        statement.bindString(19, entity.getFormaPagamento());
        statement.bindString(20, entity.getDiaVencimento());
        statement.bindLong(21, entity.getCriadoEm());
        statement.bindLong(22, entity.getAtualizadoEm());
        statement.bindLong(23, entity.getId());
      }
    };
  }

  @Override
  public Object inserir(final Cliente cliente, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCliente.insertAndReturnId(cliente);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object excluir(final Cliente cliente, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCliente.handle(cliente);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object atualizar(final Cliente cliente, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCliente.handle(cliente);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Cliente>> observarTodos() {
    final String _sql = "SELECT * FROM clientes ORDER BY nomeCompleto ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"clientes"}, new Callable<List<Cliente>>() {
      @Override
      @NonNull
      public List<Cliente> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNomeCompleto = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeCompleto");
          final int _cursorIndexOfCpf = CursorUtil.getColumnIndexOrThrow(_cursor, "cpf");
          final int _cursorIndexOfDataNascimento = CursorUtil.getColumnIndexOrThrow(_cursor, "dataNascimento");
          final int _cursorIndexOfNomeDaMae = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeDaMae");
          final int _cursorIndexOfNomeDoPai = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeDoPai");
          final int _cursorIndexOfRg = CursorUtil.getColumnIndexOrThrow(_cursor, "rg");
          final int _cursorIndexOfDataExpedicaoRg = CursorUtil.getColumnIndexOrThrow(_cursor, "dataExpedicaoRg");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfTelefone = CursorUtil.getColumnIndexOrThrow(_cursor, "telefone");
          final int _cursorIndexOfRua = CursorUtil.getColumnIndexOrThrow(_cursor, "rua");
          final int _cursorIndexOfNumero = CursorUtil.getColumnIndexOrThrow(_cursor, "numero");
          final int _cursorIndexOfComplemento = CursorUtil.getColumnIndexOrThrow(_cursor, "complemento");
          final int _cursorIndexOfCep = CursorUtil.getColumnIndexOrThrow(_cursor, "cep");
          final int _cursorIndexOfBairro = CursorUtil.getColumnIndexOrThrow(_cursor, "bairro");
          final int _cursorIndexOfCidade = CursorUtil.getColumnIndexOrThrow(_cursor, "cidade");
          final int _cursorIndexOfPlano = CursorUtil.getColumnIndexOrThrow(_cursor, "plano");
          final int _cursorIndexOfValorPlano = CursorUtil.getColumnIndexOrThrow(_cursor, "valorPlano");
          final int _cursorIndexOfFormaPagamento = CursorUtil.getColumnIndexOrThrow(_cursor, "formaPagamento");
          final int _cursorIndexOfDiaVencimento = CursorUtil.getColumnIndexOrThrow(_cursor, "diaVencimento");
          final int _cursorIndexOfCriadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "criadoEm");
          final int _cursorIndexOfAtualizadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "atualizadoEm");
          final List<Cliente> _result = new ArrayList<Cliente>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Cliente _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNomeCompleto;
            _tmpNomeCompleto = _cursor.getString(_cursorIndexOfNomeCompleto);
            final String _tmpCpf;
            _tmpCpf = _cursor.getString(_cursorIndexOfCpf);
            final String _tmpDataNascimento;
            _tmpDataNascimento = _cursor.getString(_cursorIndexOfDataNascimento);
            final String _tmpNomeDaMae;
            _tmpNomeDaMae = _cursor.getString(_cursorIndexOfNomeDaMae);
            final String _tmpNomeDoPai;
            _tmpNomeDoPai = _cursor.getString(_cursorIndexOfNomeDoPai);
            final String _tmpRg;
            _tmpRg = _cursor.getString(_cursorIndexOfRg);
            final String _tmpDataExpedicaoRg;
            _tmpDataExpedicaoRg = _cursor.getString(_cursorIndexOfDataExpedicaoRg);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpTelefone;
            _tmpTelefone = _cursor.getString(_cursorIndexOfTelefone);
            final String _tmpRua;
            _tmpRua = _cursor.getString(_cursorIndexOfRua);
            final String _tmpNumero;
            _tmpNumero = _cursor.getString(_cursorIndexOfNumero);
            final String _tmpComplemento;
            _tmpComplemento = _cursor.getString(_cursorIndexOfComplemento);
            final String _tmpCep;
            _tmpCep = _cursor.getString(_cursorIndexOfCep);
            final String _tmpBairro;
            _tmpBairro = _cursor.getString(_cursorIndexOfBairro);
            final String _tmpCidade;
            _tmpCidade = _cursor.getString(_cursorIndexOfCidade);
            final String _tmpPlano;
            _tmpPlano = _cursor.getString(_cursorIndexOfPlano);
            final String _tmpValorPlano;
            _tmpValorPlano = _cursor.getString(_cursorIndexOfValorPlano);
            final String _tmpFormaPagamento;
            _tmpFormaPagamento = _cursor.getString(_cursorIndexOfFormaPagamento);
            final String _tmpDiaVencimento;
            _tmpDiaVencimento = _cursor.getString(_cursorIndexOfDiaVencimento);
            final long _tmpCriadoEm;
            _tmpCriadoEm = _cursor.getLong(_cursorIndexOfCriadoEm);
            final long _tmpAtualizadoEm;
            _tmpAtualizadoEm = _cursor.getLong(_cursorIndexOfAtualizadoEm);
            _item = new Cliente(_tmpId,_tmpNomeCompleto,_tmpCpf,_tmpDataNascimento,_tmpNomeDaMae,_tmpNomeDoPai,_tmpRg,_tmpDataExpedicaoRg,_tmpEmail,_tmpTelefone,_tmpRua,_tmpNumero,_tmpComplemento,_tmpCep,_tmpBairro,_tmpCidade,_tmpPlano,_tmpValorPlano,_tmpFormaPagamento,_tmpDiaVencimento,_tmpCriadoEm,_tmpAtualizadoEm);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Cliente>> pesquisar(final String termo) {
    final String _sql = "\n"
            + "        SELECT * FROM clientes\n"
            + "        WHERE nomeCompleto LIKE '%' || ? || '%'\n"
            + "           OR cpf LIKE '%' || ? || '%'\n"
            + "           OR email LIKE '%' || ? || '%'\n"
            + "           OR telefone LIKE '%' || ? || '%'\n"
            + "        ORDER BY nomeCompleto ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindString(_argIndex, termo);
    _argIndex = 2;
    _statement.bindString(_argIndex, termo);
    _argIndex = 3;
    _statement.bindString(_argIndex, termo);
    _argIndex = 4;
    _statement.bindString(_argIndex, termo);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"clientes"}, new Callable<List<Cliente>>() {
      @Override
      @NonNull
      public List<Cliente> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNomeCompleto = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeCompleto");
          final int _cursorIndexOfCpf = CursorUtil.getColumnIndexOrThrow(_cursor, "cpf");
          final int _cursorIndexOfDataNascimento = CursorUtil.getColumnIndexOrThrow(_cursor, "dataNascimento");
          final int _cursorIndexOfNomeDaMae = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeDaMae");
          final int _cursorIndexOfNomeDoPai = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeDoPai");
          final int _cursorIndexOfRg = CursorUtil.getColumnIndexOrThrow(_cursor, "rg");
          final int _cursorIndexOfDataExpedicaoRg = CursorUtil.getColumnIndexOrThrow(_cursor, "dataExpedicaoRg");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfTelefone = CursorUtil.getColumnIndexOrThrow(_cursor, "telefone");
          final int _cursorIndexOfRua = CursorUtil.getColumnIndexOrThrow(_cursor, "rua");
          final int _cursorIndexOfNumero = CursorUtil.getColumnIndexOrThrow(_cursor, "numero");
          final int _cursorIndexOfComplemento = CursorUtil.getColumnIndexOrThrow(_cursor, "complemento");
          final int _cursorIndexOfCep = CursorUtil.getColumnIndexOrThrow(_cursor, "cep");
          final int _cursorIndexOfBairro = CursorUtil.getColumnIndexOrThrow(_cursor, "bairro");
          final int _cursorIndexOfCidade = CursorUtil.getColumnIndexOrThrow(_cursor, "cidade");
          final int _cursorIndexOfPlano = CursorUtil.getColumnIndexOrThrow(_cursor, "plano");
          final int _cursorIndexOfValorPlano = CursorUtil.getColumnIndexOrThrow(_cursor, "valorPlano");
          final int _cursorIndexOfFormaPagamento = CursorUtil.getColumnIndexOrThrow(_cursor, "formaPagamento");
          final int _cursorIndexOfDiaVencimento = CursorUtil.getColumnIndexOrThrow(_cursor, "diaVencimento");
          final int _cursorIndexOfCriadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "criadoEm");
          final int _cursorIndexOfAtualizadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "atualizadoEm");
          final List<Cliente> _result = new ArrayList<Cliente>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Cliente _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNomeCompleto;
            _tmpNomeCompleto = _cursor.getString(_cursorIndexOfNomeCompleto);
            final String _tmpCpf;
            _tmpCpf = _cursor.getString(_cursorIndexOfCpf);
            final String _tmpDataNascimento;
            _tmpDataNascimento = _cursor.getString(_cursorIndexOfDataNascimento);
            final String _tmpNomeDaMae;
            _tmpNomeDaMae = _cursor.getString(_cursorIndexOfNomeDaMae);
            final String _tmpNomeDoPai;
            _tmpNomeDoPai = _cursor.getString(_cursorIndexOfNomeDoPai);
            final String _tmpRg;
            _tmpRg = _cursor.getString(_cursorIndexOfRg);
            final String _tmpDataExpedicaoRg;
            _tmpDataExpedicaoRg = _cursor.getString(_cursorIndexOfDataExpedicaoRg);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpTelefone;
            _tmpTelefone = _cursor.getString(_cursorIndexOfTelefone);
            final String _tmpRua;
            _tmpRua = _cursor.getString(_cursorIndexOfRua);
            final String _tmpNumero;
            _tmpNumero = _cursor.getString(_cursorIndexOfNumero);
            final String _tmpComplemento;
            _tmpComplemento = _cursor.getString(_cursorIndexOfComplemento);
            final String _tmpCep;
            _tmpCep = _cursor.getString(_cursorIndexOfCep);
            final String _tmpBairro;
            _tmpBairro = _cursor.getString(_cursorIndexOfBairro);
            final String _tmpCidade;
            _tmpCidade = _cursor.getString(_cursorIndexOfCidade);
            final String _tmpPlano;
            _tmpPlano = _cursor.getString(_cursorIndexOfPlano);
            final String _tmpValorPlano;
            _tmpValorPlano = _cursor.getString(_cursorIndexOfValorPlano);
            final String _tmpFormaPagamento;
            _tmpFormaPagamento = _cursor.getString(_cursorIndexOfFormaPagamento);
            final String _tmpDiaVencimento;
            _tmpDiaVencimento = _cursor.getString(_cursorIndexOfDiaVencimento);
            final long _tmpCriadoEm;
            _tmpCriadoEm = _cursor.getLong(_cursorIndexOfCriadoEm);
            final long _tmpAtualizadoEm;
            _tmpAtualizadoEm = _cursor.getLong(_cursorIndexOfAtualizadoEm);
            _item = new Cliente(_tmpId,_tmpNomeCompleto,_tmpCpf,_tmpDataNascimento,_tmpNomeDaMae,_tmpNomeDoPai,_tmpRg,_tmpDataExpedicaoRg,_tmpEmail,_tmpTelefone,_tmpRua,_tmpNumero,_tmpComplemento,_tmpCep,_tmpBairro,_tmpCidade,_tmpPlano,_tmpValorPlano,_tmpFormaPagamento,_tmpDiaVencimento,_tmpCriadoEm,_tmpAtualizadoEm);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object buscarPorId(final long id, final Continuation<? super Cliente> $completion) {
    final String _sql = "SELECT * FROM clientes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Cliente>() {
      @Override
      @Nullable
      public Cliente call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNomeCompleto = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeCompleto");
          final int _cursorIndexOfCpf = CursorUtil.getColumnIndexOrThrow(_cursor, "cpf");
          final int _cursorIndexOfDataNascimento = CursorUtil.getColumnIndexOrThrow(_cursor, "dataNascimento");
          final int _cursorIndexOfNomeDaMae = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeDaMae");
          final int _cursorIndexOfNomeDoPai = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeDoPai");
          final int _cursorIndexOfRg = CursorUtil.getColumnIndexOrThrow(_cursor, "rg");
          final int _cursorIndexOfDataExpedicaoRg = CursorUtil.getColumnIndexOrThrow(_cursor, "dataExpedicaoRg");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfTelefone = CursorUtil.getColumnIndexOrThrow(_cursor, "telefone");
          final int _cursorIndexOfRua = CursorUtil.getColumnIndexOrThrow(_cursor, "rua");
          final int _cursorIndexOfNumero = CursorUtil.getColumnIndexOrThrow(_cursor, "numero");
          final int _cursorIndexOfComplemento = CursorUtil.getColumnIndexOrThrow(_cursor, "complemento");
          final int _cursorIndexOfCep = CursorUtil.getColumnIndexOrThrow(_cursor, "cep");
          final int _cursorIndexOfBairro = CursorUtil.getColumnIndexOrThrow(_cursor, "bairro");
          final int _cursorIndexOfCidade = CursorUtil.getColumnIndexOrThrow(_cursor, "cidade");
          final int _cursorIndexOfPlano = CursorUtil.getColumnIndexOrThrow(_cursor, "plano");
          final int _cursorIndexOfValorPlano = CursorUtil.getColumnIndexOrThrow(_cursor, "valorPlano");
          final int _cursorIndexOfFormaPagamento = CursorUtil.getColumnIndexOrThrow(_cursor, "formaPagamento");
          final int _cursorIndexOfDiaVencimento = CursorUtil.getColumnIndexOrThrow(_cursor, "diaVencimento");
          final int _cursorIndexOfCriadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "criadoEm");
          final int _cursorIndexOfAtualizadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "atualizadoEm");
          final Cliente _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNomeCompleto;
            _tmpNomeCompleto = _cursor.getString(_cursorIndexOfNomeCompleto);
            final String _tmpCpf;
            _tmpCpf = _cursor.getString(_cursorIndexOfCpf);
            final String _tmpDataNascimento;
            _tmpDataNascimento = _cursor.getString(_cursorIndexOfDataNascimento);
            final String _tmpNomeDaMae;
            _tmpNomeDaMae = _cursor.getString(_cursorIndexOfNomeDaMae);
            final String _tmpNomeDoPai;
            _tmpNomeDoPai = _cursor.getString(_cursorIndexOfNomeDoPai);
            final String _tmpRg;
            _tmpRg = _cursor.getString(_cursorIndexOfRg);
            final String _tmpDataExpedicaoRg;
            _tmpDataExpedicaoRg = _cursor.getString(_cursorIndexOfDataExpedicaoRg);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpTelefone;
            _tmpTelefone = _cursor.getString(_cursorIndexOfTelefone);
            final String _tmpRua;
            _tmpRua = _cursor.getString(_cursorIndexOfRua);
            final String _tmpNumero;
            _tmpNumero = _cursor.getString(_cursorIndexOfNumero);
            final String _tmpComplemento;
            _tmpComplemento = _cursor.getString(_cursorIndexOfComplemento);
            final String _tmpCep;
            _tmpCep = _cursor.getString(_cursorIndexOfCep);
            final String _tmpBairro;
            _tmpBairro = _cursor.getString(_cursorIndexOfBairro);
            final String _tmpCidade;
            _tmpCidade = _cursor.getString(_cursorIndexOfCidade);
            final String _tmpPlano;
            _tmpPlano = _cursor.getString(_cursorIndexOfPlano);
            final String _tmpValorPlano;
            _tmpValorPlano = _cursor.getString(_cursorIndexOfValorPlano);
            final String _tmpFormaPagamento;
            _tmpFormaPagamento = _cursor.getString(_cursorIndexOfFormaPagamento);
            final String _tmpDiaVencimento;
            _tmpDiaVencimento = _cursor.getString(_cursorIndexOfDiaVencimento);
            final long _tmpCriadoEm;
            _tmpCriadoEm = _cursor.getLong(_cursorIndexOfCriadoEm);
            final long _tmpAtualizadoEm;
            _tmpAtualizadoEm = _cursor.getLong(_cursorIndexOfAtualizadoEm);
            _result = new Cliente(_tmpId,_tmpNomeCompleto,_tmpCpf,_tmpDataNascimento,_tmpNomeDaMae,_tmpNomeDoPai,_tmpRg,_tmpDataExpedicaoRg,_tmpEmail,_tmpTelefone,_tmpRua,_tmpNumero,_tmpComplemento,_tmpCep,_tmpBairro,_tmpCidade,_tmpPlano,_tmpValorPlano,_tmpFormaPagamento,_tmpDiaVencimento,_tmpCriadoEm,_tmpAtualizadoEm);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
