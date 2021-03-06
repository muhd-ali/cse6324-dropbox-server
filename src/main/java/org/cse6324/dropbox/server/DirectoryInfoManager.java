package org.cse6324.dropbox.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.cse6324.dropbox.common.FileInfo;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

/**
 * DirectoryInfoManager
 */
public class DirectoryInfoManager {
    private Path directory;

    private static class MetaDataManager {
        static private String getDirectoryInfoPathInUserDirectory() {
            return ".directoryInfo";
        }
    
        static private Path getExistingFilesDirectoryInfoPathInUserDirectory(Path userPath) {
            return Paths.get(userPath.toString(), getDirectoryInfoPathInUserDirectory(), "existing.json");
        }
    
        static private Path getDeletedFilesDirectoryInfoPathInUserDirectory(Path userPath) {
            return Paths.get(userPath.toString(), getDirectoryInfoPathInUserDirectory(), "deleted.json");
        }

        static private FileInfo[] readFileInfo(Path infoFile) {
            FileInfo[] fileInfoArray = new FileInfo[0];
            try {
                String content = FileUtils.readFileToString(infoFile.toFile());
                JSONArray fileInfos = new JSONArray(content);
                fileInfoArray = new FileInfo[fileInfos.length()];
                for (int i = 0; i < fileInfoArray.length; i++) {
                    fileInfoArray[i] = new FileInfo(fileInfos.getJSONObject(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return fileInfoArray;
        }
    
        static FileInfo[] readExisitngFileInfoFor(Path userPath) {
            return readFileInfo(getExistingFilesDirectoryInfoPathInUserDirectory(userPath));
        }
    
        static FileInfo[] readDeletedFileInfoFor(Path userPath) {
            return readFileInfo(getDeletedFilesDirectoryInfoPathInUserDirectory(userPath));
        }

        private static int indexOf(FileInfo fileInfo, JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                FileInfo curr = new FileInfo(array.getJSONObject(i));
                if (curr.hasSamePathAs(fileInfo)) {
                    return i;
                }
            }
            return -1;
        }

        static private boolean addToFilesInfo(FileInfo fileInfo, Path infoFile) {
            try {
                infoFile.toFile().getParentFile().mkdirs();
                infoFile.toFile().createNewFile();
                String content = FileUtils.readFileToString(infoFile.toFile());
                if (content.equals("")) content = "[]";
                JSONArray fileInfos = new JSONArray(content);
                int fileInfoIndex = indexOf(fileInfo, fileInfos);
                if (fileInfoIndex < 0) {
                    fileInfos.put(fileInfo.json());
                    FileUtils.writeStringToFile(infoFile.toFile(), fileInfos.toString());
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    
        static private boolean removeFromFilesInfo(FileInfo fileInfo, Path infoFile) {
            try {
                String content = FileUtils.readFileToString(infoFile.toFile());
                JSONArray fileInfos = new JSONArray(content);
                int fileInfoIndex = indexOf(fileInfo, fileInfos);
                if (fileInfoIndex > -1) {
                    fileInfos.remove(fileInfoIndex);
                    FileUtils.writeStringToFile(infoFile.toFile(), fileInfos.toString());
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    
        static void saveFileInfoToExisting(Path userPath, Path filePath) {
            FileInfo fileInfo = new FileInfo(filePath, userPath);
            addToFilesInfo(fileInfo, getExistingFilesDirectoryInfoPathInUserDirectory(userPath));
            removeFromFilesInfo(fileInfo, getDeletedFilesDirectoryInfoPathInUserDirectory(userPath));
        }
    
        static void saveFileInfoToDeleted(Path userPath, Path filePath) {
            FileInfo fileInfo = new FileInfo(filePath, userPath);
            addToFilesInfo(fileInfo, getDeletedFilesDirectoryInfoPathInUserDirectory(userPath));
            removeFromFilesInfo(fileInfo, getExistingFilesDirectoryInfoPathInUserDirectory(userPath));
        }
    
    }


    DirectoryInfoManager(String userDataDirectoryPath) {
        File dir = Paths.get(".", userDataDirectoryPath).toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }
        directory = dir.toPath();
    }

    FileInfo[] readExisitngFileInfoFor(String userID) {
        Path userPath = directory.resolve(userID);
        return MetaDataManager.readExisitngFileInfoFor(userPath);
    }

    FileInfo[] readDeletedFileInfoFor(String userID) {
        Path userPath = directory.resolve(userID);
        return MetaDataManager.readDeletedFileInfoFor(userPath);
    }

    boolean saveUserFile(String userID, MultipartFile file, String filepathString) {
        try {
            byte[] bytes = file.getBytes();
            Path userPath = directory.resolve(userID);
            Path filePath = userPath.resolve(filepathString);
            filePath.getParent().toFile().mkdirs();
            Files.write(filePath, bytes);
            MetaDataManager.saveFileInfoToExisting(userPath, filePath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    Path fullPathForUser(String userID, String filepathString) {
        return directory.resolve(userID).resolve(filepathString);
    }

    boolean deleteUserFile(String userID, String filepathString) {
        Path filePath = directory.resolve(userID).resolve(filepathString);
        Path userPath = directory.resolve(userID);
        File file = filePath.toFile();
        if (file.exists()) {
            MetaDataManager.saveFileInfoToDeleted(userPath, filePath);
            return file.delete();
        }
        return false;
    }

    static DirectoryInfoManager shared = new DirectoryInfoManager("userdata");
}