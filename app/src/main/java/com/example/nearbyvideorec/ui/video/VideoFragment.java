package com.example.nearbyvideorec.ui.video;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;


import static android.app.Activity.RESULT_OK;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class VideoFragment extends Fragment {


    private VideoViewModel videoViewModel;
    private SavedUIData savedUIData;

    private Context myc;

    private String space = " ";
    private String apostrofo = "\'";


    private File f;
    private FileOutputStream fos;
    private String fileNameTxt = "myListpaths.txt";

    private Button btn_merge;
    private Button btn_intent_files;

    private Uri folderUri;
    private ArrayList<String> paths_list = new ArrayList<String>();


    private static final int REQUEST_CODE_BY_INTENT_FILE_CHOOSER = 1234;

    private String generateNameOutputFile() {
        return "Merged_" + getTimeStampString() + ".mp4";
    }

    private String getDirectoryNameMoviesPathString() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                Environment.DIRECTORY_MOVIES + File.separator;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);


        savedUIData = SavedUIData.INSTANCE;
        btn_merge = (Button) root.findViewById(R.id.btn_merge);
        btn_intent_files = (Button) root.findViewById(R.id.btn_select_files);

        // aggiunta bottone merge più listener
        btn_merge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //genera il file txt
                generateFileTxT(fileNameTxt);

                runCommand("-f concat -safe 0 -i",
                        getDirectoryNameMoviesPathString() + fileNameTxt,
                        "-c copy",
                        getDirectoryNameMoviesPathString() + generateNameOutputFile()
                );
            }
        });
        //aggiunta bottone file chooser + listener
        btn_intent_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openMyFolder();
            }
        });
        return root;

    }


    //aprire intent file chooser
    public void openMyFolder() {
        Intent chooserfile = new Intent(Intent.ACTION_GET_CONTENT);

        folderUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Environment.DIRECTORY_MOVIES + File.separator);
        chooserfile.setDataAndType(folderUri, "video/mp4");

        chooserfile.addCategory(Intent.CATEGORY_OPENABLE);
        chooserfile = Intent.createChooser(chooserfile, "Open folder");
        startActivityForResult(chooserfile, REQUEST_CODE_BY_INTENT_FILE_CHOOSER);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_CODE_BY_INTENT_FILE_CHOOSER && resultCode == RESULT_OK && data != null) {
            Uri u = data.getData();
            System.out.println("URI" + u.toString()); // XIAOMI ANDROID 10 : content://com.mi.android.globalFileexplorer.myprovider/external_files/Movies/NOME_VIDEO_SELEZIONATO.MP4




            String p = u.getLastPathSegment(); //prende il path dall'uri
            if (Build.VERSION.SDK_INT >= 27)
                try {
                    p = getPathAfterOREO(requireContext(),u);
                }catch (Exception e){
                    e.printStackTrace();
                }

            System.out.println(p);
            System.out.println("PATH" + p);  //  XIAOMI ANDROID 10 : /storage/emulated/0/Movies/NOME_VIDEO_SELEZIONATO.mp4
            paths_list.add(p);
        }
    }

    // METODO WRAPPER DEL COMANDO
    public void runCommand(String prefix, String filepathInput, String middleOption, String filepathOutput) {

        //generate string command
        String cmd = prefix + space + filepathInput + space + middleOption + space + filepathOutput;
        //execute command
        int rc = FFmpeg.execute(cmd);

        switch (rc) {
            case RETURN_CODE_SUCCESS:
                if (myc == null) {
                    myc = requireContext();
                }
                //Toast.makeText(myc, "RESULTCODE " + rc + " DONE", Toast.LENGTH_SHORT).show();
                Toast.makeText(myc, "video completo generato", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution completed successfully.");
                //pulisco arraylist di path
                paths_list.clear();
                break;

            case RETURN_CODE_CANCEL:
                if (myc == null) {
                    myc = requireContext();
                }
                Toast.makeText(myc, "RESULTCODE" + rc + "RESULT CODE CANCEL", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution cancelled by user.");
                break;
            default:
                if (myc == null) {
                    myc = requireContext();
                }
                //Toast.makeText(myc, "RESULTCODE" + rc + "RESULT CODE FAILED", Toast.LENGTH_SHORT).show();
                Toast.makeText(myc, "Selezionare almeno un video", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                Config.printLastCommandOutput(Log.INFO);
                break;
        }

    }

    //myListpatth.txt
    public void generateFileTxT(String filename) {
        f = new File(getDirectoryNameMoviesPathString(), filename);

        try {
            fos = new FileOutputStream(f);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            /*

            example
             String s =  "file" + space + apostrofo + "/storage/emulated/0/Movies/video_20201219_0532.mp4" + apostrofo + "\n";

             */

            //SCRITTURA DEI PATH SU FILE DI TESTO
            StringBuilder s = new StringBuilder();
            for (String path : paths_list) {
                s.append("file").append(space).append(apostrofo).append(path).append(apostrofo).append("\n");
            }
            fos.write(s.toString().getBytes());


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String getTimeStampString() {
        //return new SimpleDateFormat("dd-MM-yy_hh-mm-ss", Locale.getDefault()).format(new Date());
        return new SimpleDateFormat("dd-MM-yy_hh-mm-ss", Locale.getDefault()).format(new Date());
    }


        //todo da testare su cell android 7 xk il metodo è per android kitkat in poi , ho cambiato in sdk 27 io.
        //METODO DA ANDROID 8
        private static String getPathAfterOREO(Context context, Uri uri) throws URISyntaxException {
            boolean needToCheckUri = Build.VERSION.SDK_INT >= 27;
            String selection = null;
            String[] selectionArgs = null;
            // Uri is different in versions after KITKAT (Android 4.4), we need to
            // deal with different Uris.
            if (needToCheckUri && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    uri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("image".equals(type)) {
                        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    selection = "_id=?";
                    selectionArgs = new String[]{ split[1] };
                }
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = { MediaStore.Images.Media.DATA };
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
            return null;
        }



        public static boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }


        public static boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }


        public static boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }



}
