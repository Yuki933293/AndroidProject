package com.luxshare.base.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.annotation.IntDef;

import com.alibaba.android.arouter.utils.TextUtils;

import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 文件流保存数据
 */
public class FileUtils {
    private static final String TAG = "FileUtils";
    public static final int ASC = 1; // 正序
    public static final int DESC = 2; // 倒序

    @IntDef(flag = false, value = {ASC, DESC})
    //注解作用域参数、方法、成员变量
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    //仅仅在源码阶段有效
    @Retention(RetentionPolicy.SOURCE)
    public @interface Order {

    }

    public interface ReadFileCallback {
        void sucess(String content);

        void error(Exception e);
    }

    public interface WriteFileCallback {
        void sucess();

        void error(Exception e);
    }

    /**
     * 将数据写入文件(/sys/class/qcom-battery/usb_switch_to_charge)
     *
     * @param filePath 文件路径
     * @param data     字符串数据
     */
    public static void writeToFile(String data, String filePath, WriteFileCallback callback) {
        BufferedWriter writer = null;
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file, false);
            writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            fileOutputStream.write(data.getBytes());
            if (callback != null) {
                callback.sucess();
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.error(e);
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取数据，一次返回所有文本
     *
     * @param filePath 表示文件路径
     */
    public static String readFromFile(String filePath, ReadFileCallback callback) {
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream inputStream = null;
            BufferedReader reader = null;
            try {
                inputStream = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String str = null;
                StringBuffer stringBuffer = new StringBuffer();
                while ((str = reader.readLine()) != null) {
                    stringBuffer.append(str);
                }
                if (callback != null) {
                    callback.sucess(stringBuffer.toString());
                }
                return stringBuffer.toString();
            } catch (FileNotFoundException e) {
                if (callback != null) {
                    callback.error(e);
                }
            } catch (IOException e) {
                if (callback != null) {
                    callback.error(e);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.error(e);
                }
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 获取目录下所有文件(按时间倒序排序)
     *
     * @param path  目录
     * @param order ASC: 正序  DESC：倒序
     * @return 返回排序后的文件列表
     */
    public static List<File> listFileSortByModifyTime(String path, @Order int order) {
        List<File> list = getFiles(path);
        if (list != null && list.size() > 0) {
            Collections.sort(list, new Comparator<File>() {
                public int compare(File file, File newFile) {
                    if (file.lastModified() < newFile.lastModified()) {
                        return order == DESC ? 1 : -1;
                    } else if (file.lastModified() == newFile.lastModified()) {
                        return 0;
                    } else {
                        return order == DESC ? -1 : 1;
                    }
                }
            });
        }
        return list;
    }

    /**
     * 获取目录下所有文件（递归获取，包括子文件夹）
     *
     * @param dirPath 目录
     * @return 返回所有文件集合
     */
    public static List<File> getFiles(String dirPath) {
        List<File> files = new ArrayList();
        File realFile = new File(dirPath);
        if (realFile.isDirectory()) {
            File[] subFiles = realFile.listFiles();
            for (File file : subFiles) {
                if (file.isDirectory()) {
                    getFiles(file.getAbsolutePath());
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public static String getFromAssets(Context context, String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null) {
                Result += line;
            }
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 从文件读取内容
     * @param file
     * @return
     */
    public static String readFromFile(File file) {
        if ((file == null) || (!file.exists())) {
            return null;
        }
        FileInputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String str = null;
            StringBuffer content = new StringBuffer();
            while ((str = reader.readLine()) != null) {
                content.append(str);
            }
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 文件中写入字符串
     *
     * @param file
     * @param value
     */
    public static boolean write2File(File file, String value) {
        if ((file == null) || (!file.exists())) {
            return false;
        }
        try {
            FileOutputStream fout = new FileOutputStream(file);
            PrintWriter pWriter = new PrintWriter(fout);
            pWriter.println(value);
            pWriter.flush();
            pWriter.close();
            fout.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void write2File(String filePath, String content) {
        Log.i(TAG, "writeFile: ");
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            if (TextUtils.isEmpty(filePath)) {
                return;
            }
            Log.i(TAG, "writeFile: path:" + filePath);
            File file = new File(filePath);
            if (!FileUtils.checkAndCreatFile(file)) {
                Log.i(TAG, "writeFile: create fail");
                return;
            }
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(file);
                os.write(content.getBytes(StandardCharsets.UTF_8));
                Log.i(TAG, "write: over");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getSDFilesPath(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File external = context.getExternalFilesDir(null);
            if (external != null) {
                return external.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * @description 获取SD卡路径，不用在设置中这是默认存储位置
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            if (storageVolumeClazz == null) {
                return null;
            }
            Method getPath = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                getPath = storageVolumeClazz.getMethod("getPath");
            }
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                String path = null;
                android.os.storage.StorageVolume storageVolumeElement = (StorageVolume) Array.get(result, i);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    File file = storageVolumeElement.getDirectory();
                    if (file != null) {
                        path = file.getAbsolutePath();
                    }
                } else {
                    path = (String) getPath.invoke(storageVolumeElement);
                }
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                Log.i(TAG, "getStoragePath: path:" + path + ",removable:" + removable);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkAndCreatFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.isFile()) {
            return true;
        } else {
            try {
                boolean newFile = file.createNewFile();
                return newFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean checkAndCreatFile(String Filepath) {
        if (TextUtils.isEmpty(Filepath)) {
            return false;
        }
        File file = new File(Filepath);
        return checkAndCreatFile(file);
    }

    public static String getDiskCacheDir(Context context) {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
                || !Environment.isExternalStorageRemovable()) {
            if (context.getExternalCacheDir() != null) {
                return context.getExternalCacheDir().getPath();
            }
            return "";
        } else {
            if (context.getCacheDir() != null) {
                return context.getCacheDir().getPath();
            }
            return "";
        }
    }
}
