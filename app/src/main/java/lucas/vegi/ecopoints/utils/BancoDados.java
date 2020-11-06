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
            "INSERT INTO Tipo (idTipo, nome) VALUES (1, 'Pilha, bateria e lâmpada');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (2, 'Óleo de Cozinha');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (3, 'Remédio');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (4, 'Óleo Lubrificante');",
            "INSERT INTO Tipo (idTipo, nome) VALUES (5, 'Eletroletrônico');",

            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (1, 'Massa supermercado', '-20.67461', '-44.06334');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (2, 'Supermercado Peg-Pag', '-20.66895', '-44.06571');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (3, 'Supermercado Nova Aliança', '-20.6628', '-44.06882');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (4, 'Loja de Informática - Info Rios', ' -20.669043', '-44.065701');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (5, 'E. E. Dom Rodolfo', '-20.679684', '-44.058974');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (6, 'E.E. Ribeiro de Oliveira', '-20.672572', '-44.06614');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (7, 'C.M. de Educação Infantil Geralda de Melo Vieira Resende', '-20.669926', '-44.071505');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (8, 'C.M. de Educação Infantil Entre Rios', '-20.668095', '-44.066134');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (9, 'E.E. Pedro Domingues', '-20.661574', '-44.068654');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (10, 'Posto Marzano', '-20.672544', '-44.065960');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (11, 'Posto Ale', '-20.664153', '-44.068155');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (12, 'Drogaria Sayonara', '-20.663206', '-44.068598');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (13, 'Farmácia Santa Maria', '-20.671759', '-44.065796');",
            "INSERT INTO Ecopoint (idEcopoint, nome, latitude, longitude) VALUES (14, 'UBS - Dr. Alcino Lázaro', '-20.679342', '-44.058056');",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,1);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,2);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (1,3);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,5);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,6);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,7);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,8);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (2,9);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,12);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,13);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (3,14);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (4,10);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (4,11);",
            "INSERT INTO Ecopoint_Tipo (idTipo, idEcopoint) VALUES (5,4);"};

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
