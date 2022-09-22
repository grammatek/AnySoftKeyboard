package com.anysoftkeyboard.nextword;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.anysoftkeyboard.languagepack.icelandic.Autocompleter;

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

    private final Autocompleter mAutocompleter;

    public NextWordDictionaryICE(Context context, String locale) {
        mStorage = new NextWordsStorage(context, locale);
        mReusableNextWordsIterable = new SimpleIterable(mReusableNextWordsResponse);
        mAutocompleter = new Autocompleter();
    }

    @Override
    @NonNull
    public Iterable<String> getNextWords(
            @NonNull String currentWord, int maxResults, final int minWordUsage) {
        maxResults = Math.min(MAX_NEXT_SUGGESTIONS, maxResults);

        // secondly, get a list of suggestions
        Map<String, Integer> suggestions = mAutocompleter.autocomplete(currentWord + " ", maxResults);
        List<String> suggList = suggestions.keySet().stream().collect(Collectors.toList());
        NextWordsContainer nextSet = new NextWordsContainer(currentWord, suggList);
        //NextWordsContainer nextSet = mNextWordMap.get(currentWord);
        int suggestionsCount = 0;
        if (nextSet != null) {
            for (NextWord nextWord : nextSet.getNextWordSuggestions()) {
                //if (nextWord.getUsedCount() < minWordUsage) continue;
                mReusableNextWordsResponse[suggestionsCount] = nextWord.nextWord;
                suggestionsCount++;
                if (suggestionsCount == maxResults) break;
            }
        }
        mReusableNextWordsIterable.setArraySize(suggestionsCount);
        return mReusableNextWordsIterable;
    }


    private static class SimpleIterable implements Iterable<String> {
        private final String[] mStrings;
        private int mLength;

        public SimpleIterable(String[] strings) {
            mStrings = strings;
            mLength = 0;
        }

        void setArraySize(int arraySize) {
            mLength = arraySize;
        }

        @Override
        public Iterator<String> iterator() {

            return new Iterator<String>() {
                private int mIndex = 0;

                @Override
                public boolean hasNext() {
                    return mIndex < mLength;
                }

                @Override
                public String next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return mStrings[mIndex++];
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supporting remove right now");
                }
            };
        }
    }
}
