package com.anysoftkeyboard.languagepack.icelandic;

import android.content.Context;
import android.util.Log;

import org.apache.lucene.search.suggest.fst.FSTCompletion;
import org.apache.lucene.search.suggest.fst.FSTCompletionBuilder;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Autocompleter {
    final static String TAG = "ASK_ICE_Autocompleter";
    final static List<Integer> BUCKETS = Arrays.asList(10, 50, 100, 500, 1000, 5000, 10000, 20000, 40000, 10000000);
    FSTCompletion completion;

    @SuppressWarnings("StringSplitter")
    public Autocompleter()  {
        try {
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
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public List<String> autocomplete(String input, int maxNum) {
        List<String> results = new ArrayList<>();
            List<FSTCompletion.Completion> suggestions = completion.lookup(input, maxNum);
            for (FSTCompletion.Completion compl : suggestions)
                results.add(compl.toString());

        return results;
    }

    private List<String> readFile() {
        FileUtils fileUtils = new FileUtils();
        final List<String> fileContent = fileUtils.readLinesFromResourceFile("res/raw/uni_bigrams.tsv");
        return fileContent;
    }

}
