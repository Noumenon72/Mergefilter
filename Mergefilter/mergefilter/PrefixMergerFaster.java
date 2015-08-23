package mergefilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrefixMergerFaster {

    public final static Logger logger = LogManager.getLogger(PrefixMergerFaster.class.getName());

    private int mSourceLocationInResults;
    private int mSourceIndex;
    private int mPrefixIndex;
    private int mFirstMatchedSource;

    public int getFirstMatchedSource() {
        return mFirstMatchedSource;
    }

    public void setFirstMatchedSource(int firstMatchedSource) {
        if (firstMatchedSource < 0) {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "This variable represents a list index and should never be negative");
            logger.debug("\nException! negative firstMatchedSource");
            logger.debug("Results: " + mResults.toString());
            logger.debug("Sources " + mSources.toString());
            logger.debug("Binary search of results for empty string returns {}",
                    Collections.binarySearch(mResults, ""));
            throw iae;
        }
        this.mFirstMatchedSource = firstMatchedSource;
    }

    private int mLastPrefixWithMatch;
    private List<String> mSources;
    private List<String> mPrefixes;
    private List<String> mResults;

    private static int callcount = 0;

    public static void main(String[] args) {
    }

    /**
     * Merges a list of prefixes and words that start with those prefixes.
     * 
     * @param mSources
     *            must be sorted case-insensitively, no identical strings
     * @param mPrefixes
     *            must be sorted case-insensitively, no identical strings
     * @return sorted, merged list of words that (case-insensitively) start with prefixes, and unused prefixes.
     */
    public List<String> mergePrefixes(List<String> sources, List<String> prefixes) {

        if (prefixes.contains("")) {
            logger.warn("prefixes contains empty string, all sources will match");
        }

        mSourceIndex = 0;
        mPrefixIndex = 0;
        mResults = new ArrayList<String>();
        mSources = new ArrayList<String>(sources);
        mPrefixes = new ArrayList<String>(prefixes);

        mLastPrefixWithMatch = -1; // holds the index of the last prefix that
                                   // matched any source string.
        mFirstMatchedSource = -1; // when a prefix matches a source, the next
                                  // prefix also has to check if it
        // matched that source because if it doesn't, it may have to be inserted into the array

        // special cases
        if (sources.size() == 0)
            return prefixes;
        if (prefixes.size() == 0)
            return new ArrayList<String>();
        if (prefixes.get(0).equals("")) { // substring of every prefix
            mResults.addAll(sources);
            setFirstMatchedSource(0);
            mLastPrefixWithMatch = 0;
            testSuperstringPrefixes(0);
            return mResults;
        }

        checkSourceForMatches();

        logger.info("Called belongsInResults {} times", String.valueOf(callcount));
        return mResults;
    }

    /**
     * does the main work of matching after the inputs have been verified
     */
    private void checkSourceForMatches() {
        while (mPrefixIndex < mPrefixes.size()) {
            char[] sourceCharsLC = mSources.get(mSourceIndex).toLowerCase().toCharArray();
            char[] prefixCharsLC = mPrefixes.get(mPrefixIndex).toLowerCase().toCharArray();

            // compare the source to the prefix as long as it matches. If all
            // characters of prefix match, it's a match.
            boolean match = true;
            for (int i = 0; i < prefixCharsLC.length; i++) {
                if (i == sourceCharsLC.length) {
                    match = false; // all the characters in source matched, but
                                   // it's shorter than the whole prefix
                    mSourceIndex++;
                    break;
                }

                int compare = sourceCharsLC[i] - prefixCharsLC[i];
                if (compare < 0) {
                    // the source is lexicographically before the prefix. We can
                    // move on to the next.
                    mSourceIndex++;
                    match = false;
                    break;
                } else if (compare > 0) {
                    // this source (and hence all following ones) are
                    // lexicographically after the prefix. We're done
                    // checking this prefix.
                    if (mLastPrefixWithMatch == mPrefixIndex) {
                        // if it matched a source, we need to check if any
                        // upcoming prefixes might also match that source.
                        testSuperstringPrefixes(mFirstMatchedSource);
                    } else {
                        logger.debug("Adding prefix {} because it never matched a source", mPrefixes.get(mPrefixIndex));
                        mResults.add(mPrefixes.get(mPrefixIndex));
                        next_prefix();
                    }

                    match = false;
                    break;
                } else {
                    match = true; // so far... these two chars were the same at
                                  // least
                }
            }

            if (match) {
                process_match_without_adding(mPrefixIndex, mSourceIndex);
                // check if this was the first match on this prefix

                mResults.add(mSources.get(mSourceIndex));
                logger.debug("Added source {} because it matched prefix {}", mSources.get(mSourceIndex),
                        mPrefixes.get(mPrefixIndex));
                mSourceIndex++;
                // put source into list
                // make sure this prefix is not in list, unless it is there as a
                // source with the same text.
                // we could remove prefix if it was the last element, but it
                // might be a WORD equal to prefix
                // this should all be covered by never putting the prefix in the
                // list unless we're moving on with no match
                // set flag on this prefix that it matched: lastMatchedPrefix =
                // prefixes[prefixIndex].
            }
            // if we've hit the last of one but not the other, well, we know
            // there are no more matches, so we should just add
            // all the remaining words
            // if we've used all the prefixes, discard the remaining words and
            // quit
            appendRemainingPrefixes();
        }
    }

    private void appendRemainingPrefixes() {
        if (mSourceIndex >= mSources.size()) {
            // the last prefix may have matched, if it did add it
            if (mLastPrefixWithMatch != mPrefixIndex) {
                mResults.add(mPrefixes.get(mPrefixIndex));
            }
            next_prefix();
            // we've used all the sources, no more prefixes will match
            while (mPrefixIndex < mPrefixes.size()) {
                mResults.add(mPrefixes.get(mPrefixIndex));
                logger.debug("adding " + mPrefixes.get(mPrefixIndex) + " at end of source list");
                next_prefix();
            }
        }
    }

    /**
     * Checks the prefix at mPrefixIndex for following prefixes that are superstrings of it, puts them into mResults if
     * they belong, and leaves mPrefixIndex set to the position after the last superstring as if you had called
     * next_prefix(). Must be called after the sources that matched the original prefix have been added to mResults.
     * 
     * @param startInSource
     *            Position of the first source match for the original prefix.
     */
    private void testSuperstringPrefixes(int startInSource) {
        // if this prefix matched some words, and it is a substring of the next
        // ones,
        // the words it matched may have been alphabetically after this one.
        // we need to a) check if the next prefixes would belong earlier in the
        // results list than the
        // end [at the end, I think the normal checking will take care of them]
        // and b) if so,
        // make sure the prefix does not match any words in the results
        List<String> superPrefixes = getSuperstringPrefixes();
        for (String superPrefix : superPrefixes) {
            int wherePrefixBelongs = belongsInResults(superPrefix, startInSource);
            if (wherePrefixBelongs != -1) {
                if (wherePrefixBelongs == mResults.size()) {
                    mResults.add(superPrefix);
                } else {
                    mResults.add(wherePrefixBelongs, superPrefix);
                }
                logger.debug("added superprefix {} to the results", superPrefix);
            }
        }
        // check each superstring prefix against the results that the substring
        // prefix matched.

    }

    private List<String> getSuperstringPrefixes() {
        String prefixThatMatched = mPrefixes.get(mPrefixIndex);
        logger.debug("checking {} for superstring prefixes", prefixThatMatched);
        String prefixThatMatchedLC = prefixThatMatched.toLowerCase();
        List<String> supers = new ArrayList<String>();
        mPrefixIndex++;
        int counter = 0;
        while (mPrefixIndex < mPrefixes.size()) {
            counter++;
            String currentPrefix = mPrefixes.get(mPrefixIndex);
            if (currentPrefix.toLowerCase().startsWith(prefixThatMatchedLC)) {
                supers.add(currentPrefix);
                logger.debug("Added {} to the list of superstrings of {}", currentPrefix, prefixThatMatched);
            } else {
                // found all the superprefixes. Break out to leave mPrefixIndex
                // at the next (nonsuper) prefix.
                logger.debug("Left the mPrefixIndex pointer pointing to {}", mPrefixes.get(mPrefixIndex));
                break;
            }
            mPrefixIndex++;
        }
        logger.debug("checked {} prefixes for superstringness", String.valueOf(counter));
        logger.debug("Found {} prefixes superstrings of {}", supers.size(), prefixThatMatched);
        return supers;
    }

    /**
     * @param superPrefix
     * @param startInSource
     *            Position of the first source match for the original prefix.
     * @return the index of where in the results list the prefix ought to be inserted, or -1 if it doesn't belong. Be
     *         sure to create a null element at the end of the list before trying to add there.
     */
    private int belongsInResults(String superPrefix, int startInSource) {
        callcount++;
        logger.info("Calling belongsInResults for prefix {}", superPrefix);
        mSourceLocationInResults = Collections.binarySearch(mResults, mSources.get(startInSource),
                String.CASE_INSENSITIVE_ORDER);

        // error check
        if (mSourceLocationInResults < 0) {
            logger.warn("{} was not found in sources list: \n{}\nIt's probably not sorted.",
                    mResults.get(mSourceLocationInResults), mSources.toString());
            logger.debug("Binary search returned {} which indicates it thought  the word should be at position {}",
                    String.valueOf(mSourceLocationInResults), String.valueOf(-1 * (mSourceLocationInResults + 1)));
        }
        assert mSourceLocationInResults >= 0; // we added that word to the
                                              // collection and it was sorted

        while (mSourceLocationInResults < mResults.size()) {
            // if any result comes after superPrefix in the alphabet, we'll have to insert it
            int beforeOrAfter = mResults.get(mSourceLocationInResults).compareToIgnoreCase(superPrefix);
            if (beforeOrAfter == 0) {
                return -1; // this prefix is already in the results list, probably from being a source
            }
            if (beforeOrAfter > 0) {
                return findWherePrefixBelongs(superPrefix);
            }
            mSourceLocationInResults++;
        }

        // prefix didn't belong before any results, now does it belong at the
        // end?
        String finalElement = mResults.get(mResults.size() - 1);
        if (finalElement.equalsIgnoreCase(superPrefix)) {
            return -1;
        } else {
            return mResults.size();
        }
    }

    private int findWherePrefixBelongs(String prefix) {
        logger.info("calling findWherePrefixBelongs with prefix {}", prefix);
        // prefix belongs in the list before this result, as long as it doesn't
        // match any following words
        int wherePrefixBelongs = -1;
        wherePrefixBelongs = mSourceLocationInResults;
        String prefixLower = prefix.toLowerCase();
        while (mSourceLocationInResults < mResults.size()) {
            if (mResults.get(mSourceLocationInResults).toLowerCase().startsWith(prefixLower)) {
                // a word in the results list matches the prefix, meaning a word
                // in the source matched the prefix. Find it.
                int matchedSourceIndex = Collections.binarySearch(mSources, mResults.get(mSourceLocationInResults),
                        String.CASE_INSENSITIVE_ORDER);

                // error check
                if (matchedSourceIndex < 0) {
                    logger.warn("{} was not found in sources list: \n{}\nIt's probably not sorted.",
                            mResults.get(mSourceLocationInResults), mSources.toString());
                    logger.debug(
                            "Binary search returned {} which indicates it thought  the word should be at position {}",
                            String.valueOf(matchedSourceIndex), String.valueOf(-1 * (matchedSourceIndex + 1)));
                }
                assert matchedSourceIndex >= 0; // the word is in the collection
                                                // and it is sorted

                process_match_without_adding(mPrefixIndex, matchedSourceIndex);
                logger.info("Did not add prefix {} because it had a match in the list of results", prefix);
                return -1;

            } else {
                // If prefix DID belong in the list, we checked all the rest; if
                // it DIDN'T, then let's check the next result.
                mSourceLocationInResults++;
            }
        } // end while: prefix didn't belong before any of the words in the list
        return wherePrefixBelongs;
    }

    /**
     * @param prefixIndex
     * @param sourceIndex
     */
    private void process_match_without_adding(int prefixIndex, int sourceIndex) {
        if (mFirstMatchedSource == -1) {// if (mLastPrefixWithMatch !=
                                        // mPrefixIndex) {
            setFirstMatchedSource(sourceIndex);
        }
        mLastPrefixWithMatch = mPrefixIndex;
    }

    private void next_prefix() {
        mPrefixIndex++;
        mFirstMatchedSource = -1;
    }
}
