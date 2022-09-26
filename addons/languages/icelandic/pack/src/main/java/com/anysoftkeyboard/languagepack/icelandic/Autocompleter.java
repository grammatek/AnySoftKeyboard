package com.anysoftkeyboard.languagepack.icelandic;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.lucene.search.suggest.InMemorySorter;
import org.apache.lucene.search.suggest.fst.FSTCompletion;
import org.apache.lucene.search.suggest.fst.FSTCompletionBuilder;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An autocompleter based on Lucene FSTCompletion. On initialization the FST is created from
 * provided bigram data file and kept in memory for lookup.
 */

public class Autocompleter {
    final static String TAG = "ASK_ICE_Autocompleter";
    final static String BIGRAM_FILE = "res/raw/bigrams_bucketed.tsv";
    // Max number of buckets for the automaton. Should be plenty, normal range would be 10-20 buckets.
    final static int MAX_BUCKETS = 100;
    FSTCompletion completion;

    /**
    Initilaize the Autocompleter by reading bigram data from resources and create an in-memory
    FSTCompletion object for later lookup.
     */
    public Autocompleter()  {
        this(new HashMap<String, Integer>());
    }

    public Autocompleter(Map<String, Integer> userBigrams) {
        try {
            List<String> freqdictContent = readFile();
            Map<String, Integer> finalContent = mergeLists(freqdictContent, userBigrams);
            completion = initCompletionFST(finalContent);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Provide autocompletion suggestions for 'input', no more than 'maxNum' suggestions.
     * The suggestions from the FST can consist of one or two words (as one string), we
     * perform some comparisons to the input to return the correct word.
     * If the input ends with a space, we are doing next word prediction and return the
     * second word from each suggestion. For other inputs we compare the suggestions and make
     * sure we return the word starting with the input.
     *
     * @param input the string to complete
     * @param maxNum max number of suggestion to return
     * @return an ordered map, with the most likely suggestion as the first item
     */
    public Map<String, Integer> autocomplete(String input, int maxNum) {
        Map<String, Integer> results = new LinkedHashMap<>();
        final List<FSTCompletion.Completion> suggestions = completion.lookup(input, maxNum);
        for (FSTCompletion.Completion compl : suggestions) {
            final String suggestion = extractSuggestion(compl, input, results);
            if (!suggestion.isEmpty())
                results.put(suggestion, compl.bucket);
        }
        return results;
    }

    public void addBigram(String bigram) {
        // need another storage for the bigrams!
        int bucket = completion.getBucket(bigram);
        // bigram is in the automaton, but not in the highest bucket
        if (bucket > 0 && bucket < completion.getBucketCount()) {
            bucket++;
        }
        // bigram is not in the automaton, let's put it in the middle
        else if (bucket <= 0){
            bucket = completion.getBucketCount()/2;
        }
    }

    @SuppressWarnings("StringSplitter")
    private String extractSuggestion(FSTCompletion.Completion compl, String input, Map<String, Integer> currentMap) {
        String suggestion = "";
        // FSTCompletion.Completion objects contains a utf8 representation of the suggestion
        // and an int field bucket. Higher bucket value means more likely.
        // The suggestion can be one or two words, we only want to return one.
        final String[] suggArr = compl.utf8.utf8ToString().split(" ");
        // only one word in the suggestion
        if ((suggArr.length == 1) && !currentMap.containsKey(suggArr[0])) {
            suggestion = suggArr[0];
        }
        // two words in the suggestion, select the correct one, be sure the word is not already
        // in the map so that we do not override frequencies (bucket values)
        else if (suggArr.length == 2) {
            if (suggArr[0].startsWith(input) && input.length() <= suggArr[0].length()) {
                if (!currentMap.containsKey(suggArr[0]))
                    suggestion = suggArr[0];
            }
            else if (!currentMap.containsKey(suggArr[1]))
                suggestion = suggArr[1];
        }
        return suggestion;
    }

    private FSTCompletion initCompletionFST(Map<String, Integer> bucketDict) throws IOException {
        FSTCompletionBuilder FSTbuilder = new FSTCompletionBuilder(
                MAX_BUCKETS, new InMemorySorter(Comparator.naturalOrder()), Integer.MAX_VALUE);
        for (String bigram : bucketDict.keySet()) {
            try {
                BytesRef term = new BytesRef(bigram);
                Integer bucket = bucketDict.get(bigram);
                bucket = bucket == null ? 1 : bucket;
                FSTbuilder.add(term, bucket);
            } catch (NumberFormatException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return FSTbuilder.build();
    }

    @SuppressWarnings("StringSplitter")
    /*
    Create a map from systemBigrams and, if userBigrams are not empty, merge the content of userBigrams
    with the systemBigrams. To do that we increase the bucket value of each systemBigram also found
    in userBigrams by the usageCount in the userBigrams map. At the same time, we remove each found
    bigram from the userBigrams and in case they are not all found in the systemBigrams, we
    add the remaining userBigrams at the end.
    This method then returns a map of bigrams and their bucket values for the FST initialization.
    If userBigrams is not empty this will be a mixture of user bigrams and system bigrams, otherwise
    only system bigrams.
     */
    private Map<String, Integer> mergeLists(@NonNull List<String> systemBigrams, @NonNull Map<String, Integer> userBigrams) {
        final Map<String, Integer> mergedBigrams = new HashMap<>();
        // create a map from the systemBigrams list and at the same time
        // compare and merge with the userBigrams map
        for (String line : systemBigrams) {
            String[] arr = line.split("\t");
            if (arr.length != 2)
                continue;
            int bucket = Integer.parseInt(arr[1]);
            if (!userBigrams.isEmpty() && userBigrams.containsKey(arr[0])) {
                Integer usageCount = userBigrams.get(arr[0]);
                usageCount = usageCount == null ? 0 : usageCount;
                if (bucket + usageCount < MAX_BUCKETS) {
                    bucket += usageCount;
                }
                else
                    bucket = MAX_BUCKETS - 1;
                mergedBigrams.put(arr[0], bucket);
                userBigrams.remove(arr[0]);
                Log.d(TAG, "========== Updating bigrams list: " + arr[0] + " from count " +
                        usageCount + " to count " + bucket);
            }
            else
                mergedBigrams.put(arr[0], bucket);
        }
        // not all userBigrams were found in the system bigrams list, add the remaining
        // userBigrams
        if (!userBigrams.isEmpty()) {
            mergedBigrams.putAll(userBigrams);
        }
        return mergedBigrams;
    }

    private List<String> readFile() {
        FileUtils fileUtils = new FileUtils();
        return fileUtils.readLinesFromResourceFile(BIGRAM_FILE);
    }
}
