package com.anysoftkeyboard.languagepack.icelandic;

import android.util.Log;

import org.apache.lucene.search.suggest.fst.FSTCompletion;
import org.apache.lucene.search.suggest.fst.FSTCompletionBuilder;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An autocompleter based on Lucene FSTCompletion. On initialization the FST is created from
 * provided bigram data file and kept in memory for lookup.
 */

public class Autocompleter {
    final static String TAG = "ASK_ICE_Autocompleter";
    final static List<Integer> BUCKETS = Arrays.asList(10, 50, 100, 500, 1000, 5000, 10000, 20000, 40000, 10000000);
    FSTCompletion completion;

    /**
    Initilaize the Autocompleter by reading bigram data from resources and create an in-memory
    FSTCompletion object for later lookup.
     */
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

    /**
     * Provide autocompletion suggestions for 'input', no more than 'maxNum' suggestions.
     * The suggestions from the FST can consist of one or two words (as one string), we
     * preform some comparisons to the input to return the correct word.
     * If the input ends with a space, we are doing next word prediction and return the
     * second word from each suggestion. For other inputs we compare the suggestions and make
     * sure we return the word starting with the input.
     * @param input the string to complete
     * @param maxNum max number of suggestion to return
     * @return an ordered map, with the most likely suggestion as the first item
     */
    public Map<String, Integer> autocomplete(String input, int maxNum) {
        Map<String, Integer> results = new LinkedHashMap<>();
        List<FSTCompletion.Completion> suggestions = completion.lookup(input, maxNum);
        for (FSTCompletion.Completion compl : suggestions) {
            String suggestion = extractSuggestion(compl, input, results);
            if (!suggestion.isEmpty())
                results.put(suggestion, compl.bucket);
        }
        return results;
    }

    @SuppressWarnings("StringSplitter")
    private String extractSuggestion(FSTCompletion.Completion compl, String input, Map<String, Integer> currentMap) {
        String suggestion = "";
        // FSTCompletion.Completion objects contains a utf8 representation of the suggestion
        // and an int field bucket. Higher bucket value means more likely.
        // The suggestion can be one or two words, we only want to return one.
        String[] suggArr = compl.utf8.utf8ToString().split(" ");
        // only one word in the suggestion
        if ((suggArr.length == 1) && !currentMap.containsKey(suggArr[0])) {
            suggestion = suggArr[0];
        }
        // two words in the suggestion, select the correct one, be sure the word is not already
        // in the map so that we do not override frequencies (bucket values)
        else if (suggArr.length == 2) {
            if (suggArr[0].startsWith(input) && input.length() < suggArr[0].length()) {
                if (!currentMap.containsKey(suggArr[0]))
                    suggestion = suggArr[0];
            }
            else if (!currentMap.containsKey(suggArr[1]))
                suggestion = suggArr[1];
        }
        return suggestion;
    }

    private List<String> readFile() {
        FileUtils fileUtils = new FileUtils();
        final List<String> fileContent = fileUtils.readLinesFromResourceFile("res/raw/uni_bigrams.tsv");
        return fileContent;
    }
}
