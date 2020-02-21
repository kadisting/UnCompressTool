package com.xpp.unzip;


import com.github.junrar.Archive;
import com.github.junrar.UnrarCallback;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

public class RarUtils {

    public static String unrar(String rarFilePath, UnrarCallback callback) throws Exception {

      String savepath = rarFilePath.substring(0, rarFilePath.lastIndexOf(".")) + File.separator; //保存解压文件目录
        File dir = new File(savepath);
        if (dir.exists()) {
            BaseUtils.deleteDir(dir);
        }
        dir.mkdir(); //创建保存目录

        Archive archive = new Archive(new File(rarFilePath), callback);
        if(archive == null){
            throw new FileNotFoundException(rarFilePath + " NOT FOUND!");
        }
        if(archive.isEncrypted()){
            throw new Exception(rarFilePath + " IS ENCRYPTED!");
        }
        List<FileHeader> files =  archive.getFileHeaders();
        for (FileHeader fh : files) {
            if(fh.isEncrypted()){
                throw new Exception(rarFilePath + " IS ENCRYPTED!");
            }
            String fileName = fh.getFileNameW();
            if(fileName != null && fileName.trim().length() > 0){
                File saveFile = new File(dir,fileName);
                File parent =  saveFile.getParentFile();
                if(!parent.exists()){
                    parent.mkdirs();
                }
                if(!saveFile.exists()){
                    saveFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(saveFile);
                try {
                    archive.extractFile(fh, fos);
                } catch (RarException e) {
                    throw e;
                }finally{
                    try{
                        fos.flush();
                        fos.close();
                    }catch (Exception e){
                    }
                }
            }
        }
        return savepath;
    }
}
