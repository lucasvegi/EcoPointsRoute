package lucas.vegi.coletapp.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import  java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import  org.apache.poi.hssf.usermodel.HSSFSheet;
import  org.apache.poi.hssf.usermodel.HSSFWorkbook;
import  org.apache.poi.hssf.usermodel.HSSFRow;

/**
 * Created by Lucas on 25/09/2015.
 */
public class ExportExcel {

    //private static ProgressDialog PD;
    //private static Context contexto;
    //public static Handler handler = new Handler();

    public static Cursor buscaDados(Context ctx){
        BancoDados bd = BancoDados.getINSTANCE(ctx);
        Cursor c = bd.buscar("Ponto", new String[]{"idPonto","nome", "latitude", "longitude","precisao", "dt_coleta", "hr_coleta"}, "", "");
        return c;
    }

    public static void exportar(Context ctx){
        try {

            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ColetAPP/";
            File newdir = new File(dir);
            newdir.mkdirs();

            String horaExport = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US).format(new Date());
            String filename = dir + "exportColetAPP_" + horaExport + ".xls" ;
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("FirstSheet");

            HSSFRow rowhead = sheet.createRow((short)0);
            rowhead.createCell(0).setCellValue("ID");
            rowhead.createCell(1).setCellValue("LOCAL");
            rowhead.createCell(2).setCellValue("LATITUDE");
            rowhead.createCell(3).setCellValue("LONGITUDE");
            rowhead.createCell(4).setCellValue("PRECISAO");
            rowhead.createCell(5).setCellValue("DATA");
            rowhead.createCell(6).setCellValue("HORA");

            //BUSCO TODAS AS MARCAÇÕES
            Cursor c = buscaDados(ctx);

            if (c.getCount() > 0) {
                int linha = 1;
                while (c.moveToNext()) {
                    int indexID = c.getColumnIndex("idPonto");
                    int indexLocal = c.getColumnIndex("nome");
                    int indexLatitude = c.getColumnIndex("latitude");
                    int indexLongitude = c.getColumnIndex("longitude");
                    int indexPrecisao = c.getColumnIndex("precisao");
                    int indexDtColeta = c.getColumnIndex("dt_coleta");
                    int indexHrColeta = c.getColumnIndex("hr_coleta");

                    int idPonto = c.getInt(indexID);
                    String latitude = c.getString(indexLatitude);
                    String longitude = c.getString(indexLongitude);
                    String precisao = c.getString(indexPrecisao);
                    String titulo = c.getString(indexLocal);
                    String dtColeta = c.getString(indexDtColeta);
                    String hrColeta = c.getString(indexHrColeta);

                    HSSFRow row = sheet.createRow((short)linha);
                    row.createCell(0).setCellValue(idPonto+"");
                    row.createCell(1).setCellValue(titulo);
                    row.createCell(2).setCellValue(latitude);
                    row.createCell(3).setCellValue(longitude);
                    row.createCell(4).setCellValue(precisao);
                    row.createCell(5).setCellValue(dtColeta);
                    row.createCell(6).setCellValue(hrColeta);

                    linha++;
                }

                FileOutputStream fileOut = new FileOutputStream(filename);
                workbook.write(fileOut);
                fileOut.close();
                Log.i("XLS", "XLS criado com sucesso");
                Toast.makeText(ctx,"Dados exportados para " + filename, Toast.LENGTH_LONG).show();

                //SE ESTIVER CONECTADO, TENTA MANDAR EMAIL COM ANEXO
                if(estaConectado(ctx)) {
                    sendEmailWithAttachment(ctx,
                            "",
                            "Dados exportados pelo aplicativo coletAPP",
                            "Os dados coletados pelo aplicativo estão na planilha anexada à este e-mail.",
                            filename);
                }
            }else{
                Toast.makeText(ctx,"A base não possui dados para exportar", Toast.LENGTH_LONG).show();
            }
            c.close();
        } catch ( Exception e ) {
            Log.i("ERRO", "Erro ao tentar criar XLS " + e.getMessage());
        }
    }


    public static  void sendEmailWithAttachment(Context ctx, String to,String subject, String message, String fileAndLocation)
    {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("application/excel");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{to});

        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,  subject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);


        File file = new File(fileAndLocation);
        //  File file = getFileStreamPath();
        if (file.exists())
        {
            Log.v("EMAIL", "Email file_exists!" );
        }
        else
        {
            Log.v("EMAIL", "Email file does not exist!" );
        }


        Log.v("EMAIL", "SEND EMAIL FileUri=" + Uri.parse("file://" + fileAndLocation));
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+  fileAndLocation));

        ctx.startActivity(Intent.createChooser(emailIntent, "Send mail...").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static boolean estaConectado(Context ctx) {
        ConnectivityManager conMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo i = conMgr.getActiveNetworkInfo();

        if (i == null) {
            Log.v("CONEXÃO", "NULA" );
            return false;
        }
        if (!i.isConnected()) {
            Log.v("CONEXÃO", "NÃO CONECTADO" );
            return false;
        }
        if (!i.isAvailable()) {
            Log.v("CONEXÃO", "NÃO DISPONÍVEL" );
            return false;
        }

        Log.v("CONEXÃO", "CONECTADO" );
        return true;
    }

}
