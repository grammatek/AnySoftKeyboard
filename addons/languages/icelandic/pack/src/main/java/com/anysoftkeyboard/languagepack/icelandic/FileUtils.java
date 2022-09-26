package com.anysoftkeyboard.languagepack.icelandic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    private static final String TAG = "ASK_ICE_FileUtils";

    /**
     * Reads a resource file with the given resID, e.g. R.raw.uni_bigrams.tsv
     *
     * Returns the file content as a list of strings.
     *
     * @return a list of strings representing file content
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    public List<String> readLinesFromResourceFile(String filename) {
        List<String> fileContent = new ArrayList<>();
        String line = "";
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            if (is != null) {
                while ((line = reader.readLine()) != null) {
                    fileContent.add(line);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileContent;
    }
}
