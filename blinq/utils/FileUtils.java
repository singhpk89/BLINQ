package com.blinq.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Environment;

import com.blinq.provider.DatabaseHelper;
import com.blinq.provider.HeadboxDatabaseHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Handle functionalities related to files management.
 *
 * @author Johan Hansson.
 */
public class FileUtils {

    private final static String TAG = FileUtils.class.getSimpleName();

    // Folders names
    public static final String HEADBOX_FOLDER_NAME = "Headbox";
    // Files names
    public static final String LOG_FILE_NAME = "blinq_log.txt";
    public static final String COMPRESSED_FILE_NAME = "blinq_log.zip";

    // Paths
    public static final String HEADBOX_FOLDER_PATH = Environment
            .getExternalStorageDirectory().getPath()
            + File.separator
            + FileUtils.HEADBOX_FOLDER_NAME;

    public static final String COMPRESSED_FILE_PATH = FileUtils.HEADBOX_FOLDER_PATH
            + File.separator + FileUtils.COMPRESSED_FILE_NAME;

    public static final String COMPRESSED_FOLDER_PATH = FileUtils.HEADBOX_FOLDER_PATH
            + "/Compressed";

    public static final String DATA_FOLDER_PATH = FileUtils.HEADBOX_FOLDER_PATH
            + "/Data";

    public static final int BUFFER_SIZE = 2048;

    /**
     * Copy specific file to new destination.
     *
     * @param source      path to file to be copied.
     * @param destination path to target destination.
     * @param context     application context.
     * @return true if the copy done successfully, false if error occur while
     * copying.
     */
    public static boolean copyFile(String source, String destination,
                                   Context context) {

        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;

        try {

            fileInputStream = new FileInputStream(source);
            fileOutputStream = new FileOutputStream(destination);

            // Start copying
            while (true) {

                int index = fileInputStream.read();

                if (index != -1) {
                    fileOutputStream.write(index);
                } else {
                    break;
                }
            }

            fileOutputStream.flush();

            return true;

        } catch (Exception e) {

            Log.i(TAG, "Error in opening streams " + e.getMessage());

        } finally {

            // Close Input & Output streams.
            try {

                fileOutputStream.close();
                fileInputStream.close();

            } catch (IOException ioe) {

                Log.i(TAG, "Error in closing streams " + ioe.getMessage());
            }
        }

        return false;

    }

    /**
     * Create folder in external storage.
     *
     * @param path folder path to be created.
     * @return true if folder has been created successfully, false if not.
     */
    public static boolean createFolder(String path) {

        File folder = new File(path);

        boolean success = true;

        if (!folder.exists()) {

            success = folder.mkdir();
        }

        return success;
    }

    /**
     * Write data to specific file.
     *
     * @param fileName file to write on.
     * @param data     data to be written.
     * @param context  application context.
     */
    public static void writeToFile(String fileName, String data, Context context) {

        try {

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    context.openFileOutput(fileName, Context.MODE_PRIVATE));

            outputStreamWriter.write(data);
            outputStreamWriter.close();

        } catch (IOException e) {

            Log.e(TAG, "Failed in writing on a file : " + e.toString());
        }

    }

    /**
     * Get list of URIs for the contents of the given folder.
     *
     * @param folderName name of the folder.
     * @return array list of URIs for folder contents.
     */
    public static ArrayList<Uri> getUrisOfFolderContents(String folderName) {

        ArrayList<Uri> uris = new ArrayList<Uri>();

        File[] filesInTheDirectory = getListOfFilesInTheFolder(folderName);

        if (filesInTheDirectory != null) {

            // Move over folder files and get URI for each of them.
            for (File file : filesInTheDirectory) {
                uris.add(getFileURI(file));
            }

        } else {

            Log.e(TAG, "Error listing files in the directory " + folderName);
        }

        return uris;

    }

    /**
     * Get URI for the given file.
     *
     * @param file file to get URI for.
     * @return file URI.
     */
    public static Uri getFileURI(File file) {

        return Uri.fromFile(file);
    }

    /**
     * Get list of files in the given folder.
     *
     * @param folderName folder to get files from.
     * @return list of files in the folder.
     */
    public static File[] getListOfFilesInTheFolder(String folderName) {

        File root = new File(folderName);

        File[] files = root.listFiles();

        return files;
    }

    /**
     * Compress list of files into into ZIP file.
     *
     * @param files   list of files to be compressed.
     * @param zipFile ZIP file.
     */
    public static void zipFiles(String[] files, String zipFile) {

        try {

            BufferedInputStream bufferedInputStream = null;
            FileOutputStream outputStream = new FileOutputStream(zipFile);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    outputStream));

            byte data[] = new byte[BUFFER_SIZE];

            for (int index = 0; index < files.length; index++) {

                FileInputStream fileInputStream = new FileInputStream(
                        files[index]);
                bufferedInputStream = new BufferedInputStream(fileInputStream,
                        BUFFER_SIZE);
                ZipEntry zipEntry = new ZipEntry(
                        files[index].substring(files[index].lastIndexOf("/") + 1));

                out.putNextEntry(zipEntry);

                int byteCount;
                while ((byteCount = bufferedInputStream.read(data, 0,
                        BUFFER_SIZE)) != -1) {
                    out.write(data, 0, byteCount);
                }

                bufferedInputStream.close();
            }

            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Create needed folders, Save logs, Copy database & Encrypt db & compress them in order
     * to be sent by email.
     *
     * @param activity calling activity
     */
    public static void prepareDatabaseLogToSend(Activity activity, boolean sendDatabase) {

        List<String> filesToCompress = new ArrayList<String>();

        if (sendDatabase) {

            String databasePath = activity.getDatabasePath(
                    HeadboxDatabaseHelper.DATABASE_NAME).getAbsolutePath();

            String destinationPath = activity.getDatabasePath(
                    DatabaseHelper.MODIFIED_DATABASE_NAME).getAbsolutePath();

            boolean copied = copyFile(databasePath, destinationPath, activity);

            if (copied) {
                Log.d(TAG, "Encrypted has been Successfully Copied...");
            }

            boolean isDebuggable = (0 != (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
            if (!isDebuggable) {
                DatabaseHelper.getDBHelper(activity).encryptDB();
            }

            filesToCompress.add(destinationPath);
        }

        if (FileUtils.createFolder(FileUtils.HEADBOX_FOLDER_PATH)) {

            String logsPath = activity.getApplicationContext().getFilesDir()
                    .getPath()
                    + File.separator + FileUtils.LOG_FILE_NAME;

            filesToCompress.add(logsPath);

            // Compress application's data files.
            FileUtils.zipFiles(filesToCompress.toArray(new String[filesToCompress.size()]), FileUtils.COMPRESSED_FILE_PATH);

        } else {

            Log.e(TAG, "Failed to create application's data folders");
        }
    }

    /**
     * Delete given directory with it's contents.
     *
     * @param dir folder.
     * @return true if the folder deleted successfully, false if not.
     */
    public static boolean deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDirectory(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }
}
