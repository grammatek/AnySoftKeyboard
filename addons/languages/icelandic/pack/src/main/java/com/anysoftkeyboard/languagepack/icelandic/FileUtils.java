package com.anysoftkeyboard.languagepack.icelandic;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    /**
     * Reads a resource file with the given resID, e.g. R.raw.uni_bigrams.tsv
     *
     * Returns the file content as a list of strings.
     *
     * @param context
     * @param resID the resource id
     * @return a list of strings representing file content
     */
    public static List<String> readLinesFromResourceFile(Context context, int resID) {
        Resources res = context.getResources();
        List<String> fileContent = new ArrayList<>();
        String line = "";
        try {
            InputStream is = res.openRawResource(resID);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            if (is != null) {
                while ((line = reader.readLine()) != null) {
                    fileContent.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileContent;
    }
}
