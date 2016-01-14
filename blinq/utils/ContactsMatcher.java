package com.blinq.utils;

import com.blinq.models.SearchResult;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactsMatcher {

    /**
     * The target percentage.
     */
    private static final double THRESHOLD = 0.45;

    /**
     * Find the Levenshtein distance between string and each element on the
     * search results and return the matched results.
     */
    public static List<SearchResult> getSuggestedContacts(
            List<SearchResult> searchResults, List<String> queries) {

        List<SearchResult> suggestedSearchResults = new ArrayList<SearchResult>();

        for (SearchResult searchResult : searchResults) {

            for (String query : queries) {

                double rank = similarity(query, searchResult.getContact().getName());
                searchResult.setRank(rank);

                // Check if the result's rank greater than defined threshould.
                if (searchResult.getRank() > THRESHOLD) {
                    suggestedSearchResults.add(searchResult);
                    break;
                }
            }
        }

        sortResults(suggestedSearchResults);

        return suggestedSearchResults;

    }

    /**
     * Get similarity between two string using getLevenshteinDistance.
     */
    public static double similarity(String s1, String s2) {
        if (s1.length() < s2.length()) { // s1 should always be bigger
            String swap = s1;
            s1 = s2;
            s2 = swap;
        }
        int bigLen = s1.length();
        if (bigLen == 0) {
            return 1.0; /* both strings are zero length */
        }
        int distance = StringUtils.getLevenshteinDistance(s1.toLowerCase()
                .trim(), s2.toLowerCase().trim());

        return (bigLen - distance) / (double) bigLen;
    }

    /**
     * Sort results.
     *
     * @param searchResults
     */
    private static void sortResults(List<SearchResult> searchResults) {

        Collections.sort(searchResults, new Comparator<SearchResult>() {

            @Override
            public int compare(SearchResult item1, SearchResult item2) {
                return item2.getRank().compareTo(item1.getRank());
            }

        });
    }

}
