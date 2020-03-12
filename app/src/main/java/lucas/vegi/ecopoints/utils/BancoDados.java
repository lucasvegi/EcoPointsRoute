package lucas.vegi.ecopoints.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by Lucas on 12/06/2015.
 * SINGLETON
 */

public final class BancoDados {
    private static BancoDados INSTANCE;
    private static SQLiteDatabase db;

    private final String NOME_BANCO = "bdEcopoints";

    //TODO: popular o banco com tipos e ecopoints reais
    private final String[] SCRIPT_DATABASE_CREATE = new String[] {
            "CREATE TABLE Tipo (idTipo INTEGER PRIMARY KEY, nome TEXT NOT NULL);",
            "CREATE TABLE Ecopoint (idEcopoint INTEGER PRIMARY KEY, nome TEXT NOT NULL, descricao TEXT, latitude TEXT NOT NULL, longitude TEXT NOT NULL);",
            "CREATE TABLE Ecopoint_Tipo (idTipo INTEGER, idEcopoint INTEGER, CONSTRAINT pkc_relation PRIMARY KEY (idTipo, idEcopoint), CONSTRAINT fk_tipo FOREIGN KEY (idTipo) REFERENCES Tipo (idTipo), CONSTRAINT fk_ecopoint FOREIGN KEY (idEcopoint) REFERENCES Ecopoint (idEcopoint));",
            "INSERT INTO Tipo (idTipo, nome) VALUES (1, 'Pilha');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (2, 'Óleo');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (3, 'Remédio');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (4, 'Pneu');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (1, 'Quatro Pilastras', '-20.757360', '-42.875076');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (2, 'No Bugs', '-20.762948', '-42.867008');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (3, 'Biblioteca Central', '-20.761222', '-42.867780');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (4, 'Paróquia Santa Rita', '-20.753472', '-42.881833');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (5, 'Shopping Calçadão', '-20.754752', '-42.879898');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (6, 'Eletro Paulo', '-20.752619', '-42.879909');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (7, 'Droga Shop', '-20.753705', '-42.881014');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (8, 'Supermercado Amantino', '-20.752802', '-42.882706');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (9, 'Supermercado Escola', '-20.765506', '-42.869022');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (10, 'Crohma Informática', '-20.756613', '-42.875917');",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,1);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,2);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,6);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,10);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,8);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,9);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,4);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,7);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,3);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,5);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,2);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,1);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (4,3);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (4,9);"};

    public static synchronized BancoDados getINSTANCE(Context ctx){
        if(INSTANCE == null)
            INSTANCE = new BancoDados(ctx);
        return INSTANCE;
    }

    private BancoDados(Context ctx) {
        // Abre o banco de dados já existente ou então cria um banco novo
        db = ctx.openOrCreateDatabase(NOME_BANCO, Context.MODE_PRIVATE, null);
        Log.i("BANCO_DADOS", "Abriu conexão com o banco.");

        //busca por tabelas existentes no banco = "show tables" do MySQL
        //SELECT * FROM sqlite_master WHERE type = "table"
        Cursor c = buscar("sqlite_master", null, "type = 'table'", "");

        //Cria tabelas do banco de dados caso o mesmo estiver vazio.
        //Todos os banco criado pelo método openOrCreateDatabase() possui uma tabela padrão "android_metadata"
        if(c.getCount() == 1){
            for(int i = 0; i < SCRIPT_DATABASE_CREATE.length; i++){
                db.execSQL(SCRIPT_DATABASE_CREATE[i]);
            }
            Log.i("BANCO_DADOS", "Criou tabelas do banco e as populou.");
        }

        c.close();
    }

    // Insere um novo registro
    public long inserir(String tabela, ContentValues valores) {
        long id = db.insert(tabela, null, valores);

        Log.i("BANCO_DADOS", "Cadastrou registro com o id [" + id + "]");
        return id;
    }

    // Atualiza registros
    public int atualizar(String tabela, ContentValues valores, String where) {
        int count = db.update(tabela, valores, where, null);

        Log.i("BANCO_DADOS", "Atualizou [" + count + "] registros");
        return count;
    }

    // Deleta registros
    public int deletar(String tabela, String where) {
        int count = db.delete(tabela, where, null);

        Log.i("BANCO_DADOS", "Deletou [" + count + "] registros");
        return count;
    }

    // Busca registros
    public Cursor buscar(String tabela, String colunas[], String where, String orderBy) {
        Cursor c;
        if(!where.equals(""))
            c = db.query(tabela, colunas, where, null, null, null, orderBy);
        else
            c = db.query(tabela, colunas, null, null, null, null, orderBy);

        Log.i("BANCO_DADOS", "Realizou uma busca e retornou [" + c.getCount() + "] registros.");
        return c;
    }

    // Abre conexão com o banco
    public void abrir(Context ctx) {
        // Abre o banco de dados já existente
        db = ctx.openOrCreateDatabase(NOME_BANCO, Context.MODE_PRIVATE, null);
        Log.i("BANCO_DADOS", "Abriu conexão com o banco.");
    }

    // Fecha o banco
    public void fechar() {
        // fecha o banco de dados
        if (db != null) {
            db.close();
            Log.i("BANCO_DADOS", "Fechou conexão com o Banco.");

            INSTANCE = null;
            System.gc();
        }
    }

    public int apagarBase(){
        //deleta todos os pontos coletados
        return deletar("Ponto","");
    }
}
