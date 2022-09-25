package com.anysoftkeyboard.nextword;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.anysoftkeyboard.languagepack.icelandic.Autocompleter;

/**
 * This class implements a custom next word suggester, using the Lucene FST autocompleter
 * in the Icelandic language pack.
 */
public class NextWordDictionaryICE extends NextWordDictionary {

    private static final String TAG = "NextWordDictionaryICE";

    private static final int MAX_NEXT_SUGGESTIONS = 8;
    @SuppressWarnings("unused")
    private final NextWordsStorage mStorage;
    @SuppressWarnings("unused")
    private final SimpleIterable mReusableNextWordsIterable;
    private final String[] mReusableNextWordsResponse = new String[MAX_NEXT_SUGGESTIONS];

    @SuppressWarnings("unused")
    private final ArrayMap<String, NextWordsContainer> mNextWordMap = new ArrayMap<>();

    private Autocompleter mAutocompleter;

    public NextWordDictionaryICE(Context context, String locale) {
        mStorage = new NextWordsStorage(context, locale);
        mReusableNextWordsIterable = new SimpleIterable(mReusableNextWordsResponse);
    }

    @Override
    @NonNull
    public Iterable<String> getNextWords(
            @NonNull String currentWord, int maxResults, final int minWordUsage) {
        maxResults = Math.min(MAX_NEXT_SUGGESTIONS, maxResults);

        Map<String, Integer> suggestions = mAutocompleter.autocomplete(currentWord + " ", maxResults);
        List<String> suggList = suggestions.keySet().stream().collect(Collectors.toList());
        NextWordsContainer nextSet = new NextWordsContainer(currentWord, suggList);
        int suggestionsCount = 0;
        if (nextSet != null) {
            for (NextWord nextWord : nextSet.getNextWordSuggestions()) {
                mReusableNextWordsResponse[suggestionsCount] = nextWord.nextWord;
                suggestionsCount++;
                if (suggestionsCount == maxResults) break;
            }
        }
        mReusableNextWordsIterable.setArraySize(suggestionsCount);
        return mReusableNextWordsIterable;
    }

    // we need to override this when we've implemented the correct storage
    // for the bigrams
    @Override
    public void close() {
        // get bigram dict from autocompleter
        // merge with nextwordmap
        // store in format for Lucene
        mStorage.storeNextWords(mNextWordMap.values());

        // means: we have to have the nextwordfile as a parameter to the
        // autocompleter. Merge with the bigram-dict in the icelandic pack
        // use a flag to label bigrams that have been incremented by usage, we
        // don't want to loose them when the bigram-dict in the lanaguage pack is updated
    }

    @Override
    public void load() {
        for (NextWordsContainer container : mStorage.loadStoredNextWords()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Loaded " + container);
            mNextWordMap.put(container.word, container);
        }
        mAutocompleter = new Autocompleter(extractNextWordList());
    }

    private Map<String, Integer> extractNextWordList() {
        Map<String, Integer> nextWordMap = new HashMap<>();
        for (String word : mNextWordMap.keySet()) {
            NextWordsContainer currentContainer = mNextWordMap.get(word);
            if (null == currentContainer)
                continue;
            for (NextWord nextWord : currentContainer.getNextWordSuggestions()) {
                nextWordMap.put(String.format("%s %s", word, nextWord.nextWord), nextWord.getUsedCount());
            }
        }
        return nextWordMap;
    }
}
