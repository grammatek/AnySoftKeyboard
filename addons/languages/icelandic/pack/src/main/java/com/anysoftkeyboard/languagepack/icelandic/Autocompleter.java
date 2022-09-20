package com.anysoftkeyboard.languagepack.icelandic;

import android.content.Context;

import com.anysoftkeyboard.languagepack.icelandic.pack.R;

import org.apache.lucene.search.suggest.fst.FSTCompletion;
import org.apache.lucene.search.suggest.fst.FSTCompletionBuilder;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Autocompleter {
    final static List<Integer> BUCKETS = Arrays.asList(10, 50, 100, 500, 1000, 5000, 10000, 20000, 40000, 10000000);
    FSTCompletion completion;

    Context mContext;

    public Autocompleter(Context context) throws IOException {
        mContext = context;
        List<String> freqdictContent = readFile();
        FSTCompletionBuilder FSTbuilder = new FSTCompletionBuilder();
        for (String line : freqdictContent) {
            String[] arr = line.split("\t");
            BytesRef term = new BytesRef(arr[0]);
            int freq = Integer.parseInt(arr[1]);
            int bucket = 0;
            for (int b : BUCKETS) {
                int ind = BUCKETS.indexOf(b);
                if (freq > b && freq < BUCKETS.get(ind + 1)) {
                    bucket = ind;
                    break;
                }
            }
            FSTbuilder.add(term, bucket);
        }
        completion = FSTbuilder.build();
    }

    public List<String> autocomplete(String input, int maxNum) {
        List<String> results = new ArrayList<>();
            List<FSTCompletion.Completion> suggestions = completion.lookup(input, maxNum);
            for (FSTCompletion.Completion compl : suggestions)
                results.add(compl.toString());

        return results;
    }



    private List<String> readFile() {
        final List<String> fileContent = FileUtils.readLinesFromResourceFile(this.mContext,
                R.raw.uni_bigrams);

        return fileContent;
    }

}
